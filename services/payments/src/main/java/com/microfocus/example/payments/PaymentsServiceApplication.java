package com.microfocus.example.payments;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Payments Microservice
 * WARNING: Contains intentional security vulnerabilities including:
 * - Hardcoded API keys and secrets
 * - Exposed sensitive credentials
 * - Logging of sensitive payment data
 * DO NOT USE IN PRODUCTION
 */
@SpringBootApplication
public class PaymentsServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(PaymentsServiceApplication.class, args);
    }
}
