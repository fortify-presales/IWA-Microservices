package com.microfocus.example.catalog.controller;

import com.microfocus.example.catalog.service.CatalogService;
import com.microfocus.example.contracts.model.Product;
import com.microfocus.example.catalog.dto.ApiError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.net.URI;
import java.util.List;

/**
 * Catalog REST Controller
 * WARNING: Contains intentional security vulnerabilities
 */
@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*") // Intentionally permissive CORS
public class CatalogController {
    
    private static final Logger logger = LoggerFactory.getLogger(CatalogController.class);
    
    private final CatalogService catalogService;
    
    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }
    
    @GetMapping
    @Operation(summary = "List products", description = "Returns all products, optionally sorted")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "OK")})
    public ResponseEntity<List<Product>> getAllProducts(
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String order) {
        
        if (sortBy != null && order != null) {
            // VULNERABILITY: Passes unsanitized input to repository
            logger.debug("Getting products sorted by: {} {}", sortBy, order);
            return ResponseEntity.ok(catalogService.getAllProductsSorted(sortBy, order));
        }
        
        return ResponseEntity.ok(catalogService.getAllProducts());
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID", description = "Returns a single product by its ID")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "Not Found")})
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        Product product = catalogService.getProductById(id);
        if (product == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(product);
    }
    
    @GetMapping("/search")
    @Operation(summary = "Search products", description = "Search products by free-text query (vulnerable to SQL injection in this demo)")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "OK")})
    public ResponseEntity<List<Product>> searchProducts(@RequestParam String q) {
        // VULNERABILITY: SQL injection via search parameter
        logger.debug("Searching products with query: {}", q);
        return ResponseEntity.ok(catalogService.searchProducts(q));
    }
    
    @GetMapping("/category/{category}")
    @Operation(summary = "List products by category", description = "Returns products within the given category")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "OK")})
    public ResponseEntity<List<Product>> getProductsByCategory(@PathVariable String category) {
        // VULNERABILITY: SQL injection via category parameter
        logger.debug("Getting products in category: {}", category);
        return ResponseEntity.ok(catalogService.getProductsByCategory(category));
    }

    // ----------------- Secured endpoints (require API key) -----------------
    @PostMapping
    @Operation(summary = "Create product", description = "Creates a new product in the catalog")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Created"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid API key", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    @SecurityRequirement(name = "ApiKeyAuth")
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        Product created = catalogService.createProduct(product);
        if (created == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.created(URI.create("/api/products/" + created.getId())).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update product", description = "Updates an existing product by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "404", description = "Not Found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid API key", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    @SecurityRequirement(name = "ApiKeyAuth")
    public ResponseEntity<Product> updateProduct(@PathVariable Long id, @RequestBody Product product) {
        Product existing = catalogService.getProductById(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        Product updated = catalogService.updateProduct(id, product);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete product", description = "Deletes a product by ID")

    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "No Content"),
        @ApiResponse(responseCode = "404", description = "Not Found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid API key", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    @SecurityRequirement(name = "ApiKeyAuth")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        Product existing = catalogService.getProductById(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        boolean deleted = catalogService.deleteProduct(id);
        if (!deleted) {
            return ResponseEntity.status(500).build();
        }
        return ResponseEntity.noContent().build();
    }
}
