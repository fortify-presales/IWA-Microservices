package com.microfocus.example.customers.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT Service for token generation and validation
 * WARNING: Uses weak/hardcoded secret - intentional vulnerability
 */
@Service
public class JwtService {
    
    @Value("${jwt.secret}")
    private String secret; // Hardcoded weak secret in config
    
    @Value("${jwt.expiration}")
    private Long expiration;
    
    /**
     * VULNERABILITY: Uses weak secret key
     */
    private Key getSigningKey() {
        // Using weak secret directly from config
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
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
