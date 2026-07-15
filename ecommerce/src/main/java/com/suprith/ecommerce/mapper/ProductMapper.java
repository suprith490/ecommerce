package com.suprith.ecommerce.mapper;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.suprith.ecommerce.dto.ProductImageResponse;
import com.suprith.ecommerce.dto.ProductResponse;
import com.suprith.ecommerce.dto.ProductSummaryResponse;
import com.suprith.ecommerce.entity.Product;
import com.suprith.ecommerce.entity.ProductImage;

@Component
public class ProductMapper {

    public ProductResponse toResponse(Product product) {
        List<ProductImageResponse> images = product.getImages().stream()
                .map(this::toImageResponse)
                .toList();

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .sku(product.getSku())
                .description(product.getDescription())
                .specifications(product.getSpecifications())
                .price(product.getPrice())
                .offerPrice(product.getOfferPrice())
                .effectivePrice(product.getEffectivePrice())
                .discountPercentage(product.getDiscountPercentage())
                .stock(product.getStock())
                .inStock(product.isInStock())
                .lowStock(product.isLowStock())
                .active(product.isActive())
                .averageRating(product.getAverageRating())
                .ratingCount(product.getRatingCount())
                .categoryId(product.getCategory().getId())
                .categoryName(product.getCategory().getName())
                .brandId(product.getBrand() != null ? product.getBrand().getId() : null)
                .brandName(product.getBrand() != null ? product.getBrand().getName() : null)
                .images(images)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    public ProductSummaryResponse toSummary(Product product) {
        String primaryImage = product.getImages().stream()
                .filter(ProductImage::isPrimary)
                .findFirst()
                .or(() -> product.getImages().stream().min(Comparator.comparingInt(ProductImage::getSortOrder)))
                .map(ProductImage::getImageUrl)
                .orElse(null);

        return ProductSummaryResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .primaryImageUrl(primaryImage)
                .price(product.getPrice())
                .offerPrice(product.getOfferPrice())
                .effectivePrice(product.getEffectivePrice())
                .discountPercentage(product.getDiscountPercentage())
                .averageRating(product.getAverageRating())
                .ratingCount(product.getRatingCount())
                .inStock(product.isInStock())
                .build();
    }

    private ProductImageResponse toImageResponse(ProductImage image) {
        return ProductImageResponse.builder()
                .id(image.getId())
                .imageUrl(image.getImageUrl())
                .altText(image.getAltText())
                .primary(image.isPrimary())
                .sortOrder(image.getSortOrder())
                .build();
    }
}
