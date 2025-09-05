package com.paynow.cases.repository;

import com.paynow.cases.entity.PaymentCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for PaymentCase entities
 */
@Repository
public interface CaseRepository extends JpaRepository<PaymentCase, Long> {

    Optional<PaymentCase> findByCaseId(String caseId);

    Optional<PaymentCase> findByRequestId(String requestId);

    boolean existsByRequestId(String requestId);
}