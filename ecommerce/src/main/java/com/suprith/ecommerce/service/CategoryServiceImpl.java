package com.suprith.ecommerce.service;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.suprith.ecommerce.dto.CategoryRequest;
import com.suprith.ecommerce.dto.CategoryResponse;
import com.suprith.ecommerce.entity.Category;
import com.suprith.ecommerce.exception.DuplicateResourceException;
import com.suprith.ecommerce.exception.ResourceNotFoundException;
import com.suprith.ecommerce.mapper.CategoryMapper;
import com.suprith.ecommerce.repository.CategoryRepository;
import com.suprith.ecommerce.repository.ProductRepository;
import com.suprith.ecommerce.util.SlugUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @CacheEvict(value = "categoryTree", allEntries = true)
    public CategoryResponse create(CategoryRequest request) {
        String slug = resolveSlug(request.getName(), request.getSlug());

        if (categoryRepository.existsBySlug(slug)) {
            throw new DuplicateResourceException("A category with slug '" + slug + "' already exists");
        }

        Category parent = resolveParent(request.getParentId());

        Category category = Category.builder()
                .name(request.getName())
                .slug(slug)
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .active(request.getActive() == null || request.getActive())
                .parent(parent)
                .build();

        Category saved = categoryRepository.save(category);
        return categoryMapper.toResponse(saved);
    }

    @Override
    @CacheEvict(value = "categoryTree", allEntries = true)
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = findEntity(id);

        String slug = resolveSlug(request.getName(), request.getSlug());
        if (!slug.equals(category.getSlug()) && categoryRepository.existsBySlug(slug)) {
            throw new DuplicateResourceException("A category with slug '" + slug + "' already exists");
        }

        if (request.getParentId() != null && request.getParentId().equals(id)) {
            throw new IllegalArgumentException("A category cannot be its own parent");
        }

        Category parent = resolveParent(request.getParentId());

        category.setName(request.getName());
        category.setSlug(slug);
        category.setDescription(request.getDescription());
        category.setImageUrl(request.getImageUrl());
        category.setParent(parent);
        if (request.getActive() != null) {
            category.setActive(request.getActive());
        }

        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Override
    @CacheEvict(value = "categoryTree", allEntries = true)
    public void delete(Long id) {
        Category category = findEntity(id);

        if (categoryRepository.countByParentId(id) > 0) {
            throw new IllegalStateException("Cannot delete a category that has sub-categories. Remove or reassign them first.");
        }
        if (productRepository.existsByCategoryIdAndActiveTrue(id)) {
            throw new IllegalStateException("Cannot delete a category that still has products assigned to it.");
        }

        categoryRepository.delete(category);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getById(Long id) {
        return categoryMapper.toResponse(findEntity(id), true);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getBySlug(String slug) {
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + slug));
        return categoryMapper.toResponse(category, true);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable("categoryTree")
    public List<CategoryResponse> getTree() {
        return categoryRepository.findByParentIsNullOrderByNameAsc().stream()
                .map(c -> categoryMapper.toResponse(c, true))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllFlat() {
        return categoryRepository.findAll().stream()
                .map(c -> categoryMapper.toResponse(c, false))
                .toList();
    }

    private Category findEntity(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
    }

    private Category resolveParent(Long parentId) {
        if (parentId == null) {
            return null;
        }
        return categoryRepository.findById(parentId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent category not found with id: " + parentId));
    }

    private String resolveSlug(String name, String requestedSlug) {
        String base = (requestedSlug == null || requestedSlug.isBlank()) ? name : requestedSlug;
        return SlugUtil.toSlug(base);
    }
}
