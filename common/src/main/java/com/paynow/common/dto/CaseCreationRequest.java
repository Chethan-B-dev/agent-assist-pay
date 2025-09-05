package com.paynow.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Request DTO for creating review/block cases
 */
public class CaseCreationRequest {

    @NotBlank(message = "customerId is required")
    @JsonProperty("customerId")
    private String customerId;

    @NotNull(message = "amount is required")
    @JsonProperty("amount")
    private BigDecimal amount;

    @NotBlank(message = "currency is required")
    @JsonProperty("currency")
    private String currency;

    @NotBlank(message = "payeeId is required")
    @JsonProperty("payeeId")
    private String payeeId;

    @NotNull(message = "caseType is required")
    @JsonProperty("caseType")
    private CaseType caseType;

    @JsonProperty("reasons")
    private List<String> reasons;

    @JsonProperty("riskScore")
    private Integer riskScore;

    @NotBlank(message = "requestId is required")
    @JsonProperty("requestId")
    private String requestId;

    // Default constructor
    public CaseCreationRequest() {}

    public CaseCreationRequest(String customerId, BigDecimal amount, String currency,
                             String payeeId, CaseType caseType, List<String> reasons,
                             Integer riskScore, String requestId) {
        this.customerId = customerId;
        this.amount = amount;
        this.currency = currency;
        this.payeeId = payeeId;
        this.caseType = caseType;
        this.reasons = reasons;
        this.riskScore = riskScore;
        this.requestId = requestId;
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

    public CaseType getCaseType() {
        return caseType;
    }

    public void setCaseType(CaseType caseType) {
        this.caseType = caseType;
    }

    public List<String> getReasons() {
        return reasons;
    }

    public void setReasons(List<String> reasons) {
        this.reasons = reasons;
    }

    public Integer getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Integer riskScore) {
        this.riskScore = riskScore;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CaseCreationRequest that = (CaseCreationRequest) o;
        return Objects.equals(customerId, that.customerId) &&
                Objects.equals(amount, that.amount) &&
                Objects.equals(currency, that.currency) &&
                Objects.equals(payeeId, that.payeeId) &&
                caseType == that.caseType &&
                Objects.equals(reasons, that.reasons) &&
                Objects.equals(riskScore, that.riskScore) &&
                Objects.equals(requestId, that.requestId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(customerId, amount, currency, payeeId, caseType, reasons, riskScore, requestId);
    }

    @Override
    public String toString() {
        return "CaseCreationRequest{" +
                "customerId='" + customerId + '\'' +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", payeeId='" + payeeId + '\'' +
                ", caseType=" + caseType +
                ", reasons=" + reasons +
                ", riskScore=" + riskScore +
                ", requestId='" + requestId + '\'' +
                '}';
    }

    public enum CaseType {
        @JsonProperty("review")
        REVIEW,
        
        @JsonProperty("block")
        BLOCK
    }
}