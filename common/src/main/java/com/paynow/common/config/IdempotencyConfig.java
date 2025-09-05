package com.paynow.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paynow.common.service.IdempotencyService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration
@ConditionalOnClass(RedisTemplate.class)
public class IdempotencyConfig {

    @Bean
    @ConditionalOnBean(RedisTemplate.class)
    public IdempotencyService idempotencyService(
            RedisTemplate<String, String> redisTemplate,
            ObjectMapper objectMapper) {
        return new IdempotencyService(redisTemplate, objectMapper);
    }
}