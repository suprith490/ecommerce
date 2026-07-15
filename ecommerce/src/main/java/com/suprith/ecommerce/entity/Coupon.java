package com.suprith.ecommerce.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.suprith.ecommerce.enums.DiscountType;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "coupons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiscountType discountType;

    /** Percentage (e.g. 10 = 10%) or flat rupee amount, depending on discountType. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal discountValue;

    @Builder.Default
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal minOrderAmount = BigDecimal.ZERO;

    /** Only relevant for PERCENTAGE coupons — caps the discount amount. Null = uncapped. */
    @Column(precision = 12, scale = 2)
    private BigDecimal maxDiscountAmount;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    /** Null = unlimited total uses across all customers. */
    private Integer usageLimit;

    @Builder.Default
    @Column(nullable = false)
    private int usedCount = 0;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
