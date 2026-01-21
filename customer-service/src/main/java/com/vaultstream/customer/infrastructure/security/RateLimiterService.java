package com.vaultstream.customer.infrastructure.security;

import io.quarkus.redis.client.RedisClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import io.vertx.redis.client.Response;

import java.util.Arrays;

@ApplicationScoped
public class RateLimiterService {

    @Inject
    RedisClient redisClient;

    @ConfigProperty(name = "vaultstream.rate-limit.requests", defaultValue = "100")
    int requestLimit;

    @ConfigProperty(name = "vaultstream.rate-limit.window-seconds", defaultValue = "60")
    int windowSeconds;

    public boolean isAllowed(String ipAddress) {
        String key = "rate:lim:" + ipAddress;

        // INCREMENT key
        Response response = redisClient.incr(key);
        long currentCount = response.toLong();

        if (currentCount == 1) {
            // First request, set expiration
            redisClient.expire(key, String.valueOf(windowSeconds));
        }

        return currentCount <= requestLimit;
    }

    public long getRemaining(String ipAddress) {
        String key = "rate:lim:" + ipAddress;
        Response response = redisClient.get(key);

        if (response == null) {
            return requestLimit;
        }

        try {
            long used = response.toLong();
            return Math.max(0, requestLimit - used);
        } catch (NumberFormatException e) {
            return 0; // Should not happen for counters
        }
    }

    public int getLimit() {
        return requestLimit;
    }

    public int getResetSeconds() {
        // Simplified: return window size. Ideally return TTL.
        return windowSeconds;
    }
}
