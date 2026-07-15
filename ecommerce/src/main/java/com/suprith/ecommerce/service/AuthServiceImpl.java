package com.suprith.ecommerce.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.suprith.ecommerce.dto.ForgotPasswordRequest;
import com.suprith.ecommerce.dto.LoginRequest;
import com.suprith.ecommerce.dto.RegisterRequest;
import com.suprith.ecommerce.dto.ResetPasswordRequest;
import com.suprith.ecommerce.dto.UserResponse;
import com.suprith.ecommerce.entity.PasswordResetToken;
import com.suprith.ecommerce.entity.User;
import com.suprith.ecommerce.enums.Role;
import com.suprith.ecommerce.exception.EmailAlreadyExistsException;
import com.suprith.ecommerce.exception.InvalidCredentialsException;
import com.suprith.ecommerce.exception.InvalidOrExpiredTokenException;
import com.suprith.ecommerce.exception.UserNotFoundException;
import com.suprith.ecommerce.repository.PasswordResetTokenRepository;
import com.suprith.ecommerce.repository.UserRepository;
import com.suprith.ecommerce.security.JwtService;
import com.suprith.ecommerce.security.UserPrincipal;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${app.jwt.cookie-name}")
    private String cookieName;

    @Override
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("An account with email " + request.getEmail() + " already exists");
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.CUSTOMER)
                .enabled(true)
                .build();

        User saved = userRepository.save(user);
        return toUserResponse(saved);
    }

    @Override
    public UserResponse login(LoginRequest request, HttpServletResponse response) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!user.isEnabled()) {
            throw new InvalidCredentialsException("This account has been disabled. Please contact support.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        UserPrincipal principal = new UserPrincipal(user);
        String token = jwtService.generateToken(principal, request.isRememberMe());
        long maxAge = jwtService.resolveMaxAgeSeconds(request.isRememberMe());

        ResponseCookie cookie = ResponseCookie.from(cookieName, token)
                .httpOnly(true)
                .secure(false) // flip to true once the app is served over HTTPS
                .path("/")
                .maxAge(maxAge)
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return toUserResponse(user);
    }

    @Override
    public void logout(HttpServletResponse response) {
        ResponseCookie expiredCookie = ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, expiredCookie.toString());
    }

    @Override
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException("No account found with that email"));

        resetTokenRepository.deleteByUser(user);

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiryDate(LocalDateTime.now().plusMinutes(30))
                .build();

        resetTokenRepository.save(resetToken);

        // No SMTP server is wired up yet, so the token is logged for local development.
        // Once an email provider is configured, send resetToken.getToken() as a reset link instead.
        log.info("Password reset requested for {} — token: {}", user.getEmail(), resetToken.getToken());
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = resetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new InvalidOrExpiredTokenException("Invalid or expired reset token"));

        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            resetTokenRepository.delete(resetToken);
            throw new InvalidOrExpiredTokenException("This reset link has expired. Please request a new one.");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        resetTokenRepository.delete(resetToken);
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .profileImageUrl(user.getProfileImageUrl())
                .build();
    }
}
