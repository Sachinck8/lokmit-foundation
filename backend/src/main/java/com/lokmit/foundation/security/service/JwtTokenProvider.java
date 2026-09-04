package com.lokmit.foundation.security.service;

import com.lokmit.foundation.security.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Service for generating and validating JWT tokens.
 */
@Service
public class JwtTokenProvider {

    private static final Logger LOG = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final JwtConfig jwtConfig;
    private final SecretKey signingKey;

    public JwtTokenProvider(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
        this.signingKey = createSigningKey(jwtConfig.getSecret());
    }

    /**
     * Creates a signing key from the configured secret.
     */
    private SecretKey createSigningKey(String secret) {
        if (secret == null || secret.isBlank()) {
            // Fallback for development - in production, JWT_SECRET must be set
            LOG.warn("JWT_SECRET is not set. Using a development-only key. DO NOT use in production.");
            return Keys.hmacShaKeyFor("dev-only-key-that-is-at-least-32-bytes-long-for-hs256!".getBytes(StandardCharsets.UTF_8));
        }
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("JWT_SECRET must be at least 32 bytes (256 bits) for HS256");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generates a JWT access token for the given user.
     *
     * @param userId    the user ID (subject)
     * @param email     the user email
     * @param roles     the user's role codes
     * @return the generated JWT token
     */
    public String generateAccessToken(Long userId, String email, List<String> roles) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtConfig.getAccessTokenExpiration());
        String jti = UUID.randomUUID().toString();

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("roles", roles)
                .claim("jti", jti)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    /**
     * Generates a refresh token (opaque token, not a JWT).
     *
     * @return a secure random token string
     */
    public String generateRefreshToken() {
        return UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();
    }

    /**
     * Validates a JWT token.
     *
     * @param token the token to validate
     * @return true if the token is valid
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException ex) {
            LOG.debug("JWT token expired: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            LOG.debug("JWT token unsupported: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            LOG.debug("JWT token malformed: {}", ex.getMessage());
        } catch (SignatureException ex) {
            LOG.debug("JWT token signature invalid: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            LOG.debug("JWT token is null or empty: {}", ex.getMessage());
        }
        return false;
    }

    /**
     * Extracts claims from a JWT token.
     *
     * @param token the token
     * @return the claims, or null if the token is invalid
     */
    public Claims extractClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception ex) {
            LOG.debug("Failed to extract claims from JWT: {}", ex.getMessage());
            return null;
        }
    }

    /**
     * Extracts the user ID from a JWT token.
     *
     * @param token the token
     * @return the user ID, or null if the token is invalid
     */
    public Long extractUserId(String token) {
        Claims claims = extractClaims(token);
        if (claims == null) {
            return null;
        }
        try {
            return Long.valueOf(claims.getSubject());
        } catch (NumberFormatException ex) {
            LOG.debug("Invalid user ID in JWT subject");
            return null;
        }
    }

    /**
     * Extracts the roles from a JWT token.
     *
     * @param token the token
     * @return the roles, or null if the token is invalid
     */
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        Claims claims = extractClaims(token);
        if (claims == null) {
            return null;
        }
        Object roles = claims.get("roles");
        if (roles instanceof List<?> list) {
            return list.stream().map(Object::toString).toList();
        }
        return null;
    }

    /**
     * Gets the access token expiration in milliseconds.
     */
    public long getAccessTokenExpiration() {
        return jwtConfig.getAccessTokenExpiration();
    }

    /**
     * Gets the refresh token expiration in milliseconds.
     */
    public long getRefreshTokenExpiration() {
        return jwtConfig.getRefreshTokenExpiration();
    }
}