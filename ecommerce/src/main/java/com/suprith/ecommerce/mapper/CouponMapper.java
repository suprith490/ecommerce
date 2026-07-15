package com.suprith.ecommerce.mapper;

import org.springframework.stereotype.Component;

import com.suprith.ecommerce.dto.CouponResponse;
import com.suprith.ecommerce.entity.Coupon;

@Component
public class CouponMapper {

    public CouponResponse toResponse(Coupon coupon) {
        return CouponResponse.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .discountType(coupon.getDiscountType())
                .discountValue(coupon.getDiscountValue())
                .minOrderAmount(coupon.getMinOrderAmount())
                .maxDiscountAmount(coupon.getMaxDiscountAmount())
                .expiryDate(coupon.getExpiryDate())
                .active(coupon.isActive())
                .usageLimit(coupon.getUsageLimit())
                .usedCount(coupon.getUsedCount())
                .build();
    }
}
