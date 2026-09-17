package com.lokmit.foundation.employment.resume.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for resume file storage (A7.6.1), bound from the
 * {@code app.storage.*} property namespace. Mirrors the
 * {@code OutboxRelayConfig} convention: a single @ConfigurationProperties
 * bean with a safe default.
 *
 * <p>A7.6.1 scope: only the provider {@code type}. Size limits, allowed
 * types, MIME allowlist, multipart limits and upload rate limiting are
 * A7.6.2/A7.6.5 concerns and are deliberately NOT added here.</p>
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    /** Backed by FileStorage implementations; 'db' = DatabaseFileStorage. */
    private String type = "db";
}
