package com.paynow.payments.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paynow.common.dto.PaymentDecisionResponse;
import com.paynow.common.exception.PaymentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Service for handling request idempotency using Redis
 */
@Service
public class IdempotencyService {

    private static final Logger logger = LoggerFactory.getLogger(IdempotencyService.class);
    private static final String IDEMPOTENCY_PREFIX = "idempotency:";
    private static final Duration IDEMPOTENCY_TTL = Duration.ofMinutes(10); // Cache for 10 minutes

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public IdempotencyService(RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Get cached response for idempotency key
     */
    public PaymentDecisionResponse getCachedResponse(String idempotencyKey) {
        try {
            String key = IDEMPOTENCY_PREFIX + idempotencyKey;
            String cachedJson = redisTemplate.opsForValue().get(key);
            
            if (cachedJson != null) {
                logger.debug("Found cached response for idempotencyKey: {}", idempotencyKey);
                return objectMapper.readValue(cachedJson, PaymentDecisionResponse.class);
            }
            
            return null;
            
        } catch (JsonProcessingException e) {
            logger.error("Error deserializing cached response for idempotencyKey: {}", idempotencyKey, e);
            // Remove corrupted cache entry
            redisTemplate.delete(IDEMPOTENCY_PREFIX + idempotencyKey);
            return null;
        } catch (Exception e) {
            logger.warn("Error retrieving cached response for idempotencyKey: {}", idempotencyKey, e);
            return null;
        }
    }

    /**
     * Cache response for idempotency key
     */
    public void cacheResponse(String idempotencyKey, PaymentDecisionResponse response) {
        try {
            String key = IDEMPOTENCY_PREFIX + idempotencyKey;
            String responseJson = objectMapper.writeValueAsString(response);
            
            redisTemplate.opsForValue().set(key, responseJson, IDEMPOTENCY_TTL);
            logger.debug("Cached response for idempotencyKey: {}", idempotencyKey);
            
        } catch (JsonProcessingException e) {
            logger.error("Error serializing response for caching, idempotencyKey: {}", idempotencyKey, e);
            throw new PaymentException("CACHING_ERROR", "Failed to cache response", e);
        } catch (Exception e) {
            logger.warn("Error caching response for idempotencyKey: {}", idempotencyKey, e);
            // Don't throw exception for caching failures - continue processing
        }
    }

    /**
     * Check if request is in progress (to handle concurrent duplicate requests)
     */
    public boolean markRequestInProgress(String idempotencyKey) {
        try {
            String lockKey = IDEMPOTENCY_PREFIX + "lock:" + idempotencyKey;
            String lockValue = "processing";
            
            // Use SET with NX (only if not exists) and EX (expiration)
            Boolean wasSet = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, Duration.ofMinutes(5));
            
            if (Boolean.TRUE.equals(wasSet)) {
                logger.debug("Marked request as in progress: {}", idempotencyKey);
                return true;
            } else {
                logger.debug("Request already in progress: {}", idempotencyKey);
                return false;
            }
            
        } catch (Exception e) {
            logger.warn("Error marking request in progress for idempotencyKey: {}", idempotencyKey, e);
            // If we can't acquire lock, allow processing to continue
            return true;
        }
    }

    /**
     * Remove the in-progress lock
     */
    public void removeInProgressLock(String idempotencyKey) {
        try {
            String lockKey = IDEMPOTENCY_PREFIX + "lock:" + idempotencyKey;
            redisTemplate.delete(lockKey);
            logger.debug("Removed in-progress lock: {}", idempotencyKey);
        } catch (Exception e) {
            logger.warn("Error removing in-progress lock for idempotencyKey: {}", idempotencyKey, e);
        }
    }
}