package com.suprith.ecommerce.dto;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CartItemResponse {

    private Long id;
    private ProductSummaryResponse product;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;
    private int availableStock;
    private boolean inStock;
    private boolean quantityExceedsStock;
}
