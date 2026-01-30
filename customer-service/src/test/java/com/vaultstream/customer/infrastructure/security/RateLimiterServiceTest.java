package com.vaultstream.customer.infrastructure.security;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.redis.datasource.value.ValueCommands;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RateLimiterService.
 * Tests both Redis-based and fallback in-memory rate limiting.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Rate Limiter Service")
class RateLimiterServiceTest {

    @Mock
    RedisDataSource redisDS;

    @Mock
    ValueCommands<String, Long> valueCommands;

    @Mock
    KeyCommands<String> keyCommands;

    @InjectMocks
    RateLimiterService rateLimiterService;

    @BeforeEach
    void setup() throws Exception {
        // Use lenient mocking since lazy init may or may not call this
        lenient().when(redisDS.value(Long.class)).thenReturn(valueCommands);
        lenient().when(redisDS.key()).thenReturn(keyCommands);

        // Set config properties via reflection
        setField("requestLimit", 100);
        setField("windowSeconds", 60);

        // Directly inject valueCommands to bypass lazy init
        setField("valueCommands", valueCommands);
    }

    private void setField(String fieldName, Object value) throws Exception {
        Field field = RateLimiterService.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(rateLimiterService, value);
    }

    @Test
    @DisplayName("should allow request when limit not exceeded")
    void shouldAllowRequest() {
        when(valueCommands.incr(anyString())).thenReturn(1L);

        boolean allowed = rateLimiterService.isAllowed("127.0.0.1");

        assertTrue(allowed);
        verify(valueCommands).incr(contains("127.0.0.1"));
        // Verify expire() is called instead of setex() to avoid overwriting the counter
        verify(keyCommands).expire(anyString(), eq(Duration.ofSeconds(60)));
    }

    @Test
    @DisplayName("should allow request at limit boundary")
    void shouldAllowAtLimit() {
        when(valueCommands.incr(anyString())).thenReturn(100L);

        boolean allowed = rateLimiterService.isAllowed("127.0.0.1");

        assertTrue(allowed);
    }

    @Test
    @DisplayName("should block request when limit exceeded")
    void shouldBlockRequest() {
        when(valueCommands.incr(anyString())).thenReturn(101L);

        boolean allowed = rateLimiterService.isAllowed("127.0.0.1");

        assertFalse(allowed);
    }

    @Test
    @DisplayName("should use fallback when Redis fails")
    void shouldUseFallbackOnRedisFailure() {
        when(valueCommands.incr(anyString())).thenThrow(new RuntimeException("Redis connection failed"));

        // First request to fallback should succeed
        boolean allowed = rateLimiterService.isAllowed("127.0.0.1");

        assertTrue(allowed);
    }

    @Test
    @DisplayName("should return correct remaining count")
    void shouldReturnRemainingCount() {
        when(valueCommands.get(anyString())).thenReturn(30L);

        long remaining = rateLimiterService.getRemaining("127.0.0.1");

        assertEquals(70, remaining);
    }

    @Test
    @DisplayName("should return full limit when key not found")
    void shouldReturnFullLimitWhenKeyNotFound() {
        when(valueCommands.get(anyString())).thenReturn(null);

        long remaining = rateLimiterService.getRemaining("127.0.0.1");

        assertEquals(100, remaining);
    }
}
