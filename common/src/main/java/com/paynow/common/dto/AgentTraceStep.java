package com.paynow.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Objects;

/**
 * Represents a single step in the agent's decision trace
 */
public class AgentTraceStep {

    @JsonProperty("step")
    private String step;

    @JsonProperty("detail")
    private String detail;

    @JsonProperty("timestamp")
    private Instant timestamp;

    @JsonProperty("durationMs")
    private Long durationMs;

    // Default constructor
    public AgentTraceStep() {}

    public AgentTraceStep(String step, String detail) {
        this.step = step;
        this.detail = detail;
        this.timestamp = Instant.now();
    }

    public AgentTraceStep(String step, String detail, Long durationMs) {
        this.step = step;
        this.detail = detail;
        this.timestamp = Instant.now();
        this.durationMs = durationMs;
    }

    // Static factory methods for common steps
    public static AgentTraceStep plan(String detail) {
        return new AgentTraceStep("plan", detail);
    }

    public static AgentTraceStep tool(String toolName, String detail) {
        return new AgentTraceStep("tool:" + toolName, detail);
    }

    public static AgentTraceStep tool(String toolName, String detail, Long durationMs) {
        return new AgentTraceStep("tool:" + toolName, detail, durationMs);
    }

    public static AgentTraceStep decision(String detail) {
        return new AgentTraceStep("decision", detail);
    }

    public static AgentTraceStep error(String detail) {
        return new AgentTraceStep("error", detail);
    }

    // Getters and Setters
    public String getStep() {
        return step;
    }

    public void setStep(String step) {
        this.step = step;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AgentTraceStep that = (AgentTraceStep) o;
        return Objects.equals(step, that.step) &&
                Objects.equals(detail, that.detail) &&
                Objects.equals(timestamp, that.timestamp) &&
                Objects.equals(durationMs, that.durationMs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(step, detail, timestamp, durationMs);
    }

    @Override
    public String toString() {
        return "AgentTraceStep{" +
                "step='" + step + '\'' +
                ", detail='" + detail + '\'' +
                ", timestamp=" + timestamp +
                ", durationMs=" + durationMs +
                '}';
    }
}