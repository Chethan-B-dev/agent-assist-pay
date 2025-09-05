package com.paynow.gateway.filters;

import com.paynow.common.util.CorrelationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class CorrelationFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String existing = exchange.getRequest().getHeaders().getFirst(CorrelationUtils.REQUEST_ID_HEADER);
        String requestId = (existing != null && !existing.isBlank()) ? existing : CorrelationUtils.generateRequestId();

        String customerId = exchange.getRequest().getHeaders().getFirst("X-Customer-Id");
        String redactedCustomer = customerId == null ? "(n/a)" : CorrelationUtils.redactCustomerId(customerId);

        ServerHttpRequest mutated = exchange.getRequest().mutate()
                .header(CorrelationUtils.REQUEST_ID_HEADER, requestId)
                .build();

        exchange.getResponse().getHeaders().set(CorrelationUtils.REQUEST_ID_HEADER, requestId);

        log.info("Gateway inbound request: method={}, path={}, requestId={}, customerId={}",
                mutated.getMethod(), mutated.getURI().getPath(), requestId, redactedCustomer);

        return chain.filter(exchange.mutate().request(mutated).build());
    }

    @Override
    public int getOrder() {
        return -1; // run before others
    }
}
