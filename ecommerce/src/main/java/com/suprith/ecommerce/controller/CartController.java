package com.suprith.ecommerce.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.suprith.ecommerce.dto.CartItemRequest;
import com.suprith.ecommerce.dto.CartResponse;
import com.suprith.ecommerce.dto.CouponApplyRequest;
import com.suprith.ecommerce.dto.CouponApplyResponse;
import com.suprith.ecommerce.security.UserPrincipal;
import com.suprith.ecommerce.service.CartService;
import com.suprith.ecommerce.service.CouponService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final CouponService couponService;

    @GetMapping
    public ResponseEntity<CartResponse> getCart(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(cartService.getCart(principal.getId()));
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItem(@AuthenticationPrincipal UserPrincipal principal,
                                                 @Valid @RequestBody CartItemRequest request) {
        return ResponseEntity.ok(cartService.addItem(principal.getId(), request));
    }

    @PatchMapping("/items/{productId}")
    public ResponseEntity<CartResponse> updateQuantity(@AuthenticationPrincipal UserPrincipal principal,
                                                        @PathVariable Long productId,
                                                        @RequestParam int quantity) {
        return ResponseEntity.ok(cartService.updateQuantity(principal.getId(), productId, quantity));
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<CartResponse> removeItem(@AuthenticationPrincipal UserPrincipal principal,
                                                    @PathVariable Long productId) {
        return ResponseEntity.ok(cartService.removeItem(principal.getId(), productId));
    }

    @PostMapping("/items/{productId}/move-to-wishlist")
    public ResponseEntity<CartResponse> moveToWishlist(@AuthenticationPrincipal UserPrincipal principal,
                                                        @PathVariable Long productId) {
        return ResponseEntity.ok(cartService.moveToWishlist(principal.getId(), productId));
    }

    @DeleteMapping
    public ResponseEntity<CartResponse> clearCart(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(cartService.clearCart(principal.getId()));
    }

    @PostMapping("/coupon/preview")
    public ResponseEntity<CouponApplyResponse> previewCoupon(@AuthenticationPrincipal UserPrincipal principal,
                                                              @Valid @RequestBody CouponApplyRequest request) {
        CartResponse cart = cartService.getCart(principal.getId());
        return ResponseEntity.ok(couponService.preview(request.getCode(), cart.getSubtotal()));
    }
}
