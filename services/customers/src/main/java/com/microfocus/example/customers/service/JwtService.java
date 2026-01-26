package com.microfocus.example.customers.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT Service for token generation and validation
 * WARNING: Uses weak/hardcoded secret - intentional vulnerability
 */
@Service
public class JwtService {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    @Value("${jwt.secret}")
    private String secret; // Hardcoded weak secret in config
    
    @Value("${jwt.expiration}")
    private Long expiration;
    
    /**
     * VULNERABILITY: Uses weak secret key
     */
    private Key getSigningKey() {
        if (secret == null || secret.isEmpty()) {
            logger.warn("JWT secret is not configured; generating a temporary secure key.");
            return Keys.secretKeyFor(SignatureAlgorithm.HS256);
        }

        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        // Keys.hmacShaKeyFor requires at least 256 bits (32 bytes) for HS256.
        if (keyBytes.length < 32) {
            // Derive a 256-bit key deterministically from the provided secret using SHA-256.
            // This keeps the demo's short/hardcoded secret usable while satisfying the JJWT requirement.
            // Note: this is a convenience/compatibility approach for demos — using a short secret
            // remains insecure in practice. Prefer a randomly generated >=256-bit secret stored securely.
            try {
                MessageDigest sha = MessageDigest.getInstance("SHA-256");
                keyBytes = sha.digest(keyBytes);
                logger.warn("JWT secret was too short; derived a 256-bit key from the configured secret.");
            } catch (Exception e) {
                logger.error("Failed to derive SHA-256 from JWT secret, falling back to a generated key.", e);
                return Keys.secretKeyFor(SignatureAlgorithm.HS256);
            }
        }

        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    public String generateToken(String username, Long customerId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("customerId", customerId);
        claims.put("username", username);
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }
    
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }
    
    public Long extractCustomerId(String token) {
        return extractAllClaims(token).get("customerId", Long.class);
    }
    
    public boolean validateToken(String token, String username) {
        final String extractedUsername = extractUsername(token);
        return (extractedUsername.equals(username) && !isTokenExpired(token));
    }
    
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    
    private boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }
}
