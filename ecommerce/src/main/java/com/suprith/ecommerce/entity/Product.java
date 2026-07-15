package com.suprith.ecommerce.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false, unique = true)
    private String sku;

    @Column(length = 4000)
    private String description;

    @Column(length = 4000)
    private String specifications;

    /** Base listing price (MRP). */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    /** Discounted selling price. Null/equal to price means no active offer. */
    @Column(precision = 12, scale = 2)
    private BigDecimal offerPrice;

    @Builder.Default
    @Column(nullable = false)
    private int stock = 0;

    @Builder.Default
    @Column(nullable = false)
    private int lowStockThreshold = 5;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Builder.Default
    @Column(nullable = false)
    private double averageRating = 0.0;

    @Builder.Default
    @Column(nullable = false)
    private int ratingCount = 0;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<ProductImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @Builder.Default
    private List<Review> reviews = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Transient
    public BigDecimal getEffectivePrice() {
        return (offerPrice != null && offerPrice.compareTo(price) < 0) ? offerPrice : price;
    }

    @Transient
    public int getDiscountPercentage() {
        if (offerPrice == null || offerPrice.compareTo(price) >= 0 || price.signum() == 0) {
            return 0;
        }
        BigDecimal diff = price.subtract(offerPrice);
        return diff.multiply(BigDecimal.valueOf(100))
                .divide(price, 0, java.math.RoundingMode.HALF_UP)
                .intValue();
    }

    @Transient
    public boolean isInStock() {
        return stock > 0;
    }

    @Transient
    public boolean isLowStock() {
        return stock > 0 && stock <= lowStockThreshold;
    }
}
