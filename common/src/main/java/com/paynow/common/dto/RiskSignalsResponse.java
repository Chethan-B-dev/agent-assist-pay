package com.paynow.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;

/**
 * Response DTO for risk assessment queries
 */
public class RiskSignalsResponse {

    @JsonProperty("customerId")
    private String customerId;

    @JsonProperty("riskScore")
    private Integer riskScore; // 0-100, higher is riskier

    @JsonProperty("riskFactors")
    private List<RiskFactor> riskFactors;

    @JsonProperty("riskLevel")
    private RiskLevel riskLevel;

    // Default constructor
    public RiskSignalsResponse() {}

    public RiskSignalsResponse(String customerId, Integer riskScore, 
                             List<RiskFactor> riskFactors, RiskLevel riskLevel) {
        this.customerId = customerId;
        this.riskScore = riskScore;
        this.riskFactors = riskFactors;
        this.riskLevel = riskLevel;
    }

    // Getters and Setters
    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public Integer getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Integer riskScore) {
        this.riskScore = riskScore;
    }

    public List<RiskFactor> getRiskFactors() {
        return riskFactors;
    }

    public void setRiskFactors(List<RiskFactor> riskFactors) {
        this.riskFactors = riskFactors;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(RiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RiskSignalsResponse that = (RiskSignalsResponse) o;
        return Objects.equals(customerId, that.customerId) &&
                Objects.equals(riskScore, that.riskScore) &&
                Objects.equals(riskFactors, that.riskFactors) &&
                riskLevel == that.riskLevel;
    }

    @Override
    public int hashCode() {
        return Objects.hash(customerId, riskScore, riskFactors, riskLevel);
    }

    @Override
    public String toString() {
        return "RiskSignalsResponse{" +
                "customerId='" + customerId + '\'' +
                ", riskScore=" + riskScore +
                ", riskFactors=" + riskFactors +
                ", riskLevel=" + riskLevel +
                '}';
    }

    public static class RiskFactor {
        @JsonProperty("type")
        private String type;

        @JsonProperty("value")
        private String value;

        @JsonProperty("weight")
        private Integer weight; // 1-10, contribution to overall score

        public RiskFactor() {}

        public RiskFactor(String type, String value, Integer weight) {
            this.type = type;
            this.value = value;
            this.weight = weight;
        }

        // Getters and Setters
        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public Integer getWeight() {
            return weight;
        }

        public void setWeight(Integer weight) {
            this.weight = weight;
        }

        @Override
        public String toString() {
            return type + "=" + value + "(weight:" + weight + ")";
        }
    }

    public enum RiskLevel {
        @JsonProperty("low")
        LOW,
        
        @JsonProperty("medium")
        MEDIUM,
        
        @JsonProperty("high")
        HIGH,
        
        @JsonProperty("critical")
        CRITICAL
    }
}