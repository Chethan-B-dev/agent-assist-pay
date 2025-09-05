package com.paynow.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Request DTO for payment decision API
 */
public class PaymentDecisionRequest {

    @NotBlank(message = "customerId is required")
    @JsonProperty("customerId")
    private String customerId;

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", message = "amount must be greater than 0")
    @Digits(integer = 10, fraction = 2, message = "amount must have at most 2 decimal places")
    @JsonProperty("amount")
    private BigDecimal amount;

    @NotBlank(message = "currency is required")
    @Size(min = 3, max = 3, message = "currency must be 3 characters")
    @JsonProperty("currency")
    private String currency;

    @NotBlank(message = "payeeId is required")
    @JsonProperty("payeeId")
    private String payeeId;

    @NotBlank(message = "idempotencyKey is required")
    @JsonProperty("idempotencyKey")
    private String idempotencyKey;

    // Default constructor for Jackson
    public PaymentDecisionRequest() {}

    public PaymentDecisionRequest(String customerId, BigDecimal amount, String currency, 
                                String payeeId, String idempotencyKey) {
        this.customerId = customerId;
        this.amount = amount;
        this.currency = currency;
        this.payeeId = payeeId;
        this.idempotencyKey = idempotencyKey;
    }

    // Getters and Setters
    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getPayeeId() {
        return payeeId;
    }

    public void setPayeeId(String payeeId) {
        this.payeeId = payeeId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    /**
     * Returns a redacted string representation (for logging without PII)
     */
    public String toRedactedString() {
        return "PaymentDecisionRequest{" +
                "customerId='" + redactCustomerId(customerId) + '\'' +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", payeeId='" + redactCustomerId(payeeId) + '\'' +
                ", idempotencyKey='" + idempotencyKey + '\'' +
                '}';
    }

    private String redactCustomerId(String id) {
        if (id == null || id.length() <= 4) {
            return "****";
        }
        return id.substring(0, 2) + "****" + id.substring(id.length() - 2);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaymentDecisionRequest that = (PaymentDecisionRequest) o;
        return Objects.equals(customerId, that.customerId) &&
                Objects.equals(amount, that.amount) &&
                Objects.equals(currency, that.currency) &&
                Objects.equals(payeeId, that.payeeId) &&
                Objects.equals(idempotencyKey, that.idempotencyKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(customerId, amount, currency, payeeId, idempotencyKey);
    }

    @Override
    public String toString() {
        return toRedactedString();
    }
}