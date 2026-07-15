package com.suprith.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BrandRequest {

    @NotBlank(message = "Brand name is required")
    private String name;

    private String slug;

    private String logoUrl;

    private String description;

    private Boolean active;
}
