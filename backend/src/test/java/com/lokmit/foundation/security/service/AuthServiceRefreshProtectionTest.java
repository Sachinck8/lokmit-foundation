package com.lokmit.foundation.security.service;

import com.lokmit.foundation.security.config.LoginProtectionConfig;
import com.lokmit.foundation.security.dto.AuthResponse;
import com.lokmit.foundation.security.dto.RefreshTokenRequest;
import com.lokmit.foundation.security.entity.RefreshToken;
import com.lokmit.foundation.security.entity.User;
import com.lokmit.foundation.security.exception.TokenException;
import com.lokmit.foundation.security.repository.RefreshTokenRepository;
import com.lokmit.foundation.security.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Refresh-flow tests for {@link AuthService}: rotation, reuse detection,
 * family revocation, and revocation/expiry/invalid-token handling.
 *
 * <p>Pure Mockito: no Spring context, no PostgreSQL. The atomicity of
 * {@code claimValidToken} is itself database semantics (single conditional
 * UPDATE); here its contract is exercised through the service with mocks.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceRefreshProtectionTest {

    private static final String RAW_TOKEN = "raw-opaque-refresh-token";

    /** Real SHA-256 hex digest of the raw token — must match AuthService.hashToken. */
    private static final String TOKEN_HASH = sha256Hex(RAW_TOKEN);

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private AuthService authService;
    private User user;
    private RefreshToken refreshToken;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, refreshTokenRepository,
                passwordEncoder, jwtTokenProvider,
                new LoginLockoutService(userRepository, new LoginProtectionConfig()));

        user = new User();
        user.setId(1L);
        user.setEmail("admin@lokmitfoundation.org");
        user.setFullName("Platform Administrator");
        user.setUserType("STAFF");
        user.setStatus("ACTIVE");
        user.setEmailVerified(true);
        user.setPasswordHash("$2a$10$storedbcryptvalue");

        refreshToken = new RefreshToken();
        refreshToken.setId(100L);
        refreshToken.setUserId(1L);
        refreshToken.setTokenHash(TOKEN_HASH);
        refreshToken.setFamilyId(UUID.randomUUID());
        refreshToken.setExpiresAt(OffsetDateTime.now().plusDays(7));
        refreshToken.setCreatedAt(OffsetDateTime.now().minusDays(1));
    }

    private RefreshTokenRequest request() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(RAW_TOKEN);
        return request;
    }

    private void stubClaimWins() {
        when(refreshTokenRepository.claimValidToken(eq(TOKEN_HASH), any(OffsetDateTime.class))).thenReturn(1);
    }

    private void stubClaimLoses() {
        when(refreshTokenRepository.claimValidToken(eq(TOKEN_HASH), any(OffsetDateTime.class))).thenReturn(0);
    }

    private void stubTokenLookup() {
        when(refreshTokenRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(refreshToken));
    }

    private void stubSuccessfulTokenGeneration() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateAccessToken(any(), anyString(), any())).thenReturn("new-access-token");
        when(jwtTokenProvider.generateRefreshToken()).thenReturn("new-refresh-token");
        when(jwtTokenProvider.getRefreshTokenExpiration()).thenReturn(604800000L);
        when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(900000L);
    }

    private void verifyNoTokensIssued() {
        verify(jwtTokenProvider, never()).generateAccessToken(any(), anyString(), any());
        verify(jwtTokenProvider, never()).generateRefreshToken();
    }

    // ------------------------------------------------------------------
    // Valid refresh + rotation
    // ------------------------------------------------------------------

    @Test
    @DisplayName("valid refresh token succeeds and mints a new token pair")
    void validRefreshSucceeds() {
        stubClaimWins();
        stubTokenLookup();
        stubSuccessfulTokenGeneration();

        AuthResponse response = authService.refreshToken(request());

        assertNotNull(response);
        assertEquals("new-access-token", response.getAccessToken());
        assertEquals("new-refresh-token", response.getRefreshToken());
    }

    @Test
    @DisplayName("successful rotation consumes the old token before issuing new tokens")
    void rotationConsumesOldToken() {
        stubClaimWins();
        stubTokenLookup();
        stubSuccessfulTokenGeneration();

        authService.refreshToken(request());

        verify(refreshTokenRepository).claimValidToken(eq(TOKEN_HASH), any(OffsetDateTime.class));
        assertNull(refreshToken.getRevokedAt(), "Rotation consumes via consumed_at, not revocation");
    }

    @Test
    @DisplayName("refreshed token stays in the same family (lineage preserved)")
    void refreshedTokenStaysInSameFamily() {
        stubClaimWins();
        stubTokenLookup();
        stubSuccessfulTokenGeneration();
        UUID originalFamily = refreshToken.getFamilyId();

        authService.refreshToken(request());

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertEquals(originalFamily, captor.getValue().getFamilyId());
    }

    @Test
    @DisplayName("legacy pre-V10 token without family is adopted into a fresh family")
    void legacyTokenGetsFreshFamily() {
        stubClaimWins();
        stubTokenLookup();
        stubSuccessfulTokenGeneration();
        refreshToken.setFamilyId(null);

        authService.refreshToken(request());

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertNotNull(captor.getValue().getFamilyId(), "Legacy token must join a family for reuse detection");
    }

    // ------------------------------------------------------------------
    // Reuse detection
    // ------------------------------------------------------------------

    @Test
    @DisplayName("replayed consumed token is rejected and its family is revoked")
    void replayedConsumedTokenTriggersFamilyRevocation() {
        stubClaimLoses();
        stubTokenLookup();

        assertThrows(TokenException.class, () -> authService.refreshToken(request()));

        verify(refreshTokenRepository).revokeAllInFamily(eq(refreshToken.getFamilyId()), any(OffsetDateTime.class));
        verifyNoTokensIssued();
    }

    @Test
    @DisplayName("second refresh with the same token cannot mint another pair")
    void oldTokenCannotBeReusedAfterRotation() {
        stubClaimWins();
        stubTokenLookup();
        stubSuccessfulTokenGeneration();

        authService.refreshToken(request());

        // Simulate the post-rotation database state: the conditional UPDATE
        // no longer matches (token consumed), so the second caller loses.
        stubClaimLoses();

        assertThrows(TokenException.class, () -> authService.refreshToken(request()));

        // Exactly one token pair ever minted across both attempts.
        verify(jwtTokenProvider, times(1)).generateAccessToken(any(), anyString(), any());
        verify(jwtTokenProvider, times(1)).generateRefreshToken();
    }

    @Test
    @DisplayName("revoked token (e.g. after logout) is rejected as reuse")
    void revokedTokenIsRejected() {
        stubClaimLoses();
        refreshToken.setRevokedAt(OffsetDateTime.now().minusMinutes(5));
        stubTokenLookup();

        assertThrows(TokenException.class, () -> authService.refreshToken(request()));

        verify(refreshTokenRepository).revokeAllInFamily(eq(refreshToken.getFamilyId()), any(OffsetDateTime.class));
        verifyNoTokensIssued();
    }

    @Test
    @DisplayName("expired token is rejected with the expired code")
    void expiredTokenIsRejected() {
        stubClaimLoses();
        refreshToken.setExpiresAt(OffsetDateTime.now().minusMinutes(5));
        stubTokenLookup();

        TokenException ex = assertThrows(TokenException.class, () -> authService.refreshToken(request()));

        assertTrue(ex.getMessage().toLowerCase().contains("expired"),
                "Expired tokens must keep the expired error code");
        verifyNoTokensIssued();
    }

    @Test
    @DisplayName("unknown token is rejected without family side effects")
    void unknownTokenIsRejected() {
        stubClaimLoses();
        when(refreshTokenRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.empty());

        TokenException ex = assertThrows(TokenException.class, () -> authService.refreshToken(request()));

        assertTrue(ex.getMessage().contains("Invalid"));
        verify(refreshTokenRepository, never()).revokeAllInFamily(any(), any(OffsetDateTime.class));
        verifyNoTokensIssued();
    }

    @Test
    @DisplayName("reuse of a legacy token with NULL family fails without revocation")
    void legacyTokenReuseFailsGracefully() {
        stubClaimLoses();
        refreshToken.setFamilyId(null);
        stubTokenLookup();

        assertThrows(TokenException.class, () -> authService.refreshToken(request()));

        verify(refreshTokenRepository, never()).revokeAllInFamily(any(), any(OffsetDateTime.class));
        verifyNoTokensIssued();
    }

    // ------------------------------------------------------------------
    // Concurrency semantics
    // ------------------------------------------------------------------

    @Test
    @DisplayName("two concurrent refreshes with one token: only the claim winner succeeds")
    void concurrentRefreshesOnlyOneWins() {
        stubClaimWins();
        stubTokenLookup();
        stubSuccessfulTokenGeneration();
        UUID family = refreshToken.getFamilyId();

        authService.refreshToken(request());

        // The loser of the atomic claim hits the reuse path.
        stubClaimLoses();
        assertThrows(TokenException.class, () -> authService.refreshToken(request()));

        // The family revocation protects the lineage against the replay.
        verify(refreshTokenRepository).revokeAllInFamily(eq(family), any(OffsetDateTime.class));
    }

    // ------------------------------------------------------------------
    // Logout/revocation interplay
    // ------------------------------------------------------------------

    @Test
    @DisplayName("logout revokes the stored token so a later refresh is rejected")
    void logoutRevokesAndBlocksRefresh() {
        when(refreshTokenRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(refreshToken));

        authService.logout(RAW_TOKEN);

        assertNotNull(refreshToken.getRevokedAt());
    }

    @Test
    @DisplayName("logout of blank input is a harmless no-op")
    void logoutBlankInputIsNoOp() {
        authService.logout("");
        authService.logout(null);

        verify(refreshTokenRepository, never()).findByTokenHash(anyString());
    }

    @Test
    @DisplayName("refresh with locked/suspended user fails without issuing tokens")
    void refreshWithLockedUserFails() {
        stubClaimWins();
        stubTokenLookup();
        stubSuccessfulTokenGeneration();
        user.setStatus("LOCKED");

        assertThrows(Exception.class, () -> authService.refreshToken(request()));

        verifyNoTokensIssued();
    }
}
