package com.paynow.payments.controller;

import com.paynow.common.dto.PaymentDecisionRequest;
import com.paynow.common.dto.PaymentDecisionResponse;
import com.paynow.common.error.PaymentError;
import com.paynow.common.exception.PaymentException;
import com.paynow.common.util.CorrelationUtils;
import com.paynow.payments.service.PaymentDecisionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for payment decisions
 */
@RestController
@RequestMapping("/payments")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentDecisionService paymentDecisionService;

    public PaymentController(PaymentDecisionService paymentDecisionService) {
        this.paymentDecisionService = paymentDecisionService;
    }

    @PostMapping("/decide")
    public ResponseEntity<?> decidePayment(
            @Valid @RequestBody PaymentDecisionRequest request,
            HttpServletRequest httpRequest) {
        
        // Set up correlation context
        String requestId = CorrelationUtils.generateRequestId();
        CorrelationUtils.setRequestId(requestId);
        CorrelationUtils.setCustomerId(request.getCustomerId());

        try {
            logger.info("Processing payment decision request: {}", request.toRedactedString());
            
            PaymentDecisionResponse response = paymentDecisionService.processPayment(request, requestId);
            
            logger.info("Payment decision completed: decision={}, requestId={}", 
                    response.getDecision(), requestId);
            
            return ResponseEntity.ok()
                    .header(CorrelationUtils.REQUEST_ID_HEADER, requestId)
                    .body(response);
                    
        } catch (PaymentException.RateLimitException e) {
            logger.warn("Rate limit exceeded for request: {}", e.getMessage());
            PaymentError error = PaymentError.rateLimited(e.getMessage(), requestId, httpRequest.getRequestURI());
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header(CorrelationUtils.REQUEST_ID_HEADER, requestId)
                    .body(error);
                    
        } catch (PaymentException.DuplicateRequestException e) {
            logger.warn("Duplicate request detected: {}", e.getMessage());
            PaymentError error = PaymentError.badRequest(e.getMessage(), requestId, httpRequest.getRequestURI());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .header(CorrelationUtils.REQUEST_ID_HEADER, requestId)
                    .body(error);
                    
        } catch (PaymentException e) {
            logger.error("Payment processing error: {}", e.getMessage(), e);
            PaymentError error = PaymentError.badRequest(e.getMessage(), requestId, httpRequest.getRequestURI());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .header(CorrelationUtils.REQUEST_ID_HEADER, requestId)
                    .body(error);
                    
        } catch (Exception e) {
            logger.error("Unexpected error processing payment: {}", e.getMessage(), e);
            PaymentError error = PaymentError.internalError(
                    "An unexpected error occurred", requestId, httpRequest.getRequestURI());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header(CorrelationUtils.REQUEST_ID_HEADER, requestId)
                    .body(error);
        } finally {
            CorrelationUtils.clear();
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}