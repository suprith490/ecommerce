package com.suprith.ecommerce.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.suprith.ecommerce.dto.ChangePasswordRequest;
import com.suprith.ecommerce.dto.UpdateProfileRequest;
import com.suprith.ecommerce.dto.UserResponse;
import com.suprith.ecommerce.security.UserPrincipal;
import com.suprith.ecommerce.service.ProfileService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @PutMapping
    public ResponseEntity<UserResponse> updateProfile(@AuthenticationPrincipal UserPrincipal principal,
                                                       @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(profileService.updateProfile(principal.getId(), request));
    }

    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal UserPrincipal principal,
                                                @Valid @RequestBody ChangePasswordRequest request) {
        profileService.changePassword(principal.getId(), request);
        return ResponseEntity.noContent().build();
    }
}
