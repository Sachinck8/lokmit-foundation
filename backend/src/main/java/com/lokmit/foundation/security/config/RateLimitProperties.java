package com.lokmit.foundation.security.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for public-endpoint rate limiting (I-6).
 *
 * <p>Two independent limiter categories exist — the public login endpoint and
 * the public contact form — each with enable flag, request capacity, and
 * window length. Defaults are conservative and documented in README.md;
 * production overrides flow through environment variables.</p>
 */
@Configuration
@ConfigurationProperties(prefix = "app.security.rate-limit")
@Getter
@Setter
public class RateLimitProperties {

    private EndpointRateLimit login = new EndpointRateLimit();
    private EndpointRateLimit contact = new EndpointRateLimit();
    /**
     * Authenticated resume upload policy (A7.6.6). Keyed by client IP like
     * the public limiters; defaults are deliberately permissive (30/60s)
     * because legitimate candidates rarely upload more than a few times per
     * hour — the limiter only bounds abuse, not normal use.
     */
    private EndpointRateLimit resumeUpload = new EndpointRateLimit();

    public EndpointRateLimit getResumeUpload() {
        return resumeUpload;
    }

    public void setResumeUpload(EndpointRateLimit resumeUpload) {
        this.resumeUpload = resumeUpload;
    }

    /** Policy for one protected endpoint category. */
    @Getter
    @Setter
    public static class EndpointRateLimit {
        /** Whether this category's limiter is active. */
        private boolean enabled = true;
        /** Requests allowed per window per client IP. */
        private int capacity = 10;
        /** Window length in seconds. */
        private int windowSeconds = 60;
        /** Upper bound on concurrently tracked client keys (memory guard). */
        private int maxTrackedKeys = 10_000;
    }
}
