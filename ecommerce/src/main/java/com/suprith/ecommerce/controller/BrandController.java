package com.suprith.ecommerce.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.suprith.ecommerce.dto.BrandRequest;
import com.suprith.ecommerce.dto.BrandResponse;
import com.suprith.ecommerce.service.BrandService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class BrandController {

    private final BrandService brandService;

    // ---- Public storefront endpoints ----

    @GetMapping("/api/brands")
    public ResponseEntity<List<BrandResponse>> getAllActive() {
        return ResponseEntity.ok(brandService.getAllActive());
    }

    @GetMapping("/api/brands/{slug}")
    public ResponseEntity<BrandResponse> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(brandService.getBySlug(slug));
    }

    // ---- Admin management ----

    @GetMapping("/api/admin/brands")
    public ResponseEntity<List<BrandResponse>> getAll() {
        return ResponseEntity.ok(brandService.getAll());
    }

    @GetMapping("/api/admin/brands/{id}")
    public ResponseEntity<BrandResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(brandService.getById(id));
    }

    @PostMapping("/api/admin/brands")
    public ResponseEntity<BrandResponse> create(@Valid @RequestBody BrandRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(brandService.create(request));
    }

    @PutMapping("/api/admin/brands/{id}")
    public ResponseEntity<BrandResponse> update(@PathVariable Long id, @Valid @RequestBody BrandRequest request) {
        return ResponseEntity.ok(brandService.update(id, request));
    }

    @DeleteMapping("/api/admin/brands/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        brandService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
