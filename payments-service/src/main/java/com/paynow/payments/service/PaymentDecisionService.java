package com.paynow.payments.service;

import com.paynow.common.dto.PaymentDecisionRequest;
import com.paynow.common.dto.PaymentDecisionResponse;
import com.paynow.common.exception.PaymentException;
import com.paynow.common.service.IdempotencyService;
import com.paynow.payments.agent.PaymentAgent;
import com.paynow.payments.metrics.PaymentMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Main service for processing payment decisions using agent orchestration
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentDecisionService {

    private final IdempotencyService idempotencyService;
    private final RateLimitingService rateLimitingService;
    private final PaymentAgent paymentAgent;
    private final PaymentMetrics paymentMetrics;
    private final EventPublishingService eventPublishingService;

    public PaymentDecisionResponse processPayment(PaymentDecisionRequest request, String requestId) {
        Instant startTime = Instant.now();
        
        try {
            // Check idempotency first
            Optional<PaymentDecisionResponse> cachedResponse =
                    idempotencyService.checkAndMarkInProgress(
                            request.getIdempotencyKey(),
                            "in-progress",
                            PaymentDecisionResponse.class
                    );
            if (cachedResponse.isPresent()) {
                logger.info("Returning cached response for idempotencyKey: {}", request.getIdempotencyKey());
                paymentMetrics.recordRequest("cached");
                return cachedResponse.get();
            }

            // Apply rate limiting
            if (!rateLimitingService.allowRequest(request.getCustomerId())) {
                paymentMetrics.recordRequest("rate_limited");
                throw new PaymentException.RateLimitException(
                        "Rate limit exceeded for customer", requestId);
            }

            // Process payment using agent
            PaymentDecisionResponse response = paymentAgent.processPayment(request, requestId);

            // Cache the response for idempotency
            idempotencyService.cacheResponse(request.getIdempotencyKey(), response);

            // Record metrics
            paymentMetrics.recordRequest(response.getDecision().toString().toLowerCase());
            paymentMetrics.recordLatency(startTime);

            // Publish event asynchronously
            CompletableFuture.runAsync(() -> {
                try {
                    eventPublishingService.publishPaymentDecided(request, response);
                } catch (Exception e) {
                    logger.warn("Failed to publish payment.decided event: {}", e.getMessage(), e);
                }
            });

            return response;

        } catch (Exception e) {
            paymentMetrics.recordRequest("error");
            throw e;
        }
    }
}