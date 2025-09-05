package com.paynow.common.util;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * Utilities for request correlation and tracing
 */
public class CorrelationUtils {

    public static final String REQUEST_ID_HEADER = "X-Request-ID";
    public static final String REQUEST_ID_MDC_KEY = "requestId";
    public static final String CUSTOMER_ID_MDC_KEY = "customerId";

    private CorrelationUtils() {
        // Utility class
    }

    /**
     * Generate a new request ID
     */
    public static String generateRequestId() {
        return "req_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    /**
     * Set request ID in MDC for logging
     */
    public static void setRequestId(String requestId) {
        MDC.put(REQUEST_ID_MDC_KEY, requestId);
    }

    /**
     * Set redacted customer ID in MDC for logging
     */
    public static void setCustomerId(String customerId) {
        MDC.put(CUSTOMER_ID_MDC_KEY, redactCustomerId(customerId));
    }

    /**
     * Get current request ID from MDC
     */
    public static String getCurrentRequestId() {
        return MDC.get(REQUEST_ID_MDC_KEY);
    }

    /**
     * Clear all correlation data from MDC
     */
    public static void clear() {
        MDC.remove(REQUEST_ID_MDC_KEY);
        MDC.remove(CUSTOMER_ID_MDC_KEY);
    }

    /**
     * Redact customer ID for logging (keep first and last 2 chars)
     */
    public static String redactCustomerId(String customerId) {
        if (customerId == null || customerId.length() <= 4) {
            return "****";
        }
        return customerId.substring(0, 2) + "****" + customerId.substring(customerId.length() - 2);
    }
}