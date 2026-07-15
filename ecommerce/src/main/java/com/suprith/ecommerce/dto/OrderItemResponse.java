package com.suprith.ecommerce.dto;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderItemResponse {

    private Long productId;
    private String productName;
    private String productImageUrl;
    private BigDecimal unitPrice;
    private int quantity;
    private BigDecimal lineTotal;
}
