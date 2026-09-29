package com.lokmit.foundation.security.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for login brute-force protection.
 *
 * <p>Values are sourced from environment variables via application.yml.
 * A failed login locks the login identity for {@code lockoutDuration} after
 * {@code maxFailedAttempts} consecutive failures; a successful login clears
 * the state.</p>
 */
@Configuration
@ConfigurationProperties(prefix = "app.security.login-protection")
@Getter
@Setter
public class LoginProtectionConfig {

    /**
     * Number of consecutive failed logins that triggers a temporary lockout.
     * Must be at least 1.
     */
    private int maxFailedAttempts = 5;

    /**
     * Lockout duration in minutes.
     * Must be greater than 0.
     */
    private long lockoutDurationMinutes = 15;
}
