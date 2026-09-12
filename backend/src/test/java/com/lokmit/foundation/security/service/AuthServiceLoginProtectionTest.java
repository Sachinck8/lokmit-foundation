package com.lokmit.foundation.security.service;

import com.lokmit.foundation.security.config.LoginProtectionConfig;
import com.lokmit.foundation.security.dto.AuthResponse;
import com.lokmit.foundation.security.dto.LoginRequest;
import com.lokmit.foundation.security.entity.User;
import com.lokmit.foundation.security.exception.AuthenticationFailedException;
import com.lokmit.foundation.security.repository.RefreshTokenRepository;
import com.lokmit.foundation.security.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Login-flow tests for {@link AuthService} with real {@link LoginLockoutService}
 * wired in, proving the brute-force protection end to end at the service layer.
 *
 * <p>The {@link UserRepository} is mocked, so the {@code REQUIRES_NEW}
 * transaction propagation is not exercised here (pure Mockito, no Spring
 * context, no PostgreSQL) — but the full counter → lock → expiry → reset
 * cycle runs through the real production logic of both services.</p>
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceLoginProtectionTest {

    private static final String EMAIL = "admin@lokmitfoundation.org";
    private static final String PASSWORD = "SecureP@ss123";

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private LoginProtectionConfig loginProtectionConfig;
    private AuthService authService;
    private User user;

    @BeforeEach
    void setUp() {
        loginProtectionConfig = new LoginProtectionConfig();
        LoginLockoutService lockoutService = new LoginLockoutService(userRepository, loginProtectionConfig);
        authService = new AuthService(userRepository, refreshTokenRepository, passwordEncoder,
                jwtTokenProvider, lockoutService);

        user = new User();
        user.setId(1L);
        user.setEmail(EMAIL);
        user.setFullName("Platform Administrator");
        user.setUserType("STAFF");
        user.setStatus("ACTIVE");
        user.setEmailVerified(true);
        user.setPasswordHash("$2a$10$storedbcryptvalue");
        user.setFailedLoginAttempts(0);
    }

    private LoginRequest loginRequest() {
        LoginRequest request = new LoginRequest();
        request.setEmail(EMAIL);
        request.setPassword(PASSWORD);
        return request;
    }

    private void stubExistingUser() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
    }

    /**
     * Mirrors real database semantics for the failure-recording path: the
     * pessimistic-lock re-read inside recordFailedAttempt returns the same row.
     * Only needed in tests whose flow reaches a failed password check.
     */
    private void stubUserForUpdate() {
        when(userRepository.findByEmailForUpdate(EMAIL)).thenReturn(Optional.of(user));
    }

    private void stubSuccessfulCredentials() {
        when(passwordEncoder.matches(PASSWORD, user.getPasswordHash())).thenReturn(true);
    }

    private void stubFailedCredentials() {
        when(passwordEncoder.matches(PASSWORD, user.getPasswordHash())).thenReturn(false);
    }

    private void stubSuccessfulTokenGeneration() {
        when(jwtTokenProvider.generateAccessToken(any(), anyString(), any())).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken()).thenReturn("refresh-token");
        when(jwtTokenProvider.getRefreshTokenExpiration()).thenReturn(604800000L);
        when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(900000L);
    }

    // ------------------------------------------------------------------
    // Valid login
    // ------------------------------------------------------------------

    @Test
    @DisplayName("valid login succeeds and returns tokens")
    void validLoginSucceeds() {
        stubExistingUser();
        stubSuccessfulCredentials();
        stubSuccessfulTokenGeneration();

        AuthResponse response = authService.login(loginRequest());

        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals(0, user.getFailedLoginAttempts());
    }

    // ------------------------------------------------------------------
    // Failed login increments the counter
    // ------------------------------------------------------------------

    @Test
    @DisplayName("failed password increments the attempt counter and returns generic error")
    void failedPasswordIncrementsCounter() {
        stubExistingUser();
        stubUserForUpdate();
        stubFailedCredentials();

        AuthenticationFailedException ex = assertThrows(AuthenticationFailedException.class,
                () -> authService.login(loginRequest()));

        assertEquals("Invalid email or password", ex.getMessage());
        assertEquals(1, user.getFailedLoginAttempts());
        assertNull(user.getLockedUntil(), "No lock below threshold");
        assertNotNull(user.getFailedLoginWindowStartedAt(), "Failure window opened");
    }

    // ------------------------------------------------------------------
    // Unknown email remains generic
    // ------------------------------------------------------------------

    @Test
    @DisplayName("unknown email returns the same generic failure and records nothing")
    void unknownEmailRemainsGeneric() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest();
        request.setEmail("ghost@example.com");
        request.setPassword(PASSWORD);

        AuthenticationFailedException ex = assertThrows(AuthenticationFailedException.class,
                () -> authService.login(request));

        assertEquals("Invalid email or password", ex.getMessage());
        verify(userRepository, never()).save(any(User.class));
        verify(userRepository, never()).findByEmailForUpdate(anyString());
    }

    // ------------------------------------------------------------------
    // Lockout after threshold, rejection while locked
    // ------------------------------------------------------------------

    @Test
    @DisplayName("lockout activates after the configured number of failures")
    void lockoutOccursAfterThreshold() {
        stubExistingUser();
        stubUserForUpdate();
        stubFailedCredentials();

        for (int i = 1; i <= loginProtectionConfig.getMaxFailedAttempts(); i++) {
            assertThrows(AuthenticationFailedException.class, () -> authService.login(loginRequest()));
            assertEquals(i, user.getFailedLoginAttempts());
        }

        assertNotNull(user.getLockedUntil(), "Account is temporarily locked at threshold");
        assertTrue(user.getLockedUntil().isAfter(OffsetDateTime.now()),
                "Lock extends into the future");
    }

    @Test
    @DisplayName("login while locked is rejected with the generic message")
    void loginWhileLockedIsRejected() {
        user.setFailedLoginAttempts(loginProtectionConfig.getMaxFailedAttempts());
        user.setLockedUntil(OffsetDateTime.now().plusMinutes(10));
        stubExistingUser();

        // No credential stub: the lock must short-circuit before the password
        // check runs — asserted below via verify(passwordEncoder, never()).

        AuthenticationFailedException ex = assertThrows(AuthenticationFailedException.class,
                () -> authService.login(loginRequest()));

        assertEquals("Invalid email or password", ex.getMessage());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtTokenProvider, never()).generateAccessToken(any(), anyString(), any());
    }

    @Test
    @DisplayName("locked rejection message matches wrong-password and unknown-email messages")
    void allFailureOutcomesShareOneMessage() {
        stubExistingUser();
        stubUserForUpdate();
        stubFailedCredentials();

        String wrongPasswordMessage = assertThrows(AuthenticationFailedException.class,
                () -> authService.login(loginRequest())).getMessage();

        user.setLockedUntil(OffsetDateTime.now().plusMinutes(10));
        String lockedMessage = assertThrows(AuthenticationFailedException.class,
                () -> authService.login(loginRequest())).getMessage();

        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());
        LoginRequest ghost = new LoginRequest();
        ghost.setEmail("ghost@example.com");
        ghost.setPassword(PASSWORD);
        String unknownEmailMessage = assertThrows(AuthenticationFailedException.class,
                () -> authService.login(ghost)).getMessage();

        assertEquals(wrongPasswordMessage, lockedMessage);
        assertEquals(wrongPasswordMessage, unknownEmailMessage);
        assertEquals("Invalid email or password", unknownEmailMessage);
    }

    // ------------------------------------------------------------------
    // Reset on success
    // ------------------------------------------------------------------

    @Test
    @DisplayName("successful login after prior failures resets the counter and lock")
    void successfulLoginResetsCounter() {
        user.setFailedLoginAttempts(3);
        user.setFailedLoginWindowStartedAt(OffsetDateTime.now().minusMinutes(1));
        stubExistingUser();
        stubSuccessfulCredentials();
        stubSuccessfulTokenGeneration();

        AuthResponse response = authService.login(loginRequest());

        assertNotNull(response);
        assertEquals(0, user.getFailedLoginAttempts());
        assertNull(user.getFailedLoginWindowStartedAt());
        assertNull(user.getLockedUntil());
    }

    @Test
    @DisplayName("successful login after an expired lockout resets all state")
    void successfulLoginAfterExpiredLockoutResetsState() {
        user.setFailedLoginAttempts(5);
        user.setFailedLoginWindowStartedAt(OffsetDateTime.now().minusMinutes(30));
        user.setLockedUntil(OffsetDateTime.now().minusSeconds(5)); // expired
        stubExistingUser();
        stubSuccessfulCredentials();
        stubSuccessfulTokenGeneration();

        AuthResponse response = authService.login(loginRequest());

        assertNotNull(response, "Expired lockout must not block a valid login");
        assertEquals(0, user.getFailedLoginAttempts());
        assertNull(user.getLockedUntil());
    }

    // ------------------------------------------------------------------
    // Lockout expiry
    // ------------------------------------------------------------------

    @Test
    @DisplayName("failed attempt after expiry re-arms counting from the expired state")
    void failedAttemptAfterExpiryStillRecords() {
        user.setFailedLoginAttempts(5);
        user.setFailedLoginWindowStartedAt(OffsetDateTime.now().minusMinutes(30));
        user.setLockedUntil(OffsetDateTime.now().minusSeconds(5)); // expired
        stubExistingUser();
        stubUserForUpdate();
        stubFailedCredentials();

        assertThrows(AuthenticationFailedException.class, () -> authService.login(loginRequest()));

        // Counter keeps incrementing from the stored value; the new failure is recorded.
        assertEquals(6, user.getFailedLoginAttempts());
    }

    @Test
    @DisplayName("attempt exactly at threshold boundary locks; below does not")
    void exactThresholdBoundary() {
        stubExistingUser();
        stubUserForUpdate();
        stubFailedCredentials();

        int threshold = loginProtectionConfig.getMaxFailedAttempts();
        for (int i = 1; i < threshold; i++) {
            assertThrows(AuthenticationFailedException.class, () -> authService.login(loginRequest()));
            assertNull(user.getLockedUntil(), "No lock at attempt " + i + " (below threshold)");
        }

        assertThrows(AuthenticationFailedException.class, () -> authService.login(loginRequest()));
        assertEquals(threshold, user.getFailedLoginAttempts());
        assertNotNull(user.getLockedUntil(), "Lock engages exactly at the threshold");
    }

    // ------------------------------------------------------------------
    // last_login and token flow unchanged
    // ------------------------------------------------------------------

    @Test
    @DisplayName("successful login still stamps last_login and issues both tokens")
    void successfulLoginPreservesExistingBehavior() {
        stubExistingUser();
        stubSuccessfulCredentials();
        stubSuccessfulTokenGeneration();

        authService.login(loginRequest());

        assertNotNull(user.getLastLoginAt());
        verify(jwtTokenProvider, times(1)).generateAccessToken(any(), anyString(), any());
        verify(jwtTokenProvider, times(1)).generateRefreshToken();
        verify(refreshTokenRepository, times(1)).save(any());
    }
}
