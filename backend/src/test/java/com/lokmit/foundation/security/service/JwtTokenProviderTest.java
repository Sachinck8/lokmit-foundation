package com.lokmit.foundation.security.service;

import com.lokmit.foundation.security.config.JwtConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private static final String TEST_SECRET = "test-secret-key-that-is-at-least-32-bytes-long-for-hs256-algorithm";

    @BeforeEach
    void setUp() {
        JwtConfig jwtConfig = new JwtConfig();
        jwtConfig.setSecret(TEST_SECRET);
        jwtConfig.setAccessTokenExpiration(900000); // 15 minutes
        jwtConfig.setRefreshTokenExpiration(604800000); // 7 days
        jwtTokenProvider = new JwtTokenProvider(jwtConfig);
    }

    @Test
    void generateAccessToken_shouldCreateValidToken() {
        String token = jwtTokenProvider.generateAccessToken(1L, "test@example.com", List.of("ROLE_ADMIN"));

        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
    }

    @Test
    void validateToken_shouldReturnTrueForValidToken() {
        String token = jwtTokenProvider.generateAccessToken(1L, "test@example.com", List.of("ROLE_ADMIN"));

        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
    }

    @Test
    void validateToken_shouldReturnFalseForInvalidToken() {
        assertThat(jwtTokenProvider.validateToken("invalid-token")).isFalse();
    }

    @Test
    void validateToken_shouldReturnFalseForMalformedToken() {
        assertThat(jwtTokenProvider.validateToken("not.a.jwt")).isFalse();
    }

    @Test
    void validateToken_shouldReturnFalseForEmptyToken() {
        assertThat(jwtTokenProvider.validateToken("")).isFalse();
    }

    @Test
    void extractUserId_shouldReturnCorrectUserId() {
        String token = jwtTokenProvider.generateAccessToken(42L, "test@example.com", List.of("ROLE_ADMIN"));

        assertThat(jwtTokenProvider.extractUserId(token)).isEqualTo(42L);
    }

    @Test
    void extractUserId_shouldReturnNullForInvalidToken() {
        assertThat(jwtTokenProvider.extractUserId("invalid-token")).isNull();
    }

    @Test
    void extractRoles_shouldReturnCorrectRoles() {
        List<String> roles = List.of("ROLE_ADMIN", "ROLE_EDITOR");
        String token = jwtTokenProvider.generateAccessToken(1L, "test@example.com", roles);

        assertThat(jwtTokenProvider.extractRoles(token)).containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_EDITOR");
    }

    @Test
    void extractRoles_shouldReturnNullForInvalidToken() {
        assertThat(jwtTokenProvider.extractRoles("invalid-token")).isNull();
    }

    @Test
    void generateRefreshToken_shouldCreateUniqueTokens() {
        String token1 = jwtTokenProvider.generateRefreshToken();
        String token2 = jwtTokenProvider.generateRefreshToken();

        assertThat(token1).isNotEqualTo(token2);
        assertThat(token1).isNotEmpty();
        assertThat(token2).isNotEmpty();
    }

    @Test
    void token_shouldContainCorrectClaims() {
        String token = jwtTokenProvider.generateAccessToken(1L, "user@example.com", List.of("ROLE_USER"));

        var claims = jwtTokenProvider.extractClaims(token);

        assertThat(claims).isNotNull();
        assertThat(claims.getSubject()).isEqualTo("1");
        assertThat(claims.get("email", String.class)).isEqualTo("user@example.com");
        assertThat(claims.get("roles", List.class)).containsExactly("ROLE_USER");
        assertThat(claims.get("jti", String.class)).isNotEmpty();
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isNotNull();
    }

    @Test
    void token_shouldExpireCorrectly() {
        // Deterministic expiry check: a NEGATIVE expiration produces a token
        // whose exp claim is already in the past at the moment it is issued,
        // so no wall-clock sleep is needed (sleep-based expiry tests are
        // timing-flaky on loaded machines).
        JwtConfig expiredConfig = new JwtConfig();
        expiredConfig.setSecret(TEST_SECRET);
        expiredConfig.setAccessTokenExpiration(-5000);
        expiredConfig.setRefreshTokenExpiration(-5000);
        JwtTokenProvider expiredProvider = new JwtTokenProvider(expiredConfig);

        String token = expiredProvider.generateAccessToken(1L, "test@example.com", List.of("ROLE_ADMIN"));

        assertThat(expiredProvider.validateToken(token))
                .as("A token whose expiration is already in the past must be rejected")
                .isFalse();
    }
}