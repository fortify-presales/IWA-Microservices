package com.microfocus.example.prescriptions;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Prescriptions Microservice
 * WARNING: Contains intentional security vulnerabilities including:
 * - Insecure Direct Object References (IDOR)
 * - Missing authorization checks
 * - Exposure of sensitive medical data
 * DO NOT USE IN PRODUCTION
 */
@SpringBootApplication
public class PrescriptionsServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(PrescriptionsServiceApplication.class, args);
    }
}
