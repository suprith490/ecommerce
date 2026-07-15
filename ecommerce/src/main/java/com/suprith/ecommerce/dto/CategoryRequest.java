package com.suprith.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CategoryRequest {

    @NotBlank(message = "Category name is required")
    private String name;

    /** Optional — auto-generated from name if blank. */
    private String slug;

    private String description;

    private String imageUrl;

    private Long parentId;

    private Boolean active;
}
