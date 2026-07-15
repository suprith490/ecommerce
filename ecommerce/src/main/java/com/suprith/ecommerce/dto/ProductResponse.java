package com.suprith.ecommerce.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductResponse {

    private Long id;
    private String name;
    private String slug;
    private String sku;
    private String description;
    private String specifications;

    private BigDecimal price;
    private BigDecimal offerPrice;
    private BigDecimal effectivePrice;
    private int discountPercentage;

    private int stock;
    private boolean inStock;
    private boolean lowStock;

    private boolean active;

    private double averageRating;
    private int ratingCount;

    private Long categoryId;
    private String categoryName;
    private Long brandId;
    private String brandName;

    private List<ProductImageResponse> images;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
