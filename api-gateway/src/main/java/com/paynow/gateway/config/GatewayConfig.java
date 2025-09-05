package com.paynow.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class GatewayConfig {

    @Bean
    public KeyResolver customerIdKeyResolver() {
        return exchange -> {
            String header = exchange.getRequest().getHeaders().getFirst("X-Customer-Id");
            if (header != null && !header.isBlank()) {
                return Mono.just("cust:" + header);
            }
            String qp = exchange.getRequest().getQueryParams().getFirst("customerId");
            if (qp != null && !qp.isBlank()) {
                return Mono.just("cust:" + qp);
            }
            return Mono.just("cust:anonymous");
        };
    }
}
