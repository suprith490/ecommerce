package com.suprith.ecommerce.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProductRequest {

    @NotBlank(message = "Product name is required")
    private String name;

    /** Optional — auto-generated from name if blank. */
    private String slug;

    @NotBlank(message = "SKU is required")
    private String sku;

    private String description;

    private String specifications;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Price cannot be negative")
    private BigDecimal price;

    @DecimalMin(value = "0.0", inclusive = true, message = "Offer price cannot be negative")
    private BigDecimal offerPrice;

    @Min(value = 0, message = "Stock cannot be negative")
    private int stock;

    @Min(value = 0, message = "Low stock threshold cannot be negative")
    private int lowStockThreshold;

    @NotNull(message = "Category is required")
    private Long categoryId;

    private Long brandId;

    private Boolean active;

    @Valid
    private List<ProductImageRequest> images;
}
