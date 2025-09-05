package com.paynow.payments.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * Token bucket rate limiting service using Redis
 */
@Service
public class RateLimitingService {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitingService.class);
    
    private static final String RATE_LIMIT_PREFIX = "rate_limit:";
    private static final int MAX_REQUESTS_PER_SECOND = 5;
    private static final int BUCKET_CAPACITY = 10; // Allow burst up to 10 requests
    private static final Duration WINDOW_SIZE = Duration.ofSeconds(1);

    private final RedisTemplate<String, String> redisTemplate;
    private final RedisScript<Long> tokenBucketScript;

    public RateLimitingService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        
        // Lua script for atomic token bucket implementation
        this.tokenBucketScript = RedisScript.of("""
            local key = KEYS[1]
            local capacity = tonumber(ARGV[1])
            local tokens_per_second = tonumber(ARGV[2])
            local requested_tokens = tonumber(ARGV[3])
            local current_time = tonumber(ARGV[4])
            
            local bucket = redis.call('HMGET', key, 'tokens', 'last_refill')
            local tokens = tonumber(bucket[1]) or capacity
            local last_refill = tonumber(bucket[2]) or current_time
            
            -- Calculate tokens to add based on time elapsed
            local time_elapsed = math.max(0, current_time - last_refill)
            local tokens_to_add = math.floor(time_elapsed * tokens_per_second)
            
            -- Update tokens, capped at capacity
            tokens = math.min(capacity, tokens + tokens_to_add)
            
            -- Check if we have enough tokens
            if tokens >= requested_tokens then
                tokens = tokens - requested_tokens
                
                -- Update the bucket
                redis.call('HMSET', key, 'tokens', tokens, 'last_refill', current_time)
                redis.call('EXPIRE', key, 3600) -- Expire after 1 hour of inactivity
                
                return 1 -- Allow
            else
                -- Still update last_refill time and current tokens
                redis.call('HMSET', key, 'tokens', tokens, 'last_refill', current_time)
                redis.call('EXPIRE', key, 3600)
                
                return 0 -- Deny
            end
            """, Long.class);
    }

    /**
     * Check if request is allowed for the given customer
     */
    public boolean allowRequest(String customerId) {
        return allowRequest(customerId, 1);
    }

    /**
     * Check if the specified number of tokens are available for the customer
     */
    public boolean allowRequest(String customerId, int requestedTokens) {
        try {
            String key = RATE_LIMIT_PREFIX + customerId;
            long currentTimeSeconds = System.currentTimeMillis() / 1000;

            Long result = redisTemplate.execute(
                tokenBucketScript,
                List.of(key),
                String.valueOf(BUCKET_CAPACITY),
                String.valueOf(MAX_REQUESTS_PER_SECOND),
                String.valueOf(requestedTokens),
                String.valueOf(currentTimeSeconds)
            );

            boolean allowed = result != null && result == 1L;
            
            if (!allowed) {
                logger.info("Rate limit exceeded for customer: {}", customerId);
            }
            
            return allowed;

        } catch (Exception e) {
            logger.error("Error checking rate limit for customer: {}", customerId, e);
            // Fail open - allow request if rate limiting fails
            return true;
        }
    }
}