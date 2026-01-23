package com.microfocus.example.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Inventory Microservice
 * WARNING: Contains intentional security vulnerabilities including:
 * - XML External Entity (XXE) attacks
 * - Unsafe XML parsing
 * DO NOT USE IN PRODUCTION
 */
@SpringBootApplication
public class InventoryServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(InventoryServiceApplication.class, args);
    }
}
