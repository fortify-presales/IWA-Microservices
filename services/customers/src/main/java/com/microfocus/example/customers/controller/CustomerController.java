package com.microfocus.example.customers.controller;

import com.microfocus.example.contracts.model.Customer;
import com.microfocus.example.customers.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
     * VULNERABILITY: Stores password in plain text
     */
    @PostMapping("/register")
    @Operation(summary = "Register a new customer", description = "Registers a new customer")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(mediaType = "application/json", schema = @Schema(implementation = Customer.class), examples = @ExampleObject(value = "{\"username\":\"new.user\",\"password\":\"P@ssw0rd!\",\"email\":\"new.user@example.com\",\"firstName\":\"New\",\"lastName\":\"User\",\"phone\":\"555-0100\",\"address\":\"123 Demo St\",\"city\":\"DemoCity\",\"state\":\"CA\",\"zipCode\":\"90210\"}")))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Registration successful"),
        @ApiResponse(responseCode = "400", description = "Bad request")
    })
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody Customer customer) {
        logger.debug("Registering new customer: {}", customer.getUsername());

        try {
            Customer created = customerService.registerCustomer(customer);
            return ResponseEntity.ok(Map.of(
                "customerId", created.getId(),
                "username", created.getUsername()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * VULNERABILITY: No authorization check on object ownership - scope-only access
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get customer by ID", description = "Returns customer details for the given ID")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Customer found"),
        @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    public ResponseEntity<Customer> getCustomerById(@PathVariable Long id) {
        Customer customer = customerService.getCustomerById(id);
        if (customer == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(customer);
    }

    /**
     * VULNERABILITY: Exposes all customers with plain text passwords
     */
    @GetMapping
    @Operation(summary = "List all customers", description = "Returns a list of all customers (intentionally insecure in this demo)")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "OK")})
    public ResponseEntity<List<Customer>> getAllCustomers() {
        return ResponseEntity.ok(customerService.getAllCustomers());
    }

    @PostMapping("/{id}")
    @Operation(summary = "Update customer", description = "Updates customer details for the given ID")
    @SecurityRequirement(name = "bearerAuth")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(mediaType = "application/json", schema = @Schema(implementation = Customer.class), examples = @ExampleObject(value = "{\"username\":\"john.doe\",\"email\":\"john.doe@example.com\",\"firstName\":\"John\",\"lastName\":\"Doe\"}")))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Update successful"),
        @ApiResponse(responseCode = "400", description = "Bad request (validation)"),
        @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    public ResponseEntity<Object> updateCustomerById(@PathVariable Long id, @Valid @RequestBody Customer update) {
        Customer existing = customerService.getCustomerById(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }

        update.setId(id);
        Customer updated = customerService.updateCustomer(update);
        if (updated == null) {
            return ResponseEntity.status(400).body(Map.of("error", "Update failed"));
        }

        return ResponseEntity.ok(updated);
    }
}
