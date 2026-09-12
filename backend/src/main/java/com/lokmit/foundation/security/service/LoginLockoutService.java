package com.lokmit.foundation.security.service;

import com.lokmit.foundation.security.config.LoginProtectionConfig;
import com.lokmit.foundation.security.entity.User;
import com.lokmit.foundation.security.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * Persistent, account-level login brute-force protection.
 *
 * <p>Tracks consecutive failed login attempts against the {@code users} table
 * (columns added by V9__login_protection.sql). Because the state lives in the
 * database — not in memory — it is shared by every backend instance in a
 * multi-instance deployment and survives application restarts. No in-memory
 * limiter is involved, so there are no per-instance inconsistencies.</p>
 *
 * <p>Concurrency: {@link #recordFailedAttempt(String)} runs in its own
 * transaction ({@code REQUIRES_NEW}) and re-reads the user row with a
 * pessimistic write lock ({@code SELECT ... FOR UPDATE}), so simultaneous
 * failed logins for the same identity are serialized on the row lock and the
 * configured threshold cannot be bypassed by parallel requests. It must run
 * in a separate transaction because the caller's login transaction rolls back
 * when authentication fails — the failure record must survive that rollback.</p>
 *
 * <p>Anti-enumeration: every outcome (unknown email, wrong password, locked)
 * is reported by the caller as the same generic authentication failure; this
 * class neither throws on unknown emails nor exposes whether an identity
 * exists, and never returns remaining lockout time. Passwords, tokens and
 * secrets are never logged.</p>
 */
@Service
public class LoginLockoutService {

    private static final Logger LOG = LoggerFactory.getLogger(LoginLockoutService.class);

    private final UserRepository userRepository;
    private final LoginProtectionConfig config;

    public LoginLockoutService(UserRepository userRepository, LoginProtectionConfig config) {
        this.userRepository = userRepository;
        this.config = config;
    }

    /**
     * Returns {@code true} while the temporary brute-force lockout is active.
     * The lock is a fixed-duration timestamp: it simply expires once
     * {@code lockedUntil} is in the past. Never reveals remaining time.
     */
    public boolean isLocked(User user) {
        return user.getLockedUntil() != null && user.getLockedUntil().isAfter(OffsetDateTime.now());
    }

    /**
     * Records one failed login attempt for the given login identity.
     *
     * <p>Atomic under concurrency: the row is locked, the counter incremented
     * and — exactly when the configured threshold is reached — the temporary
     * lock is set for the configured duration. Unknown emails are silently
     * ignored so the operation cannot be used to enumerate accounts.
     * Runs in {@code REQUIRES_NEW} so the record survives the caller's
     * rollback.</p>
     *
     * @param email normalized login identity (lower-cased, trimmed)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailedAttempt(String email) {
        userRepository.findByEmailForUpdate(email).ifPresent(user -> {
            int attempts = user.getFailedLoginAttempts() + 1;
            OffsetDateTime now = OffsetDateTime.now();

            user.setFailedLoginAttempts(attempts);
            if (user.getFailedLoginWindowStartedAt() == null) {
                user.setFailedLoginWindowStartedAt(now);
            }
            if (attempts >= config.getMaxFailedAttempts()) {
                user.setLockedUntil(now.plusMinutes(config.getLockoutDurationMinutes()));
                LOG.warn("Temporary login lockout activated for {} minute(s) after {} failed attempts",
                        config.getLockoutDurationMinutes(), attempts);
            } else {
                LOG.debug("Failed login attempt {} of {} recorded", attempts, config.getMaxFailedAttempts());
            }
            userRepository.save(user);
        });
    }

    /**
     * Clears the failed-attempt counter and any temporary lockout after a
     * successful authentication. Mutates the managed entity so the reset
     * commits atomically with the caller's login transaction.
     */
    public void resetOnSuccess(User user) {
        user.setFailedLoginAttempts(0);
        user.setFailedLoginWindowStartedAt(null);
        user.setLockedUntil(null);
    }
}
