package com.vulntrack.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI vulnTrackOpenAPI() {
        return new OpenAPI()
            .info(
                new Info()
                    .title("VulnTrack API")
                    .description(
                        "A vulnerability management system API for tracking and managing security issues"
                    )
                    .version("v1.0.0")
                    .contact(
                        new Contact()
                            .name("VulnTrack")
                            .url("https://github.com/digiator42/vulntrack")
                    )
                    .license(
                        new License()
                            .name("MIT License")
                            .url("https://opensource.org/licenses/MIT")
                    )
            )

            .servers(
                List.of(
                    new Server()
                        .url("http://localhost:8080")
                        .description("Development server")
                )
            )

            // Add JWT Security Scheme
            .components(
                new Components()
                    .addSecuritySchemes(
                        "bearerAuth", new SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                            .description("Enter JWT token obtained from /auth/login")
                    )
            )

            // Apply security globally
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}