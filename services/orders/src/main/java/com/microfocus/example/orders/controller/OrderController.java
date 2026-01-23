package com.microfocus.example.orders.controller;

import com.microfocus.example.contracts.model.Order;
import com.microfocus.example.orders.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Orders REST Controller
 * WARNING: Contains intentional security vulnerabilities including IDOR and insecure deserialization
 */
@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);
    
    private final OrderService orderService;
    
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }
    
    /**
     * VULNERABILITY: IDOR - No authorization check
     */
    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
        logger.debug("Fetching order: {}", id);
        Order order = orderService.getOrderById(id);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(order);
    }
    
    /**
     * VULNERABILITY: IDOR - No check if user has access to these orders
     */
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<Order>> getOrdersByCustomerId(@PathVariable Long customerId) {
        return ResponseEntity.ok(orderService.getOrdersByCustomerId(customerId));
    }
    
    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    /**
     * VULNERABILITY: Exposes a search endpoint that is vulnerable to SQL Injection
     * if the backend does not properly parameterize inputs. This endpoint
     * forwards the raw `q` parameter to the service/repository which concatenates
     * it into SQL.
     */
    @GetMapping("/search")
    public ResponseEntity<List<Order>> searchOrders(@RequestParam("q") String q) {
        return ResponseEntity.ok(orderService.searchOrders(q));
    }
    
    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody Order order) {
        logger.debug("Creating order for customer: {}", order.getCustomerId());
        Order created = orderService.createOrder(order);
        return ResponseEntity.ok(created);
    }
    
    /**
     * VULNERABILITY: IDOR - No check if user owns this order
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<Void> updateOrderStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        String status = request.get("status");
        logger.debug("Updating order {} to status: {}", id, status);
        orderService.updateOrderStatus(id, status);
        return ResponseEntity.ok().build();
    }
    
    /**
     * VULNERABILITY: Insecure Deserialization
     * Accepts serialized order data and deserializes it without validation
     */
    @PostMapping("/import")
    public ResponseEntity<?> importOrder(@RequestBody Map<String, String> request) {
        try {
            String serializedData = request.get("data");
            logger.debug("Importing serialized order data");
            
            // Deserialize potentially malicious data
            Order order = orderService.deserializeOrder(serializedData);
            Order created = orderService.createOrder(order);
            
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            logger.error("Error importing order", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Export order as serialized data
     */
    @GetMapping("/{id}/export")
    public ResponseEntity<?> exportOrder(@PathVariable Long id) {
        try {
            Order order = orderService.getOrderById(id);
            if (order == null) {
                return ResponseEntity.notFound().build();
            }
            
            String serialized = orderService.serializeOrder(order);
            return ResponseEntity.ok(Map.of("data", serialized));
        } catch (Exception e) {
            logger.error("Error exporting order", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
