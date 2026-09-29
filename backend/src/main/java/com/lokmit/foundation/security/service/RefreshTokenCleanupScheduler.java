package com.lokmit.foundation.security.service;

import com.lokmit.foundation.security.config.RefreshTokenCleanupConfig;
import com.lokmit.foundation.security.repository.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * Scheduled housekeeping for refresh tokens.
 *
 * <p>Deletes only rows that can never authenticate again:
 * expired tokens immediately, and consumed/revoked tokens once they are
 * older than one refresh-token lifetime (keeping them available as
 * reuse-detection evidence until then). Active, unconsumed, unrevoked,
 * unexpired tokens are never touched. See
 * {@link RefreshTokenRepository#deleteUselessTokens} for the exact
 * predicate.</p>
 *
 * <p>Multi-instance behavior: the cleanup runs on every instance, which is
 * safe by design — the delete predicate is evaluated per row, so concurrent
 * runs delete each useless row at most once and never touch useful ones.
 * The delete is a single indexed statement; no distributed lock is needed.
 * Disable with {@code app.security.refresh-token.cleanup-interval-minutes=0}.</p>
 *
 * <p>Never logs tokens or token hashes.</p>
 */
@Component
public class RefreshTokenCleanupScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(RefreshTokenCleanupScheduler.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenCleanupConfig config;
    private final JwtTokenProvider jwtTokenProvider;

    public RefreshTokenCleanupScheduler(RefreshTokenRepository refreshTokenRepository,
                                        RefreshTokenCleanupConfig config,
                                        JwtTokenProvider jwtTokenProvider) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.config = config;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * One cleanup pass. Frequency is configuration-driven via
     * {@code app.security.refresh-token.cleanup-interval-minutes}.
     * The method is a no-op when cleanup is disabled (interval &lt;= 0),
     * which also keeps tests deterministic when they invoke
     * {@link #cleanupExpiredTokens()} directly.
     */
    @Scheduled(fixedDelayString =
            "#{T(java.time.Duration).ofMinutes(${app.security.refresh-token.cleanup-interval-minutes:60}).toMillis()}")
    @Transactional
    public void cleanupExpiredTokens() {
        if (config.getCleanupIntervalMinutes() <= 0) {
            return;
        }

        OffsetDateTime now = OffsetDateTime.now();
        // Consumed/revoked rows stay as reuse-detection evidence for one full
        // refresh-token lifetime after their state change.
        OffsetDateTime evidenceCutoff =
                now.minusSeconds(jwtTokenProvider.getRefreshTokenExpiration() / 1000);

        int deleted = refreshTokenRepository.deleteUselessTokens(now, evidenceCutoff);
        if (deleted > 0) {
            LOG.info("Refresh-token cleanup removed {} expired/consumed/revoked token(s)", deleted);
        }
    }
}
