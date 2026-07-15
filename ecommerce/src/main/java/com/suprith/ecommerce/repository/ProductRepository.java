package com.suprith.ecommerce.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.suprith.ecommerce.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    @EntityGraph(attributePaths = {"images", "category", "brand"})
    Optional<Product> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySku(String sku);

    boolean existsByCategoryIdAndActiveTrue(Long categoryId);

    boolean existsByBrandIdAndActiveTrue(Long brandId);

    @EntityGraph(attributePaths = {"images", "category", "brand"})
    Page<Product> findByStockLessThanEqualAndStockGreaterThanAndActiveTrue(int threshold, int floor, Pageable pageable);

    @EntityGraph(attributePaths = {"images", "category", "brand"})
    Page<Product> findByStockAndActiveTrue(int stock, Pageable pageable);

    long countByStockLessThanEqualAndStockGreaterThanAndActiveTrue(int threshold, int floor);

    long countByStockAndActiveTrue(int stock);

    // Every product listing screen (PLP, home sections, admin product table, inventory tabs) maps
    // category/brand/images for each row — without this graph that's 1 + 3N queries per page instead of 1.
    @Override
    @EntityGraph(attributePaths = {"images", "category", "brand"})
    Page<Product> findAll(org.springframework.data.jpa.domain.Specification<Product> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"images", "category", "brand"})
    Page<Product> findAll(Pageable pageable);
}
