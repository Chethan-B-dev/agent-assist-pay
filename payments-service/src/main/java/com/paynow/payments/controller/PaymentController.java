package com.paynow.payments.controller;

import com.paynow.common.dto.PaymentDecisionRequest;
import com.paynow.common.dto.PaymentDecisionResponse;
import com.paynow.common.error.PaymentError;
import com.paynow.common.exception.PaymentException;
import com.paynow.common.util.CorrelationUtils;
import com.paynow.payments.service.PaymentDecisionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for payment decisions
 */
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentDecisionService paymentDecisionService;

    @PostMapping("/decide")
    public ResponseEntity<?> decidePayment(
            @Valid @RequestBody PaymentDecisionRequest request,
            HttpServletRequest httpRequest) {
        // Use propagated request ID only; do not set/clear MDC here (centralized in gateway)
        String requestIdHeader = httpRequest.getHeader(CorrelationUtils.REQUEST_ID_HEADER);
        String requestId = (requestIdHeader != null && !requestIdHeader.isBlank())
                ? requestIdHeader
                : "req_missing";

        try {
            log.info("Processing payment decision request: {}", request.toRedactedString());

            PaymentDecisionResponse response = paymentDecisionService.processPayment(request, requestId);

            log.info("Payment decision completed: decision={}, requestId={}",
                    response.getDecision(), requestId);

            return ResponseEntity.ok()
                    .header(CorrelationUtils.REQUEST_ID_HEADER, requestId)
                    .body(response);

        } catch (PaymentException.RateLimitException e) {
            log.warn("Rate limit exceeded for request: {}", e.getMessage());
            PaymentError error = PaymentError.rateLimited(e.getMessage(), requestId, httpRequest.getRequestURI());
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header(CorrelationUtils.REQUEST_ID_HEADER, requestId)
                    .body(error);

        } catch (PaymentException.DuplicateRequestException e) {
            log.warn("Duplicate request detected: {}", e.getMessage());
            PaymentError error = PaymentError.badRequest(e.getMessage(), requestId, httpRequest.getRequestURI());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .header(CorrelationUtils.REQUEST_ID_HEADER, requestId)
                    .body(error);

        } catch (PaymentException e) {
            log.error("Payment processing error: {}", e.getMessage(), e);
            PaymentError error = PaymentError.badRequest(e.getMessage(), requestId, httpRequest.getRequestURI());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .header(CorrelationUtils.REQUEST_ID_HEADER, requestId)
                    .body(error);

        } catch (Exception e) {
            log.error("Unexpected error processing payment: {}", e.getMessage(), e);
            PaymentError error = PaymentError.internalError(
                    "An unexpected error occurred", requestId, httpRequest.getRequestURI());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header(CorrelationUtils.REQUEST_ID_HEADER, requestId)
                    .body(error);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}