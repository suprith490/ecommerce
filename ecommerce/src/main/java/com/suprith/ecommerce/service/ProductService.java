package com.suprith.ecommerce.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.suprith.ecommerce.dto.ProductRequest;
import com.suprith.ecommerce.dto.ProductResponse;
import com.suprith.ecommerce.dto.ProductSummaryResponse;

public interface ProductService {

    ProductResponse create(ProductRequest request);

    ProductResponse update(Long id, ProductRequest request);

    void delete(Long id);

    ProductResponse getById(Long id);

    ProductResponse getBySlug(String slug);

    Page<ProductSummaryResponse> getPublicCatalog(Long categoryId, Long brandId, String search, Pageable pageable);

    Page<ProductResponse> getAllForAdmin(Pageable pageable);

    Page<ProductResponse> getLowStock(Pageable pageable);

    Page<ProductResponse> getOutOfStock(Pageable pageable);

    ProductResponse updateStock(Long id, int newStock);
}
