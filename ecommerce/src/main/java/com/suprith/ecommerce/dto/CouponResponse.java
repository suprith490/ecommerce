package com.suprith.ecommerce.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.suprith.ecommerce.enums.DiscountType;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CouponResponse {

    private Long id;
    private String code;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal minOrderAmount;
    private BigDecimal maxDiscountAmount;
    private LocalDateTime expiryDate;
    private boolean active;
    private Integer usageLimit;
    private int usedCount;
}
