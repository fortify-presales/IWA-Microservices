package com.microfocus.example.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(exchange -> exchange
                .pathMatchers(
                    "/health",
                    "/actuator/health",
                    "/swagger-ui.html",
                    "/swagger-ui/index.html",
                    "/swagger-ui/**",
                    "/webjars/**",
                    "/v3/api-docs/**",
                    "/login",
                    "/logout",
                    "/oauth2/**",
                    "/.well-known/**"
                ).permitAll()
                .pathMatchers(HttpMethod.POST, "/api/customers/register").permitAll()
                .pathMatchers(HttpMethod.GET, "/api/customers/**").hasAuthority("SCOPE_customers.read")
                .pathMatchers(HttpMethod.POST, "/api/customers/**").hasAuthority("SCOPE_customers.write")
                .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
            .build();
    }
}
