package com.suprith.ecommerce.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.suprith.ecommerce.dto.CategoryResponse;
import com.suprith.ecommerce.entity.Category;

@Component
public class CategoryMapper {

    public CategoryResponse toResponse(Category category) {
        return toResponse(category, false);
    }

    public CategoryResponse toResponse(Category category, boolean includeChildren) {
        CategoryResponse.CategoryResponseBuilder builder = CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .imageUrl(category.getImageUrl())
                .active(category.isActive());

        if (category.getParent() != null) {
            builder.parentId(category.getParent().getId())
                    .parentName(category.getParent().getName());
        }

        if (includeChildren && category.getChildren() != null) {
            List<CategoryResponse> children = category.getChildren().stream()
                    .map(child -> toResponse(child, true))
                    .toList();
            builder.subCategories(children);
        }

        return builder.build();
    }
}
