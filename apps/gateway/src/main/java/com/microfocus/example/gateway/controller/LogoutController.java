package com.microfocus.example.gateway.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
public class LogoutController {

    @Value("${services.auth.url:http://localhost:9000}")
    private String authBaseUrl;

    @GetMapping("/logout-to-auth")
    public Mono<Void> logoutToAuth(@RequestParam(value = "post_logout_redirect_uri", required = false) String postLogoutRedirectUri,
                                   ServerWebExchange exchange) {
        // Build logout (end-session) URL for the auth server
        String endSession = authBaseUrl + "/oauth2/logout";
        if (postLogoutRedirectUri != null && !postLogoutRedirectUri.isEmpty()) {
            String encoded = URLEncoder.encode(postLogoutRedirectUri, StandardCharsets.UTF_8);
            endSession = endSession + "?post_logout_redirect_uri=" + encoded;
        }
        exchange.getResponse().getHeaders().set(HttpHeaders.LOCATION, endSession);
        exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.FOUND);
        return exchange.getResponse().setComplete();
    }
}
