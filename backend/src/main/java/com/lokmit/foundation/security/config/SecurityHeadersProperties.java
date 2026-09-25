package com.lokmit.foundation.security.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for HTTP security response headers.
 *
 * <p>Only the environment-dependent knobs live here. The static policy
 * (Content-Security-Policy, Referrer-Policy, Permissions-Policy) is defined
 * once in {@link SecurityConfig} because it does not vary per environment.</p>
 *
 * <p>HSTS is <strong>off by default</strong> (development commonly runs on
 * plain HTTP; browsers ignore HSTS over HTTP, but the explicit switch keeps
 * the intent clear) and <strong>on in the {@code prod} profile</strong> via
 * {@code APP_HSTS_ENABLED}.</p>
 */
@Configuration
@ConfigurationProperties(prefix = "app.security.headers")
@Getter
@Setter
public class SecurityHeadersProperties {

    /**
     * Emit {@code Strict-Transport-Security}. Browsers only honor the header
     * on HTTPS responses, so enabling it is safe even while TLS terminates at
     * a proxy; it is still a deliberate, documented choice.
     */
    private boolean hstsEnabled = false;

    /** HSTS max-age in seconds (default: one year). */
    private long hstsMaxAgeSeconds = 31536000L;

    /** Whether the HSTS rule covers subdomains. */
    private boolean hstsIncludeSubdomains = true;
}
