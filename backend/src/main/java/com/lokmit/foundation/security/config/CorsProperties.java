package com.lokmit.foundation.security.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for the backend CORS policy.
 *
 * <p>Origins are supplied through {@code APP_CORS_ALLOWED_ORIGINS}
 * (comma-separated). The application-level default in application.yml covers
 * local development; the production profile requires the variable to be set
 * explicitly. Values are never defaulted to a wildcard.</p>
 */
@Configuration
@ConfigurationProperties(prefix = "app.security.cors")
@Getter
@Setter
public class CorsProperties {

    /**
     * Allowed browser origins, e.g. {@code https://app.example.org}. The API
     * uses Bearer-token authentication, so credentials (cookies) are never
     * allowed cross-origin.
     */
    private List<String> allowedOrigins = new ArrayList<>();
}
