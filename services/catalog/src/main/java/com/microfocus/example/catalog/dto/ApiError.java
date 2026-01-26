package com.microfocus.example.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "API error response")
public class ApiError {

    @Schema(description = "Short error code", example = "API_KEY_MISSING")
    private String code;

    @Schema(description = "Human-readable error message", example = "Missing or invalid API key")
    private String message;

    @Schema(description = "Header required for this API", example = "X-API-KEY")
    private String requiredHeader;

    @Schema(description = "Additional details or remediation guidance", example = "Provide X-API-KEY header with a valid API key. Contact admin@example.com for provisioning.")
    private String details;

    @Schema(description = "Timestamp when the error occurred")
    private LocalDateTime timestamp;

    public ApiError() {}

    public ApiError(String code, String message, String requiredHeader, String details, LocalDateTime timestamp) {
        this.code = code;
        this.message = message;
        this.requiredHeader = requiredHeader;
        this.details = details;
        this.timestamp = timestamp;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRequiredHeader() {
        return requiredHeader;
    }

    public void setRequiredHeader(String requiredHeader) {
        this.requiredHeader = requiredHeader;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}

