package com.paynow.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

import java.util.List;

@Configuration
public class RedisScriptConfig {

    @Bean
    public DefaultRedisScript<List> tokenBucketRateLimiterScript() {
        DefaultRedisScript<List> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("scripts/token-bucket-rate-limiter.lua")));
        redisScript.setResultType(List.class);
        return redisScript;
    }
}