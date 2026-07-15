package com.suprith.ecommerce.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.suprith.ecommerce.dto.ReviewRequest;
import com.suprith.ecommerce.dto.ReviewResponse;

public interface ReviewService {

    ReviewResponse addReview(Long productId, Long userId, ReviewRequest request);

    ReviewResponse updateReview(Long reviewId, Long userId, ReviewRequest request);

    void deleteReview(Long reviewId, Long userId, boolean isAdmin);

    Page<ReviewResponse> getReviewsForProduct(Long productId, Long currentUserId, Pageable pageable);

    Page<ReviewResponse> getAllForAdmin(Pageable pageable);

    ReviewResponse toggleLike(Long reviewId, Long userId);
}
