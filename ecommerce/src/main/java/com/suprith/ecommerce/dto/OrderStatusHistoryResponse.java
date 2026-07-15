package com.suprith.ecommerce.dto;

import java.time.LocalDateTime;

import com.suprith.ecommerce.enums.OrderStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderStatusHistoryResponse {

    private OrderStatus status;
    private String note;
    private LocalDateTime changedAt;
}
