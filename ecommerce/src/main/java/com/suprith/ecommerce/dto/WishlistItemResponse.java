package com.suprith.ecommerce.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WishlistItemResponse {

    private Long id;
    private ProductSummaryResponse product;
    private LocalDateTime addedAt;
}
