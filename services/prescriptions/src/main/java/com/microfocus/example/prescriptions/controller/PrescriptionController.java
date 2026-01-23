package com.microfocus.example.prescriptions.controller;

import com.microfocus.example.contracts.model.Prescription;
import com.microfocus.example.prescriptions.service.PrescriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Prescriptions REST Controller
 * WARNING: Contains intentional IDOR vulnerabilities - no authorization checks
 */
@RestController
@RequestMapping("/api/prescriptions")
@CrossOrigin(origins = "*")
public class PrescriptionController {
    
    private static final Logger logger = LoggerFactory.getLogger(PrescriptionController.class);
    
    private final PrescriptionService prescriptionService;
    
    public PrescriptionController(PrescriptionService prescriptionService) {
        this.prescriptionService = prescriptionService;
    }
    
    /**
     * VULNERABILITY: IDOR - No authorization check
     * Any user can access any prescription by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Prescription> getPrescriptionById(@PathVariable Long id) {
        logger.debug("Fetching prescription: {}", id);
        Prescription prescription = prescriptionService.getPrescriptionById(id);
        if (prescription == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(prescription);
    }
    
    /**
     * VULNERABILITY: IDOR - No check if requesting user is the customer
     */
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<Prescription>> getPrescriptionsByCustomerId(@PathVariable Long customerId) {
        logger.debug("Fetching prescriptions for customer: {}", customerId);
        return ResponseEntity.ok(prescriptionService.getPrescriptionsByCustomerId(customerId));
    }
    
    /**
     * VULNERABILITY: Exposes all prescriptions
     */
    @GetMapping
    public ResponseEntity<List<Prescription>> getAllPrescriptions() {
        return ResponseEntity.ok(prescriptionService.getAllPrescriptions());
    }
    
    /**
     * VULNERABILITY: IDOR - Anyone can request a refill for any prescription
     */
    @PostMapping("/{id}/refill")
    public ResponseEntity<Map<String, String>> requestRefill(@PathVariable Long id) {
        logger.debug("Processing refill request for prescription: {}", id);
        
        Prescription prescription = prescriptionService.getPrescriptionById(id);
        if (prescription == null) {
            return ResponseEntity.notFound().build();
        }
        
        if (prescription.getRefillsRemaining() <= 0) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "No refills remaining"));
        }
        
        prescriptionService.processRefill(id);
        return ResponseEntity.ok(Map.of("message", "Refill processed successfully"));
    }
    
    /**
     * VULNERABILITY: IDOR - Anyone can modify prescription status
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<Void> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        String status = request.get("status");
        logger.debug("Updating prescription {} to status: {}", id, status);
        prescriptionService.updatePrescriptionStatus(id, status);
        return ResponseEntity.ok().build();
    }

    /**
     * VULNERABILITY: XML External Entity (XXE) parsing endpoint
     * Accepts XML input and parses it without secure parser configuration.
     */
    @PostMapping(value = "/parse-xml", consumes = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<?> parseXml(@RequestBody String xml) {
        try {
            String root = prescriptionService.parseXml(xml);
            return ResponseEntity.ok(Map.of("root", root));
        } catch (Exception e) {
            logger.error("XML parse error", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
