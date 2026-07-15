package com.suprith.ecommerce.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.suprith.ecommerce.dto.ChangePasswordRequest;
import com.suprith.ecommerce.dto.UpdateProfileRequest;
import com.suprith.ecommerce.dto.UserResponse;
import com.suprith.ecommerce.entity.User;
import com.suprith.ecommerce.exception.InvalidCredentialsException;
import com.suprith.ecommerce.exception.ResourceNotFoundException;
import com.suprith.ecommerce.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ProfileServiceImpl implements ProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findUser(userId);
        user.setName(request.getName());
        user.setProfileImageUrl(request.getProfileImageUrl());
        User saved = userRepository.save(user);

        return UserResponse.builder()
                .id(saved.getId())
                .name(saved.getName())
                .email(saved.getEmail())
                .role(saved.getRole())
                .profileImageUrl(saved.getProfileImageUrl())
                .build();
    }

    @Override
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = findUser(userId);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }
}
