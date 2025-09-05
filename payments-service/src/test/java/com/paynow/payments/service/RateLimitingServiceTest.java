package com.paynow.payments.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test for RateLimitingService
 */
@ExtendWith(MockitoExtension.class)
class RateLimitingServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    private RateLimitingService rateLimitingService;

    @BeforeEach
    void setUp() {
        rateLimitingService = new RateLimitingService(redisTemplate);
    }

    @Test
    void shouldAllowRequestWhenUnderLimit() {
        // Given
        String customerId = "c_123";
        when(redisTemplate.execute(any(RedisScript.class), any(List.class), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(1L); // Allow

        // When
        boolean allowed = rateLimitingService.allowRequest(customerId);

        // Then
        assertTrue(allowed);
        verify(redisTemplate).execute(any(RedisScript.class), eq(List.of("rate_limit:" + customerId)), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void shouldBlockRequestWhenOverLimit() {
        // Given
        String customerId = "c_456";
        when(redisTemplate.execute(any(RedisScript.class), any(List.class), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(0L); // Deny

        // When
        boolean allowed = rateLimitingService.allowRequest(customerId);

        // Then
        assertFalse(allowed);
        verify(redisTemplate).execute(any(RedisScript.class), eq(List.of("rate_limit:" + customerId)), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void shouldFailOpenOnRedisError() {
        // Given
        String customerId = "c_789";
        when(redisTemplate.execute(any(RedisScript.class), any(List.class), anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Redis error"));

        // When
        boolean allowed = rateLimitingService.allowRequest(customerId);

        // Then
        assertTrue(allowed); // Fail open
    }
}