package com.paynow.cases.service;

import com.paynow.cases.entity.PaymentCase;
import com.paynow.cases.repository.CaseRepository;
import com.paynow.common.dto.CaseCreationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for case management operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CaseService {

    private final CaseRepository caseRepository;

    @Transactional
    public String createCase(CaseCreationRequest request) {
        // Check for duplicate case creation
        if (caseRepository.existsByRequestId(request.getRequestId())) {
            PaymentCase existingCase = caseRepository.findByRequestId(request.getRequestId()).orElseThrow();
            log.info("Case already exists for requestId: {}, returning existing caseId: {}", 
                    request.getRequestId(), existingCase.getCaseId());
            return existingCase.getCaseId();
        }

        String caseId = "case_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        PaymentCase paymentCase = new PaymentCase();
        paymentCase.setCaseId(caseId);
        paymentCase.setCustomerId(request.getCustomerId());
        paymentCase.setAmount(request.getAmount());
        paymentCase.setCurrency(request.getCurrency());
        paymentCase.setPayeeId(request.getPayeeId());
        paymentCase.setCaseType(PaymentCase.CaseType.valueOf(request.getCaseType().name()));
        paymentCase.setStatus(PaymentCase.CaseStatus.OPEN);
        paymentCase.setRiskScore(request.getRiskScore());
        paymentCase.setRequestId(request.getRequestId());
        paymentCase.setCreatedAt(LocalDateTime.now());

        // Convert reasons list to comma-separated string for simple storage
        if (request.getReasons() != null && !request.getReasons().isEmpty()) {
            paymentCase.setReasons(String.join(",", request.getReasons()));
        }

        caseRepository.save(paymentCase);

        log.info("Case created successfully: caseId={}, type={}, customer={}", 
                caseId, request.getCaseType(), request.getCustomerId());

        return caseId;
    }
}