package com.microfocus.example.customers.service;

import org.springframework.stereotype.Service;

/**
 * Legacy placeholder retained for compatibility.
 * Token issuance and validation are now handled by the dedicated OAuth2 auth-server.
 */
@Service
public class JwtService {

    public String generateToken(String username, Long customerId) {
        throw new UnsupportedOperationException("Local JWT generation is deprecated. Use auth-server /oauth2/token.");
    }

    public String extractUsername(String token) {
        throw new UnsupportedOperationException("Local JWT parsing is deprecated. Configure OAuth2 resource server.");
    }

    public boolean validateToken(String token, String username) {
        throw new UnsupportedOperationException("Local JWT validation is deprecated. Configure OAuth2 resource server.");
    }
}
