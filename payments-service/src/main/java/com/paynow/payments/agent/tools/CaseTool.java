package com.paynow.payments.agent.tools;

import com.paynow.common.dto.CaseCreationRequest;
import com.paynow.common.exception.PaymentException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;

/**
 * Agent tool for interacting with case service
 */
@Component
@Slf4j
public class CaseTool {

    private final WebClient webClient;
    private final String caseServiceUrl;

    public CaseTool(WebClient.Builder webClientBuilder,
                   @Value("${services.case.url:http://localhost:8083}") String caseServiceUrl) {
        this.webClient = webClientBuilder
                .baseUrl(caseServiceUrl)
                .build();
        this.caseServiceUrl = caseServiceUrl;
    }

    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 200))
    public void createCase(CaseCreationRequest request) {
        try {
            log.debug("Creating case for customer: {} with type: {}", 
                    request.getCustomerId(), request.getCaseType());
            
            webClient
                    .post()
                    .uri("/cases")
                    .header("X-API-Key", "internal-service-key") // In production, use proper service auth
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();

            log.debug("Case created successfully for customer: {}", request.getCustomerId());

        } catch (WebClientResponseException e) {
            log.error("Case service error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new PaymentException("CASE_SERVICE_ERROR", 
                    "Failed to create case: " + e.getMessage(), e);
                    
        } catch (Exception e) {
            log.error("Unexpected error calling case service: {}", e.getMessage(), e);
            throw new PaymentException("CASE_SERVICE_ERROR", 
                    "Failed to create case", e);
        }
    }
}