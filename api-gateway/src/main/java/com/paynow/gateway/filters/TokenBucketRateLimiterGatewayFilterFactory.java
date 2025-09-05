package com.paynow.gateway.filters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paynow.common.error.PaymentError;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Custom Redis Lua token-bucket rate limiter.
 * Keyed by customerId extracted from path or request body.
 */
@Component("TokenBucketRateLimiter")
@Slf4j
public class TokenBucketRateLimiterGatewayFilterFactory extends AbstractGatewayFilterFactory<TokenBucketRateLimiterGatewayFilterFactory.Config> implements Ordered {

    @Autowired
    private ReactiveStringRedisTemplate redisTemplate;

    @Autowired
    private DefaultRedisScript<List> redisScript;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Pattern to extract customerId from URL paths like /accounts/{customerId}/...
    private static final Pattern CUSTOMER_ID_PATTERN = Pattern.compile("/(?:accounts|payments)/([^/]+)");

    public TokenBucketRateLimiterGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            // First try to extract customerId from the path
            String customerId = extractCustomerIdFromPath(exchange.getRequest());

            if (customerId == null) {
                customerId = extractCustomerIdFromQueryParams(exchange.getRequest());
                if (customerId != null) {
                    log.info("fetched customerid {} from query parameters", customerId);
                }
            }

            // If found in path, apply rate limiting
            if (customerId != null) {
                return applyRateLimit(customerId, exchange, chain, config);
            }

            // If it's a POST to /payments/decide, try to extract from body
            if (exchange.getRequest().getPath().value().equals("/payments/decide") &&
                    exchange.getRequest().getMethod().name().equals("POST")) {

                // Use DataBufferUtils.join() to avoid multiple subscriptions
                return DataBufferUtils.join(exchange.getRequest().getBody())
                        .flatMap(dataBuffer -> {
                            try {
                                // Read the buffer content
                                byte[] bytes = new byte[dataBuffer.readableByteCount()];
                                dataBuffer.read(bytes);

                                // Create a cached copy for downstream consumers
                                DataBuffer cachedBuffer = exchange.getResponse().bufferFactory().wrap(bytes);

                                // Create a decorated request with the cached body
                                ServerHttpRequestDecorator decoratedRequest = new ServerHttpRequestDecorator(exchange.getRequest()) {
                                    @Override
                                    public Flux<DataBuffer> getBody() {
                                        return Flux.just(cachedBuffer);
                                    }
                                };

                                // Create a new exchange with the decorated request
                                ServerWebExchange mutatedExchange = exchange.mutate()
                                        .request(decoratedRequest)
                                        .build();

                                // Try to extract customerId from the body
                                try {
                                    Map<String, Object> requestBody = objectMapper.readValue(bytes, Map.class);
                                    if (requestBody.containsKey("customerId")) {
                                        String id = requestBody.get("customerId").toString();
                                        log.info("fetched customerid {} from post request body", id);
                                        return applyRateLimit(id, mutatedExchange, chain, config);
                                    }
                                } catch (Exception e) {
                                    log.warn("Failed to parse request body for customerId: {}", e.getMessage());
                                }

                                // If we can't extract customerId, proceed with the mutated exchange
                                return chain.filter(mutatedExchange);

                            } finally {
                                // Make sure to release the data buffer to avoid memory leaks
                                DataBufferUtils.release(dataBuffer);
                            }
                        });
            }

            // For all other cases, just proceed with the request
            return chain.filter(exchange);
        };
    }

    private String extractCustomerIdFromQueryParams(ServerHttpRequest request) {
        // Get the query parameters
        String queryParam = request.getQueryParams().getFirst("customerId");
        if (queryParam != null && !queryParam.isEmpty()) {
            return queryParam;
        }
        return null;
    }

    private String extractCustomerIdFromPath(ServerHttpRequest request) {
        String path = request.getPath().value();
        Matcher matcher = CUSTOMER_ID_PATTERN.matcher(path);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private Mono<Void> applyRateLimit(String customerId, ServerWebExchange exchange, GatewayFilterChain chain, Config config) {
        String keyBase = config.getKeyPrefix() + ":" + customerId;
        String tokensKey = keyBase + ":tokens";
        String tsKey = keyBase + ":ts";

        long nowSeconds = Instant.now().getEpochSecond();
        int ttl = Math.max((int) (config.getCapacity() / Math.max(1.0, config.getRefillRatePerSecond()) * 3), 60);

        List<String> keys = Arrays.asList(tokensKey, tsKey);
        String capacity = String.valueOf(config.getCapacity());
        String refill = String.valueOf(config.getRefillRatePerSecond());
        String now = String.valueOf(nowSeconds);
        String requested = String.valueOf(config.getTokensPerRequest());
        String ttlArg = String.valueOf(ttl);

        return redisTemplate.execute(redisScript, keys, Arrays.asList(capacity, refill, now, requested, ttlArg))
                .single()
                .cast(List.class)
                .flatMap(res -> {
                    int allowed = Integer.parseInt(String.valueOf(res.get(0)));
                    int remaining = Integer.parseInt(String.valueOf(res.get(1)));

                    exchange.getResponse().getHeaders().set("X-RateLimit-Limit", String.valueOf((int) config.getCapacity()));
                    exchange.getResponse().getHeaders().set("X-RateLimit-Remaining", String.valueOf(Math.max(remaining, 0)));

                    if (allowed == 1) {
                        return chain.filter(exchange);
                    } else {
                        return tooManyRequests(exchange, customerId);
                    }
                })
                .onErrorResume(Throwable.class, ex -> {
                    log.warn("Rate limiter error for customerId {}, allowing request: {}", customerId, ex.toString());
                    return chain.filter(exchange);
                });
    }

    private Mono<Void> tooManyRequests(ServerWebExchange exchange, String customerId) {
        String requestId = exchange.getResponse().getHeaders().getFirst("X-Request-ID");
        if (requestId == null) requestId = "req_limited";

        // Redact customerId for logging and error messages
        String redactedCustomerId = redactCustomerId(customerId);
        log.warn("Rate limit exceeded for customer: {}", redactedCustomerId);

        PaymentError error = PaymentError.rateLimited(
                "Too many requests for this customer",
                requestId,
                exchange.getRequest().getURI().getPath());

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(error);
        } catch (Exception e) {
            bytes = ("{\"error\":\"RATE_LIMITED\",\"message\":\"Too many requests\",\"requestId\":\"" + requestId + "\"}").getBytes(StandardCharsets.UTF_8);
        }

        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        exchange.getResponse().getHeaders().set(HttpHeaders.CACHE_CONTROL, "no-store");
        exchange.getResponse().getHeaders().set("X-Request-ID", requestId);

        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
    }

    private String redactCustomerId(String customerId) {
        if (customerId == null || customerId.length() <= 4) {
            return "****";
        }
        return customerId.substring(0, 2) + "****" + customerId.substring(customerId.length() - 2);
    }

    @Override
    public int getOrder() {
        // After auth
        return 2;
    }

    @Data
    public static class Config {
        private double capacity = 10; // bucket size
        private double refillRatePerSecond = 5; // tokens per second (5 req/sec per customer)
        private double tokensPerRequest = 1; // tokens consumed per request
        private String keyPrefix = "rate:customer"; // Changed from rate:token to rate:customer
        private String headerName = "X-API-Key"; // Not used anymore, but kept for backward compatibility
    }
}