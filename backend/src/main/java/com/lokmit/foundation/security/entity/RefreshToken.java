package com.lokmit.foundation.security.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Maps to the existing 'refresh_tokens' table created in V2__identity_schema.sql.
 * Only the token hash is stored - never the raw token.
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 255)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    /**
     * Checks if this refresh token is expired.
     */
    public boolean isExpired() {
        return OffsetDateTime.now().isAfter(expiresAt);
    }

    /**
     * Checks if this refresh token has been revoked.
     */
    public boolean isRevoked() {
        return revokedAt != null;
    }

    /**
     * Checks if this refresh token is valid (not expired and not revoked).
     */
    public boolean isValid() {
        return !isExpired() && !isRevoked();
    }
}