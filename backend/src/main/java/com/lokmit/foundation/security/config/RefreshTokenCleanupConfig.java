package com.lokmit.foundation.security.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for refresh-token housekeeping.
 */
@Configuration
@ConfigurationProperties(prefix = "app.security.refresh-token")
@Getter
@Setter
public class RefreshTokenCleanupConfig {

    /**
     * Interval between refresh-token cleanup runs, in minutes.
     * Set to 0 (or less) to disable scheduled cleanup entirely.
     */
    private long cleanupIntervalMinutes = 60;
}
