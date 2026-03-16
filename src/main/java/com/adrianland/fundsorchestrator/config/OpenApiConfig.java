package com.adrianland.fundsorchestrator.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger UI configuration.
 *
 * Server URLs are intentionally omitted so SpringDoc auto-detects
 * the correct host in every environment (local, Docker, EC2, App Runner).
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI fundsOrchestratorOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("BTG Pactual - Investment Fund Orchestrator API")
                        .description("""
                                REST API for self-service management of BTG Pactual investment funds.
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Adrian Landazuri")
                                .email("adrianland@yopmail.com"))
                        .license(new License()
                                .name("Internal - BTG Pactual")
                                .url("https://www.BTG.com")));
    }
}
