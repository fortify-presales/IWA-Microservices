package com.microfocus.example.orders;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Orders Microservice
 * WARNING: Contains intentional security vulnerabilities including:
 * - Insecure deserialization
 * - Insecure direct object references (IDOR)
 * DO NOT USE IN PRODUCTION
 */
@SpringBootApplication
public class OrdersServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(OrdersServiceApplication.class, args);
    }
}
