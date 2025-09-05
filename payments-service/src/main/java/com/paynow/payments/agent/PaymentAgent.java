package com.paynow.payments.agent;

import com.paynow.common.dto.AccountBalanceResponse;
import com.paynow.common.dto.AgentTraceStep;
import com.paynow.common.dto.CaseCreationRequest;
import com.paynow.common.dto.PaymentDecisionRequest;
import com.paynow.common.dto.PaymentDecisionResponse;
import com.paynow.common.dto.RiskSignalsResponse;
import com.paynow.common.exception.PaymentException;
import com.paynow.payments.agent.tools.AccountTool;
import com.paynow.payments.agent.tools.CaseTool;
import com.paynow.payments.agent.tools.RiskTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * AI Agent orchestrator for payment decisions
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentAgent {
    
    private static final BigDecimal HIGH_AMOUNT_THRESHOLD = new BigDecimal("500.00");
    private static final BigDecimal VERY_HIGH_AMOUNT_THRESHOLD = new BigDecimal("1000.00");

    private final AccountTool accountTool;
    private final RiskTool riskTool;
    private final CaseTool caseTool;

    @Retryable(value = {Exception.class}, maxAttempts = 2, backoff = @Backoff(delay = 500))
    public PaymentDecisionResponse processPayment(PaymentDecisionRequest request, String requestId) {
        List<AgentTraceStep> agentTrace = new ArrayList<>();
        List<String> reasons = new ArrayList<>();

        try {
            // Step 1: Planning
            agentTrace.add(AgentTraceStep.plan(
                    "Analyzing payment: " + request.getAmount() + " " + request.getCurrency() + 
                    " - will check balance, risk signals, and apply decision rules"));

            // Step 2: Gather information in parallel
            long toolStartTime = System.currentTimeMillis();
            
            CompletableFuture<AccountBalanceResponse> balanceFuture = CompletableFuture
                    .supplyAsync(() -> {
                        long start = System.currentTimeMillis();
                        AccountBalanceResponse balance = accountTool.getBalance(request.getCustomerId());
                        long duration = System.currentTimeMillis() - start;
                        agentTrace.add(AgentTraceStep.tool("getBalance", 
                                String.format("balance=%.2f, available=%.2f, status=%s", 
                                        balance.getBalance(), balance.getAvailableBalance(), 
                                        balance.getAccountStatus()), duration));
                        return balance;
                    });

            CompletableFuture<RiskSignalsResponse> riskFuture = CompletableFuture
                    .supplyAsync(() -> {
                        long start = System.currentTimeMillis();
                        RiskSignalsResponse risk = riskTool.getRiskSignals(request.getCustomerId(), request.getAmount());
                        long duration = System.currentTimeMillis() - start;
                        
                        String riskDetails = String.format("riskScore=%d, level=%s, factors=%s",
                                risk.getRiskScore(), risk.getRiskLevel(), 
                                risk.getRiskFactors().stream()
                                        .map(RiskSignalsResponse.RiskFactor::toString)
                                        .toList());
                        
                        agentTrace.add(AgentTraceStep.tool("getRiskSignals", riskDetails, duration));
                        return risk;
                    });

            // Wait for both tools to complete
            CompletableFuture.allOf(balanceFuture, riskFuture).join();
            
            AccountBalanceResponse balance = balanceFuture.get();
            RiskSignalsResponse risk = riskFuture.get();

            long toolsCompletedTime = System.currentTimeMillis();
            log.info("Agent tools completed in {}ms", toolsCompletedTime - toolStartTime);

            // Step 3: Decision logic
            PaymentDecisionResponse.DecisionType decision = makeDecision(
                    request, balance, risk, reasons, agentTrace);

            // Step 3.1: Concurrency safety - reserve balance on ALLOW
            if (decision == PaymentDecisionResponse.DecisionType.ALLOW) {
                long reserveStart = System.currentTimeMillis();
                try {
                    accountTool.reserveBalance(request.getCustomerId(), request.getAmount(), requestId);
                    long reserveDuration = System.currentTimeMillis() - reserveStart;
                    agentTrace.add(AgentTraceStep.tool("reserveBalance",
                            String.format("reserved=%.2f", request.getAmount()),
                            reserveDuration));
                } catch (PaymentException e) {
                    reasons.add("reserve_failed");
                    agentTrace.add(AgentTraceStep.error("reserveBalance failed: " + e.getMessage()));
                    agentTrace.add(AgentTraceStep.decision("Reservation failed after ALLOW - BLOCK"));
                    decision = PaymentDecisionResponse.DecisionType.BLOCK;
                } catch (Exception e) {
                    reasons.add("reserve_failed");
                    agentTrace.add(AgentTraceStep.error("reserveBalance unexpected error: " + e.getMessage()));
                    agentTrace.add(AgentTraceStep.decision("Reservation failed after ALLOW - BLOCK"));
                    decision = PaymentDecisionResponse.DecisionType.BLOCK;
                }
            }

            // Step 4: Create case if needed (for review or block)
            if (decision != PaymentDecisionResponse.DecisionType.ALLOW) {
                long caseStartTime = System.currentTimeMillis();
                
                CaseCreationRequest caseRequest = new CaseCreationRequest(
                        request.getCustomerId(), request.getAmount(), request.getCurrency(),
                        request.getPayeeId(), 
                        decision == PaymentDecisionResponse.DecisionType.REVIEW ? 
                                CaseCreationRequest.CaseType.REVIEW : CaseCreationRequest.CaseType.BLOCK,
                        new ArrayList<>(reasons), risk.getRiskScore(), requestId);

                caseTool.createCase(caseRequest);
                
                long caseDuration = System.currentTimeMillis() - caseStartTime;
                agentTrace.add(AgentTraceStep.tool("createCase", 
                        "Created " + decision.toString().toLowerCase() + " case with ID: " + 
                        "case_" + requestId.substring(4), caseDuration));
            }

            // Step 5: Final decision trace
            agentTrace.add(AgentTraceStep.decision(
                    String.format("Final decision: %s based on %d factors", 
                            decision, reasons.size())));

            return new PaymentDecisionResponse(decision, reasons, agentTrace, requestId);

        } catch (Exception e) {
            log.error("Agent processing failed: {}", e.getMessage(), e);
            agentTrace.add(AgentTraceStep.error("Agent processing failed: " + e.getMessage()));
            
            // Return a safe decision on agent failure
            reasons.add("agent_processing_error");
            return new PaymentDecisionResponse(
                    PaymentDecisionResponse.DecisionType.REVIEW, 
                    reasons, agentTrace, requestId);
        }
    }

    private PaymentDecisionResponse.DecisionType makeDecision(
            PaymentDecisionRequest request,
            AccountBalanceResponse balance,
            RiskSignalsResponse risk,
            List<String> reasons,
            List<AgentTraceStep> agentTrace) {

        agentTrace.add(AgentTraceStep.plan("Evaluating decision factors"));

        // Check account status first
        if (!"ACTIVE".equals(balance.getAccountStatus())) {
            reasons.add("account_not_active");
            agentTrace.add(AgentTraceStep.decision("Account not active - BLOCK"));
            return PaymentDecisionResponse.DecisionType.BLOCK;
        }

        // Check insufficient funds
        if (balance.getAvailableBalance().compareTo(request.getAmount()) < 0) {
            reasons.add("insufficient_funds");
            agentTrace.add(AgentTraceStep.decision("Insufficient funds - BLOCK"));
            return PaymentDecisionResponse.DecisionType.BLOCK;
        }

        // Risk-based decisions
        boolean hasHighRisk = false;
        
        if (risk.getRiskLevel() == RiskSignalsResponse.RiskLevel.CRITICAL) {
            reasons.add("critical_risk_level");
            hasHighRisk = true;
        } else if (risk.getRiskLevel() == RiskSignalsResponse.RiskLevel.HIGH) {
            reasons.add("high_risk_level");
            hasHighRisk = true;
        }

        if (risk.getRiskScore() > 80) {
            reasons.add("high_risk_score");
            hasHighRisk = true;
        }

        // Check individual risk factors
        for (RiskSignalsResponse.RiskFactor factor : risk.getRiskFactors()) {
            if (factor.getWeight() >= 8) { // High weight factors
                reasons.add("risk_factor_" + factor.getType());
                if (factor.getWeight() >= 9) {
                    hasHighRisk = true;
                }
            }
        }

        // Amount-based rules
        boolean highAmountTransaction = request.getAmount().compareTo(HIGH_AMOUNT_THRESHOLD) > 0;
        boolean veryHighAmountTransaction = request.getAmount().compareTo(VERY_HIGH_AMOUNT_THRESHOLD) > 0;

        if (veryHighAmountTransaction) {
            reasons.add("very_high_amount_transaction");
            hasHighRisk = true;
        } else if (highAmountTransaction) {
            reasons.add("high_amount_transaction");
        }

        // Final decision logic
        if (hasHighRisk) {
            if (veryHighAmountTransaction || risk.getRiskLevel() == RiskSignalsResponse.RiskLevel.CRITICAL) {
                agentTrace.add(AgentTraceStep.decision("High risk + critical factors - BLOCK"));
                return PaymentDecisionResponse.DecisionType.BLOCK;
            } else {
                agentTrace.add(AgentTraceStep.decision("High risk detected - REVIEW"));
                return PaymentDecisionResponse.DecisionType.REVIEW;
            }
        }

        if (highAmountTransaction && risk.getRiskLevel() == RiskSignalsResponse.RiskLevel.MEDIUM) {
            reasons.add("medium_risk_high_amount");
            agentTrace.add(AgentTraceStep.decision("Medium risk + high amount - REVIEW"));
            return PaymentDecisionResponse.DecisionType.REVIEW;
        }

        // If we get here, allow the transaction
        if (reasons.isEmpty()) {
            reasons.add("low_risk_transaction");
        }
        
        agentTrace.add(AgentTraceStep.decision("Low risk transaction - ALLOW"));
        return PaymentDecisionResponse.DecisionType.ALLOW;
    }
}