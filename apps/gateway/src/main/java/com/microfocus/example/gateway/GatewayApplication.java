package com.microfocus.example.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API Gateway
 * WARNING: This application contains intentional security vulnerabilities for testing purposes
 * - No rate limiting
 * - No authentication/authorization
 * - Permissive CORS
 * - No request validation
 * DO NOT USE IN PRODUCTION
 */
@SpringBootApplication
public class GatewayApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
