package com.suprith.ecommerce.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.suprith.ecommerce.enums.OrderStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardStatsResponse {

    // ---- Summary cards ----
    private BigDecimal totalRevenue;
    private long totalOrders;
    private long totalUsers;
    private long totalProducts;
    private long totalCategories;
    private long pendingOrders;
    private long lowStockCount;
    private long outOfStockCount;

    // ---- Chart.js-ready datasets ----
    private List<MonthlyRevenuePoint> revenueByMonth;
    private Map<OrderStatus, Long> orderStatusBreakdown;
    private List<TopSellingProductResponse> topSellingProducts;

    // ---- Recent activity feeds ----
    private List<OrderSummaryResponse> recentOrders;
    private List<AdminUserResponse> recentUsers;
    private List<ProductSummaryResponse> recentProducts;
}
