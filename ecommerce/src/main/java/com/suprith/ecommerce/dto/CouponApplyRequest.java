package com.suprith.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CouponApplyRequest {

    @NotBlank(message = "Coupon code is required")
    private String code;
}
