package com.microfocus.example.catalog.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microfocus.example.catalog.dto.ApiError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Simple API Key authentication filter for demo purposes.
 * Checks for X-API-KEY header and compares to a hard-coded demo key.
 */
@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(ApiKeyAuthFilter.class);
    private static final String API_KEY_HEADER = "X-API-KEY";
    private static final String DEMO_API_KEY = "demo-secret-key"; // Intentionally weak/hardcoded for demo

    private final ObjectMapper mapper;

    public ApiKeyAuthFilter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // Protect non-GET operations under /api/products (POST, PUT, DELETE)
        if (path.startsWith("/api/products") && !"GET".equalsIgnoreCase(method)) {
            String providedKey = request.getHeader(API_KEY_HEADER);
            if (providedKey == null || !DEMO_API_KEY.equals(providedKey)) {
                logger.warn("Missing or invalid API key for {} request to {}", method, path);

                ApiError error = new ApiError(
                        providedKey == null ? "API_KEY_MISSING" : "API_KEY_INVALID",
                        "Missing or invalid API key",
                        API_KEY_HEADER,
                        "Provide the X-API-KEY header with a valid API key. Contact the API owner to obtain a key.",
                        LocalDateTime.now()
                );

                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setContentType("application/json");
                response.getWriter().write(mapper.writeValueAsString(error));
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
