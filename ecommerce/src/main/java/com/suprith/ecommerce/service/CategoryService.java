package com.suprith.ecommerce.service;

import java.util.List;

import com.suprith.ecommerce.dto.CategoryRequest;
import com.suprith.ecommerce.dto.CategoryResponse;

public interface CategoryService {

    CategoryResponse create(CategoryRequest request);

    CategoryResponse update(Long id, CategoryRequest request);

    void delete(Long id);

    CategoryResponse getById(Long id);

    CategoryResponse getBySlug(String slug);

    List<CategoryResponse> getTree();

    List<CategoryResponse> getAllFlat();
}
