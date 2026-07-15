package com.suprith.ecommerce.service;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.suprith.ecommerce.dto.BrandRequest;
import com.suprith.ecommerce.dto.BrandResponse;
import com.suprith.ecommerce.entity.Brand;
import com.suprith.ecommerce.exception.DuplicateResourceException;
import com.suprith.ecommerce.exception.ResourceNotFoundException;
import com.suprith.ecommerce.mapper.BrandMapper;
import com.suprith.ecommerce.repository.BrandRepository;
import com.suprith.ecommerce.repository.ProductRepository;
import com.suprith.ecommerce.util.SlugUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class BrandServiceImpl implements BrandService {

    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;
    private final BrandMapper brandMapper;

    @Override
    @CacheEvict(value = "activeBrands", allEntries = true)
    public BrandResponse create(BrandRequest request) {
        if (brandRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("A brand named '" + request.getName() + "' already exists");
        }

        String slug = resolveSlug(request.getName(), request.getSlug());
        if (brandRepository.existsBySlug(slug)) {
            throw new DuplicateResourceException("A brand with slug '" + slug + "' already exists");
        }

        Brand brand = Brand.builder()
                .name(request.getName())
                .slug(slug)
                .logoUrl(request.getLogoUrl())
                .description(request.getDescription())
                .active(request.getActive() == null || request.getActive())
                .build();

        return brandMapper.toResponse(brandRepository.save(brand));
    }

    @Override
    @CacheEvict(value = "activeBrands", allEntries = true)
    public BrandResponse update(Long id, BrandRequest request) {
        Brand brand = findEntity(id);

        String slug = resolveSlug(request.getName(), request.getSlug());
        if (!slug.equals(brand.getSlug()) && brandRepository.existsBySlug(slug)) {
            throw new DuplicateResourceException("A brand with slug '" + slug + "' already exists");
        }

        brand.setName(request.getName());
        brand.setSlug(slug);
        brand.setLogoUrl(request.getLogoUrl());
        brand.setDescription(request.getDescription());
        if (request.getActive() != null) {
            brand.setActive(request.getActive());
        }

        return brandMapper.toResponse(brandRepository.save(brand));
    }

    @Override
    @CacheEvict(value = "activeBrands", allEntries = true)
    public void delete(Long id) {
        Brand brand = findEntity(id);

        if (productRepository.existsByBrandIdAndActiveTrue(id)) {
            throw new IllegalStateException("Cannot delete a brand that still has products assigned to it.");
        }

        brandRepository.delete(brand);
    }

    @Override
    @Transactional(readOnly = true)
    public BrandResponse getById(Long id) {
        return brandMapper.toResponse(findEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public BrandResponse getBySlug(String slug) {
        Brand brand = brandRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found: " + slug));
        return brandMapper.toResponse(brand);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable("activeBrands")
    public List<BrandResponse> getAllActive() {
        return brandRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(brandMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BrandResponse> getAll() {
        return brandRepository.findAll().stream()
                .map(brandMapper::toResponse)
                .toList();
    }

    private Brand findEntity(Long id) {
        return brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + id));
    }

    private String resolveSlug(String name, String requestedSlug) {
        String base = (requestedSlug == null || requestedSlug.isBlank()) ? name : requestedSlug;
        return SlugUtil.toSlug(base);
    }
}
