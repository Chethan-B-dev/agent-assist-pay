package com.paynow.accounts.repository;

import com.paynow.accounts.entity.BalanceReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Repository for BalanceReservation entities
 */
@Repository
public interface BalanceReservationRepository extends JpaRepository<BalanceReservation, Long> {

    Optional<BalanceReservation> findByRequestId(String requestId);

    boolean existsByRequestId(String requestId);

    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM BalanceReservation r " +
           "WHERE r.customerId = :customerId AND r.status = :status")
    BigDecimal sumByCustomerIdAndStatus(@Param("customerId") String customerId, 
                                       @Param("status") BalanceReservation.ReservationStatus status);
}