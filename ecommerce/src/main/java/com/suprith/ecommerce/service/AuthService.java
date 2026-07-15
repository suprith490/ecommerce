package com.suprith.ecommerce.service;

import com.suprith.ecommerce.dto.ForgotPasswordRequest;
import com.suprith.ecommerce.dto.LoginRequest;
import com.suprith.ecommerce.dto.RegisterRequest;
import com.suprith.ecommerce.dto.ResetPasswordRequest;
import com.suprith.ecommerce.dto.UserResponse;

import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {

    UserResponse register(RegisterRequest request);

    UserResponse login(LoginRequest request, HttpServletResponse response);

    void logout(HttpServletResponse response);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);
}
