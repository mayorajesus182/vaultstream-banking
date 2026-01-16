package com.vaultstream.customer.infrastructure.rest.exception;

import com.vaultstream.common.dto.ErrorResponse;
import com.vaultstream.common.exception.BusinessRuleViolationException;
import com.vaultstream.common.exception.ResourceNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Global exception handler for REST endpoints.
 */
@Slf4j
@Provider
public class GlobalExceptionHandler implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception exception) {

        if (exception instanceof ResourceNotFoundException ex) {
            log.warn("⚠️ Resource not found: {}", ex.getMessage());
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(ErrorResponse.of(
                            404,
                            "Not Found",
                            ex.getErrorCode(),
                            ex.getMessage(),
                            null))
                    .build();
        }

        if (exception instanceof BusinessRuleViolationException ex) {
            log.warn("⚠️ Business rule violation: {}", ex.getMessage());
            int status = ex.getErrorCode().startsWith("DUPLICATE") ? 409 : 400;
            return Response.status(status)
                    .entity(ErrorResponse.of(
                            status,
                            status == 409 ? "Conflict" : "Bad Request",
                            ex.getErrorCode(),
                            ex.getMessage(),
                            null))
                    .build();
        }

        if (exception instanceof ConstraintViolationException ex) {
            log.warn("⚠️ Validation error: {}", ex.getMessage());
            List<ErrorResponse.FieldError> fieldErrors = ex.getConstraintViolations().stream()
                    .map(this::toFieldError)
                    .toList();

            ErrorResponse response = ErrorResponse.builder()
                    .status(400)
                    .error("Bad Request")
                    .errorCode("VALIDATION_ERROR")
                    .message("Validation failed")
                    .fieldErrors(fieldErrors)
                    .build();

            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(response)
                    .build();
        }

        if (exception instanceof IllegalArgumentException ex) {
            log.warn("⚠️ Invalid argument: {}", ex.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponse.of(
                            400,
                            "Bad Request",
                            "INVALID_ARGUMENT",
                            ex.getMessage(),
                            null))
                    .build();
        }

        // Unexpected error
        log.error("❌ Unexpected error", exception);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(ErrorResponse.of(
                        500,
                        "Internal Server Error",
                        "INTERNAL_ERROR",
                        "An unexpected error occurred",
                        null))
                .build();
    }

    private ErrorResponse.FieldError toFieldError(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        // Remove method name prefix (e.g., "createCustomer.arg0.firstName" ->
        // "firstName")
        if (path.contains(".")) {
            path = path.substring(path.lastIndexOf('.') + 1);
        }
        return ErrorResponse.FieldError.builder()
                .field(path)
                .message(violation.getMessage())
                .rejectedValue(violation.getInvalidValue())
                .build();
    }
}
