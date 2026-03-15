package com.adrianland.fundsorchestrator.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI fundsOrchestratorOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("BTG Pactual – Investment Fund Orchestrator API")
                        .description("""
                                REST API for self-service management of BTG Pactual investment funds.
                                
                                **Features**
                                - Subscribe / cancel fund memberships
                                - View full transaction history
                                - Idempotent requests via `X-Idempotency-Key` header
                                - Email & SMS notifications (Strategy pattern)
                                
                                **Single-Table DynamoDB design** – all entities share the `FondosBTG` table
                                with `PK` (partition key) and `SK` (sort key).
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Adrian Land")
                                .email("adrianland@example.com"))
                        .license(new License()
                                .name("Internal – BTG Pactual")
                                .url("https://www.btgpactual.com")))
                .servers(List.of(
                        new Server().url("http://localhost:" + serverPort).description("Local / Docker"),
                        new Server().url("https://api.btgfunds.com").description("Production (AWS App Runner)")));
    }
}
