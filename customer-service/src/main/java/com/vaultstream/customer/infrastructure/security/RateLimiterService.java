package com.vaultstream.customer.infrastructure.security;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.value.ValueCommands;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Rate Limiter Service using Redis with atomic operations.
 * 
 * Uses SETEX + INCR pattern with proper atomicity.
 * Includes Circuit Breaker fallback for Redis failures.
 */
@Slf4j
@ApplicationScoped
public class RateLimiterService {

    @Inject
    RedisDataSource redisDS;

    private ValueCommands<String, Long> valueCommands;

    // Fallback in-memory limiter when Redis is unavailable
    private final ConcurrentHashMap<String, AtomicLong> fallbackCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> fallbackExpiry = new ConcurrentHashMap<>();

    @ConfigProperty(name = "vaultstream.rate-limit.requests", defaultValue = "100")
    int requestLimit;

    @ConfigProperty(name = "vaultstream.rate-limit.window-seconds", defaultValue = "60")
    int windowSeconds;

    /**
     * Initialize valueCommands lazily.
     * This is needed for test mocking to work correctly.
     */
    public void init() {
        if (this.valueCommands == null && this.redisDS != null) {
            this.valueCommands = redisDS.value(Long.class);
        }
    }

    private ValueCommands<String, Long> getValueCommands() {
        if (valueCommands == null) {
            init();
        }
        return valueCommands;
    }

    /**
     * Check if the request is allowed under rate limit.
     * Uses atomic Redis operations to prevent race conditions.
     */
    @CircuitBreaker(requestVolumeThreshold = 10, failureRatio = 0.5, delay = 5000)
    @Fallback(fallbackMethod = "isAllowedFallback")
    public boolean isAllowed(String ipAddress) {
        String key = "rate:lim:" + ipAddress;

        try {
            ValueCommands<String, Long> cmds = getValueCommands();
            if (cmds == null) {
                log.warn("Redis not available, using fallback");
                return isAllowedFallback(ipAddress);
            }

            // Atomic increment with TTL check
            Long currentCount = cmds.incr(key);

            // Only set expiration if this is the first request (counter was just created)
            if (currentCount != null && currentCount == 1L) {
                cmds.setex(key, windowSeconds, currentCount);
            }

            return currentCount != null && currentCount <= requestLimit;
        } catch (Exception e) {
            log.warn("Redis rate limit check failed, using fallback: {}", e.getMessage());
            return isAllowedFallback(ipAddress);
        }
    }

    /**
     * Fallback method when Redis is unavailable.
     * Uses in-memory rate limiting with cleanup.
     */
    public boolean isAllowedFallback(String ipAddress) {
        log.debug("Using in-memory fallback rate limiter for: {}", ipAddress);

        long now = System.currentTimeMillis();
        long windowMs = windowSeconds * 1000L;

        // Clean up expired entries
        fallbackExpiry.entrySet().removeIf(e -> e.getValue() < now);
        fallbackCounters.keySet().removeIf(k -> !fallbackExpiry.containsKey(k));

        // Get or create counter
        Long expiry = fallbackExpiry.get(ipAddress);
        if (expiry == null || expiry < now) {
            // New window
            fallbackCounters.put(ipAddress, new AtomicLong(1));
            fallbackExpiry.put(ipAddress, now + windowMs);
            return true;
        }

        // Increment existing counter
        AtomicLong counter = fallbackCounters.computeIfAbsent(ipAddress, k -> new AtomicLong(0));
        long count = counter.incrementAndGet();

        return count <= requestLimit;
    }

    /**
     * Get remaining requests for an IP.
     */
    public long getRemaining(String ipAddress) {
        String key = "rate:lim:" + ipAddress;

        try {
            ValueCommands<String, Long> cmds = getValueCommands();
            if (cmds == null) {
                return requestLimit;
            }
            Long used = cmds.get(key);
            if (used == null) {
                return requestLimit;
            }
            return Math.max(0, requestLimit - used);
        } catch (Exception e) {
            log.warn("Failed to get remaining from Redis: {}", e.getMessage());
            AtomicLong counter = fallbackCounters.get(ipAddress);
            if (counter == null) {
                return requestLimit;
            }
            return Math.max(0, requestLimit - counter.get());
        }
    }

    public int getLimit() {
        return requestLimit;
    }

    public int getResetSeconds() {
        return windowSeconds;
    }
}
