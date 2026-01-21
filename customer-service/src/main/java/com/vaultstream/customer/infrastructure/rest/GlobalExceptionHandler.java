package com.vaultstream.customer.infrastructure.rest;

import com.vaultstream.common.dto.ErrorResponse;
import com.vaultstream.common.exception.BusinessRuleViolationException;
import com.vaultstream.common.exception.ConcurrencyException;
import com.vaultstream.common.exception.InvalidEmailException;
import com.vaultstream.common.exception.InvalidPhoneNumberException;
import com.vaultstream.common.exception.InvalidStatusTransitionException;
import com.vaultstream.common.exception.MinimumAgeRequiredException;
import com.vaultstream.common.exception.ResourceNotFoundException;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Global exception handler for all REST endpoints.
 * 
 * Provides consistent error responses across the application
 * and handles correlation IDs for distributed tracing.
 */
@Slf4j
@Provider
public class GlobalExceptionHandler implements ExceptionMapper<Throwable> {

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(Throwable exception) {
        String correlationId = MDC.get("correlationId");
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }

        String path = uriInfo != null ? uriInfo.getPath() : "unknown";

        log.error("Exception caught [correlationId={}]: {}", correlationId, exception.getMessage(), exception);

        // Handle domain exceptions
        if (exception instanceof ResourceNotFoundException) {
            return buildResponse(
                    Response.Status.NOT_FOUND,
                    "Not Found",
                    "RESOURCE_NOT_FOUND",
                    exception.getMessage(),
                    path,
                    correlationId);
        }

        if (exception instanceof BusinessRuleViolationException bre) {
            return buildResponse(
                    Response.Status.BAD_REQUEST,
                    "Bad Request",
                    bre.getErrorCode(),
                    bre.getMessage(),
                    path,
                    correlationId);
        }

        if (exception instanceof InvalidStatusTransitionException iste) {
            return buildResponse(
                    Response.Status.BAD_REQUEST,
                    "Bad Request",
                    iste.getErrorCode(),
                    iste.getMessage(),
                    path,
                    correlationId);
        }

        if (exception instanceof InvalidEmailException
                || exception instanceof InvalidPhoneNumberException
                || exception instanceof MinimumAgeRequiredException) {
            return buildResponse(
                    Response.Status.BAD_REQUEST,
                    "Bad Request",
                    ((com.vaultstream.common.exception.VaultStreamException) exception).getErrorCode(),
                    exception.getMessage(),
                    path,
                    correlationId);
        }

        // Handle validation exceptions
        if (exception instanceof ConstraintViolationException cve) {
            String message = cve.getConstraintViolations().stream()
                    .map(cv -> cv.getPropertyPath() + ": " + cv.getMessage())
                    .collect(Collectors.joining(", "));

            return buildResponse(
                    Response.Status.BAD_REQUEST,
                    "Bad Request",
                    "VALIDATION_ERROR",
                    message,
                    path,
                    correlationId);
        }

        // Handle concurrency exceptions
        if (exception instanceof OptimisticLockException) {
            return buildResponse(
                    Response.Status.CONFLICT,
                    "Conflict",
                    "CONCURRENCY_ERROR",
                    "The resource was modified by another user. Please refresh and try again.",
                    path,
                    correlationId);
        }

        if (exception instanceof ConcurrencyException ce) {
            return buildResponse(
                    Response.Status.CONFLICT,
                    "Conflict",
                    ce.getErrorCode(),
                    ce.getMessage(),
                    path,
                    correlationId);
        }

        // Handle JAX-RS exceptions
        if (exception instanceof WebApplicationException wae) {
            Response response = wae.getResponse();
            return buildResponse(
                    Response.Status.fromStatusCode(response.getStatus()),
                    response.getStatusInfo().getReasonPhrase(),
                    "WEB_APPLICATION_ERROR",
                    exception.getMessage() != null ? exception.getMessage() : "An error occurred",
                    path,
                    correlationId);
        }

        // Generic exception - don't expose internal details
        log.error("Unhandled exception [correlationId={}]", correlationId, exception);
        return buildResponse(
                Response.Status.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "INTERNAL_ERROR",
                "An unexpected error occurred. Please contact support with correlation ID: " + correlationId,
                path,
                correlationId);
    }

    private Response buildResponse(
            Response.Status status,
            String error,
            String errorCode,
            String message,
            String path,
            String correlationId) {

        ErrorResponse errorResponse = ErrorResponse.of(
                status.getStatusCode(),
                error,
                errorCode,
                message,
                path,
                correlationId);

        return Response
                .status(status)
                .entity(errorResponse)
                .header("X-Correlation-ID", correlationId)
                .build();
    }
}
