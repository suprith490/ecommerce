package com.suprith.ecommerce.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReviewResponse {

    private Long id;
    private Long productId;
    private String productName;
    private Long userId;
    private String userName;
    private int rating;
    private String title;
    private String comment;
    private List<String> imageUrls;
    private int likeCount;
    private boolean likedByCurrentUser;
    private boolean edited;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
