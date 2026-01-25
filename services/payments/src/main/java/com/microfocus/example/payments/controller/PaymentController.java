package com.microfocus.example.payments.controller;

import com.microfocus.example.payments.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Parameter;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Payments REST Controller
 * WARNING: Contains intentional security vulnerabilities
 */
@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*")
public class PaymentController {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);
    
    private final PaymentService paymentService;
    
    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
    
    /**
     * VULNERABILITY: Logs sensitive payment data
     */
    @PostMapping("/process")
    @Operation(summary = "Process payment", description = "Processes a payment request (demo logs sensitive data)")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Processed"), @ApiResponse(responseCode = "400", description = "Bad request")})
    public ResponseEntity<Map<String, Object>> processPayment(@RequestBody Map<String, Object> paymentData) {
        logger.info("Received payment request: {}", paymentData); // Logs full card details!
        
        Map<String, Object> result = paymentService.processPayment(paymentData);
        return ResponseEntity.ok(result);
    }
    
    /**
     * VULNERABILITY: Exposes hardcoded API credentials
     */
    @GetMapping("/config")
    @Operation(summary = "Get payment gateway config", description = "Returns payment gateway configuration (may expose hardcoded secrets in demo)")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "OK")})
    public ResponseEntity<Map<String, String>> getConfig() {
        logger.warn("Payment gateway configuration requested");
        return ResponseEntity.ok(paymentService.getPaymentGatewayConfig());
    }
    
    @PostMapping("/refund")
    @Operation(summary = "Refund payment", description = "Refunds a payment by ID and amount")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Refunded"), @ApiResponse(responseCode = "400", description = "Bad request")})
    public ResponseEntity<Map<String, Object>> refundPayment(@RequestBody Map<String, Object> refundData) {
        String paymentId = (String) refundData.get("paymentId");
        BigDecimal amount = new BigDecimal(refundData.get("amount").toString());
        
        Map<String, Object> result = paymentService.refundPayment(paymentId, amount);
        return ResponseEntity.ok(result);
    }

    /**
     * VULNERABILITY: Insecure JSON deserialization endpoint
     * Accepts arbitrary JSON and deserializes it with Jackson default typing enabled.
     * Intended for testing detection by security scanners.
     */
    @PostMapping(value = "/deserialize-json", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Deserialize JSON", description = "Deserializes JSON with unsafe settings (for demo)")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Deserialized"), @ApiResponse(responseCode = "400", description = "Bad request")})
    public ResponseEntity<?> deserializeJson(@RequestBody String json) {
        try {
            Object obj = paymentService.deserializeJson(json);
            return ResponseEntity.ok(Map.of("result", obj));
        } catch (Exception e) {
            logger.error("JSON deserialization error", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
