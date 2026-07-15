package com.suprith.ecommerce.mapper;

import org.springframework.stereotype.Component;

import com.suprith.ecommerce.dto.ReviewResponse;
import com.suprith.ecommerce.entity.Review;

@Component
public class ReviewMapper {

    public ReviewResponse toResponse(Review review, boolean likedByCurrentUser) {
        return ReviewResponse.builder()
                .id(review.getId())
                .productId(review.getProduct().getId())
                .productName(review.getProduct().getName())
                .userId(review.getUser().getId())
                .userName(review.getUser().getName())
                .rating(review.getRating())
                .title(review.getTitle())
                .comment(review.getComment())
                .imageUrls(review.getImageUrls())
                .likeCount(review.getLikeCount())
                .likedByCurrentUser(likedByCurrentUser)
                .edited(review.isEdited())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}
