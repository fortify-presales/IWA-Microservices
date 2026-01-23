package com.microfocus.example.catalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Catalog Microservice - Product Catalog Management
 * WARNING: This application contains intentional security vulnerabilities for testing purposes
 * DO NOT USE IN PRODUCTION
 */
@SpringBootApplication
public class CatalogServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(CatalogServiceApplication.class, args);
    }
}
