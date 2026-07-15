package com.suprith.ecommerce.dto;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CouponApplyResponse {

    private String code;
    private BigDecimal discountAmount;
    private BigDecimal newTotal;
    private String message;
}
