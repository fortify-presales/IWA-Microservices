package com.microfocus.example.customers.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request payload to validate a JWT token")
public class TokenValidationRequest {

    @Schema(description = "JWT token to validate", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String token;

    @Schema(description = "Username to validate against the token", example = "jdoe", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String username;

    public TokenValidationRequest() {
    }

    public TokenValidationRequest(String token, String username) {
        this.token = token;
        this.username = username;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
