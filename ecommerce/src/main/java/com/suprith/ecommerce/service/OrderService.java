package com.suprith.ecommerce.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.suprith.ecommerce.dto.CheckoutRequest;
import com.suprith.ecommerce.dto.OrderResponse;
import com.suprith.ecommerce.dto.OrderStatusUpdateRequest;
import com.suprith.ecommerce.dto.OrderSummaryResponse;

public interface OrderService {

    OrderResponse placeOrder(Long userId, CheckoutRequest request);

    OrderResponse getOrderDetail(Long userId, Long orderId);

    Page<OrderSummaryResponse> getOrderHistory(Long userId, Pageable pageable);

    OrderResponse cancelOrder(Long userId, Long orderId, String reason);

    OrderResponse requestReturn(Long userId, Long orderId, String reason);

    // ---- Admin ----

    Page<OrderResponse> getAllForAdmin(Pageable pageable);

    OrderResponse getByIdForAdmin(Long orderId);

    OrderResponse updateStatus(Long orderId, OrderStatusUpdateRequest request);
}
