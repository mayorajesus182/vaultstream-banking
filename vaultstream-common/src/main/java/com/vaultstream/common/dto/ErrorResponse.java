package com.vaultstream.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**
 * Standardized error response for all VaultStream APIs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "ErrorResponse", description = "Standard error response")
public class ErrorResponse {

    @Schema(description = "Timestamp of the error", example = "2026-01-20T10:05:00Z")
    private Instant timestamp;

    @Schema(description = "HTTP status code", example = "400")
    private int status;

    @Schema(description = "HTTP status reason", example = "Bad Request")
    private String error;

    @Schema(description = "Platform specific error code", example = "INVALID_EMAIL")
    private String errorCode;

    @Schema(description = "Detailed error message", example = "The email format is invalid")
    private String message;

    @Schema(description = "Request path", example = "/api/v1/customers")
    private String path;

    @Schema(description = "Unique correlation ID for tracing", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
    private String correlationId;

    @Schema(description = "List of validation errors (optional)")
    private List<FieldError> fieldErrors;

    /**
     * Field-level validation error
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "FieldError", description = "Validation error details")
    public static class FieldError {
        @Schema(description = "Field name", example = "email")
        private String field;

        @Schema(description = "Error message", example = "must be a well-formed email address")
        private String message;

        @Schema(description = "Rejected value", example = "invalid-email")
        private Object rejectedValue;
    }

    public static ErrorResponse of(int status, String error, String errorCode, String message, String path,
            String correlationId) {
        return ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status)
                .error(error)
                .errorCode(errorCode)
                .message(message)
                .path(path)
                .correlationId(correlationId)
                .build();
    }
}
