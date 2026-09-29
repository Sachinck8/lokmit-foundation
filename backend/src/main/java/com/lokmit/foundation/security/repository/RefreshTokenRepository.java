package com.lokmit.foundation.security.repository;

import com.lokmit.foundation.security.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    boolean existsByTokenHash(String tokenHash);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revokedAt = :now WHERE rt.userId = :userId AND rt.revokedAt IS NULL")
    void revokeAllByUserId(@Param("userId") Long userId, @Param("now") OffsetDateTime now);

    /**
     * Atomically claims a refresh token for a refresh operation: only the
     * first caller that flips {@code consumed_at} from NULL to a timestamp
     * for a still-valid (unconsumed, unrevoked, unexpired) token wins.
     *
     * <p>Executed as a single conditional UPDATE, the database evaluates the
     * row predicate once — two simultaneous refreshes with the same token
     * cannot both satisfy {@code consumed_at IS NULL}. On PostgreSQL the
     * row is exclusively write-locked for the duration of the statement, so
     * consumption is serialized at the database level and works across
     * application instances without any application-level synchronization.
     * Returns 1 when this caller won the token, 0 when it was already
     * consumed, revoked or expired.</p>
     */
    @Modifying
    @Query("""
            UPDATE RefreshToken rt
               SET rt.consumedAt = :now
             WHERE rt.tokenHash = :tokenHash
               AND rt.consumedAt IS NULL
               AND rt.revokedAt IS NULL
               AND rt.expiresAt > :now
            """)
    int claimValidToken(@Param("tokenHash") String tokenHash, @Param("now") OffsetDateTime now);

    /**
     * Revokes every still-valid token in a token family (reuse response).
     * Already-revoked rows keep their original revoked_at timestamp.
     */
    @Modifying
    @Query("""
            UPDATE RefreshToken rt
               SET rt.revokedAt = :now
             WHERE rt.familyId = :familyId
               AND rt.revokedAt IS NULL
            """)
    int revokeAllInFamily(@Param("familyId") UUID familyId, @Param("now") OffsetDateTime now);

    /**
     * Reuse-detection lookup: the family of a consumed or revoked token.
     */
    @Query("""
            SELECT rt.familyId
              FROM RefreshToken rt
             WHERE rt.tokenHash = :tokenHash
               AND rt.familyId IS NOT NULL
            """)
    Optional<UUID> findFamilyIdByTokenHash(@Param("tokenHash") String tokenHash);

    /**
     * Deletes refresh tokens that can never authenticate again:
     * <ul>
     *   <li>expired tokens (useless by time) — deleted immediately;</li>
     *   <li>consumed or revoked tokens — deleted only once they are older
     *       than {@code evidenceCutoff}, keeping them available as reuse-
     *       detection evidence for one full refresh-token lifetime after
     *       they were consumed/revoked.</li>
     * </ul>
     * Rows that are merely old but still unconsumed, unrevoked and unexpired
     * are never touched, so active sessions are always preserved.
     *
     * <p>The predicate is evaluated per row, so this query is safe to run
     * concurrently on multiple instances — a row is deleted at most once and
     * only when it can never authenticate again.</p>
     *
     * @param now           current timestamp
     * @param evidenceCutoff consumed/revoked timestamps older than this are
     *                      deletable (typically now minus one token lifetime)
     */
    @Modifying
    @Query("""
            DELETE FROM RefreshToken rt
             WHERE rt.expiresAt < :now
                OR (rt.consumedAt IS NOT NULL AND rt.consumedAt < :evidenceCutoff)
                OR (rt.revokedAt IS NOT NULL AND rt.revokedAt < :evidenceCutoff)
            """)
    int deleteUselessTokens(@Param("now") OffsetDateTime now,
                            @Param("evidenceCutoff") OffsetDateTime evidenceCutoff);
}
