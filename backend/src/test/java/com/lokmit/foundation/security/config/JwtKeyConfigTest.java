package com.lokmit.foundation.security.config;

import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for the environment-aware JWT signing key policy.
 *
 * <p>Production profiles ({@code prod}, {@code production}) must fail fast
 * when JWT_SECRET is missing, blank, or too short. The default (development)
 * profile keeps a development-only fallback so local development and the
 * test suite work without configuration. A secret that IS configured is
 * validated in every environment; the secret value itself is never exposed
 * in assertion output.</p>
 */
class JwtKeyConfigTest {

    private static final String VALID_SECRET =
            "test-secret-key-that-is-at-least-32-bytes-long-for-hs256-algorithm";

    private final JwtKeyConfig jwtKeyConfig = new JwtKeyConfig();
    private final JwtConfig jwtConfig = new JwtConfig();

    private SecretKey resolve(MockEnvironment environment) {
        return jwtKeyConfig.jwtSigningKey(jwtConfig, environment);
    }

    // ------------------------------------------------------------------
    // Production: JWT_SECRET required, never a fallback
    // ------------------------------------------------------------------

    @Test
    void production_withValidSecret_resolvesWorkingKey() {
        jwtConfig.setSecret(VALID_SECRET);
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");

        SecretKey key = resolve(env);

        assertThat(key).isNotNull();
        // The resolved key must actually verify a token signed with it (round-trip).
        String token = io.jsonwebtoken.Jwts.builder()
                .subject("1")
                .signWith(key)
                .compact();
        boolean valid = io.jsonwebtoken.Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject()
                .equals("1");
        assertThat(valid).isTrue();
    }

    @Test
    void productionProfileAlias_production_withValidSecret_resolvesKey() {
        jwtConfig.setSecret(VALID_SECRET);
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("production");

        assertThat(resolve(env)).isNotNull();
    }

    @Test
    void production_withMissingSecret_failsFast() {
        jwtConfig.setSecret(null);
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");

        assertThatThrownBy(() -> resolve(env))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET")
                .hasMessageContaining("production");
    }

    @Test
    void production_withEmptySecret_failsFast() {
        jwtConfig.setSecret("");
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");

        assertThatThrownBy(() -> resolve(env))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    void production_withBlankSecret_failsFast() {
        jwtConfig.setSecret("   ");
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("production");

        assertThatThrownBy(() -> resolve(env))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    void production_withTooShortSecret_failsFast() {
        jwtConfig.setSecret("short-secret");
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");

        assertThatThrownBy(() -> resolve(env))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 32 bytes");
    }

    @Test
    void production_with31ByteSecret_failsFast() {
        jwtConfig.setSecret("a".repeat(31));
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");

        assertThatThrownBy(() -> resolve(env))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 32 bytes");
    }

    // ------------------------------------------------------------------
    // Development (default profile): dev fallback retained, config honored
    // ------------------------------------------------------------------

    @Test
    void development_withMissingSecret_usesDevelopmentFallbackKey() {
        jwtConfig.setSecret(null);
        MockEnvironment env = new MockEnvironment();

        SecretKey key = resolve(env);

        assertThat(key).isNotNull();
        // Fallback key must be a usable HS256 key (round-trip proof).
        String token = io.jsonwebtoken.Jwts.builder()
                .subject("dev")
                .signWith(key)
                .compact();
        assertThat(io.jsonwebtoken.Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject())
                .isEqualTo("dev");
    }

    @Test
    void development_withValidSecret_usesConfiguredSecret() {
        jwtConfig.setSecret(VALID_SECRET);
        MockEnvironment env = new MockEnvironment();

        SecretKey key = resolve(env);

        SecretKey expected = Keys.hmacShaKeyFor(VALID_SECRET.getBytes(StandardCharsets.UTF_8));
        assertThat(key.getAlgorithm()).isEqualTo(expected.getAlgorithm());
        assertThat(key.getEncoded()).isEqualTo(expected.getEncoded());
    }

    @Test
    void development_withTooShortSecret_failsFast() {
        jwtConfig.setSecret("too-short");
        MockEnvironment env = new MockEnvironment();

        assertThatThrownBy(() -> resolve(env))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 32 bytes");
    }

    // ------------------------------------------------------------------
    // Boundary: exactly 32 bytes is acceptable
    // ------------------------------------------------------------------

    @Test
    void anyProfile_withExactly32ByteSecret_isAccepted() {
        jwtConfig.setSecret("a".repeat(32));
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");

        assertThat(resolve(env)).isNotNull();
    }
}
