package com.suprith.ecommerce.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.suprith.ecommerce.dto.ForgotPasswordRequest;
import com.suprith.ecommerce.dto.LoginRequest;
import com.suprith.ecommerce.dto.RegisterRequest;
import com.suprith.ecommerce.dto.ResetPasswordRequest;
import com.suprith.ecommerce.dto.UserResponse;
import com.suprith.ecommerce.enums.Role;
import com.suprith.ecommerce.security.UserPrincipal;
import com.suprith.ecommerce.service.AuthService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletResponse response) {
        UserResponse userResponse = authService.login(request, response);
        return ResponseEntity.ok(userResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        authService.logout(response);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        // Deliberately generic message so this endpoint can't be used to enumerate registered emails.
        return ResponseEntity.ok(Map.of("message",
                "If an account exists for that email, a password reset link has been generated."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(Map.of("message", "Password has been reset successfully."));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserResponse response = UserResponse.builder()
                .id(principal.getId())
                .name(principal.getName())
                .email(principal.getEmail())
                .role(Role.valueOf(principal.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "")))
                .profileImageUrl(principal.getProfileImageUrl())
                .build();

        return ResponseEntity.ok(response);
    }
}
