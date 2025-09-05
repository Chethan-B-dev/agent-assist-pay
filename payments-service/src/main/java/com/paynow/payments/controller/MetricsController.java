package com.paynow.payments.controller;

import com.paynow.payments.metrics.PaymentMetrics;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class MetricsController {

    private final PaymentMetrics paymentMetrics;

    @GetMapping("/metrics/p95")
    public Map<String, Double> getP95Latency() {
        return Map.of("p95_latency_ms", paymentMetrics.getP95Latency());
    }
}