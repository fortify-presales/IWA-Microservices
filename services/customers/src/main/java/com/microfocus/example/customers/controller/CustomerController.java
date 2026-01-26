package com.microfocus.example.customers.controller;

import com.microfocus.example.contracts.model.Customer;
import com.microfocus.example.customers.service.CustomerService;
import com.microfocus.example.customers.dto.TokenValidationRequest;
import com.microfocus.example.customers.dto.LoginRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

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
    @Operation(summary = "Authenticate user and return JWT token", description = "Authenticates a user with username and password and returns a JWT token on success")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoginRequest.class), examples = @ExampleObject(value = "{\"username\":\"john.doe\",\"password\":\"password123\"}")))
     @ApiResponses({
         @ApiResponse(responseCode = "200", description = "Authentication successful", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"token\":\"<token>\",\"customerId\":1,\"username\":\"john.doe\"}"))),
         @ApiResponse(responseCode = "400", description = "Bad request (validation errors)", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"timestamp\":\"2026-01-26T12:34:56.789+00:00\",\"status\":400,\"error\":\"Bad Request\",\"message\":\"Validation failed for one or more fields\",\"path\":\"/api/customers/login\",\"errors\":[\"username: must not be blank\",\"password: must not be blank\"]}"))),
         @ApiResponse(responseCode = "401", description = "Invalid credentials")
     })
     public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest credentials) {
         String username = credentials.getUsername();
         String password = credentials.getPassword();

         logger.debug("Login attempt for user: {}", username);

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
     @Operation(summary = "Register a new customer", description = "Registers a new customer and returns an authentication token")
     @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(mediaType = "application/json", schema = @Schema(implementation = Customer.class), examples = @ExampleObject(value = "{\"username\":\"new.user\",\"password\":\"P@ssw0rd!\",\"email\":\"new.user@example.com\",\"firstName\":\"New\",\"lastName\":\"User\",\"phone\":\"555-0100\",\"address\":\"123 Demo St\",\"city\":\"DemoCity\",\"state\":\"CA\",\"zipCode\":\"90210\"}")))
     @ApiResponses({
         @ApiResponse(responseCode = "200", description = "Registration successful", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"token\":\"<token>\",\"customerId\":2,\"username\":\"new.user\"}"))),
         @ApiResponse(responseCode = "400", description = "Bad request (validation errors)", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"timestamp\":\"2026-01-26T12:34:56.789+00:00\",\"status\":400,\"error\":\"Bad Request\",\"message\":\"Validation failed for one or more fields\",\"path\":\"/api/customers/register\",\"errors\":[\"username: must not be blank\",\"password: must not be blank\"]}"))),
         @ApiResponse(responseCode = "400", description = "Bad request")
     })
     public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody Customer customer) {
         logger.debug("Registering new customer: {}", customer.getUsername());

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
     @Operation(summary = "Get customer by ID", description = "Returns customer details for the given ID")
     @ApiResponses({
         @ApiResponse(responseCode = "200", description = "Customer found"),
         @ApiResponse(responseCode = "404", description = "Customer not found")
     })
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
     @Operation(summary = "List all customers", description = "Returns a list of all customers (intentionally insecure in this demo)")
     @ApiResponses({@ApiResponse(responseCode = "200", description = "OK")})
     public ResponseEntity<List<Customer>> getAllCustomers() {
         return ResponseEntity.ok(customerService.getAllCustomers());
     }

     /**
      * Update customer by ID. Requires a Bearer JWT token in Authorization header.
      * The token must belong to the username being updated.
      */
     @PostMapping("/{id}")
     @Operation(summary = "Update customer", description = "Updates customer details for the given ID. Requires Authorization: Bearer <token>")
     @SecurityRequirement(name = "bearerAuth")
     @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(mediaType = "application/json", schema = @Schema(implementation = Customer.class), examples = @ExampleObject(value = "{\"username\":\"john.doe\",\"email\":\"john.doe@example.com\",\"firstName\":\"John\",\"lastName\":\"Doe\"}")))
     @ApiResponses({
         @ApiResponse(responseCode = "200", description = "Update successful", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"id\":1,\"username\":\"john.doe\",\"email\":\"john.doe@example.com\"}"))),
         @ApiResponse(responseCode = "400", description = "Bad request (validation)", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"timestamp\":\"2026-01-26T12:34:56.789+00:00\",\"status\":400,\"error\":\"Bad Request\",\"message\":\"Validation failed for one or more fields\",\"path\":\"/api/customers/{id}\",\"errors\":[\"username: must not be blank\"]}"))),
         @ApiResponse(responseCode = "401", description = "Unauthorized"),
         @ApiResponse(responseCode = "403", description = "Forbidden - token does not match target user")
     })
     public ResponseEntity<Object> updateCustomerById(@PathVariable Long id, @Valid @RequestBody Customer update, @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
         // Extract token
         if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
             return ResponseEntity.status(401).body(Map.of("error", "Missing or invalid Authorization header"));
         }

         String token = authorizationHeader.substring(7);

         // Validate token and ensure it matches the username provided
         String tokenUsername;
         try {
             tokenUsername = customerService.extractUsernameFromToken(token);
             if (tokenUsername == null || !customerService.validateToken(token, tokenUsername)) {
                 return ResponseEntity.status(401).body(Map.of("error", "Invalid or expired token"));
             }
         } catch (Exception e) {
             // Catch any exception (including JWT parsing issues) and return 401 for malformed/invalid tokens
             return ResponseEntity.status(401).body(Map.of("error", "Invalid or malformed token"));
         }

         // Optional policy: require that token username matches the username being updated
         if (!tokenUsername.equals(update.getUsername())) {
             return ResponseEntity.status(403).body(Map.of("error", "Token does not allow updating this user"));
         }

         // Ensure target exists
         Customer existing = customerService.getCustomerById(id);
         if (existing == null) {
             return ResponseEntity.notFound().build();
         }

         // Set ID on the update object to ensure correct update
         update.setId(id);
         Customer updated = customerService.updateCustomer(update);
         if (updated == null) {
             return ResponseEntity.status(400).body(Map.of("error", "Update failed"));
         }

         return ResponseEntity.ok(updated);
     }

     @PostMapping("/validate")
     @Operation(summary = "Validate JWT token", description = "Validates a JWT token and returns token status and username if valid")
     @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(mediaType = "application/json", schema = @Schema(implementation = TokenValidationRequest.class), examples = @ExampleObject(value = "{\"token\":\"<token>\",\"username\":\"john.doe\"}")))
     @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Validation result", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"valid\":true,\"username\":\"john.doe\"}"))),
        @ApiResponse(responseCode = "400", description = "Bad request (validation errors)", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"timestamp\":\"2026-01-26T12:34:56.789+00:00\",\"status\":400,\"error\":\"Bad Request\",\"message\":\"Validation failed for one or more fields\",\"path\":\"/api/customers/validate\",\"errors\":[\"token: must not be blank\",\"username: must not be blank\"]}"))),
     })
      public ResponseEntity<Map<String, Object>> validateToken(@Valid @RequestBody TokenValidationRequest request) {
           String token = request.getToken();
           String username = request.getUsername();

           boolean valid = customerService.validateToken(token, username);

           Map<String, Object> response = new HashMap<>();
           response.put("valid", valid);

           if (valid) {
               response.put("username", customerService.extractUsernameFromToken(token));
           }

           return ResponseEntity.ok(response);
       }
 }
