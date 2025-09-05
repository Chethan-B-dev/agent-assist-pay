package com.paynow.risk.controller;

import com.paynow.common.dto.RiskSignalsResponse;
import com.paynow.common.error.PaymentError;
import com.paynow.common.util.CorrelationUtils;
import com.paynow.risk.service.RiskService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * REST controller for risk assessment operations
 */
@RestController
@RequestMapping("/risk")
public class RiskController {

    private static final Logger logger = LoggerFactory.getLogger(RiskController.class);

    private final RiskService riskService;

    public RiskController(RiskService riskService) {
        this.riskService = riskService;
    }

    @GetMapping("/{customerId}/signals")
    public ResponseEntity<?> getRiskSignals(
            @PathVariable String customerId,
            @RequestParam(required = false, defaultValue = "0") BigDecimal amount,
            HttpServletRequest request) {
        
        String requestId = CorrelationUtils.generateRequestId();
        CorrelationUtils.setRequestId(requestId);
        CorrelationUtils.setCustomerId(customerId);

        try {
            logger.info("Getting risk signals for customer: {}, amount: {}", 
                    CorrelationUtils.redactCustomerId(customerId), amount);
            
            RiskSignalsResponse response = riskService.getRiskSignals(customerId, amount);
            
            return ResponseEntity.ok()
                    .header(CorrelationUtils.REQUEST_ID_HEADER, requestId)
                    .body(response);
                    
        } catch (Exception e) {
            logger.error("Error getting risk signals: {}", e.getMessage(), e);
            PaymentError error = PaymentError.internalError("Failed to get risk signals", requestId, request.getRequestURI());
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