package com.vaultstream.customer.infrastructure.rest;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter that manages correlation IDs for distributed tracing.
 * 
 * Extracts or generates a correlation ID for each request and:
 * - Stores it in MDC for logging
 * - Adds it to response headers
 * - Cleans up MDC after request
 */
@Slf4j
@Provider
public class CorrelationIdFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String MDC_KEY = "correlationId";

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String correlationId = requestContext.getHeaderString(CORRELATION_ID_HEADER);

        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
            log.debug("Generated new correlation ID: {}", correlationId);
        } else {
            log.debug("Using provided correlation ID: {}", correlationId);
        }

        // Store in MDC for logging
        MDC.put(MDC_KEY, correlationId);

        // Store in request context for response filter
        requestContext.setProperty(MDC_KEY, correlationId);
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException {
        // Add correlation ID to response headers
        String correlationId = (String) requestContext.getProperty(MDC_KEY);
        if (correlationId != null) {
            responseContext.getHeaders().add(CORRELATION_ID_HEADER, correlationId);
        }

        // Clean up MDC
        MDC.remove(MDC_KEY);
    }
}
