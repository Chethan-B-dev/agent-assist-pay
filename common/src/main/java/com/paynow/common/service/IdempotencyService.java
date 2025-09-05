package com.paynow.common.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paynow.common.exception.PaymentException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.Collections;
import java.util.Optional;

@Slf4j
public class IdempotencyService {

    private static final String IDEMPOTENCY_PREFIX = "idempotency:";
    private static final Duration IDEMPOTENCY_TTL = Duration.ofMinutes(10);

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    // Lua script for atomic check-and-set operation
    private static final String CHECK_AND_SET_SCRIPT =
            "local exists = redis.call('EXISTS', KEYS[1]) " +
                    "if exists == 0 then " +
                    "  redis.call('SET', KEYS[1], ARGV[1], 'EX', ARGV[2]) " +
                    "  return 0 " + // Not found, newly created
                    "else " +
                    "  return redis.call('GET', KEYS[1]) " + // Return existing value
                    "end";

    private final RedisScript<Object> checkAndSetScript;

    public IdempotencyService(RedisTemplate<String, String> redisTemplate,
                              ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.checkAndSetScript = new DefaultRedisScript<>(CHECK_AND_SET_SCRIPT, Object.class);
    }

    /**
     * Checks if a response is already cached for the given idempotency key.
     * If not, it atomically marks the request as in-progress.
     */
    public <T> Optional<T> checkAndMarkInProgress(String idempotencyKey, String inProgressMarker, Class<T> responseType) {
        try {
            String key = IDEMPOTENCY_PREFIX + idempotencyKey;

            // Execute Lua script for atomic check-and-set
            Object result = redisTemplate.execute(
                    checkAndSetScript,
                    Collections.singletonList(key),
                    inProgressMarker,
                    String.valueOf(IDEMPOTENCY_TTL.getSeconds())
            );

            if (result instanceof Long && (Long)result == 0) {
                // Key didn't exist and was set to in-progress
                logger.debug("New request, marked as in-progress: {}", idempotencyKey);
                return Optional.empty();
            } else if (result instanceof String) {
                // Key existed, got cached value
                String cachedJson = (String) result;
                if (cachedJson.equals(inProgressMarker)) {
                    // Request is already being processed
                    logger.debug("Concurrent request detected for key: {}", idempotencyKey);
                    return Optional.empty();
                }

                try {
                    T cachedResponse = objectMapper.readValue(cachedJson, responseType);
                    logger.debug("Found cached response for idempotencyKey: {}", idempotencyKey);
                    return Optional.of(cachedResponse);
                } catch (JsonProcessingException e) {
                    logger.error("Error deserializing cached response: {}", idempotencyKey, e);
                    redisTemplate.delete(key);
                    return Optional.empty();
                }
            }

            // Unexpected result type
            logger.warn("Unexpected result from Redis script: {}", result);
            return Optional.empty();

        } catch (Exception e) {
            logger.warn("Error in idempotency check for key: {}", idempotencyKey, e);
            return Optional.empty();
        }
    }

    /**
     * Cache response for idempotency key
     */
    public <T> void cacheResponse(String idempotencyKey, T response) {
        try {
            String key = IDEMPOTENCY_PREFIX + idempotencyKey;
            String responseJson = objectMapper.writeValueAsString(response);

            redisTemplate.opsForValue().set(key, responseJson, IDEMPOTENCY_TTL);
            logger.debug("Cached response for idempotencyKey: {}", idempotencyKey);

        } catch (JsonProcessingException e) {
            logger.error("Error serializing response for caching: {}", idempotencyKey, e);
            throw new PaymentException("CACHING_ERROR", "Failed to cache response", e);
        } catch (Exception e) {
            logger.warn("Error caching response for idempotencyKey: {}", idempotencyKey, e);
        }
    }

    /**
     * Remove idempotency key (useful for error cases or testing)
     */
    public void removeIdempotencyKey(String idempotencyKey) {
        try {
            String key = IDEMPOTENCY_PREFIX + idempotencyKey;
            redisTemplate.delete(key);
            logger.debug("Removed idempotency key: {}", idempotencyKey);
        } catch (Exception e) {
            logger.warn("Error removing idempotency key: {}", idempotencyKey, e);
        }
    }
}