package com.carddemo.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger configuration for the CardDemo REST API.
 * Provides interactive API documentation at /swagger-ui.html.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI cardDemoOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CardDemo API")
                        .description("REST API for the CardDemo credit card management system. " +
                                "Migrated from COBOL/CICS/VSAM mainframe to Java 17/Spring Boot.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("CardDemo Migration Team"))
                        .license(new License()
                                .name("Apache 2.0")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .schemaRequirement("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT authentication token obtained from /api/auth/login"));
    }
}
