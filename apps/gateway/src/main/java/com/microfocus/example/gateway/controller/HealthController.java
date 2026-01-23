package com.microfocus.example.gateway.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Health Check Controller
 */
@RestController
public class HealthController {
    
    @Value("${services.catalog.url}")
    private String catalogServiceUrl;
    
    @Value("${services.customers.url}")
    private String customersServiceUrl;
    
    @Value("${services.orders.url}")
    private String ordersServiceUrl;
    
    @Value("${services.payments.url}")
    private String paymentsServiceUrl;
    
    @Value("${services.prescriptions.url}")
    private String prescriptionsServiceUrl;
    
    @Value("${services.inventory.url}")
    private String inventoryServiceUrl;
    
    @Value("${services.notifications.url}")
    private String notificationsServiceUrl;
    
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "API Gateway");
        
        Map<String, String> services = new HashMap<>();
        services.put("catalog", catalogServiceUrl);
        services.put("customers", customersServiceUrl);
        services.put("orders", ordersServiceUrl);
        services.put("payments", paymentsServiceUrl);
        services.put("prescriptions", prescriptionsServiceUrl);
        services.put("inventory", inventoryServiceUrl);
        services.put("notifications", notificationsServiceUrl);
        
        health.put("services", services);
        
        return health;
    }
}
