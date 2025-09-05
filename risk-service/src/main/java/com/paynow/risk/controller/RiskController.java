package com.paynow.risk.controller;

import com.paynow.common.dto.RiskSignalsResponse;
import com.paynow.common.error.PaymentError;
import com.paynow.common.util.CorrelationUtils;
import com.paynow.risk.service.RiskService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * REST controller for risk assessment operations
 */
@RestController
@RequestMapping("/risk")
@RequiredArgsConstructor
@Slf4j
public class RiskController {

    private final RiskService riskService;

    @GetMapping("/{customerId}/signals")
    public ResponseEntity<?> getRiskSignals(
            @PathVariable String customerId,
            @RequestParam(required = false, defaultValue = "0") BigDecimal amount,
            HttpServletRequest request) {

        String propagated = request.getHeader(CorrelationUtils.REQUEST_ID_HEADER);
        String requestId = (propagated != null && !propagated.isBlank()) ? propagated : "req_missing";

        try {
            log.info("Getting risk signals for customer: {}, amount: {}",
                    CorrelationUtils.redactCustomerId(customerId), amount);

            RiskSignalsResponse response = riskService.getRiskSignals(customerId, amount);

            return ResponseEntity.ok()
                    .header(CorrelationUtils.REQUEST_ID_HEADER, requestId)
                    .body(response);

        } catch (Exception e) {
            log.error("Error getting risk signals: {}", e.getMessage(), e);
            PaymentError error = PaymentError.internalError("Failed to get risk signals", requestId, request.getRequestURI());
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