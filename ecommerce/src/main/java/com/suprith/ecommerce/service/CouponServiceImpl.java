package com.suprith.ecommerce.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.suprith.ecommerce.dto.CouponApplyResponse;
import com.suprith.ecommerce.dto.CouponRequest;
import com.suprith.ecommerce.dto.CouponResponse;
import com.suprith.ecommerce.entity.Coupon;
import com.suprith.ecommerce.enums.DiscountType;
import com.suprith.ecommerce.exception.DuplicateResourceException;
import com.suprith.ecommerce.exception.InvalidCouponException;
import com.suprith.ecommerce.exception.ResourceNotFoundException;
import com.suprith.ecommerce.mapper.CouponMapper;
import com.suprith.ecommerce.repository.CouponRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;
    private final CouponMapper couponMapper;

    @Override
    public CouponResponse create(CouponRequest request) {
        String code = request.getCode().trim().toUpperCase();
        if (couponRepository.existsByCodeIgnoreCase(code)) {
            throw new DuplicateResourceException("A coupon with code '" + code + "' already exists");
        }

        Coupon coupon = Coupon.builder()
                .code(code)
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .minOrderAmount(request.getMinOrderAmount() != null ? request.getMinOrderAmount() : BigDecimal.ZERO)
                .maxDiscountAmount(request.getMaxDiscountAmount())
                .expiryDate(request.getExpiryDate())
                .active(request.getActive() == null || request.getActive())
                .usageLimit(request.getUsageLimit())
                .build();

        return couponMapper.toResponse(couponRepository.save(coupon));
    }

    @Override
    public CouponResponse update(Long id, CouponRequest request) {
        Coupon coupon = findEntity(id);

        String code = request.getCode().trim().toUpperCase();
        if (!code.equals(coupon.getCode()) && couponRepository.existsByCodeIgnoreCase(code)) {
            throw new DuplicateResourceException("A coupon with code '" + code + "' already exists");
        }

        coupon.setCode(code);
        coupon.setDiscountType(request.getDiscountType());
        coupon.setDiscountValue(request.getDiscountValue());
        coupon.setMinOrderAmount(request.getMinOrderAmount() != null ? request.getMinOrderAmount() : BigDecimal.ZERO);
        coupon.setMaxDiscountAmount(request.getMaxDiscountAmount());
        coupon.setExpiryDate(request.getExpiryDate());
        coupon.setUsageLimit(request.getUsageLimit());
        if (request.getActive() != null) {
            coupon.setActive(request.getActive());
        }

        return couponMapper.toResponse(couponRepository.save(coupon));
    }

    @Override
    public void delete(Long id) {
        couponRepository.delete(findEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public CouponResponse getById(Long id) {
        return couponMapper.toResponse(findEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CouponResponse> getAll() {
        return couponRepository.findAll().stream().map(couponMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CouponApplyResponse preview(String code, BigDecimal subtotal) {
        Coupon coupon = validateForCheckout(code, subtotal);
        BigDecimal discount = calculateDiscount(coupon, subtotal);
        BigDecimal newTotal = subtotal.subtract(discount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        return CouponApplyResponse.builder()
                .code(coupon.getCode())
                .discountAmount(discount)
                .newTotal(newTotal)
                .message("Coupon applied! You saved \u20B9" + discount)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Coupon validateForCheckout(String code, BigDecimal subtotal) {
        Coupon coupon = couponRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new InvalidCouponException("Invalid coupon code"));

        if (!coupon.isActive()) {
            throw new InvalidCouponException("This coupon is no longer active");
        }
        if (coupon.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new InvalidCouponException("This coupon has expired");
        }
        if (subtotal.compareTo(coupon.getMinOrderAmount()) < 0) {
            throw new InvalidCouponException("This coupon requires a minimum order of \u20B9" + coupon.getMinOrderAmount());
        }
        if (coupon.getUsageLimit() != null && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            throw new InvalidCouponException("This coupon has reached its usage limit");
        }

        return coupon;
    }

    @Override
    public BigDecimal calculateDiscount(Coupon coupon, BigDecimal subtotal) {
        BigDecimal discount;
        if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {
            discount = subtotal.multiply(coupon.getDiscountValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            if (coupon.getMaxDiscountAmount() != null && discount.compareTo(coupon.getMaxDiscountAmount()) > 0) {
                discount = coupon.getMaxDiscountAmount();
            }
        } else {
            discount = coupon.getDiscountValue();
        }
        // Never let the discount exceed the subtotal itself.
        return discount.min(subtotal).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public void recordUsage(Coupon coupon) {
        coupon.setUsedCount(coupon.getUsedCount() + 1);
        couponRepository.save(coupon);
    }

    private Coupon findEntity(Long id) {
        return couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found with id: " + id));
    }
}
