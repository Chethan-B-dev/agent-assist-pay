package com.paynow.payments.agent.tools;

import com.paynow.common.dto.AccountBalanceResponse;
import com.paynow.common.exception.PaymentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.math.BigDecimal;

/**
 * Agent tool for interacting with accounts service
 */
@Component
public class AccountTool {

    private static final Logger logger = LoggerFactory.getLogger(AccountTool.class);

    private final WebClient webClient;
    private final String accountsServiceUrl;

    public AccountTool(WebClient.Builder webClientBuilder,
                      @Value("${services.accounts.url:http://localhost:8081}") String accountsServiceUrl) {
        this.webClient = webClientBuilder
                .baseUrl(accountsServiceUrl)
                .build();
        this.accountsServiceUrl = accountsServiceUrl;
    }

    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 200))
    public AccountBalanceResponse getBalance(String customerId) {
        try {
            logger.debug("Calling accounts service for customer balance: {}", customerId);
            
            AccountBalanceResponse response = webClient
                    .get()
                    .uri("/accounts/{customerId}/balance", customerId)
                    .header("X-API-Key", "internal-service-key") // In production, use proper service auth
                    .retrieve()
                    .bodyToMono(AccountBalanceResponse.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();

            logger.debug("Balance retrieved successfully for customer: {}", customerId);
            return response;

        } catch (WebClientResponseException.NotFound e) {
            logger.warn("Account not found for customer: {}", customerId);
            throw new PaymentException.AccountNotFoundException(
                    "Account not found for customer", null);
                    
        } catch (WebClientResponseException e) {
            logger.error("Accounts service error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new PaymentException("ACCOUNTS_SERVICE_ERROR", 
                    "Failed to retrieve account balance: " + e.getMessage(), e);
                    
        } catch (Exception e) {
            logger.error("Unexpected error calling accounts service: {}", e.getMessage(), e);
            throw new PaymentException("ACCOUNTS_SERVICE_ERROR", 
                    "Failed to retrieve account balance", e);
        }
    }

    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 200))
    public void reserveBalance(String customerId, BigDecimal amount, String requestId) {
        try {
            logger.debug("Reserving balance for customer: {}", customerId);
            webClient
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path("/accounts/{customerId}/reserve")
                        .queryParam("amount", amount)
                        .queryParam("requestId", requestId)
                        .build(customerId))
                .header("X-API-Key", "internal-service-key")
                .retrieve()
                .toBodilessEntity()
                .timeout(Duration.ofSeconds(5))
                .block();
        } catch (WebClientResponseException.BadRequest e) {
            String body = e.getResponseBodyAsString();
            if (body != null && body.contains("INSUFFICIENT_FUNDS")) {
                throw new PaymentException.InsufficientFundsException("Insufficient funds during reservation", requestId);
            }
            logger.error("Accounts service reserve error: {} - {}", e.getStatusCode(), body);
            throw new PaymentException("ACCOUNTS_SERVICE_ERROR", "Failed to reserve balance: " + e.getMessage(), e);
        } catch (WebClientResponseException e) {
            logger.error("Accounts service reserve error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new PaymentException("ACCOUNTS_SERVICE_ERROR", "Failed to reserve balance: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error reserving balance: {}", e.getMessage(), e);
            throw new PaymentException("ACCOUNTS_SERVICE_ERROR", "Failed to reserve balance", e);
        }
    }
}