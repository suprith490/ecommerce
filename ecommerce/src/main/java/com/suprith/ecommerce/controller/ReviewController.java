package com.suprith.ecommerce.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.suprith.ecommerce.dto.ReviewRequest;
import com.suprith.ecommerce.dto.ReviewResponse;
import com.suprith.ecommerce.security.UserPrincipal;
import com.suprith.ecommerce.service.ReviewService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/api/products/{productId}/reviews")
    public ResponseEntity<Page<ReviewResponse>> getReviews(
            @PathVariable Long productId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Long currentUserId = principal != null ? principal.getId() : null;
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(reviewService.getReviewsForProduct(productId, currentUserId, pageable));
    }

    @PostMapping("/api/products/{productId}/reviews")
    public ResponseEntity<ReviewResponse> addReview(
            @PathVariable Long productId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ReviewRequest request) {

        ReviewResponse response = reviewService.addReview(productId, principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/api/reviews/{reviewId}")
    public ResponseEntity<ReviewResponse> updateReview(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ReviewRequest request) {

        return ResponseEntity.ok(reviewService.updateReview(reviewId, principal.getId(), request));
    }

    @DeleteMapping("/api/reviews/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal UserPrincipal principal) {

        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        reviewService.deleteReview(reviewId, principal.getId(), isAdmin);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/reviews/{reviewId}/like")
    public ResponseEntity<ReviewResponse> toggleLike(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(reviewService.toggleLike(reviewId, principal.getId()));
    }
}
