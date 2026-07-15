package com.suprith.ecommerce.dto;

import com.suprith.ecommerce.enums.Role;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponse {

    private Long id;

    private String name;

    private String email;

    private Role role;

    private String profileImageUrl;
}