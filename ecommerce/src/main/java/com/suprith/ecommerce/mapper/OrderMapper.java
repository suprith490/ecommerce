package com.suprith.ecommerce.mapper;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.suprith.ecommerce.dto.AddressResponse;
import com.suprith.ecommerce.dto.OrderItemResponse;
import com.suprith.ecommerce.dto.OrderResponse;
import com.suprith.ecommerce.dto.OrderStatusHistoryResponse;
import com.suprith.ecommerce.dto.OrderSummaryResponse;
import com.suprith.ecommerce.entity.Order;
import com.suprith.ecommerce.entity.OrderItem;
import com.suprith.ecommerce.entity.OrderStatusHistory;
import com.suprith.ecommerce.entity.ShippingAddressSnapshot;

@Component
public class OrderMapper {

    public OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(this::toItemResponse)
                .toList();

        List<OrderStatusHistoryResponse> history = order.getStatusHistory().stream()
                .map(this::toHistoryResponse)
                .toList();

        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .customerName(order.getUser().getName())
                .customerEmail(order.getUser().getEmail())
                .shippingAddress(toAddressResponse(order.getShippingAddress()))
                .items(items)
                .subtotal(order.getSubtotal())
                .shippingCharge(order.getShippingCharge())
                .gst(order.getGst())
                .discountAmount(order.getDiscountAmount())
                .couponCode(order.getCouponCode())
                .totalAmount(order.getTotalAmount())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .cancelReason(order.getCancelReason())
                .statusHistory(history)
                .placedAt(order.getPlacedAt())
                .expectedDeliveryDate(order.getExpectedDeliveryDate())
                .build();
    }

    public OrderSummaryResponse toSummary(Order order) {
        String firstImage = order.getItems().stream()
                .min(Comparator.comparing(OrderItem::getId))
                .map(OrderItem::getProductImageUrl)
                .orElse(null);

        int itemCount = order.getItems().stream().mapToInt(OrderItem::getQuantity).sum();

        return OrderSummaryResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .itemCount(itemCount)
                .firstProductImageUrl(firstImage)
                .placedAt(order.getPlacedAt())
                .build();
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        return OrderItemResponse.builder()
                .productId(item.getProduct() != null ? item.getProduct().getId() : null)
                .productName(item.getProductName())
                .productImageUrl(item.getProductImageUrl())
                .unitPrice(item.getUnitPrice())
                .quantity(item.getQuantity())
                .lineTotal(item.getLineTotal())
                .build();
    }

    private OrderStatusHistoryResponse toHistoryResponse(OrderStatusHistory history) {
        return OrderStatusHistoryResponse.builder()
                .status(history.getStatus())
                .note(history.getNote())
                .changedAt(history.getChangedAt())
                .build();
    }

    private AddressResponse toAddressResponse(ShippingAddressSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        return AddressResponse.builder()
                .fullName(snapshot.getFullName())
                .phone(snapshot.getPhone())
                .addressLine1(snapshot.getAddressLine1())
                .addressLine2(snapshot.getAddressLine2())
                .city(snapshot.getCity())
                .state(snapshot.getState())
                .postalCode(snapshot.getPostalCode())
                .country(snapshot.getCountry())
                .build();
    }
}
