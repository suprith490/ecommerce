package com.suprith.ecommerce.service;

import com.suprith.ecommerce.dto.ChangePasswordRequest;
import com.suprith.ecommerce.dto.UpdateProfileRequest;
import com.suprith.ecommerce.dto.UserResponse;

public interface ProfileService {

    UserResponse updateProfile(Long userId, UpdateProfileRequest request);

    void changePassword(Long userId, ChangePasswordRequest request);
}
