package com.appcompras.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Set;

@Component
public class ApiVersionFilter extends OncePerRequestFilter {

    private static final Set<String> SUPPORTED_VERSIONS = Set.of("1", "v1");
    private static final String VERSION_HEADER = "X-API-Version";

    private final ObjectMapper objectMapper;

    public ApiVersionFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String version = request.getHeader(VERSION_HEADER);
        if (version == null || version.isBlank() || SUPPORTED_VERSIONS.contains(version.trim().toLowerCase())) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.setContentType("application/json");
        String requestId = (String) request.getAttribute("requestId");
        ApiError body = new ApiError(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "UNSUPPORTED_API_VERSION",
                "Unsupported API version. Use X-API-Version: 1",
                request.getRequestURI(),
                requestId
        );
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
