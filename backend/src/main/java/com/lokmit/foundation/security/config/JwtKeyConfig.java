package com.lokmit.foundation.security.config;

import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Resolves the JWT signing key from configuration with fail-fast semantics.
 *
 * <p>The secret is supplied exclusively through the {@code JWT_SECRET}
 * environment variable (see docs/CONVENTIONS.md, section 9). This
 * configuration makes the secret requirement environment-aware:</p>
 *
 * <ul>
 *   <li><strong>Production profiles ({@code prod}, {@code production}):</strong>
 *       startup aborts immediately when {@code JWT_SECRET} is missing, blank,
 *       or shorter than 32 bytes (256 bits for HS256). No fallback secret is
 *       ever used in production.</li>
 *   <li><strong>Development (default/no profile):</strong> when
 *       {@code JWT_SECRET} is not configured, a fixed development-only key is
 *       used so the existing local development setup and the test suite keep
 *       working unchanged. A warning is logged; the secret value itself is
 *       never logged.</li>
 * </ul>
 *
 * <p>Keeping this decision in one configuration bean (rather than scattered
 * environment checks) means the signing key is resolved exactly once at
 * startup: an invalid production configuration aborts boot before any
 * endpoint can serve requests.</p>
 */
@Configuration
public class JwtKeyConfig {

    private static final Logger LOG = LoggerFactory.getLogger(JwtKeyConfig.class);

    /** Minimum secret length for HS256 (RFC 7518): 256 bits = 32 bytes. */
    static final int MIN_SECRET_BYTES = 32;

    /** Fixed, well-known key for development only — never acceptable in production. */
    private static final String DEV_FALLBACK_KEY =
            "dev-only-key-that-is-at-least-32-bytes-long-for-hs256!";

    /**
     * Resolves the JWT signing key bean.
     *
     * @param jwtConfig   JWT configuration properties (secret sourced from JWT_SECRET)
     * @param environment Spring environment used to detect active profiles
     * @return the signing key to use for HS256
     * @throws IllegalStateException if a production profile is active and no
     *                               usable JWT_SECRET is configured
     * @throws IllegalArgumentException if a configured secret is shorter than
     *                                  the HS256 minimum in any environment
     */
    @Bean
    public SecretKey jwtSigningKey(JwtConfig jwtConfig, Environment environment) {
        String secret = jwtConfig.getSecret();

        if (isProduction(environment)) {
            if (secret == null || secret.isBlank()) {
                throw new IllegalStateException(
                        "FATAL: JWT_SECRET is not configured but a production profile is active. "
                                + "Set the JWT_SECRET environment variable to a strong secret of at least "
                                + MIN_SECRET_BYTES + " bytes and restart. Startup is aborted so the "
                                + "application never runs with a predictable signing key.");
            }
            LOG.info("JWT signing key resolved from configured JWT_SECRET (production).");
            return requireValidKey(secret);
        }

        if (secret == null || secret.isBlank()) {
            LOG.warn("JWT_SECRET is not set - using a development-only key. DO NOT run in production.");
            return Keys.hmacShaKeyFor(DEV_FALLBACK_KEY.getBytes(StandardCharsets.UTF_8));
        }

        return requireValidKey(secret);
    }

    /**
     * True when any production profile ({@code prod} / {@code production}) is active.
     */
    private boolean isProduction(Environment environment) {
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> profile.equals("prod") || profile.equals("production"));
    }

    /**
     * Validates the configured secret length and derives the HS256 key.
     * The secret value itself is never included in any log output.
     */
    private SecretKey requireValidKey(String secret) {
        if (secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalArgumentException(
                    "JWT_SECRET must be at least " + MIN_SECRET_BYTES
                            + " bytes (256 bits) for HS256. Startup is aborted.");
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
