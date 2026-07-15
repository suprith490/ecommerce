package com.suprith.ecommerce.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.suprith.ecommerce.constants.PricingConstants;
import com.suprith.ecommerce.dto.CheckoutRequest;
import com.suprith.ecommerce.dto.OrderResponse;
import com.suprith.ecommerce.dto.OrderStatusUpdateRequest;
import com.suprith.ecommerce.dto.OrderSummaryResponse;
import com.suprith.ecommerce.entity.Address;
import com.suprith.ecommerce.entity.CartItem;
import com.suprith.ecommerce.entity.Coupon;
import com.suprith.ecommerce.entity.Order;
import com.suprith.ecommerce.entity.OrderItem;
import com.suprith.ecommerce.entity.OrderStatusHistory;
import com.suprith.ecommerce.entity.Product;
import com.suprith.ecommerce.entity.ShippingAddressSnapshot;
import com.suprith.ecommerce.entity.User;
import com.suprith.ecommerce.enums.OrderStatus;
import com.suprith.ecommerce.enums.PaymentMethod;
import com.suprith.ecommerce.enums.PaymentStatus;
import com.suprith.ecommerce.exception.InsufficientStockException;
import com.suprith.ecommerce.exception.InvalidOrderStateException;
import com.suprith.ecommerce.exception.ResourceNotFoundException;
import com.suprith.ecommerce.mapper.OrderMapper;
import com.suprith.ecommerce.repository.AddressRepository;
import com.suprith.ecommerce.repository.CartItemRepository;
import com.suprith.ecommerce.repository.OrderRepository;
import com.suprith.ecommerce.repository.ProductRepository;
import com.suprith.ecommerce.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {

    private static final Set<OrderStatus> CANCELLABLE_STATUSES =
            EnumSet.of(OrderStatus.PLACED, OrderStatus.CONFIRMED, OrderStatus.PACKED);

    private static final Set<OrderStatus> TERMINAL_STATUSES =
            EnumSet.of(OrderStatus.CANCELLED, OrderStatus.RETURNED);

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final AddressRepository addressRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CouponService couponService;
    private final OrderMapper orderMapper;

    @Override
    public OrderResponse placeOrder(Long userId, CheckoutRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        List<CartItem> cartItems = cartItemRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (cartItems.isEmpty()) {
            throw new IllegalStateException("Your cart is empty");
        }

        Address address = addressRepository.findByIdAndUserId(request.getAddressId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery address not found"));

        // Re-validate stock at the moment of purchase — it may have changed since items were added to the cart.
        for (CartItem item : cartItems) {
            Product product = item.getProduct();
            if (item.getQuantity() > product.getStock()) {
                throw new InsufficientStockException(
                        "Only " + product.getStock() + " unit(s) of \"" + product.getName() + "\" left in stock");
            }
        }

        BigDecimal subtotal = cartItems.stream()
                .map(item -> item.getProduct().getEffectivePrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        Coupon appliedCoupon = null;
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            appliedCoupon = couponService.validateForCheckout(request.getCouponCode(), subtotal);
            discountAmount = couponService.calculateDiscount(appliedCoupon, subtotal);
        }

        BigDecimal discountedSubtotal = subtotal.subtract(discountAmount).max(BigDecimal.ZERO);

        // Free-shipping threshold is evaluated on the pre-discount subtotal, matching what the cart preview shows.
        BigDecimal shipping = (subtotal.signum() > 0 && subtotal.compareTo(PricingConstants.FREE_SHIPPING_THRESHOLD) < 0)
                ? PricingConstants.FLAT_SHIPPING_CHARGE
                : BigDecimal.ZERO;

        BigDecimal gst = discountedSubtotal.multiply(PricingConstants.GST_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = discountedSubtotal.add(shipping).add(gst).setScale(2, RoundingMode.HALF_UP);

        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .user(user)
                .status(OrderStatus.PLACED)
                .shippingAddress(toSnapshot(address))
                .subtotal(subtotal)
                .shippingCharge(shipping)
                .gst(gst)
                .discountAmount(discountAmount)
                .couponCode(appliedCoupon != null ? appliedCoupon.getCode() : null)
                .totalAmount(total)
                .paymentMethod(request.getPaymentMethod())
                // No real payment gateway is wired up yet: COD stays PENDING until delivery,
                // while CARD/UPI are marked PAID immediately as a stand-in for a real gateway
                // callback (Razorpay/Stripe/etc. would replace this with a verified webhook).
                .paymentStatus(request.getPaymentMethod() == PaymentMethod.COD ? PaymentStatus.PENDING : PaymentStatus.PAID)
                .expectedDeliveryDate(LocalDateTime.now().plusDays(5))
                .build();

        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();
            BigDecimal unitPrice = product.getEffectivePrice();
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity())).setScale(2, RoundingMode.HALF_UP);

            String primaryImage = product.getImages().stream()
                    .filter(img -> img.isPrimary())
                    .findFirst()
                    .map(img -> img.getImageUrl())
                    .orElse(product.getImages().isEmpty() ? null : product.getImages().get(0).getImageUrl());

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .productName(product.getName())
                    .productImageUrl(primaryImage)
                    .unitPrice(unitPrice)
                    .quantity(cartItem.getQuantity())
                    .lineTotal(lineTotal)
                    .build();
            order.getItems().add(orderItem);

            product.setStock(product.getStock() - cartItem.getQuantity());
            productRepository.save(product);
        }

        order.getStatusHistory().add(OrderStatusHistory.builder()
                .order(order)
                .status(OrderStatus.PLACED)
                .note("Order placed successfully")
                .build());

        Order saved = orderRepository.save(order);

        if (appliedCoupon != null) {
            couponService.recordUsage(appliedCoupon);
        }

        cartItemRepository.deleteByUserId(userId);

        return orderMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderDetail(Long userId, Long orderId) {
        return orderMapper.toResponse(findOwned(userId, orderId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> getOrderHistory(Long userId, Pageable pageable) {
        return orderRepository.findByUserIdOrderByPlacedAtDesc(userId, pageable).map(orderMapper::toSummary);
    }

    @Override
    public OrderResponse cancelOrder(Long userId, Long orderId, String reason) {
        Order order = findOwned(userId, orderId);

        if (!CANCELLABLE_STATUSES.contains(order.getStatus())) {
            throw new InvalidOrderStateException(
                    "This order can no longer be cancelled (current status: " + order.getStatus() + ")");
        }

        for (OrderItem item : order.getItems()) {
            if (item.getProduct() != null) {
                Product product = item.getProduct();
                product.setStock(product.getStock() + item.getQuantity());
                productRepository.save(product);
            }
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelReason(reason);
        order.getStatusHistory().add(OrderStatusHistory.builder()
                .order(order)
                .status(OrderStatus.CANCELLED)
                .note(reason)
                .build());

        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Override
    public OrderResponse requestReturn(Long userId, Long orderId, String reason) {
        Order order = findOwned(userId, orderId);

        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new InvalidOrderStateException("Only delivered orders can be returned");
        }

        order.setStatus(OrderStatus.RETURN_REQUESTED);
        order.getStatusHistory().add(OrderStatusHistory.builder()
                .order(order)
                .status(OrderStatus.RETURN_REQUESTED)
                .note(reason)
                .build());

        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllForAdmin(Pageable pageable) {
        return orderRepository.findAll(pageable).map(orderMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getByIdForAdmin(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        return orderMapper.toResponse(order);
    }

    @Override
    public OrderResponse updateStatus(Long orderId, OrderStatusUpdateRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (TERMINAL_STATUSES.contains(order.getStatus())) {
            throw new InvalidOrderStateException(
                    "Order status cannot be changed once it is " + order.getStatus());
        }

        order.setStatus(request.getStatus());
        order.getStatusHistory().add(OrderStatusHistory.builder()
                .order(order)
                .status(request.getStatus())
                .note(request.getNote())
                .build());

        return orderMapper.toResponse(orderRepository.save(order));
    }

    private Order findOwned(Long userId, Long orderId) {
        return orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    }

    private ShippingAddressSnapshot toSnapshot(Address address) {
        return ShippingAddressSnapshot.builder()
                .fullName(address.getFullName())
                .phone(address.getPhone())
                .addressLine1(address.getAddressLine1())
                .addressLine2(address.getAddressLine2())
                .city(address.getCity())
                .state(address.getState())
                .postalCode(address.getPostalCode())
                .country(address.getCountry())
                .build();
    }

    private String generateOrderNumber() {
        String datePart = LocalDateTime.now().toLocalDate().toString().replace("-", "");
        String candidate;
        do {
            int randomPart = ThreadLocalRandom.current().nextInt(100000, 999999);
            candidate = "ORD" + datePart + randomPart;
        } while (orderRepository.existsByOrderNumber(candidate));
        return candidate;
    }
}
