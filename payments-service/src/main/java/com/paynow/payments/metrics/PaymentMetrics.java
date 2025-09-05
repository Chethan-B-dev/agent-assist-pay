package com.paynow.payments.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

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

    public double getP95Latency() {
        // Get a snapshot of the current timer statistics
        return Arrays.stream(requestLatencyTimer.takeSnapshot()
                // Get all percentile values as an array
                .percentileValues())
                // Convert the array to a stream for filtering
                // Find the percentile that's approximately 0.95 (95%)
                // We use Math.abs to find values within 0.001 of 0.95
                .filter(v -> Math.abs(v.percentile() - 0.95) < 0.001)
                // Take the first matching percentile
                .findFirst()
                // If we found a match, convert its value to milliseconds
                .map(v -> v.value(TimeUnit.MILLISECONDS))
                // If no 95th percentile was found, return 0.0 as a default
                .orElse(0.0);
    }
}