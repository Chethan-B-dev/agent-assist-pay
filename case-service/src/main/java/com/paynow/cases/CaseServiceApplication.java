package com.paynow.cases;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for PayNow Case Service
 */
@SpringBootApplication(scanBasePackages = {"com.paynow.cases", "com.paynow.common"})
public class CaseServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CaseServiceApplication.class, args);
    }
}