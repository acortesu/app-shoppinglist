package com.appcompras.security;

import com.appcompras.config.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

@Component
public class SecurityErrorHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public SecurityErrorHandlers(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        writeError(response, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication required", request);
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        writeError(response, HttpStatus.FORBIDDEN, "FORBIDDEN", "Insufficient permissions", request);
    }

    private void writeError(
            HttpServletResponse response,
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request
    ) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        String requestId = (String) request.getAttribute("requestId");
        ApiError body = new ApiError(Instant.now(), status.value(), code, message, request.getRequestURI(), requestId);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
