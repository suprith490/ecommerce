package com.suprith.ecommerce.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.suprith.ecommerce.dto.ReviewRequest;
import com.suprith.ecommerce.dto.ReviewResponse;
import com.suprith.ecommerce.entity.Product;
import com.suprith.ecommerce.entity.Review;
import com.suprith.ecommerce.entity.ReviewLike;
import com.suprith.ecommerce.entity.User;
import com.suprith.ecommerce.exception.DuplicateResourceException;
import com.suprith.ecommerce.exception.ResourceNotFoundException;
import com.suprith.ecommerce.mapper.ReviewMapper;
import com.suprith.ecommerce.repository.ProductRepository;
import com.suprith.ecommerce.repository.ReviewLikeRepository;
import com.suprith.ecommerce.repository.ReviewRepository;
import com.suprith.ecommerce.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewLikeRepository reviewLikeRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ReviewMapper reviewMapper;

    @Override
    public ReviewResponse addReview(Long productId, Long userId, ReviewRequest request) {
        if (reviewRepository.existsByProductIdAndUserId(productId, userId)) {
            throw new DuplicateResourceException("You have already reviewed this product. Edit your existing review instead.");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Review review = Review.builder()
                .product(product)
                .user(user)
                .rating(request.getRating())
                .title(request.getTitle())
                .comment(request.getComment())
                .imageUrls(request.getImageUrls() != null ? request.getImageUrls() : java.util.List.of())
                .build();

        Review saved = reviewRepository.save(review);
        recalculateProductRating(productId);

        return reviewMapper.toResponse(saved, false);
    }

    @Override
    public ReviewResponse updateReview(Long reviewId, Long userId, ReviewRequest request) {
        Review review = findEntity(reviewId);

        if (!review.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You can only edit your own review");
        }

        review.setRating(request.getRating());
        review.setTitle(request.getTitle());
        review.setComment(request.getComment());
        review.setImageUrls(request.getImageUrls() != null ? request.getImageUrls() : java.util.List.of());
        review.setEdited(true);

        Review saved = reviewRepository.save(review);
        recalculateProductRating(review.getProduct().getId());

        boolean liked = reviewLikeRepository.existsByReviewIdAndUserId(reviewId, userId);
        return reviewMapper.toResponse(saved, liked);
    }

    @Override
    public void deleteReview(Long reviewId, Long userId, boolean isAdmin) {
        Review review = findEntity(reviewId);

        if (!isAdmin && !review.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You can only delete your own review");
        }

        Long productId = review.getProduct().getId();
        reviewRepository.delete(review);
        recalculateProductRating(productId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviewsForProduct(Long productId, Long currentUserId, Pageable pageable) {
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId, pageable)
                .map(review -> {
                    boolean liked = currentUserId != null
                            && reviewLikeRepository.existsByReviewIdAndUserId(review.getId(), currentUserId);
                    return reviewMapper.toResponse(review, liked);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getAllForAdmin(Pageable pageable) {
        return reviewRepository.findAll(pageable).map(review -> reviewMapper.toResponse(review, false));
    }

    @Override
    public ReviewResponse toggleLike(Long reviewId, Long userId) {
        Review review = findEntity(reviewId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        boolean alreadyLiked = reviewLikeRepository.existsByReviewIdAndUserId(reviewId, userId);

        if (alreadyLiked) {
            reviewLikeRepository.deleteByReviewIdAndUserId(reviewId, userId);
            review.setLikeCount(Math.max(0, review.getLikeCount() - 1));
        } else {
            reviewLikeRepository.save(ReviewLike.builder().review(review).user(user).build());
            review.setLikeCount(review.getLikeCount() + 1);
        }

        Review saved = reviewRepository.save(review);
        return reviewMapper.toResponse(saved, !alreadyLiked);
    }

    private void recalculateProductRating(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
        double average = reviewRepository.findAverageRatingByProductId(productId);
        long count = reviewRepository.countByProductId(productId);

        product.setAverageRating(Math.round(average * 10.0) / 10.0);
        product.setRatingCount((int) count);
        productRepository.save(product);
    }

    private Review findEntity(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));
    }
}
