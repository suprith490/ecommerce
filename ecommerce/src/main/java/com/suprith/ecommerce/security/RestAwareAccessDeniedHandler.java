package com.suprith.ecommerce.security;

import java.io.IOException;
import java.time.LocalDateTime;

import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suprith.ecommerce.dto.ApiErrorResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RestAwareAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                        AccessDeniedException accessDeniedException) throws IOException {

        if (request.getRequestURI().startsWith("/api/")) {
            ApiErrorResponse body = ApiErrorResponse.builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpServletResponse.SC_FORBIDDEN)
                    .error("Forbidden")
                    .message("You do not have permission to perform this action")
                    .path(request.getRequestURI())
                    .build();

            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(objectMapper.writeValueAsString(body));
            return;
        }

        // Module 8 added a real error/403.html — dispatch to it via the standard
        // container error mechanism instead of redirecting away from the problem.
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
    }
}
