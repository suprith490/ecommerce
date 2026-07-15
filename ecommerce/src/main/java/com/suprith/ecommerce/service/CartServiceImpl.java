package com.suprith.ecommerce.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.suprith.ecommerce.constants.PricingConstants;
import com.suprith.ecommerce.dto.CartItemRequest;
import com.suprith.ecommerce.dto.CartItemResponse;
import com.suprith.ecommerce.dto.CartResponse;
import com.suprith.ecommerce.entity.CartItem;
import com.suprith.ecommerce.entity.Product;
import com.suprith.ecommerce.entity.User;
import com.suprith.ecommerce.entity.WishlistItem;
import com.suprith.ecommerce.exception.InsufficientStockException;
import com.suprith.ecommerce.exception.ResourceNotFoundException;
import com.suprith.ecommerce.mapper.ProductMapper;
import com.suprith.ecommerce.repository.CartItemRepository;
import com.suprith.ecommerce.repository.ProductRepository;
import com.suprith.ecommerce.repository.UserRepository;
import com.suprith.ecommerce.repository.WishlistItemRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CartServiceImpl implements CartService {

    private final CartItemRepository cartItemRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ProductMapper productMapper;

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCart(Long userId) {
        return buildCartResponse(userId);
    }

    @Override
    public CartResponse addItem(Long userId, CartItemRequest request) {
        Product product = findProduct(request.getProductId());
        User user = findUser(userId);

        CartItem item = cartItemRepository.findByUserIdAndProductId(userId, request.getProductId())
                .orElseGet(() -> CartItem.builder().user(user).product(product).quantity(0).build());

        int newQuantity = item.getQuantity() + request.getQuantity();
        validateStock(product, newQuantity);

        item.setQuantity(newQuantity);
        cartItemRepository.save(item);

        return buildCartResponse(userId);
    }

    @Override
    public CartResponse updateQuantity(Long userId, Long productId, int quantity) {
        if (quantity <= 0) {
            return removeItem(userId, productId);
        }

        CartItem item = cartItemRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("This product is not in your cart"));

        validateStock(item.getProduct(), quantity);
        item.setQuantity(quantity);
        cartItemRepository.save(item);

        return buildCartResponse(userId);
    }

    @Override
    public CartResponse removeItem(Long userId, Long productId) {
        cartItemRepository.deleteByUserIdAndProductId(userId, productId);
        return buildCartResponse(userId);
    }

    @Override
    public CartResponse clearCart(Long userId) {
        cartItemRepository.deleteByUserId(userId);
        return buildCartResponse(userId);
    }

    @Override
    public CartResponse moveToWishlist(Long userId, Long productId) {
        CartItem item = cartItemRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("This product is not in your cart"));

        if (!wishlistItemRepository.existsByUserIdAndProductId(userId, productId)) {
            wishlistItemRepository.save(WishlistItem.builder()
                    .user(item.getUser())
                    .product(item.getProduct())
                    .build());
        }

        cartItemRepository.delete(item);
        return buildCartResponse(userId);
    }

    private CartResponse buildCartResponse(Long userId) {
        List<CartItem> cartItems = cartItemRepository.findByUserIdOrderByCreatedAtDesc(userId);

        List<CartItemResponse> itemResponses = cartItems.stream()
                .map(this::toItemResponse)
                .toList();

        BigDecimal subtotal = itemResponses.stream()
                .map(CartItemResponse::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal shipping = (subtotal.signum() > 0 && subtotal.compareTo(PricingConstants.FREE_SHIPPING_THRESHOLD) < 0)
                ? PricingConstants.FLAT_SHIPPING_CHARGE
                : BigDecimal.ZERO;

        BigDecimal gst = subtotal.multiply(PricingConstants.GST_RATE).setScale(2, RoundingMode.HALF_UP);

        BigDecimal total = subtotal.add(shipping).add(gst).setScale(2, RoundingMode.HALF_UP);

        BigDecimal amountToFreeShipping = subtotal.signum() > 0
                ? PricingConstants.FREE_SHIPPING_THRESHOLD.subtract(subtotal).max(BigDecimal.ZERO)
                : BigDecimal.ZERO;

        return CartResponse.builder()
                .items(itemResponses)
                .itemCount(itemResponses.size())
                .subtotal(subtotal)
                .shippingCharge(shipping)
                .gst(gst)
                .total(total)
                .amountToFreeShipping(amountToFreeShipping)
                .build();
    }

    private CartItemResponse toItemResponse(CartItem item) {
        Product product = item.getProduct();
        BigDecimal unitPrice = product.getEffectivePrice();
        BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity())).setScale(2, RoundingMode.HALF_UP);

        return CartItemResponse.builder()
                .id(item.getId())
                .product(productMapper.toSummary(product))
                .quantity(item.getQuantity())
                .unitPrice(unitPrice)
                .lineTotal(lineTotal)
                .availableStock(product.getStock())
                .inStock(product.isInStock())
                .quantityExceedsStock(item.getQuantity() > product.getStock())
                .build();
    }

    private void validateStock(Product product, int requestedQuantity) {
        if (requestedQuantity > product.getStock()) {
            throw new InsufficientStockException(
                    "Only " + product.getStock() + " unit(s) of \"" + product.getName() + "\" left in stock");
        }
    }

    private Product findProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }
}
