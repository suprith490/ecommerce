package com.suprith.ecommerce.dto;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductSummaryResponse {

    private Long id;
    private String name;
    private String slug;
    private String primaryImageUrl;
    private BigDecimal price;
    private BigDecimal offerPrice;
    private BigDecimal effectivePrice;
    private int discountPercentage;
    private double averageRating;
    private int ratingCount;
    private boolean inStock;
}
