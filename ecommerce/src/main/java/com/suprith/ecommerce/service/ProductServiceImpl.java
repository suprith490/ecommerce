package com.suprith.ecommerce.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.suprith.ecommerce.dto.ProductImageRequest;
import com.suprith.ecommerce.dto.ProductRequest;
import com.suprith.ecommerce.dto.ProductResponse;
import com.suprith.ecommerce.dto.ProductSummaryResponse;
import com.suprith.ecommerce.entity.Brand;
import com.suprith.ecommerce.entity.Category;
import com.suprith.ecommerce.entity.Product;
import com.suprith.ecommerce.entity.ProductImage;
import com.suprith.ecommerce.exception.DuplicateResourceException;
import com.suprith.ecommerce.exception.ResourceNotFoundException;
import com.suprith.ecommerce.mapper.ProductMapper;
import com.suprith.ecommerce.repository.BrandRepository;
import com.suprith.ecommerce.repository.CategoryRepository;
import com.suprith.ecommerce.repository.ProductRepository;
import com.suprith.ecommerce.util.SlugUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final ProductMapper productMapper;

    @Override
    public ProductResponse create(ProductRequest request) {
        if (productRepository.existsBySku(request.getSku())) {
            throw new DuplicateResourceException("A product with SKU '" + request.getSku() + "' already exists");
        }

        String slug = resolveSlug(request.getName(), request.getSlug());
        if (productRepository.existsBySlug(slug)) {
            throw new DuplicateResourceException("A product with slug '" + slug + "' already exists");
        }

        Category category = findCategory(request.getCategoryId());
        Brand brand = resolveBrand(request.getBrandId());

        Product product = Product.builder()
                .name(request.getName())
                .slug(slug)
                .sku(request.getSku())
                .description(request.getDescription())
                .specifications(request.getSpecifications())
                .price(request.getPrice())
                .offerPrice(request.getOfferPrice())
                .stock(request.getStock())
                .lowStockThreshold(request.getLowStockThreshold() > 0 ? request.getLowStockThreshold() : 5)
                .active(request.getActive() == null || request.getActive())
                .category(category)
                .brand(brand)
                .build();

        applyImages(product, request.getImages());

        Product saved = productRepository.save(product);
        return productMapper.toResponse(saved);
    }

    @Override
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = findEntity(id);

        if (!request.getSku().equals(product.getSku()) && productRepository.existsBySku(request.getSku())) {
            throw new DuplicateResourceException("A product with SKU '" + request.getSku() + "' already exists");
        }

        String slug = resolveSlug(request.getName(), request.getSlug());
        if (!slug.equals(product.getSlug()) && productRepository.existsBySlug(slug)) {
            throw new DuplicateResourceException("A product with slug '" + slug + "' already exists");
        }

        Category category = findCategory(request.getCategoryId());
        Brand brand = resolveBrand(request.getBrandId());

        product.setName(request.getName());
        product.setSlug(slug);
        product.setSku(request.getSku());
        product.setDescription(request.getDescription());
        product.setSpecifications(request.getSpecifications());
        product.setPrice(request.getPrice());
        product.setOfferPrice(request.getOfferPrice());
        product.setStock(request.getStock());
        product.setLowStockThreshold(request.getLowStockThreshold() > 0 ? request.getLowStockThreshold() : product.getLowStockThreshold());
        product.setCategory(category);
        product.setBrand(brand);
        if (request.getActive() != null) {
            product.setActive(request.getActive());
        }

        if (request.getImages() != null) {
            product.getImages().clear();
            applyImages(product, request.getImages());
        }

        return productMapper.toResponse(productRepository.save(product));
    }

    @Override
    public void delete(Long id) {
        Product product = findEntity(id);
        productRepository.delete(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        return productMapper.toResponse(findEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getBySlug(String slug) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + slug));
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductSummaryResponse> getPublicCatalog(Long categoryId, Long brandId, String search, Pageable pageable) {
        Specification<Product> spec = Specification.where(activeSpec());

        if (categoryId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId));
        }
        if (brandId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("brand").get("id"), brandId));
        }
        if (search != null && !search.isBlank()) {
            String pattern = "%" + search.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("name")), pattern));
        }

        return productRepository.findAll(spec, pageable).map(productMapper::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllForAdmin(Pageable pageable) {
        return productRepository.findAll(pageable).map(productMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getLowStock(Pageable pageable) {
        // stock between 1 and each product's own lowStockThreshold can't be expressed as a single
        // derived query bound, so we filter defensively at the DB level for stock > 0 and small,
        // then rely on the entity's isLowStock() for the exact per-product threshold in the UI layer.
        return productRepository.findByStockLessThanEqualAndStockGreaterThanAndActiveTrue(50, 0, pageable)
                .map(productMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getOutOfStock(Pageable pageable) {
        return productRepository.findByStockAndActiveTrue(0, pageable).map(productMapper::toResponse);
    }

    @Override
    public ProductResponse updateStock(Long id, int newStock) {
        if (newStock < 0) {
            throw new IllegalArgumentException("Stock cannot be negative");
        }
        Product product = findEntity(id);
        product.setStock(newStock);
        return productMapper.toResponse(productRepository.save(product));
    }

    private void applyImages(Product product, List<ProductImageRequest> imageRequests) {
        if (imageRequests == null) {
            return;
        }
        boolean hasPrimary = imageRequests.stream().anyMatch(ProductImageRequest::isPrimary);
        List<ProductImage> images = new ArrayList<>();
        for (int i = 0; i < imageRequests.size(); i++) {
            ProductImageRequest req = imageRequests.get(i);
            images.add(ProductImage.builder()
                    .product(product)
                    .imageUrl(req.getImageUrl())
                    .altText(req.getAltText())
                    .primary(hasPrimary ? req.isPrimary() : i == 0)
                    .sortOrder(req.getSortOrder())
                    .build());
        }
        product.getImages().addAll(images);
    }

    private Specification<Product> activeSpec() {
        return (root, query, cb) -> cb.isTrue(root.get("active"));
    }

    private Product findEntity(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    private Category findCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));
    }

    private Brand resolveBrand(Long brandId) {
        if (brandId == null) {
            return null;
        }
        return brandRepository.findById(brandId)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + brandId));
    }

    private String resolveSlug(String name, String requestedSlug) {
        String base = (requestedSlug == null || requestedSlug.isBlank()) ? name : requestedSlug;
        return SlugUtil.toSlug(base);
    }
}
