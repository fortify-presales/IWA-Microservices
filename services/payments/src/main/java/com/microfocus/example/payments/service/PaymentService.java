package com.microfocus.example.payments.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.io.IOException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Payment Service
 * WARNING: Contains hardcoded secrets and logs sensitive data
 */
@Service
public class PaymentService {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);
    
    // VULNERABILITY: Hardcoded API credentials
    // SECURE ALTERNATIVES:
    // 1. Use environment variables: System.getenv("STRIPE_API_KEY")
    // 2. Use Spring @Value with external configuration: @Value("${stripe.api.key}")
    // 3. Use secret management services: AWS Secrets Manager, Azure Key Vault, HashiCorp Vault
    // 4. Never commit secrets to source control - use .gitignore for config files
    // 5. Rotate secrets regularly and use different keys per environment
    private static final String STRIPE_API_KEY = "sk_test_FakeStripeKeyForDemo1234567890";
    private static final String PAYPAL_CLIENT_ID = "FakePayPalClientIdForDemo1234567890";
    private static final String PAYPAL_SECRET = "FakePayPalSecretForDemo1234567890";
    
    @Value("${payment.gateway.api.key}")
    private String gatewayApiKey; // Also hardcoded in config
    
    @Value("${payment.gateway.secret}")
    private String webhookSecret;
    
    /**
     * VULNERABILITY: Logs credit card details
     */
    public Map<String, Object> processPayment(Map<String, Object> paymentData) {
        String cardNumber = (String) paymentData.get("cardNumber");
        String cvv = (String) paymentData.get("cvv");
        String expiryDate = (String) paymentData.get("expiryDate");
        BigDecimal amount = new BigDecimal(paymentData.get("amount").toString());
        
        // VULNERABILITY: Logs sensitive payment information
        logger.info("Processing payment - Card: {}, CVV: {}, Expiry: {}, Amount: {}", 
                   cardNumber, cvv, expiryDate, amount);
        
        // Simulate payment processing with hardcoded credentials
        String paymentId = UUID.randomUUID().toString();
        
        logger.debug("Using API Key: {} for payment processing", STRIPE_API_KEY);
        logger.debug("Using webhook secret: {} for verification", webhookSecret);
        
        Map<String, Object> response = new HashMap<>();
        response.put("paymentId", paymentId);
        response.put("status", "SUCCESS");
        response.put("amount", amount);
        response.put("cardLast4", cardNumber.substring(cardNumber.length() - 4));
        
        return response;
    }
    
    /**
     * VULNERABILITY: Exposes API credentials
     */
    public Map<String, String> getPaymentGatewayConfig() {
        Map<String, String> config = new HashMap<>();
        config.put("stripeApiKey", STRIPE_API_KEY);
        config.put("paypalClientId", PAYPAL_CLIENT_ID);
        config.put("paypalSecret", PAYPAL_SECRET);
        config.put("gatewayApiKey", gatewayApiKey);
        config.put("webhookSecret", webhookSecret);
        return config;
    }
    
    public Map<String, Object> refundPayment(String paymentId, BigDecimal amount) {
        logger.info("Processing refund for payment: {} amount: {}", paymentId, amount);
        
        Map<String, Object> response = new HashMap<>();
        response.put("refundId", UUID.randomUUID().toString());
        response.put("status", "REFUNDED");
        response.put("amount", amount);
        
        return response;
    }

    /**
     * VULNERABILITY: Insecure JSON Deserialization
     * This method enables Jackson default typing which allows polymorphic
     * deserialization. If untrusted input is provided, this can lead to
     * remote code execution in certain environments.
     *
     * SECURE ALTERNATIVES:
     * - Avoid enabling default typing. Use explicit typed deserialization.
     * - Use a fixed DTO class instead of `Object.class`.
     * - Use a whitelist for permitted polymorphic types.
     */
    public Object deserializeJson(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        // Intentionally enabling default typing - insecure
        mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        return mapper.readValue(json, Object.class);
    }
}
