package com.paynow.cases.controller;

import com.paynow.cases.service.CaseService;
import com.paynow.common.dto.CaseCreationRequest;
import com.paynow.common.error.PaymentError;
import com.paynow.common.util.CorrelationUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for case management operations
 */
@RestController
@RequestMapping("/cases")
public class CaseController {

    private static final Logger logger = LoggerFactory.getLogger(CaseController.class);

    private final CaseService caseService;

    public CaseController(CaseService caseService) {
        this.caseService = caseService;
    }

    @PostMapping
    public ResponseEntity<?> createCase(
            @Valid @RequestBody CaseCreationRequest request,
            HttpServletRequest httpRequest) {
        
        String requestId = request.getRequestId();
        CorrelationUtils.setRequestId(requestId);
        CorrelationUtils.setCustomerId(request.getCustomerId());

        try {
            logger.info("Creating case for customer: {}, type: {}", 
                    CorrelationUtils.redactCustomerId(request.getCustomerId()), request.getCaseType());
            
            String caseId = caseService.createCase(request);
            
            return ResponseEntity.ok()
                    .header(CorrelationUtils.REQUEST_ID_HEADER, requestId)
                    .body("{\"caseId\": \"" + caseId + "\", \"status\": \"created\"}");
                    
        } catch (Exception e) {
            logger.error("Error creating case: {}", e.getMessage(), e);
            PaymentError error = PaymentError.internalError("Failed to create case", requestId, httpRequest.getRequestURI());
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