package com.suprith.ecommerce.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.suprith.ecommerce.dto.AdminUserResponse;

public interface UserManagementService {

    Page<AdminUserResponse> getAllUsers(String search, Pageable pageable);

    AdminUserResponse getById(Long id);

    AdminUserResponse disableUser(Long targetUserId, Long requestingAdminId);

    AdminUserResponse enableUser(Long targetUserId);

    void deleteUser(Long targetUserId, Long requestingAdminId);
}
