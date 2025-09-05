package com.paynow.risk.service;

import com.paynow.common.dto.RiskSignalsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Risk assessment service with stubbed logic
 * In production, this would integrate with real fraud detection systems
 */
@Service
public class RiskService {

    private static final Logger logger = LoggerFactory.getLogger(RiskService.class);
    private static final Random random = new Random();

    private static final BigDecimal HIGH_AMOUNT_THRESHOLD = new BigDecimal("500.00");
    private static final BigDecimal VERY_HIGH_AMOUNT_THRESHOLD = new BigDecimal("1000.00");

    public RiskSignalsResponse getRiskSignals(String customerId, BigDecimal amount) {
        logger.debug("Calculating risk signals for customer: {} with amount: {}", customerId, amount);

        List<RiskSignalsResponse.RiskFactor> riskFactors = new ArrayList<>();
        int baseRiskScore = calculateBaseRisk(customerId);

        // Amount-based risk
        if (amount.compareTo(VERY_HIGH_AMOUNT_THRESHOLD) > 0) {
            riskFactors.add(new RiskSignalsResponse.RiskFactor("high_amount", "amount > 1000", 9));
            baseRiskScore += 30;
        } else if (amount.compareTo(HIGH_AMOUNT_THRESHOLD) > 0) {
            riskFactors.add(new RiskSignalsResponse.RiskFactor("medium_amount", "amount > 500", 6));
            baseRiskScore += 15;
        }

        // Customer-specific risk factors (stubbed based on customer ID patterns)
        addCustomerRiskFactors(customerId, riskFactors, baseRiskScore);

        // Behavioral risk factors (simulated)
        addBehavioralRiskFactors(customerId, riskFactors);

        // Calculate final risk score
        int finalRiskScore = Math.min(100, Math.max(0, 
                baseRiskScore + riskFactors.stream().mapToInt(RiskSignalsResponse.RiskFactor::getWeight).sum()));

        RiskSignalsResponse.RiskLevel riskLevel = determineRiskLevel(finalRiskScore);

        RiskSignalsResponse response = new RiskSignalsResponse(
                customerId, finalRiskScore, riskFactors, riskLevel);

        logger.debug("Risk assessment completed: customer={}, riskScore={}, level={}", 
                customerId, finalRiskScore, riskLevel);

        return response;
    }

    private int calculateBaseRisk(String customerId) {
        // Simulate customer history-based risk
        int hash = Math.abs(customerId.hashCode());
        return (hash % 30) + 10; // Base risk between 10-40
    }

    private void addCustomerRiskFactors(String customerId, List<RiskSignalsResponse.RiskFactor> factors, int baseScore) {
        // Simulate various risk patterns based on customer ID
        if (customerId.contains("fraud") || customerId.contains("risk")) {
            factors.add(new RiskSignalsResponse.RiskFactor("fraud_history", "previous fraud alerts", 10));
        }
        
        if (customerId.contains("dispute")) {
            factors.add(new RiskSignalsResponse.RiskFactor("recent_disputes", "disputes in last 30 days", 7));
        }
        
        if (customerId.contains("new") || customerId.endsWith("_new")) {
            factors.add(new RiskSignalsResponse.RiskFactor("new_customer", "account age < 30 days", 5));
        }

        // Simulate device/location changes
        if (random.nextInt(10) < 3) { // 30% chance
            factors.add(new RiskSignalsResponse.RiskFactor("device_change", "new device detected", 4));
        }

        if (random.nextInt(10) < 2) { // 20% chance  
            factors.add(new RiskSignalsResponse.RiskFactor("location_change", "unusual location", 6));
        }
    }

    private void addBehavioralRiskFactors(String customerId, List<RiskSignalsResponse.RiskFactor> factors) {
        // Simulate time-based risk
        int hour = java.time.LocalTime.now().getHour();
        if (hour < 6 || hour > 23) {
            factors.add(new RiskSignalsResponse.RiskFactor("unusual_time", "transaction at unusual hour", 3));
        }

        // Simulate velocity risk
        if (random.nextInt(10) < 2) { // 20% chance
            factors.add(new RiskSignalsResponse.RiskFactor("high_velocity", "multiple recent transactions", 5));
        }

        // Simulate network risk
        if (random.nextInt(20) < 1) { // 5% chance
            factors.add(new RiskSignalsResponse.RiskFactor("proxy_usage", "transaction via proxy/VPN", 8));
        }
    }

    private RiskSignalsResponse.RiskLevel determineRiskLevel(int riskScore) {
        if (riskScore >= 80) {
            return RiskSignalsResponse.RiskLevel.CRITICAL;
        } else if (riskScore >= 60) {
            return RiskSignalsResponse.RiskLevel.HIGH;
        } else if (riskScore >= 40) {
            return RiskSignalsResponse.RiskLevel.MEDIUM;
        } else {
            return RiskSignalsResponse.RiskLevel.LOW;
        }
    }
}