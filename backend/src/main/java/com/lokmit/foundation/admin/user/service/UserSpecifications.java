package com.lokmit.foundation.admin.user.service;

import com.lokmit.foundation.security.entity.User;
import org.springframework.data.jpa.domain.Specification;

import java.util.Set;

/**
 * Parameterized query builders for the admin user list.
 *
 * <p>Uses the JPA Criteria API via Spring Data {@link Specification}s — values
 * are bound as query parameters, never concatenated into SQL, so there is no
 * injection surface. Predicates compose so search, status filter, role filter
 * and ordering run entirely database-side. The status values are the existing
 * {@code chk_users_status} domain; statuses that do not exist are rejected at
 * the controller, never invented here.</p>
 */
final class UserSpecifications {

    private UserSpecifications() {
        throw new AssertionError("Utility class must not be instantiated.");
    }

    static Specification<User> hasStatus(String status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    /**
     * Case-insensitive partial match over email and full name — the identity
     * fields that actually exist on the users table. Bound as a LIKE
     * parameter; no wildcard escaping issue since the caller-supplied term is
     * wrapped, not concatenated into the query.
     */
    static Specification<User> emailOrNameContains(String term) {
        String like = "%" + term.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("email")), like),
                cb.like(cb.lower(root.get("fullName")), like));
    }

    /**
     * Membership in the given role. DISTINCT is applied by the caller so a
     * user with the role plus others is not duplicated by the join.
     */
    static Specification<User> hasRoleCode(String roleCode) {
        return (root, query, cb) -> {
            // Force an in-memory distinct on the joined result set to avoid
            // duplicate rows when a user matches through multiple roles.
            query.distinct(true);
            var roleJoin = root.joinSet("roles");
            return cb.equal(roleJoin.get("code"), roleCode);
        };
    }

    static Specification<User> notInIds(Set<Long> ids) {
        return (root, query, cb) -> root.get("id").in(ids).not();
    }

    /**
     * Users whose status is ACTIVE and who hold the SUPER_ADMIN role.
     * Backs the "last active SUPER_ADMIN" protection count.
     */
    static Specification<User> activeSuperAdmins() {
        return (root, query, cb) -> {
            query.distinct(true);
            var roleJoin = root.joinSet("roles");
            return cb.and(
                    cb.equal(root.get("status"), "ACTIVE"),
                    cb.equal(roleJoin.get("code"), "SUPER_ADMIN"));
        };
    }
}
