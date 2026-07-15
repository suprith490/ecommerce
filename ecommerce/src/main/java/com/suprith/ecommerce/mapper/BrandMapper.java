package com.suprith.ecommerce.mapper;

import org.springframework.stereotype.Component;

import com.suprith.ecommerce.dto.BrandResponse;
import com.suprith.ecommerce.entity.Brand;

@Component
public class BrandMapper {

    public BrandResponse toResponse(Brand brand) {
        return BrandResponse.builder()
                .id(brand.getId())
                .name(brand.getName())
                .slug(brand.getSlug())
                .logoUrl(brand.getLogoUrl())
                .description(brand.getDescription())
                .active(brand.isActive())
                .build();
    }
}
