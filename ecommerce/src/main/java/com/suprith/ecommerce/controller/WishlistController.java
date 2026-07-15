package com.suprith.ecommerce.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.suprith.ecommerce.dto.WishlistResponse;
import com.suprith.ecommerce.security.UserPrincipal;
import com.suprith.ecommerce.service.WishlistService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    public ResponseEntity<WishlistResponse> getWishlist(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(wishlistService.getWishlist(principal.getId()));
    }

    @PostMapping("/items/{productId}")
    public ResponseEntity<WishlistResponse> addItem(@AuthenticationPrincipal UserPrincipal principal,
                                                     @PathVariable Long productId) {
        return ResponseEntity.ok(wishlistService.addItem(principal.getId(), productId));
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<WishlistResponse> removeItem(@AuthenticationPrincipal UserPrincipal principal,
                                                        @PathVariable Long productId) {
        return ResponseEntity.ok(wishlistService.removeItem(principal.getId(), productId));
    }

    @PostMapping("/items/{productId}/move-to-cart")
    public ResponseEntity<WishlistResponse> moveToCart(@AuthenticationPrincipal UserPrincipal principal,
                                                        @PathVariable Long productId,
                                                        @RequestParam(defaultValue = "1") int quantity) {
        return ResponseEntity.ok(wishlistService.moveToCart(principal.getId(), productId, quantity));
    }
}
