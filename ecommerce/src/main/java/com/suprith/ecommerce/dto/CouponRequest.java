package com.suprith.ecommerce.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.suprith.ecommerce.enums.DiscountType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CouponRequest {

    @NotBlank(message = "Coupon code is required")
    private String code;

    @NotNull(message = "Discount type is required")
    private DiscountType discountType;

    @NotNull(message = "Discount value is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Discount value must be greater than 0")
    private BigDecimal discountValue;

    @DecimalMin(value = "0.0", message = "Minimum order amount cannot be negative")
    private BigDecimal minOrderAmount;

    private BigDecimal maxDiscountAmount;

    @NotNull(message = "Expiry date is required")
    private LocalDateTime expiryDate;

    private Boolean active;

    private Integer usageLimit;
}
