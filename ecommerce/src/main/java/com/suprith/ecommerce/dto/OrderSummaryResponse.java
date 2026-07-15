package com.suprith.ecommerce.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.suprith.ecommerce.enums.OrderStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderSummaryResponse {

    private Long id;
    private String orderNumber;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private int itemCount;
    private String firstProductImageUrl;
    private LocalDateTime placedAt;
}
