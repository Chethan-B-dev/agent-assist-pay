package com.paynow.risk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for PayNow Risk Service
 */
@SpringBootApplication(scanBasePackages = {"com.paynow.risk", "com.paynow.common"})
public class RiskServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RiskServiceApplication.class, args);
    }
}