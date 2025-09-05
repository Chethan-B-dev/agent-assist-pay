package com.paynow.payments.agent.tools;

import com.paynow.common.dto.RiskSignalsResponse;
import com.paynow.common.exception.PaymentException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * Agent tool for interacting with risk service
 */
@Component
@Slf4j
public class RiskTool {

    private final WebClient webClient;
    private final String riskServiceUrl;

    public RiskTool(WebClient.Builder webClientBuilder,
                   @Value("${services.risk.url:http://localhost:8082}") String riskServiceUrl) {
        this.webClient = webClientBuilder
                .baseUrl(riskServiceUrl)
                .build();
        this.riskServiceUrl = riskServiceUrl;
    }

    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 200))
    public RiskSignalsResponse getRiskSignals(String customerId, BigDecimal amount) {
        try {
            log.debug("Calling risk service for customer: {} with amount: {}", customerId, amount);
            
            RiskSignalsResponse response = webClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/risk/{customerId}/signals")
                            .queryParam("amount", amount.toString())
                            .build(customerId))
                    .header("X-API-Key", "internal-service-key") // In production, use proper service auth
                    .retrieve()
                    .bodyToMono(RiskSignalsResponse.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();

            log.debug("Risk signals retrieved successfully for customer: {} - risk score: {}", 
                    customerId, response.getRiskScore());
            return response;

        } catch (WebClientResponseException e) {
            log.error("Risk service error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new PaymentException("RISK_SERVICE_ERROR", 
                    "Failed to retrieve risk signals: " + e.getMessage(), e);
                    
        } catch (Exception e) {
            log.error("Unexpected error calling risk service: {}", e.getMessage(), e);
            throw new PaymentException("RISK_SERVICE_ERROR", 
                    "Failed to retrieve risk signals", e);
        }
    }
}