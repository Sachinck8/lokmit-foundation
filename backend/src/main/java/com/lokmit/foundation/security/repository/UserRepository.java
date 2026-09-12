package com.lokmit.foundation.security.repository;

import com.lokmit.foundation.security.entity.User;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /**
     * Fetches a user for failed-login bookkeeping under a pessimistic write
     * lock ({@code SELECT ... FOR UPDATE}), so concurrent failed logins for
     * the same identity serialize on the row and the lockout threshold cannot
     * be bypassed by parallel requests.
     *
     * <p>The {@code jakarta.persistence.lock.timeout} hint of {@code 0} makes a
     * blocked read fail fast instead of piling up behind a long-held lock.
     * Only the login-protection service uses this method.</p>
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "0"))
    Optional<User> findByEmailForUpdate(String email);
}