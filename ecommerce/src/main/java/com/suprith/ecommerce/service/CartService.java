package com.suprith.ecommerce.service;

import com.suprith.ecommerce.dto.CartItemRequest;
import com.suprith.ecommerce.dto.CartResponse;

public interface CartService {

    CartResponse getCart(Long userId);

    CartResponse addItem(Long userId, CartItemRequest request);

    CartResponse updateQuantity(Long userId, Long productId, int quantity);

    CartResponse removeItem(Long userId, Long productId);

    CartResponse clearCart(Long userId);

    CartResponse moveToWishlist(Long userId, Long productId);
}
