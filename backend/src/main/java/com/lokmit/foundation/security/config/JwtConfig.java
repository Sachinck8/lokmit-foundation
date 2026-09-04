package com.lokmit.foundation.security.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for JWT token generation.
 * Values are sourced from environment variables via application.yml.
 */
@Configuration
@ConfigurationProperties(prefix = "app.security.jwt")
@Getter
@Setter
public class JwtConfig {

    /**
     * Secret key for signing JWTs. Must be set via JWT_SECRET environment variable.
     * Should be at least 256 bits (32 bytes) for HS256.
     */
    private String secret;

    /**
     * Access token expiration in milliseconds.
     */
    private long accessTokenExpiration = 900000; // 15 minutes

    /**
     * Refresh token expiration in milliseconds.
     */
    private long refreshTokenExpiration = 604800000; // 7 days
}