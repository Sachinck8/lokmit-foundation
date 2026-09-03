package com.lokmit.foundation.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI metadata for the LOKMIT FOUNDATION API.
 *
 * <p>Declared on a Spring-managed bean so springdoc processes it at startup.
 * Actual endpoint paths are documented per controller via springdoc.</p>
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "LOKMIT FOUNDATION API",
                description = "REST API for the LOKMIT FOUNDATION digital platform "
                        + "(corporate consultancy, skill development, project advisory, "
                        + "employment and knowledge platform).",
                version = "v1"),
        servers = {
                @Server(url = "http://localhost:8080", description = "Local development"),
                @Server(url = "/", description = "Same-origin (production)")
        })
public class OpenApiConfig {
}