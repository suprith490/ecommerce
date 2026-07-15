package com.suprith.ecommerce.dto;

import com.suprith.ecommerce.enums.OrderStatus;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderStatusUpdateRequest {

    @NotNull(message = "Status is required")
    private OrderStatus status;

    private String note;
}
