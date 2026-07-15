package com.suprith.ecommerce.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.suprith.ecommerce.dto.ProductRequest;
import com.suprith.ecommerce.dto.ProductResponse;
import com.suprith.ecommerce.dto.ProductSummaryResponse;
import com.suprith.ecommerce.service.ProductService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // ---- Public storefront endpoints ----

    @GetMapping("/api/products")
    public ResponseEntity<Page<ProductSummaryResponse>> browse(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(productService.getPublicCatalog(categoryId, brandId, search, pageable));
    }

    @GetMapping("/api/products/{slug}")
    public ResponseEntity<ProductResponse> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(productService.getBySlug(slug));
    }

    // ---- Admin management ----

    @GetMapping("/api/admin/products")
    public ResponseEntity<Page<ProductResponse>> getAllForAdmin(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(productService.getAllForAdmin(PageRequest.of(page, size, Sort.by("id").descending())));
    }

    @GetMapping("/api/admin/products/{id}")
    public ResponseEntity<ProductResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getById(id));
    }

    @PostMapping("/api/admin/products")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(request));
    }

    @PutMapping("/api/admin/products/{id}")
    public ResponseEntity<ProductResponse> update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.update(id, request));
    }

    @DeleteMapping("/api/admin/products/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/api/admin/products/{id}/stock")
    public ResponseEntity<ProductResponse> updateStock(@PathVariable Long id, @RequestParam int stock) {
        return ResponseEntity.ok(productService.updateStock(id, stock));
    }

    @GetMapping("/api/admin/products/inventory/low-stock")
    public ResponseEntity<Page<ProductResponse>> lowStock(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(productService.getLowStock(PageRequest.of(page, size)));
    }

    @GetMapping("/api/admin/products/inventory/out-of-stock")
    public ResponseEntity<Page<ProductResponse>> outOfStock(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(productService.getOutOfStock(PageRequest.of(page, size)));
    }
}
