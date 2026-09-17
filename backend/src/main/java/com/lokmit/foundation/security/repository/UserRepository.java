package com.lokmit.foundation.security.repository;

import com.lokmit.foundation.security.entity.User;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /**
     * Fetches a user for failed-login bookkeeping under a pessimistic write
     * lock ({@code SELECT ... FOR UPDATE}), so concurrent failed logins for
     * the same identity serialize on the row and the lockout threshold cannot
     * be bypassed by parallel requests.
     *
     * <p>The query is explicit because {@code findByEmailForUpdate} is not a
     * derivable method name (Spring Data would parse {@code ForUpdate} as a
     * nonexistent property and fail at startup). The JPQL selects by the
     * {@code email} property; the {@code @Lock} annotation below supplies the
     * {@code FOR UPDATE} clause in the generated SQL — it is NOT part of the
     * JPQL itself.</p>
     *
     * <p>The {@code jakarta.persistence.lock.timeout} hint of {@code 0} makes a
     * blocked read fail fast instead of piling up behind a long-held lock.
     * Only the login-protection service uses this method.</p>
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "0"))
    @Query("SELECT u FROM User u WHERE u.email = :email")
    Optional<User> findByEmailForUpdate(@Param("email") String email);
}