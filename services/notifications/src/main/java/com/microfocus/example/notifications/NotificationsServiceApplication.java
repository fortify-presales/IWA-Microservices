package com.microfocus.example.notifications;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Notifications Microservice
 * WARNING: Contains intentional security vulnerabilities including:
 * - Command Injection
 * - Unsafe system command execution
 * - Path traversal vulnerabilities
 * DO NOT USE IN PRODUCTION
 */
@SpringBootApplication
public class NotificationsServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(NotificationsServiceApplication.class, args);
    }
}
