package com.suprith.ecommerce.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.suprith.ecommerce.dto.CartItemRequest;
import com.suprith.ecommerce.dto.WishlistItemResponse;
import com.suprith.ecommerce.dto.WishlistResponse;
import com.suprith.ecommerce.entity.Product;
import com.suprith.ecommerce.entity.User;
import com.suprith.ecommerce.entity.WishlistItem;
import com.suprith.ecommerce.exception.DuplicateResourceException;
import com.suprith.ecommerce.exception.ResourceNotFoundException;
import com.suprith.ecommerce.mapper.ProductMapper;
import com.suprith.ecommerce.repository.ProductRepository;
import com.suprith.ecommerce.repository.UserRepository;
import com.suprith.ecommerce.repository.WishlistItemRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class WishlistServiceImpl implements WishlistService {

    private final WishlistItemRepository wishlistItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ProductMapper productMapper;
    private final CartService cartService;

    @Override
    @Transactional(readOnly = true)
    public WishlistResponse getWishlist(Long userId) {
        return buildResponse(userId);
    }

    @Override
    public WishlistResponse addItem(Long userId, Long productId) {
        if (wishlistItemRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new DuplicateResourceException("This product is already in your wishlist");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        wishlistItemRepository.save(WishlistItem.builder().user(user).product(product).build());
        return buildResponse(userId);
    }

    @Override
    public WishlistResponse removeItem(Long userId, Long productId) {
        wishlistItemRepository.deleteByUserIdAndProductId(userId, productId);
        return buildResponse(userId);
    }

    @Override
    public WishlistResponse moveToCart(Long userId, Long productId, int quantity) {
        WishlistItem item = wishlistItemRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("This product is not in your wishlist"));

        CartItemRequest cartRequest = new CartItemRequest();
        cartRequest.setProductId(productId);
        cartRequest.setQuantity(Math.max(quantity, 1));
        cartService.addItem(userId, cartRequest);

        wishlistItemRepository.delete(item);
        return buildResponse(userId);
    }

    private WishlistResponse buildResponse(Long userId) {
        List<WishlistItemResponse> items = wishlistItemRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(item -> WishlistItemResponse.builder()
                        .id(item.getId())
                        .product(productMapper.toSummary(item.getProduct()))
                        .addedAt(item.getCreatedAt())
                        .build())
                .toList();

        return WishlistResponse.builder()
                .items(items)
                .itemCount(items.size())
                .build();
    }
}
