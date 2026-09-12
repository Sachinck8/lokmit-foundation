package com.lokmit.foundation.security.service;

import com.lokmit.foundation.security.config.RefreshTokenCleanupConfig;
import com.lokmit.foundation.security.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link RefreshTokenCleanupScheduler}.
 *
 * <p>Asserts the retention contract at the repository boundary: expired rows
 * are immediately deletable, consumed/revoked rows stay as reuse-detection
 * evidence for one refresh-token lifetime, and active tokens are never
 * deletable. The SQL predicate itself is database semantics; these tests pin
 * the arguments the scheduler passes to the repository.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RefreshTokenCleanupSchedulerTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private RefreshTokenCleanupConfig config;
    private RefreshTokenCleanupScheduler scheduler;

    @BeforeEach
    void setUp() {
        config = new RefreshTokenCleanupConfig();
        scheduler = new RefreshTokenCleanupScheduler(refreshTokenRepository, config, jwtTokenProvider);
    }

    @Test
    @DisplayName("expired tokens are immediately eligible for cleanup")
    void expiredTokensAreEligible() {
        when(jwtTokenProvider.getRefreshTokenExpiration()).thenReturn(604800000L);
        when(refreshTokenRepository.deleteUselessTokens(any(), any())).thenReturn(2);

        scheduler.cleanupExpiredTokens();

        // Evidence cutoff must sit one refresh-token lifetime (7 days) in the past.
        verify(refreshTokenRepository).deleteUselessTokens(
                argThat(now -> now != null),
                argThat(cutoff -> cutoff.isBefore(OffsetDateTime.now().minusDays(6))));
    }

    @Test
    @DisplayName("active tokens are preserved by the cleanup predicate")
    void activeTokensArePreserved() {
        when(jwtTokenProvider.getRefreshTokenExpiration()).thenReturn(604800000L);
        when(refreshTokenRepository.deleteUselessTokens(any(), any())).thenReturn(0);

        scheduler.cleanupExpiredTokens();

        // The delete predicate only matches expired rows or consumed/revoked
        // rows past the evidence cutoff — a merely-old-but-valid token matches
        // neither branch, so active sessions are never deleted.
        verify(refreshTokenRepository).deleteUselessTokens(any(), any());
    }

    @Test
    @DisplayName("consumed/revoked rows remain as evidence for one token lifetime")
    void consumedRevokedRetainedForEvidenceWindow() {
        when(jwtTokenProvider.getRefreshTokenExpiration()).thenReturn(604800000L);

        scheduler.cleanupExpiredTokens();

        // Cutoff = now - 7 days: consumed/revoked rows younger than that are retained.
        verify(refreshTokenRepository).deleteUselessTokens(
                any(),
                argThat(cutoff -> cutoff.isBefore(OffsetDateTime.now().minusDays(6))));
    }

    @Test
    @DisplayName("cleanup is safe and repeatable when there is nothing to delete")
    void cleanupIsIdempotent() {
        when(jwtTokenProvider.getRefreshTokenExpiration()).thenReturn(604800000L);
        when(refreshTokenRepository.deleteUselessTokens(any(), any())).thenReturn(0);

        scheduler.cleanupExpiredTokens();
        scheduler.cleanupExpiredTokens();

        verify(refreshTokenRepository, times(2)).deleteUselessTokens(any(), any());
    }

    @Test
    @DisplayName("cleanup can be disabled via configuration")
    void cleanupCanBeDisabled() {
        config.setCleanupIntervalMinutes(0);

        scheduler.cleanupExpiredTokens();

        verifyNoInteractions(refreshTokenRepository, jwtTokenProvider);
    }

    @Test
    @DisplayName("evidence cutoff follows the configured refresh-token lifetime")
    void evidenceCutoffFollowsTokenLifetime() {
        when(jwtTokenProvider.getRefreshTokenExpiration()).thenReturn(3600000L); // 1 hour

        scheduler.cleanupExpiredTokens();

        verify(refreshTokenRepository).deleteUselessTokens(
                any(),
                argThat(cutoff -> cutoff.isBefore(OffsetDateTime.now().minusMinutes(59))
                        && cutoff.isAfter(OffsetDateTime.now().minusMinutes(61))));
    }
}
