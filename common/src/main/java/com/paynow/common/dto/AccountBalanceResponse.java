package com.paynow.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Response DTO for account balance queries
 */
public class AccountBalanceResponse {

    @JsonProperty("customerId")
    private String customerId;

    @JsonProperty("balance")
    private BigDecimal balance;

    @JsonProperty("availableBalance")
    private BigDecimal availableBalance;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("accountStatus")
    private String accountStatus;

    // Default constructor
    public AccountBalanceResponse() {}

    public AccountBalanceResponse(String customerId, BigDecimal balance, 
                                BigDecimal availableBalance, String currency, String accountStatus) {
        this.customerId = customerId;
        this.balance = balance;
        this.availableBalance = availableBalance;
        this.currency = currency;
        this.accountStatus = accountStatus;
    }

    // Getters and Setters
    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }

    public void setAvailableBalance(BigDecimal availableBalance) {
        this.availableBalance = availableBalance;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(String accountStatus) {
        this.accountStatus = accountStatus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AccountBalanceResponse that = (AccountBalanceResponse) o;
        return Objects.equals(customerId, that.customerId) &&
                Objects.equals(balance, that.balance) &&
                Objects.equals(availableBalance, that.availableBalance) &&
                Objects.equals(currency, that.currency) &&
                Objects.equals(accountStatus, that.accountStatus);
    }

    @Override
    public int hashCode() {
        return Objects.hash(customerId, balance, availableBalance, currency, accountStatus);
    }

    @Override
    public String toString() {
        return "AccountBalanceResponse{" +
                "customerId='" + customerId + '\'' +
                ", balance=" + balance +
                ", availableBalance=" + availableBalance +
                ", currency='" + currency + '\'' +
                ", accountStatus='" + accountStatus + '\'' +
                '}';
    }
}