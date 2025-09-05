package com.paynow.accounts.service;

import com.paynow.accounts.entity.Account;
import com.paynow.accounts.entity.BalanceReservation;
import com.paynow.accounts.repository.AccountRepository;
import com.paynow.accounts.repository.BalanceReservationRepository;
import com.paynow.common.dto.AccountBalanceResponse;
import com.paynow.common.exception.PaymentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Service for account operations with transactional safety
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountRepository accountRepository;
    private final BalanceReservationRepository reservationRepository;

    public AccountBalanceResponse getBalance(String customerId) {
        Account account = accountRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new PaymentException.AccountNotFoundException(
                        "Account not found for customer: " + customerId, null));

        // Calculate available balance (balance - pending reservations)
        BigDecimal pendingReservations = reservationRepository.sumByCustomerIdAndStatus(
                customerId, BalanceReservation.ReservationStatus.PENDING);
        
        if (pendingReservations == null) {
            pendingReservations = BigDecimal.ZERO;
        }

        BigDecimal availableBalance = account.getBalance().subtract(pendingReservations);

        return new AccountBalanceResponse(
                customerId,
                account.getBalance(),
                availableBalance,
                account.getCurrency(),
                account.getStatus().toString()
        );
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void reserveBalance(String customerId, BigDecimal amount, String requestId) {
        // Check for duplicate reservation request
        if (reservationRepository.existsByRequestId(requestId)) {
            log.warn("Duplicate reservation request: {}", requestId);
            return; // Idempotent - already processed
        }

        Account account = accountRepository.findByCustomerIdForUpdate(customerId)
                .orElseThrow(() -> new PaymentException.AccountNotFoundException(
                        "Account not found for customer: " + customerId, requestId));

        if (account.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new PaymentException("ACCOUNT_NOT_ACTIVE", 
                    "Account is not active for customer: " + customerId, requestId);
        }

        // Calculate current available balance
        BigDecimal pendingReservations = reservationRepository.sumByCustomerIdAndStatus(
                customerId, BalanceReservation.ReservationStatus.PENDING);
        
        if (pendingReservations == null) {
            pendingReservations = BigDecimal.ZERO;
        }

        BigDecimal availableBalance = account.getBalance().subtract(pendingReservations);

        if (availableBalance.compareTo(amount) < 0) {
            throw new PaymentException.InsufficientFundsException(
                    String.format("Insufficient funds. Available: %s, Requested: %s", 
                            availableBalance, amount), requestId);
        }

        // Create reservation
        BalanceReservation reservation = new BalanceReservation();
        reservation.setCustomerId(customerId);
        reservation.setAmount(amount);
        reservation.setRequestId(requestId);
        reservation.setStatus(BalanceReservation.ReservationStatus.PENDING);
        reservation.setCreatedAt(LocalDateTime.now());
        reservation.setExpiresAt(LocalDateTime.now().plusMinutes(30)); // Expire after 30 minutes

        reservationRepository.save(reservation);

        log.info("Balance reserved: customer={}, amount={}, requestId={}", 
                customerId, amount, requestId);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void commitReservation(String requestId) {
        BalanceReservation reservation = reservationRepository.findByRequestId(requestId)
                .orElseThrow(() -> new PaymentException("RESERVATION_NOT_FOUND", 
                        "Reservation not found: " + requestId, requestId));

        if (reservation.getStatus() != BalanceReservation.ReservationStatus.PENDING) {
            throw new PaymentException("RESERVATION_NOT_PENDING", 
                    "Reservation is not pending: " + requestId, requestId);
        }

        // Debit the account
        Account account = accountRepository.findByCustomerIdForUpdate(reservation.getCustomerId())
                .orElseThrow(() -> new PaymentException.AccountNotFoundException(
                        "Account not found for customer: " + reservation.getCustomerId(), requestId));

        account.setBalance(account.getBalance().subtract(reservation.getAmount()));
        accountRepository.save(account);

        // Mark reservation as committed
        reservation.setStatus(BalanceReservation.ReservationStatus.COMMITTED);
        reservation.setCommittedAt(LocalDateTime.now());
        reservationRepository.save(reservation);

        log.info("Reservation committed: requestId={}, amount={}", requestId, reservation.getAmount());
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void releaseReservation(String requestId) {
        BalanceReservation reservation = reservationRepository.findByRequestId(requestId)
                .orElseThrow(() -> new PaymentException("RESERVATION_NOT_FOUND", 
                        "Reservation not found: " + requestId, requestId));

        if (reservation.getStatus() != BalanceReservation.ReservationStatus.PENDING) {
            log.warn("Attempting to release non-pending reservation: {}", requestId);
            return;
        }

        reservation.setStatus(BalanceReservation.ReservationStatus.RELEASED);
        reservation.setCommittedAt(LocalDateTime.now());
        reservationRepository.save(reservation);

        log.info("Reservation released: requestId={}, amount={}", requestId, reservation.getAmount());
    }
}