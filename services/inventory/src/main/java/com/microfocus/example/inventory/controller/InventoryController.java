package com.microfocus.example.inventory.controller;

import com.microfocus.example.inventory.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<Map<Long, Integer>> getAllInventory() {
        return ResponseEntity.ok(inventoryService.getAllInventory());
    }
    
    @GetMapping("/{productId}")
    public ResponseEntity<Map<String, Object>> getStock(@PathVariable Long productId) {
        Integer stock = inventoryService.getStock(productId);
        return ResponseEntity.ok(Map.of("productId", productId, "quantity", stock));
    }
    
    @PutMapping("/{productId}")
    public ResponseEntity<Void> updateStock(
            @PathVariable Long productId,
            @RequestBody Map<String, Integer> request) {
        Integer quantity = request.get("quantity");
        inventoryService.updateStock(productId, quantity);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/{productId}/adjust")
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
    public ResponseEntity<Map<String, Object>> importInventory(@RequestBody String xmlData) {
        logger.info("Received XML import request");
        logger.debug("XML content length: {}", xmlData.length());
        
        Map<String, Object> result = inventoryService.importInventoryFromXml(xmlData);
        return ResponseEntity.ok(result);
    }
    
    @GetMapping(value = "/export", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> exportInventory() {
        String xml = inventoryService.exportInventoryToXml();
        return ResponseEntity.ok(xml);
    }
}
