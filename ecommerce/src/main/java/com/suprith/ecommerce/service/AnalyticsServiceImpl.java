package com.suprith.ecommerce.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.suprith.ecommerce.dto.AdminUserResponse;
import com.suprith.ecommerce.dto.DashboardStatsResponse;
import com.suprith.ecommerce.dto.MonthlyRevenuePoint;
import com.suprith.ecommerce.dto.SalesReportResponse;
import com.suprith.ecommerce.dto.TopSellingProductResponse;
import com.suprith.ecommerce.entity.Order;
import com.suprith.ecommerce.entity.OrderItem;
import com.suprith.ecommerce.entity.User;
import com.suprith.ecommerce.enums.OrderStatus;
import com.suprith.ecommerce.mapper.OrderMapper;
import com.suprith.ecommerce.mapper.ProductMapper;
import com.suprith.ecommerce.repository.CategoryRepository;
import com.suprith.ecommerce.repository.OrderItemRepository;
import com.suprith.ecommerce.repository.OrderRepository;
import com.suprith.ecommerce.repository.ProductRepository;
import com.suprith.ecommerce.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsServiceImpl implements AnalyticsService {

    private static final DateTimeFormatter MONTH_KEY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final int REVENUE_CHART_MONTHS = 6;
    private static final int TOP_PRODUCTS_LIMIT = 5;
    private static final int RECENT_ITEMS_LIMIT = 5;
    private static final int LOW_STOCK_CEILING = 50;

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final OrderMapper orderMapper;
    private final ProductMapper productMapper;

    @Override
    public DashboardStatsResponse getDashboardStats() {
        long pendingOrders = countInPipeline();

        return DashboardStatsResponse.builder()
                .totalRevenue(orderRepository.sumRevenue(OrderStatus.CANCELLED))
                .totalOrders(orderRepository.count())
                .totalUsers(userRepository.count())
                .totalProducts(productRepository.count())
                .totalCategories(categoryRepository.count())
                .pendingOrders(pendingOrders)
                .lowStockCount(productRepository.countByStockLessThanEqualAndStockGreaterThanAndActiveTrue(LOW_STOCK_CEILING, 0))
                .outOfStockCount(productRepository.countByStockAndActiveTrue(0))
                .revenueByMonth(buildRevenueByMonth())
                .orderStatusBreakdown(buildStatusBreakdown())
                .topSellingProducts(buildTopSellingProducts(TOP_PRODUCTS_LIMIT))
                .recentOrders(orderRepository
                        .findAll(PageRequest.of(0, RECENT_ITEMS_LIMIT, Sort.by("placedAt").descending()))
                        .map(orderMapper::toSummary)
                        .getContent())
                .recentUsers(userRepository
                        .findAll(PageRequest.of(0, RECENT_ITEMS_LIMIT, Sort.by("createdAt").descending()))
                        .map(this::toAdminUserResponse)
                        .getContent())
                .recentProducts(productRepository
                        .findAll(PageRequest.of(0, RECENT_ITEMS_LIMIT, Sort.by("createdAt").descending()))
                        .map(productMapper::toSummary)
                        .getContent())
                .build();
    }

    @Override
    public SalesReportResponse getSalesReport(LocalDate fromDate, LocalDate toDate) {
        LocalDateTime from = fromDate.atStartOfDay();
        LocalDateTime to = toDate.atTime(23, 59, 59);

        List<Order> orders = orderRepository.findByPlacedAtBetweenAndStatusNot(from, to, OrderStatus.CANCELLED);

        BigDecimal totalRevenue = orders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        long totalOrders = orders.size();
        BigDecimal averageOrderValue = totalOrders > 0
                ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Map<Long, TopSellingProductResponse> byProduct = new LinkedHashMap<>();
        for (Order order : orders) {
            for (OrderItem item : order.getItems()) {
                if (item.getProduct() == null) {
                    continue;
                }
                Long productId = item.getProduct().getId();
                TopSellingProductResponse existing = byProduct.get(productId);
                long newTotal = (existing != null ? existing.getTotalSold() : 0) + item.getQuantity();
                byProduct.put(productId, TopSellingProductResponse.builder()
                        .productId(productId)
                        .productName(item.getProductName())
                        .totalSold(newTotal)
                        .build());
            }
        }

        List<TopSellingProductResponse> topProducts = byProduct.values().stream()
                .sorted(Comparator.comparingLong(TopSellingProductResponse::getTotalSold).reversed())
                .limit(TOP_PRODUCTS_LIMIT)
                .toList();

        return SalesReportResponse.builder()
                .fromDate(fromDate)
                .toDate(toDate)
                .totalRevenue(totalRevenue)
                .totalOrders(totalOrders)
                .averageOrderValue(averageOrderValue)
                .topSellingProducts(topProducts)
                .build();
    }

    private long countInPipeline() {
        return orderRepository.countByStatus(OrderStatus.PLACED)
                + orderRepository.countByStatus(OrderStatus.CONFIRMED)
                + orderRepository.countByStatus(OrderStatus.PACKED)
                + orderRepository.countByStatus(OrderStatus.SHIPPED)
                + orderRepository.countByStatus(OrderStatus.OUT_FOR_DELIVERY);
    }

    private List<MonthlyRevenuePoint> buildRevenueByMonth() {
        List<Order> orders = orderRepository.findByStatusNot(OrderStatus.CANCELLED);

        Map<String, BigDecimal> revenueByMonth = new LinkedHashMap<>();
        Map<String, Long> countByMonth = new LinkedHashMap<>();
        for (Order order : orders) {
            String key = order.getPlacedAt().format(MONTH_KEY_FORMAT);
            revenueByMonth.merge(key, order.getTotalAmount(), BigDecimal::add);
            countByMonth.merge(key, 1L, Long::sum);
        }

        List<MonthlyRevenuePoint> points = new ArrayList<>();
        YearMonth cursor = YearMonth.now().minusMonths(REVENUE_CHART_MONTHS - 1L);
        for (int i = 0; i < REVENUE_CHART_MONTHS; i++) {
            String key = cursor.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            points.add(MonthlyRevenuePoint.builder()
                    .month(key)
                    .revenue(revenueByMonth.getOrDefault(key, BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP))
                    .orderCount(countByMonth.getOrDefault(key, 0L))
                    .build());
            cursor = cursor.plusMonths(1);
        }
        return points;
    }

    private Map<OrderStatus, Long> buildStatusBreakdown() {
        Map<OrderStatus, Long> breakdown = new LinkedHashMap<>();
        for (OrderStatus status : OrderStatus.values()) {
            breakdown.put(status, orderRepository.countByStatus(status));
        }
        return breakdown;
    }

    private List<TopSellingProductResponse> buildTopSellingProducts(int limit) {
        return orderItemRepository.findTopSellingProducts(PageRequest.of(0, limit)).stream()
                .map(p -> TopSellingProductResponse.builder()
                        .productId(p.getProductId())
                        .productName(p.getProductName())
                        .totalSold(p.getTotalSold())
                        .build())
                .toList();
    }

    private AdminUserResponse toAdminUserResponse(User user) {
        return AdminUserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
