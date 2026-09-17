package com.lokmit.foundation.security.service;

import com.lokmit.foundation.security.config.LoginProtectionConfig;
import com.lokmit.foundation.security.entity.User;
import com.lokmit.foundation.security.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LoginLockoutService} — the persistent,
 * account-level login brute-force protection.
 *
 * <p>Pure Mockito tests: no Spring context, no PostgreSQL. The threshold and
 * boundary logic is exercised directly against the service.</p>
 */
@ExtendWith(MockitoExtension.class)
class LoginLockoutServiceTest {

    @Mock
    private UserRepository userRepository;

    private LoginProtectionConfig config;
    private LoginLockoutService service;

    @BeforeEach
    void setUp() {
        config = new LoginProtectionConfig();
        service = new LoginLockoutService(userRepository, config);
    }

    private User user(int failedAttempts, OffsetDateTime lockedUntil) {
        User user = new User();
        user.setId(1L);
        user.setEmail("admin@lokmitfoundation.org");
        user.setFailedLoginAttempts(failedAttempts);
        user.setLockedUntil(lockedUntil);
        return user;
    }

    // ------------------------------------------------------------------
    // Default configuration values
    // ------------------------------------------------------------------

    @Test
    @DisplayName("default configuration: 5 max failed attempts, 15 minute lockout")
    void defaultConfigurationValues() {
        assertEquals(5, config.getMaxFailedAttempts());
        assertEquals(15, config.getLockoutDurationMinutes());
    }

    // ------------------------------------------------------------------
    // recordFailedAttempt — incrementing and threshold
    // ------------------------------------------------------------------

    @Test
    @DisplayName("failed attempt increments counter without triggering lock below threshold")
    void failedAttemptIncrementsCounterBelowThreshold() {
        User user = user(0, null);
        when(userRepository.findByEmailForUpdate("admin@lokmitfoundation.org"))
                .thenReturn(Optional.of(user));

        service.recordFailedAttempt("admin@lokmitfoundation.org");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(1, captor.getValue().getFailedLoginAttempts());
        assertNull(captor.getValue().getLockedUntil(), "No lock below the configured threshold");
        assertNotNull(captor.getValue().getFailedLoginWindowStartedAt(),
                "First failure opens the failure window");
    }

    @Test
    @DisplayName("lockout activates exactly at the configured threshold (boundary)")
    void lockoutActivatesExactlyAtThreshold() {
        User user = user(config.getMaxFailedAttempts() - 1, null);
        when(userRepository.findByEmailForUpdate("admin@lokmitfoundation.org"))
                .thenReturn(Optional.of(user));

        OffsetDateTime before = OffsetDateTime.now();
        service.recordFailedAttempt("admin@lokmitfoundation.org");
        OffsetDateTime after = OffsetDateTime.now();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(config.getMaxFailedAttempts(), captor.getValue().getFailedLoginAttempts(),
                "Counter reaches exactly the threshold");
        assertNotNull(captor.getValue().getLockedUntil(), "Threshold failure locks the account");

        OffsetDateTime expectedEarliest = before.plusMinutes(config.getLockoutDurationMinutes());
        OffsetDateTime expectedLatest = after.plusMinutes(config.getLockoutDurationMinutes());
        assertFalse(captor.getValue().getLockedUntil().isBefore(expectedEarliest),
                "Lock expiry must be at least lockoutDuration after the failure");
        assertTrue(captor.getValue().getLockedUntil().isAfter(expectedLatest.minusSeconds(1)),
                "Lock expiry must be roughly lockoutDuration after the failure");
    }

    @Test
    @DisplayName("attempt one below threshold does not lock (boundary)")
    void attemptBelowThresholdDoesNotLock() {
        User user = user(config.getMaxFailedAttempts() - 2, null);
        when(userRepository.findByEmailForUpdate("admin@lokmitfoundation.org"))
                .thenReturn(Optional.of(user));

        service.recordFailedAttempt("admin@lokmitfoundation.org");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(config.getMaxFailedAttempts() - 1, captor.getValue().getFailedLoginAttempts());
        assertNull(captor.getValue().getLockedUntil());
    }

    @Test
    @DisplayName("configured custom values are respected (threshold 3, duration 7)")
    void customConfigurationValuesAreRespected() {
        config.setMaxFailedAttempts(3);
        config.setLockoutDurationMinutes(7);

        User user = user(2, null);
        when(userRepository.findByEmailForUpdate("admin@lokmitfoundation.org"))
                .thenReturn(Optional.of(user));

        OffsetDateTime before = OffsetDateTime.now();
        service.recordFailedAttempt("admin@lokmitfoundation.org");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(3, captor.getValue().getFailedLoginAttempts());
        assertNotNull(captor.getValue().getLockedUntil());
        OffsetDateTime expiry = captor.getValue().getLockedUntil();
        assertFalse(expiry.isBefore(before.plusMinutes(7)),
                "Lock expiry must follow the configured 7 minute duration");
        assertTrue(expiry.isBefore(before.plusMinutes(8)),
                "Lock expiry must not run far beyond the configured duration");
    }

    @Test
    @DisplayName("unknown email is silently ignored (no enumeration surface)")
    void unknownEmailIsIgnored() {
        when(userRepository.findByEmailForUpdate("ghost@example.com"))
                .thenReturn(Optional.empty());

        service.recordFailedAttempt("ghost@example.com");

        verify(userRepository, never()).save(any(User.class));
    }

    // ------------------------------------------------------------------
    // isLocked — active, expired, and absent lock state
    // ------------------------------------------------------------------

    @Test
    @DisplayName("isLocked is true while lockedUntil is in the future")
    void isLockedWhileActive() {
        User user = user(5, OffsetDateTime.now().plusMinutes(10));
        assertTrue(service.isLocked(user));
    }

    @Test
    @DisplayName("isLocked is false once the lockout has expired")
    void isLockedFalseAfterExpiry() {
        User user = user(5, OffsetDateTime.now().minusSeconds(1));
        assertFalse(service.isLocked(user), "Expired lock must not reject logins");
    }

    @Test
    @DisplayName("isLocked is false when never locked")
    void isLockedFalseWhenNeverLocked() {
        User user = user(0, null);
        assertFalse(service.isLocked(user));
    }

    // ------------------------------------------------------------------
    // resetOnSuccess
    // ------------------------------------------------------------------

    @Test
    @DisplayName("successful login clears counter, failure window and lock")
    void resetOnSuccessClearsAllState() {
        User user = user(4, OffsetDateTime.now().plusMinutes(10));
        user.setFailedLoginWindowStartedAt(OffsetDateTime.now().minusMinutes(2));

        service.resetOnSuccess(user);

        assertEquals(0, user.getFailedLoginAttempts());
        assertNull(user.getFailedLoginWindowStartedAt());
        assertNull(user.getLockedUntil());
    }
}
