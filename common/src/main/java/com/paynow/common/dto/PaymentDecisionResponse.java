package com.paynow.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;

/**
 * Response DTO for payment decision API
 */
public class PaymentDecisionResponse {

    @JsonProperty("decision")
    private DecisionType decision;

    @JsonProperty("reasons")
    private List<String> reasons;

    @JsonProperty("agentTrace")
    private List<AgentTraceStep> agentTrace;

    @JsonProperty("requestId")
    private String requestId;

    // Default constructor
    public PaymentDecisionResponse() {}

    public PaymentDecisionResponse(DecisionType decision, List<String> reasons, 
                                 List<AgentTraceStep> agentTrace, String requestId) {
        this.decision = decision;
        this.reasons = reasons;
        this.agentTrace = agentTrace;
        this.requestId = requestId;
    }

    // Getters and Setters
    public DecisionType getDecision() {
        return decision;
    }

    public void setDecision(DecisionType decision) {
        this.decision = decision;
    }

    public List<String> getReasons() {
        return reasons;
    }

    public void setReasons(List<String> reasons) {
        this.reasons = reasons;
    }

    public List<AgentTraceStep> getAgentTrace() {
        return agentTrace;
    }

    public void setAgentTrace(List<AgentTraceStep> agentTrace) {
        this.agentTrace = agentTrace;
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
        PaymentDecisionResponse that = (PaymentDecisionResponse) o;
        return decision == that.decision &&
                Objects.equals(reasons, that.reasons) &&
                Objects.equals(agentTrace, that.agentTrace) &&
                Objects.equals(requestId, that.requestId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(decision, reasons, agentTrace, requestId);
    }

    @Override
    public String toString() {
        return "PaymentDecisionResponse{" +
                "decision=" + decision +
                ", reasons=" + reasons +
                ", agentTrace=" + agentTrace +
                ", requestId='" + requestId + '\'' +
                '}';
    }

    public enum DecisionType {
        @JsonProperty("allow")
        ALLOW,
        
        @JsonProperty("review")
        REVIEW,
        
        @JsonProperty("block")
        BLOCK
    }
}