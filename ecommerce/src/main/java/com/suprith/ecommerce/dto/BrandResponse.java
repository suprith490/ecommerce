package com.suprith.ecommerce.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BrandResponse {

    private Long id;
    private String name;
    private String slug;
    private String logoUrl;
    private String description;
    private boolean active;
}
