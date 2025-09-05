package com.paynow.gateway.filters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paynow.common.error.PaymentError;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApiKeyAuthFilter implements GlobalFilter, Ordered {

    @Value("${app.api.keys}")
    private String apiKeysRaw;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        HttpMethod method = exchange.getRequest().getMethod();
        if (path.startsWith("/actuator") || method == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }
        String apiKey = exchange.getRequest().getHeaders().getFirst("X-API-Key");
        List<String> allowed = Arrays.stream(apiKeysRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        if (apiKey == null || allowed.stream().noneMatch(apiKey::equals)) {
            String requestId = exchange.getResponse().getHeaders().getFirst("X-Request-ID");
            if (requestId == null) {
                requestId = "req_denied";
            }
            PaymentError error = PaymentError.unauthorized("Invalid API key", requestId, exchange.getRequest().getURI().getPath());
            byte[] bytes;
            try {
                bytes = objectMapper.writeValueAsString(error).getBytes(StandardCharsets.UTF_8);
            } catch (Exception e) {
                bytes = ("{\"error\":\"UNAUTHORIZED\",\"message\":\"Invalid API key\",\"requestId\":\"" + requestId + "\"}").getBytes(StandardCharsets.UTF_8);
            }
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
            exchange.getResponse().getHeaders().set(HttpHeaders.CACHE_CONTROL, "no-store");
            exchange.getResponse().getHeaders().set("X-Request-ID", requestId);
            return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        // Run after correlation filter
        return 1;
    }
}
