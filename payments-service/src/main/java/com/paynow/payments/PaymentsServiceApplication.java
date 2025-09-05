package com.paynow.payments;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Main application class for PayNow Payments Service
 */
@SpringBootApplication(scanBasePackages = {"com.paynow.payments", "com.paynow.common"})
@ConfigurationPropertiesScan
public class PaymentsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentsServiceApplication.class, args);
    }
}