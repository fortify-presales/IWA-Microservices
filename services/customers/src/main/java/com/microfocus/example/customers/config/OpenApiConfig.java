package com.microfocus.example.customers.config;

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
                .title("Customers Service API")
                .description("APIs for managing customers and authentication")
                .version("v1"));
        openAPI.setTags(List.of(new Tag().name("Customers").description("Customer management and auth endpoints")));
        if (serverUrl != null && !serverUrl.isBlank()) {
            openAPI.setServers(List.of(new Server().url(serverUrl)));
        }
        return openAPI;
    }
}
