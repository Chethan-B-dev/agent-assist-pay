package com.paynow.common.exception;

/**
 * Base exception for payment-related errors
 */
public class PaymentException extends RuntimeException {

    private final String errorCode;
    private final String requestId;

    public PaymentException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.requestId = null;
    }

    public PaymentException(String errorCode, String message, String requestId) {
        super(message);
        this.errorCode = errorCode;
        this.requestId = requestId;
    }

    public PaymentException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.requestId = null;
    }

    public PaymentException(String errorCode, String message, String requestId, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.requestId = requestId;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getRequestId() {
        return requestId;
    }

    // Common payment exceptions
    public static class InsufficientFundsException extends PaymentException {
        public InsufficientFundsException(String message, String requestId) {
            super("INSUFFICIENT_FUNDS", message, requestId);
        }
    }

    public static class AccountNotFoundException extends PaymentException {
        public AccountNotFoundException(String message, String requestId) {
            super("ACCOUNT_NOT_FOUND", message, requestId);
        }
    }

    public static class RateLimitException extends PaymentException {
        public RateLimitException(String message, String requestId) {
            super("RATE_LIMITED", message, requestId);
        }
    }

    public static class DuplicateRequestException extends PaymentException {
        public DuplicateRequestException(String message, String requestId) {
            super("DUPLICATE_REQUEST", message, requestId);
        }
    }

    public static class ValidationException extends PaymentException {
        public ValidationException(String message, String requestId) {
            super("VALIDATION_ERROR", message, requestId);
        }
    }
}