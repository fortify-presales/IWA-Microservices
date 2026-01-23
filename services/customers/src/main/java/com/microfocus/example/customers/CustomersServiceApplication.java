package com.microfocus.example.customers;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Customers Microservice - Customer Management and Authentication
 * WARNING: Contains intentional security vulnerabilities including:
 * - Weak authentication
 * - Plain text password storage
 * - Hardcoded JWT secrets
 * DO NOT USE IN PRODUCTION
 */
@SpringBootApplication
public class CustomersServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(CustomersServiceApplication.class, args);
    }
}
