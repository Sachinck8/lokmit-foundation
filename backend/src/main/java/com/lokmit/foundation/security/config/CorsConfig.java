package com.lokmit.foundation.security.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Explicit CORS policy for the API.
 *
 * <p>Design rules:</p>
 * <ul>
 *   <li><strong>Explicit allow-list only</strong> — origins come from
 *       {@code APP_CORS_ALLOWED_ORIGINS}; a configured {@code *} is rejected
 *       at startup because this API authenticates with Bearer tokens and a
 *       wildcard would let any website read API responses.</li>
 *   <li><strong>No credentialed CORS</strong> — {@code allowCredentials(false)}
 *       always; cookies are not part of the authentication model, so there is
 *       no reason to expose that surface.</li>
 *   <li><strong>Minimum surface</strong> — only the methods the API actually
 *       uses (GET, POST, PATCH, OPTIONS for preflight) and the headers the
 *       browser must be allowed to send (Authorization, Content-Type).
 *       No response headers are exposed beyond the defaults.</li>
 * </ul>
 *
 * <p>The source is registered through Spring Security's standard
 * {@code .cors(...)} integration in {@link SecurityConfig}, so preflights are
 * answered before authorization and all other requests keep their normal
 * security treatment. Cross-origin behavior of server-side clients (the Vite
 * dev proxy, curl) is unaffected.</p>
 */
@Configuration
@EnableConfigurationProperties(CorsProperties.class)
public class CorsConfig {

    private static final Logger LOG = LoggerFactory.getLogger(CorsConfig.class);

    /** Methods actually exposed by the API; OPTIONS is listed for clarity of preflight support. */
    private static final List<String> ALLOWED_METHODS = List.of("GET", "POST", "PATCH", "OPTIONS");

    /** Headers the browser must be allowed to send on cross-origin API calls. */
    private static final List<String> ALLOWED_HEADERS = List.of("Authorization", "Content-Type");

    @Bean
    public CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
        List<String> origins = properties.getAllowedOrigins().stream()
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList();

        if (origins.stream().anyMatch("*"::equals)) {
            throw new IllegalStateException(
                    "APP_CORS_ALLOWED_ORIGINS must not contain '*'. List the exact frontend "
                            + "origin(s) instead; this API does not permit a wildcard origin.");
        }

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        if (origins.isEmpty()) {
            // No origins configured: register an empty policy. Cross-origin browser
            // calls are then rejected while same-origin/proxied flows keep working.
            LOG.warn("No CORS origins configured (APP_CORS_ALLOWED_ORIGINS) - cross-origin browser access is disabled.");
            source.registerCorsConfiguration("/**", new CorsConfiguration());
            return source;
        }

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(origins);
        config.setAllowedMethods(ALLOWED_METHODS);
        config.setAllowedHeaders(ALLOWED_HEADERS);
        config.setAllowCredentials(false);
        // Sane browser cache window for preflight results; kept finite so origin
        // changes propagate without an application restart.
        config.setMaxAge(3600L);

        source.registerCorsConfiguration("/**", config);
        LOG.info("CORS configured for {} explicit origin(s)", origins.size());
        return source;
    }
}
