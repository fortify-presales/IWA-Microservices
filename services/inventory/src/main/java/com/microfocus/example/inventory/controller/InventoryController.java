package com.microfocus.example.inventory.controller;

import com.microfocus.example.inventory.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Parameter;

import java.util.Map;

/**
 * Inventory REST Controller
 * WARNING: Contains intentional XXE vulnerability
 */
@RestController
@RequestMapping("/api/inventory")
@CrossOrigin(origins = "*")
public class InventoryController {
    
    private static final Logger logger = LoggerFactory.getLogger(InventoryController.class);
    
    private final InventoryService inventoryService;
    
    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }
    
    @GetMapping
    @Operation(summary = "Get inventory map", description = "Returns current inventory levels for all products")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "OK")})
    public ResponseEntity<Map<Long, Integer>> getAllInventory() {
        return ResponseEntity.ok(inventoryService.getAllInventory());
    }
    
    @GetMapping("/{productId}")
    @Operation(summary = "Get stock for product", description = "Returns stock quantity for a specific product ID")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "OK")})
    public ResponseEntity<Map<String, Object>> getStock(@PathVariable Long productId) {
        Integer stock = inventoryService.getStock(productId);
        return ResponseEntity.ok(Map.of("productId", productId, "quantity", stock));
    }
    
    @PutMapping("/{productId}")
    @Operation(summary = "Update stock", description = "Update stock quantity for a product")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Updated")})
    public ResponseEntity<Void> updateStock(
            @PathVariable Long productId,
            @RequestBody Map<String, Integer> request) {
        Integer quantity = request.get("quantity");
        inventoryService.updateStock(productId, quantity);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/{productId}/adjust")
    @Operation(summary = "Adjust stock", description = "Adjust stock quantity by a signed integer amount")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Adjusted")})
    public ResponseEntity<Map<String, Object>> adjustStock(
            @PathVariable Long productId,
            @RequestBody Map<String, Integer> request) {
        Integer adjustment = request.get("adjustment");
        inventoryService.adjustStock(productId, adjustment);
        Integer newQuantity = inventoryService.getStock(productId);
        return ResponseEntity.ok(Map.of("productId", productId, "quantity", newQuantity));
    }
    
    /**
     * VULNERABILITY: XXE - Accepts and parses XML without validation
     */
    @PostMapping(value = "/import", consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE, MediaType.TEXT_PLAIN_VALUE})
    @Operation(summary = "Import inventory (XML)", description = "Imports inventory from XML; intentionally vulnerable to XXE in this demo")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Import result"), @ApiResponse(responseCode = "400", description = "Bad request")})
    public ResponseEntity<Map<String, Object>> importInventory(@RequestBody String xmlData) {
        logger.info("Received XML import request");
        logger.debug("XML content length: {}", xmlData.length());
        
        Map<String, Object> result = inventoryService.importInventoryFromXml(xmlData);
        return ResponseEntity.ok(result);
    }
    
    @GetMapping(value = "/export", produces = MediaType.APPLICATION_XML_VALUE)
    @Operation(summary = "Export inventory (XML)", description = "Exports current inventory as XML")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "XML export")})
    public ResponseEntity<String> exportInventory() {
        String xml = inventoryService.exportInventoryToXml();
        return ResponseEntity.ok(xml);
    }
}
