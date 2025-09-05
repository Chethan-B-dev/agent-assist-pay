package com.paynow.payments.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Custom metrics for payment processing
 */
@Component
public class PaymentMetrics {

    private final Counter totalRequestsCounter;
    private final Counter allowDecisionCounter;
    private final Counter reviewDecisionCounter;
    private final Counter blockDecisionCounter;
    private final Counter errorCounter;
    private final Counter rateLimitedCounter;
    private final Counter cachedResponseCounter;
    private final Timer requestLatencyTimer;

    public PaymentMetrics(MeterRegistry meterRegistry) {
        // Total requests counter
        this.totalRequestsCounter = Counter.builder("payment.requests.total")
                .description("Total number of payment decision requests")
                .register(meterRegistry);

        // Decision counters
        this.allowDecisionCounter = Counter.builder("payment.decisions.allow.total")
                .description("Number of ALLOW decisions")
                .register(meterRegistry);

        this.reviewDecisionCounter = Counter.builder("payment.decisions.review.total")
                .description("Number of REVIEW decisions")
                .register(meterRegistry);

        this.blockDecisionCounter = Counter.builder("payment.decisions.block.total")
                .description("Number of BLOCK decisions")
                .register(meterRegistry);

        // Error counters
        this.errorCounter = Counter.builder("payment.requests.error.total")
                .description("Number of requests that resulted in errors")
                .register(meterRegistry);

        this.rateLimitedCounter = Counter.builder("payment.requests.rate_limited.total")
                .description("Number of requests that were rate limited")
                .register(meterRegistry);

        this.cachedResponseCounter = Counter.builder("payment.requests.cached.total")
                .description("Number of requests served from cache")
                .register(meterRegistry);

        // Latency timer
        this.requestLatencyTimer = Timer.builder("payment.requests.duration")
                .description("Request processing time")
                .register(meterRegistry);
    }

    /**
     * Record a payment request with its outcome
     */
    public void recordRequest(String outcome) {
        totalRequestsCounter.increment();
        
        switch (outcome.toLowerCase()) {
            case "allow" -> allowDecisionCounter.increment();
            case "review" -> reviewDecisionCounter.increment();
            case "block" -> blockDecisionCounter.increment();
            case "error" -> errorCounter.increment();
            case "rate_limited" -> rateLimitedCounter.increment();
            case "cached" -> cachedResponseCounter.increment();
        }
    }

    /**
     * Record request latency
     */
    public void recordLatency(Instant startTime) {
        Duration duration = Duration.between(startTime, Instant.now());
        requestLatencyTimer.record(duration);
    }

    /**
     * Record latency with explicit duration
     */
    public void recordLatency(Duration duration) {
        requestLatencyTimer.record(duration);
    }

    // Getters for testing and monitoring
    public double getTotalRequests() {
        return totalRequestsCounter.count();
    }

    public double getAllowDecisions() {
        return allowDecisionCounter.count();
    }

    public double getReviewDecisions() {
        return reviewDecisionCounter.count();
    }

    public double getBlockDecisions() {
        return blockDecisionCounter.count();
    }

    public double getErrorCount() {
        return errorCounter.count();
    }

    public double getRateLimitedCount() {
        return rateLimitedCounter.count();
    }

    public double getCachedResponseCount() {
        return cachedResponseCounter.count();
    }

    public double getP95Latency() {
        // Get all percentile values
        var percentiles = requestLatencyTimer.takeSnapshot().percentileValues();

        // Find the 95th percentile or closest to it
        for (var percentile : percentiles) {
            if (Math.abs(percentile.percentile() - 0.95) < 0.001) {
                return percentile.value();
            }
        }

        // Fallback if exact 95th percentile isn't found
        return requestLatencyTimer.mean(java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    public double getAverageLatency() {
        return requestLatencyTimer.mean(java.util.concurrent.TimeUnit.MILLISECONDS);
    }
}