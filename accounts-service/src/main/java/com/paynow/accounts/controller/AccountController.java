package com.paynow.accounts.controller;

import com.paynow.accounts.service.AccountService;
import com.paynow.common.dto.AccountBalanceResponse;
import com.paynow.common.error.PaymentError;
import com.paynow.common.exception.PaymentException;
import com.paynow.common.util.CorrelationUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * REST controller for account operations
 */
@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);
    private final AccountService accountService;

    @GetMapping("/{customerId}/balance")
    public ResponseEntity<?> getBalance(@PathVariable String customerId, HttpServletRequest request) {
        String requestId = CorrelationUtils.generateRequestId();
        CorrelationUtils.setRequestId(requestId);
        CorrelationUtils.setCustomerId(customerId);

        try {
            logger.info("Getting balance for customer: {}", CorrelationUtils.redactCustomerId(customerId));
            
            AccountBalanceResponse response = accountService.getBalance(customerId);
            
            return ResponseEntity.ok()
                    .header(CorrelationUtils.REQUEST_ID_HEADER, requestId)
                    .body(response);
                    
        } catch (PaymentException.AccountNotFoundException e) {
            logger.warn("Account not found: {}", e.getMessage());
            PaymentError error = PaymentError.badRequest(e.getMessage(), requestId, request.getRequestURI());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .header(CorrelationUtils.REQUEST_ID_HEADER, requestId)
                    .body(error);
                    
        } catch (Exception e) {
            logger.error("Error getting balance: {}", e.getMessage(), e);
            PaymentError error = PaymentError.internalError("Failed to get balance", requestId, request.getRequestURI());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header(CorrelationUtils.REQUEST_ID_HEADER, requestId)
                    .body(error);
        } finally {
            CorrelationUtils.clear();
        }
    }

    @PostMapping("/{customerId}/reserve")
    public ResponseEntity<?> reserveBalance(
            @PathVariable String customerId,
            @RequestParam BigDecimal amount,
            @RequestParam String requestId,
            HttpServletRequest request) {
        
        CorrelationUtils.setRequestId(requestId);
        CorrelationUtils.setCustomerId(customerId);

        try {
            logger.info("Reserving balance for customer: {}, amount: {}", 
                    CorrelationUtils.redactCustomerId(customerId), amount);
            
            accountService.reserveBalance(customerId, amount, requestId);
            
            return ResponseEntity.ok()
                    .header(CorrelationUtils.REQUEST_ID_HEADER, requestId)
                    .body("{\"status\": \"reserved\"}");
                    
        } catch (PaymentException e) {
            logger.warn("Failed to reserve balance: {}", e.getMessage());
            PaymentError error = PaymentError.badRequest(e.getMessage(), requestId, request.getRequestURI());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .header(CorrelationUtils.REQUEST_ID_HEADER, requestId)
                    .body(error);
                    
        } catch (Exception e) {
            logger.error("Error reserving balance: {}", e.getMessage(), e);
            PaymentError error = PaymentError.internalError("Failed to reserve balance", requestId, request.getRequestURI());
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