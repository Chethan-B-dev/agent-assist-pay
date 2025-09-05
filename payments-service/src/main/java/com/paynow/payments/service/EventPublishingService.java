package com.paynow.payments.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paynow.common.dto.PaymentDecisionRequest;
import com.paynow.common.dto.PaymentDecisionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for publishing payment events
 * In production, this would integrate with Kafka, SQS, or other message queues
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EventPublishingService {

    private final ObjectMapper objectMapper;
    /**
     * Publish payment.decided event
     */
    public void publishPaymentDecided(PaymentDecisionRequest request, PaymentDecisionResponse response) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "payment.decided");
            event.put("eventId", "evt_" + response.getRequestId().substring(4));
            event.put("timestamp", Instant.now().toString());
            event.put("requestId", response.getRequestId());
            
            // Event payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("customerId", request.getCustomerId());
            payload.put("amount", request.getAmount());
            payload.put("currency", request.getCurrency());
            payload.put("payeeId", request.getPayeeId());
            payload.put("decision", response.getDecision());
            payload.put("reasons", response.getReasons());
            payload.put("idempotencyKey", request.getIdempotencyKey());
            
            event.put("payload", payload);
            
            // Log event to dedicated event logger (could be configured to send to Kafka)
            String eventJson = objectMapper.writeValueAsString(event);
            log.info("PAYMENT_DECIDED: {}", eventJson);
            
            // In production, you might also:
            // - Send to Kafka topic
            // - Send to SQS queue
            // - Send to internal message bus
            // kafkaTemplate.send("payment.decided", eventJson);
            
            log.debug("Published payment.decided event for requestId: {}", response.getRequestId());
            
        } catch (JsonProcessingException e) {
            log.error("Error serializing payment.decided event for requestId: {}", 
                    response.getRequestId(), e);
        } catch (Exception e) {
            log.error("Error publishing payment.decided event for requestId: {}", 
                    response.getRequestId(), e);
        }
    }

    /**
     * Publish balance.reserved event (when payment is allowed)
     */
    public void publishBalanceReserved(PaymentDecisionRequest request, String requestId) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "balance.reserved");
            event.put("eventId", "evt_" + requestId.substring(4));
            event.put("timestamp", Instant.now().toString());
            event.put("requestId", requestId);
            
            Map<String, Object> payload = new HashMap<>();
            payload.put("customerId", request.getCustomerId());
            payload.put("amount", request.getAmount());
            payload.put("currency", request.getCurrency());
            payload.put("idempotencyKey", request.getIdempotencyKey());
            
            event.put("payload", payload);
            
            String eventJson = objectMapper.writeValueAsString(event);
            log.info("BALANCE_RESERVED: {}", eventJson);
            
            log.debug("Published balance.reserved event for requestId: {}", requestId);
            
        } catch (Exception e) {
            log.error("Error publishing balance.reserved event for requestId: {}", requestId, e);
        }
    }
}