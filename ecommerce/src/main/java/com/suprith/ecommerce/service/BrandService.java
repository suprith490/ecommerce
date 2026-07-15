package com.suprith.ecommerce.service;

import java.util.List;

import com.suprith.ecommerce.dto.BrandRequest;
import com.suprith.ecommerce.dto.BrandResponse;

public interface BrandService {

    BrandResponse create(BrandRequest request);

    BrandResponse update(Long id, BrandRequest request);

    void delete(Long id);

    BrandResponse getById(Long id);

    BrandResponse getBySlug(String slug);

    List<BrandResponse> getAllActive();

    List<BrandResponse> getAll();
}
