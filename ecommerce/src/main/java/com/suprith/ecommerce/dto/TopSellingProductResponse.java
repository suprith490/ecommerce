package com.suprith.ecommerce.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TopSellingProductResponse {

    private Long productId;
    private String productName;
    private long totalSold;
}
