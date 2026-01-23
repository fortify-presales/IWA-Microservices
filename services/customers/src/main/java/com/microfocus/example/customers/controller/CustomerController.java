package com.microfocus.example.customers.controller;

import com.microfocus.example.contracts.model.Customer;
import com.microfocus.example.customers.service.CustomerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Customer REST Controller
 * WARNING: Contains intentional security vulnerabilities
 */
@RestController
@RequestMapping("/api/customers")
@CrossOrigin(origins = "*") // Intentionally permissive CORS
public class CustomerController {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomerController.class);
    
    private final CustomerService customerService;
    
    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }
    
    /**
     * VULNERABILITY: Weak authentication, SQL injection, plain text passwords
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");
        
        logger.debug("Login attempt for user: {} with password: {}", username, password); // Logs password!
        
        String token = customerService.authenticateAndGenerateToken(username, password);
        
        if (token != null) {
            Customer customer = customerService.getCustomerByUsername(username);
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("customerId", customer.getId());
            response.put("username", customer.getUsername());
            return ResponseEntity.ok(response);
        }
        
        return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
    }
    
    /**
     * VULNERABILITY: Stores password in plain text
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Customer customer) {
        logger.debug("Registering new customer: {} with password: {}", 
                    customer.getUsername(), customer.getPassword()); // Logs password!
        
        try {
            Customer created = customerService.registerCustomer(customer);
            String token = customerService.authenticateAndGenerateToken(
                created.getUsername(), created.getPassword());
            
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("customerId", created.getId());
            response.put("username", created.getUsername());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * VULNERABILITY: No authorization check - anyone can view any customer
     */
    @GetMapping("/{id}")
    public ResponseEntity<Customer> getCustomerById(@PathVariable Long id) {
        Customer customer = customerService.getCustomerById(id);
        if (customer == null) {
            return ResponseEntity.notFound().build();
        }
        // Returns customer with plain text password!
        return ResponseEntity.ok(customer);
    }
    
    /**
     * VULNERABILITY: Exposes all customers with plain text passwords
     */
    @GetMapping
    public ResponseEntity<List<Customer>> getAllCustomers() {
        return ResponseEntity.ok(customerService.getAllCustomers());
    }
    
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        String username = request.get("username");
        
        boolean valid = customerService.validateToken(token, username);
        
        Map<String, Object> response = new HashMap<>();
        response.put("valid", valid);
        
        if (valid) {
            response.put("username", customerService.extractUsernameFromToken(token));
        }
        
        return ResponseEntity.ok(response);
    }
}
