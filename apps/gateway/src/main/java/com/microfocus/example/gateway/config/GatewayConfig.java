package com.microfocus.example.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Gateway Route Configuration
 * WARNING: No authentication or authorization
 */
@Configuration
public class GatewayConfig {
    
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
    
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            // Catalog Service routes
            .route("catalog", r -> r
                .path("/api/products/**")
                .uri(catalogServiceUrl))
            
            // Customers Service routes
            .route("customers", r -> r
                .path("/api/customers/**")
                .uri(customersServiceUrl))
            
            // Orders Service routes
            .route("orders", r -> r
                .path("/api/orders/**")
                .uri(ordersServiceUrl))
            
            // Payments Service routes
            .route("payments", r -> r
                .path("/api/payments/**")
                .uri(paymentsServiceUrl))
            
            // Prescriptions Service routes
            .route("prescriptions", r -> r
                .path("/api/prescriptions/**")
                .uri(prescriptionsServiceUrl))
            
            // Inventory Service routes
            .route("inventory", r -> r
                .path("/api/inventory/**")
                .uri(inventoryServiceUrl))
            
            // Notifications Service routes
            .route("notifications", r -> r
                .path("/api/notifications/**")
                .uri(notificationsServiceUrl))
            
            .build();
    }
}
