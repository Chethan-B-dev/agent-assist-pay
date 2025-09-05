package com.paynow.gateway.filters;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * No-op filter placeholder. We rely on the client (or upstream) to provide X-Customer-Id header.
 * The KeyResolver uses X-Customer-Id header or customerId query param; defaults to anonymous.
 */
@Component
public class CustomerIdInjectionFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return 0; // after correlation but before auth and rate limiting if needed
    }
}
