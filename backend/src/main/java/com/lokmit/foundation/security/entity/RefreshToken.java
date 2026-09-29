package com.lokmit.foundation.security.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

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
     * Groups all refresh tokens minted from one successful authentication
     * (login or refresh). NULL for legacy rows created before V10. A replayed
     * token triggers revocation of every still-valid token in its family.
     */
    @Column(name = "family_id")
    private UUID familyId;

    /**
     * Timestamp recorded atomically when a refresh flow trades this token in
     * for a new pair. Set tokens are consumed; presenting one again is reuse.
     */
    @Column(name = "consumed_at")
    private OffsetDateTime consumedAt;

    /**
     * Checks if this refresh token is expired.
     */
    public boolean isExpired() {
        return OffsetDateTime.now().isAfter(expiresAt);
    }

    /**
     * Checks if this refresh token has been revoked (manually or by reuse response).
     */
    public boolean isRevoked() {
        return revokedAt != null;
    }

    /**
     * Checks if this refresh token has already been traded in during rotation.
     */
    public boolean isConsumed() {
        return consumedAt != null;
    }

    /**
     * Checks if this refresh token is valid (not expired and not revoked).
     */
    public boolean isValid() {
        return !isExpired() && !isRevoked();
    }
}