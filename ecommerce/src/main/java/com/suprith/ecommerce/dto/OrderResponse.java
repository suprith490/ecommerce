package com.suprith.ecommerce.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.suprith.ecommerce.enums.OrderStatus;
import com.suprith.ecommerce.enums.PaymentMethod;
import com.suprith.ecommerce.enums.PaymentStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderResponse {

    private Long id;
    private String orderNumber;
    private OrderStatus status;

    private String customerName;
    private String customerEmail;

    private AddressResponse shippingAddress;

    private List<OrderItemResponse> items;

    private BigDecimal subtotal;
    private BigDecimal shippingCharge;
    private BigDecimal gst;
    private BigDecimal discountAmount;
    private String couponCode;
    private BigDecimal totalAmount;

    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;

    private String cancelReason;

    private List<OrderStatusHistoryResponse> statusHistory;

    private LocalDateTime placedAt;
    private LocalDateTime expectedDeliveryDate;
}
