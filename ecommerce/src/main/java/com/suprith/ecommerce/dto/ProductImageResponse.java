package com.suprith.ecommerce.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductImageResponse {

    private Long id;
    private String imageUrl;
    private String altText;
    private boolean primary;
    private int sortOrder;
}
