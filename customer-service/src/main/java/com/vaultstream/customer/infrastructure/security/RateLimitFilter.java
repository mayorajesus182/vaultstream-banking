package com.vaultstream.customer.infrastructure.security;

import com.vaultstream.common.dto.ErrorResponse;
import io.vertx.core.http.HttpServerRequest;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException; 
import java.time.Instant;
import java.util.UUID;

@Provider
public class RateLimitFilter implements ContainerRequestFilter {

    @Inject
    RateLimiterService rateLimiterService;

    @Context
    HttpServerRequest request;

    @Override
    public void filter(ContainerRequestContext context) throws IOException {
        // Skip rate limiting for non-API paths
        String path = context.getUriInfo().getPath();
        if (!path.startsWith("/api")) {
            return;
        }

        String clientIp = request.remoteAddress().host();

        if (!rateLimiterService.isAllowed(clientIp)) {
            ErrorResponse error = ErrorResponse.builder()
                    .status(429)
                    .error("Too Many Requests")
                    .errorCode("TOO_MANY_REQUESTS")
                    .message("Rate limit exceeded")
                    .correlationId(UUID.randomUUID().toString())
                    .timestamp(Instant.now())
                    .path(path)
                    .build();

            context.abortWith(Response.status(429)
                    .entity(error)
                    .header("X-RateLimit-Limit", rateLimiterService.getLimit())
                    .header("X-RateLimit-Remaining", 0)
                    .header("X-RateLimit-Reset", rateLimiterService.getResetSeconds())
                    .build());
            return;
        }

        // Add headers for allowed requests (optional, but good practice)
        context.getHeaders().add("X-RateLimit-Limit", String.valueOf(rateLimiterService.getLimit()));
        context.getHeaders().add("X-RateLimit-Remaining", String.valueOf(rateLimiterService.getRemaining(clientIp)));
    }
}
