package com.suprith.ecommerce.dto;

import java.time.LocalDateTime;

import com.suprith.ecommerce.enums.Role;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminUserResponse {

    private Long id;
    private String name;
    private String email;
    private Role role;
    private boolean enabled;
    private LocalDateTime createdAt;
}
