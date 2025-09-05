package com.paynow.cases.controller;

import com.paynow.cases.service.CaseService;
import com.paynow.common.dto.CaseCreationRequest;
import com.paynow.common.error.PaymentError;
import com.paynow.common.util.CorrelationUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for case management operations
 */
@RestController
@RequestMapping("/cases")
@Slf4j
@RequiredArgsConstructor
public class CaseController {

    private final CaseService caseService;

    @PostMapping
    public ResponseEntity<?> createCase(
            @Valid @RequestBody CaseCreationRequest request,
            HttpServletRequest httpRequest) {

        String requestId = request.getRequestId() != null ? request.getRequestId()
                : (httpRequest.getHeader(CorrelationUtils.REQUEST_ID_HEADER) != null ? httpRequest.getHeader(CorrelationUtils.REQUEST_ID_HEADER) : "req_missing");

        try {
            log.info("Creating case for customer: {}, type: {}",
                    CorrelationUtils.redactCustomerId(request.getCustomerId()), request.getCaseType());

            String caseId = caseService.createCase(request);

            return ResponseEntity.ok()
                    .header(CorrelationUtils.REQUEST_ID_HEADER, requestId)
                    .body("{\"caseId\": \"" + caseId + "\", \"status\": \"created\"}");

        } catch (Exception e) {
            log.error("Error creating case: {}", e.getMessage(), e);
            PaymentError error = PaymentError.internalError("Failed to create case", requestId, httpRequest.getRequestURI());
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