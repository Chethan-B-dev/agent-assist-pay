package com.paynow.payments.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paynow.common.dto.AgentTraceStep;
import com.paynow.common.dto.PaymentDecisionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test for IdempotencyService
 */
@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private IdempotencyService idempotencyService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        idempotencyService = new IdempotencyService(redisTemplate, objectMapper);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void shouldCacheAndRetrieveResponse() throws Exception {
        // Given
        String idempotencyKey = "test-key-123";
        PaymentDecisionResponse response = new PaymentDecisionResponse(
                PaymentDecisionResponse.DecisionType.ALLOW,
                List.of("low_risk_transaction"),
                List.of(AgentTraceStep.plan("test")),
                "req_123"
        );

        String responseJson = objectMapper.writeValueAsString(response);
        when(valueOperations.get("idempotency:" + idempotencyKey)).thenReturn(responseJson);

        // When
        idempotencyService.cacheResponse(idempotencyKey, response);
        PaymentDecisionResponse cachedResponse = idempotencyService.getCachedResponse(idempotencyKey);

        // Then
        assertNotNull(cachedResponse);
        assertEquals(response.getDecision(), cachedResponse.getDecision());
        assertEquals(response.getRequestId(), cachedResponse.getRequestId());

        verify(valueOperations).set(eq("idempotency:" + idempotencyKey), eq(responseJson), any(Duration.class));
        verify(valueOperations).get("idempotency:" + idempotencyKey);
    }

    @Test
    void shouldReturnNullForNonExistentKey() {
        // Given
        String idempotencyKey = "non-existent-key";
        when(valueOperations.get("idempotency:" + idempotencyKey)).thenReturn(null);

        // When
        PaymentDecisionResponse result = idempotencyService.getCachedResponse(idempotencyKey);

        // Then
        assertNull(result);
        verify(valueOperations).get("idempotency:" + idempotencyKey);
    }
}