package com.suprith.ecommerce.dto;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CategoryResponse {

    private Long id;
    private String name;
    private String slug;
    private String description;
    private String imageUrl;
    private boolean active;
    private Long parentId;
    private String parentName;
    private List<CategoryResponse> subCategories;
}
