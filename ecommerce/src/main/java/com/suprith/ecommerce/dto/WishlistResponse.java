package com.suprith.ecommerce.dto;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WishlistResponse {

    private List<WishlistItemResponse> items;
    private int itemCount;
}
