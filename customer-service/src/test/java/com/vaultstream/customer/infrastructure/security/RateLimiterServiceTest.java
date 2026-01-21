package com.vaultstream.customer.infrastructure.security;

import io.quarkus.redis.client.RedisClient;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.redis.client.Response;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@QuarkusTest
@DisplayName("Rate Limiter Service")
class RateLimiterServiceTest {

    @Inject
    RateLimiterService rateLimiterService;

    @InjectMock
    RedisClient redisClient;

    @BeforeEach
    void setup() {
        // Mock redis expire to avoid NPE if called
        Response okResponse = mock(Response.class);
        when(redisClient.expire(anyString(), anyString())).thenReturn(okResponse);
    }

    @Test
    @DisplayName("should allow request when limit not exceeded")
    void shouldAllowRequest() {
        // Mock INCR returning 1
        Response response = mock(Response.class);
        when(response.toLong()).thenReturn(1L);
        when(redisClient.incr(anyString())).thenReturn(response);

        boolean allowed = rateLimiterService.isAllowed("127.0.0.1");

        assertTrue(allowed);
        verify(redisClient).incr(contains("127.0.0.1"));
    }

    @Test
    @DisplayName("should block request when limit exceeded")
    void shouldBlockRequest() {
        // Mock INCR returning limit + 1
        Response response = mock(Response.class);
        when(response.toLong()).thenReturn(101L); // Default limit is 100
        when(redisClient.incr(anyString())).thenReturn(response);

        boolean allowed = rateLimiterService.isAllowed("127.0.0.1");

        assertFalse(allowed);
    }
}
