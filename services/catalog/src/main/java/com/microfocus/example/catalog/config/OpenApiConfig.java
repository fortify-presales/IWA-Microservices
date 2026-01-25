package com.microfocus.example.catalog.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

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
        return openAPI;
    }
}
