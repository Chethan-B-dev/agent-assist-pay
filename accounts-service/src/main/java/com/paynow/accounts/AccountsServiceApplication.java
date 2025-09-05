package com.paynow.accounts;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for PayNow Accounts Service
 */
@SpringBootApplication(scanBasePackages = {"com.paynow.accounts", "com.paynow.common"})
public class AccountsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AccountsServiceApplication.class, args);
    }
}