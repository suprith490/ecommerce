package com.suprith.ecommerce.service;

import java.math.BigDecimal;
import java.util.List;

import com.suprith.ecommerce.dto.CouponApplyResponse;
import com.suprith.ecommerce.dto.CouponRequest;
import com.suprith.ecommerce.dto.CouponResponse;
import com.suprith.ecommerce.entity.Coupon;

public interface CouponService {

    CouponResponse create(CouponRequest request);

    CouponResponse update(Long id, CouponRequest request);

    void delete(Long id);

    CouponResponse getById(Long id);

    List<CouponResponse> getAll();

    /** Validates the coupon against the given subtotal and returns it with the computed discount, without persisting anything. */
    CouponApplyResponse preview(String code, BigDecimal subtotal);

    /** Throws if invalid; returns the entity + discount amount for use during order placement. */
    Coupon validateForCheckout(String code, BigDecimal subtotal);

    BigDecimal calculateDiscount(Coupon coupon, BigDecimal subtotal);

    void recordUsage(Coupon coupon);
}
