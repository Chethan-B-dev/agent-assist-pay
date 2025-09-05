package com.paynow.common.error;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Objects;

/**
 * Standard error response model
 */
public class PaymentError {

    @JsonProperty("error")
    private String error;

    @JsonProperty("message")
    private String message;

    @JsonProperty("requestId")
    private String requestId;

    @JsonProperty("timestamp")
    private Instant timestamp;

    @JsonProperty("path")
    private String path;

    public PaymentError() {
        this.timestamp = Instant.now();
    }

    public PaymentError(String error, String message, String requestId, String path) {
        this.error = error;
        this.message = message;
        this.requestId = requestId;
        this.path = path;
        this.timestamp = Instant.now();
    }

    // Static factory methods for common errors
    public static PaymentError badRequest(String message, String requestId, String path) {
        return new PaymentError("BAD_REQUEST", message, requestId, path);
    }

    public static PaymentError unauthorized(String message, String requestId, String path) {
        return new PaymentError("UNAUTHORIZED", message, requestId, path);
    }

    public static PaymentError rateLimited(String message, String requestId, String path) {
        return new PaymentError("RATE_LIMITED", message, requestId, path);
    }

    public static PaymentError internalError(String message, String requestId, String path) {
        return new PaymentError("INTERNAL_ERROR", message, requestId, path);
    }

    public static PaymentError serviceUnavailable(String message, String requestId, String path) {
        return new PaymentError("SERVICE_UNAVAILABLE", message, requestId, path);
    }

    // Getters and Setters
    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaymentError that = (PaymentError) o;
        return Objects.equals(error, that.error) &&
                Objects.equals(message, that.message) &&
                Objects.equals(requestId, that.requestId) &&
                Objects.equals(timestamp, that.timestamp) &&
                Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(error, message, requestId, timestamp, path);
    }

    @Override
    public String toString() {
        return "PaymentError{" +
                "error='" + error + '\'' +
                ", message='" + message + '\'' +
                ", requestId='" + requestId + '\'' +
                ", timestamp=" + timestamp +
                ", path='" + path + '\'' +
                '}';
    }
}