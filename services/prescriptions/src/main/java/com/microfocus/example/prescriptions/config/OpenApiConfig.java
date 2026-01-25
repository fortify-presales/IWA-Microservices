package com.microfocus.example.prescriptions.config;

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
                .title("Prescriptions Service API")
                .description("Prescription records and retrieval APIs")
                .version("v1"));
        openAPI.setTags(List.of(new Tag().name("Prescriptions").description("Prescription retrieval, refills and status endpoints")));
        if (serverUrl != null && !serverUrl.isBlank()) {
            openAPI.setServers(List.of(new Server().url(serverUrl)));
        }
        return openAPI;
    }
}
