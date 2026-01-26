package com.microfocus.example.catalog.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String API_KEY_NAME = "ApiKeyAuth";
    private static final String HEADER_NAME = "X-API-KEY";

    @Bean
    public OpenAPI customOpenAPI(@Value("${OPENAPI_SERVER_URL:}") String serverUrl) {
        OpenAPI openAPI = new OpenAPI()
            .info(new Info()
                .title("Catalog Service API")
                .description("Product catalog and search APIs")
                .version("v1"));
        openAPI.setTags(List.of(new Tag().name("Products").description("Product catalog and search endpoints")));
        if (serverUrl != null && !serverUrl.isBlank()) {
            openAPI.setServers(List.of(new Server().url(serverUrl)));
        }
        openAPI.components(new Components()
                .addSecuritySchemes(API_KEY_NAME, new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name(HEADER_NAME)
                        .description("API Key needed to access certain endpoints")));
        // Note: Do NOT add a global SecurityRequirement here — we only want endpoints annotated with
        // @SecurityRequirement(name = "ApiKeyAuth") to be shown as protected in OpenAPI/Swagger.
        return openAPI;
    }
}
