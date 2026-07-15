package com.suprith.ecommerce.service;

import com.suprith.ecommerce.dto.WishlistResponse;

public interface WishlistService {

    WishlistResponse getWishlist(Long userId);

    WishlistResponse addItem(Long userId, Long productId);

    WishlistResponse removeItem(Long userId, Long productId);

    WishlistResponse moveToCart(Long userId, Long productId, int quantity);
}
