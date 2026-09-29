package com.lokmit.foundation.admin.user.service;

import com.lokmit.foundation.admin.user.dto.AdminUserResponse;
import com.lokmit.foundation.admin.user.dto.UpdateUserRolesRequest;
import com.lokmit.foundation.admin.user.dto.UpdateUserStatusRequest;
import com.lokmit.foundation.common.exception.BadRequestException;
import com.lokmit.foundation.common.exception.NotFoundException;
import com.lokmit.foundation.security.entity.User;
import com.lokmit.foundation.security.repository.RefreshTokenRepository;
import com.lokmit.foundation.security.repository.RoleRepository;
import com.lokmit.foundation.security.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Administrative user management (A3).
 *
 * <p><strong>Protection rules enforced here, never in the frontend:</strong></p>
 *
 * <ul>
 *   <li><em>Self-protection:</em> an administrator cannot move their own
 *       account to a non-ACTIVE status or strip their own roles — accidental
 *       self-lockout is rejected with 400.</li>
 *   <li><em>Last-SUPER_ADMIN protection:</em> the system must always retain
 *       at least one ACTIVE SUPER_ADMIN. Disabling the final one, or removing
 *       the SUPER_ADMIN role from the final one, is rejected with 400.</li>
 *   <li><em>No privilege escalation:</em> granting the SUPER_ADMIN role is
 *       restricted to callers who themselves hold ROLE_SUPER_ADMIN (the
 *       users:manage permission already restricts the whole API to
 *       SUPER_ADMIN in the current seed; this rule additionally keeps the
 *       service safe if users:manage is ever granted more widely).</li>
 * </ul>
 *
 * <p>Side effects: moving an account out of ACTIVE revokes all its refresh
 * tokens (I-8 repository support) so existing sessions end immediately —
 * matching the A1 fail-closed JWT filter, which already refuses
 * non-ACTIVE accounts on their next request.</p>
 */
@Service
public class AdminUserService {

    private static final Logger LOG = LoggerFactory.getLogger(AdminUserService.class);

    /** The only status values the users.check constraint admits. */
    public static final Set<String> VALID_STATUSES = Set.of("ACTIVE", "LOCKED", "SUSPENDED", "DELETED");

    /** Role codes seeded by V2; used for filter validation and assignment. */
    public static final Set<String> VALID_ROLE_CODES =
            Set.of("SUPER_ADMIN", "ADMIN", "EDITOR", "MODERATOR", "CANDIDATE", "EMPLOYER", "CLIENT");

    public static final String SUPER_ADMIN_CODE = "SUPER_ADMIN";
    public static final String ACTIVE_STATUS = "ACTIVE";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public AdminUserService(UserRepository userRepository,
                            RoleRepository roleRepository,
                            RefreshTokenRepository refreshTokenRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    /**
     * Paginated, database-side filtered user list. Newest first.
     *
     * @param search optional case-insensitive match on email/full name
     * @param status optional exact status filter (validated domain value)
     * @param role   optional exact role-code filter (validated domain value)
     */
    @Transactional(readOnly = true)
    public Page<AdminUserResponse> listUsers(String search, String status, String role, Pageable pageable) {
        Specification<User> spec = Specification.where(null);

        if (search != null && !search.isBlank()) {
            spec = spec.and(UserSpecifications.emailOrNameContains(search.trim()));
        }
        if (status != null && !status.isBlank()) {
            spec = spec.and(UserSpecifications.hasStatus(status.trim().toUpperCase(Locale.ROOT)));
        }
        if (role != null && !role.isBlank()) {
            spec = spec.and(UserSpecifications.hasRoleCode(role.trim().toUpperCase(Locale.ROOT)));
        }

        return userRepository.findAll(spec, pageable).map(AdminUserMapper::toResponse);
    }

    /**
     * Safe administrative detail view of a single user.
     */
    @Transactional(readOnly = true)
    public AdminUserResponse getUser(long id) {
        return userRepository.findById(id)
                .map(AdminUserMapper::toResponse)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    /**
     * Changes an account status. Only the status field is touched — this is
     * not a generic user update. Self-demotion to non-ACTIVE is rejected, and
     * disabling the last ACTIVE SUPER_ADMIN is rejected. Deactivation also
     * revokes the user's refresh tokens so their sessions end immediately.
     */
    @Transactional
    public AdminUserResponse updateStatus(long targetUserId, UpdateUserStatusRequest request,
                                          Long actorUserId, boolean actorIsSuperAdmin) {
        String newStatus = request.getStatus().trim().toUpperCase(Locale.ROOT);
        if (!VALID_STATUSES.contains(newStatus)) {
            throw new BadRequestException("Status must be one of: ACTIVE, LOCKED, SUSPENDED, DELETED");
        }

        // Self-protection first (fail fast): an administrator must never be
        // able to lock themselves out through this endpoint.
        if (actorUserId != null && actorUserId == targetUserId
                && !ACTIVE_STATUS.equals(newStatus)) {
            throw new BadRequestException("Administrators cannot change their own account status to a non-active state");
        }

        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        // Privilege preservation: a caller without ROLE_SUPER_ADMIN must not
        // be able to disable a SUPER_ADMIN account (kept safe for the case
        // where users:manage is ever granted more widely than SUPER_ADMIN).
        if (!actorIsSuperAdmin && target.getRoles().stream()
                .anyMatch(r -> SUPER_ADMIN_CODE.equals(r.getCode()))) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Only a SUPER_ADMIN can change the status of a SUPER_ADMIN account");
        }

        boolean statusChangesToInactive = !ACTIVE_STATUS.equals(newStatus)
                && ACTIVE_STATUS.equals(target.getStatus());
        if (statusChangesToInactive && isLastActiveSuperAdmin(target)) {
            throw new BadRequestException(
                    "Cannot disable the last active SUPER_ADMIN account");
        }

        boolean wasActive = ACTIVE_STATUS.equals(target.getStatus());
        target.setStatus(newStatus);
        target.setUpdatedAt(OffsetDateTime.now());
        AdminUserResponse response = AdminUserMapper.toResponse(userRepository.save(target));

        if (wasActive && !ACTIVE_STATUS.equals(newStatus)) {
            // I-8 repository support: end the user's active sessions
            // immediately. The A1 fail-closed JWT filter already refuses
            // non-ACTIVE accounts, so this only accelerates refresh denial.
            refreshTokenRepository.revokeAllByUserId(targetUserId, OffsetDateTime.now());
            LOG.info("Revoked refresh tokens after status change of user id {}", targetUserId);
        }

        LOG.info("User id {} status changed to {} by actor id {}", targetUserId, newStatus, actorUserId);
        return response;
    }

    /**
     * Replaces a user's role assignment (full replacement).
     *
     * <p>Rules: the actor cannot strip their own roles (self-lockout), the
     * last ACTIVE SUPER_ADMIN cannot lose the SUPER_ADMIN role, and granting
     * SUPER_ADMIN requires the caller to themselves hold ROLE_SUPER_ADMIN.</p>
     */
    @Transactional
    public AdminUserResponse updateRoles(long targetUserId, UpdateUserRolesRequest request,
                                         Long actorUserId, boolean actorIsSuperAdmin) {
        Set<String> requested = request.getRoles();
        for (String code : requested) {
            if (!VALID_ROLE_CODES.contains(code)) {
                throw new BadRequestException("Unknown role code: " + code);
            }
        }

        // Self-protection first (fail fast): stripping one's own roles is a
        // self-lockout path and is always rejected.
        if (actorUserId != null && actorUserId == targetUserId) {
            throw new BadRequestException("Administrators cannot change their own role assignments");
        }

        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        boolean dropsSuperAdmin = !requested.contains(SUPER_ADMIN_CODE)
                && target.getRoles().stream().anyMatch(r -> SUPER_ADMIN_CODE.equals(r.getCode()));
        if (dropsSuperAdmin && isLastActiveSuperAdmin(target)) {
            throw new BadRequestException("Cannot remove the SUPER_ADMIN role from the last active SUPER_ADMIN account");
        }

        if (requested.contains(SUPER_ADMIN_CODE) && !actorIsSuperAdmin) {
            // Handled by the global exception handler as 403 — this is an
            // authorization boundary, not a malformed request.
            throw new org.springframework.security.access.AccessDeniedException(
                    "Only a SUPER_ADMIN can grant the SUPER_ADMIN role");
        }

        Set<com.lokmit.foundation.security.entity.Role> resolved =
                requested.stream()
                        .map(code -> roleRepository.findByCode(code)
                                .orElseThrow(() -> new BadRequestException("Unknown role code: " + code)))
                        .collect(Collectors.toSet());

        target.getRoles().clear();
        target.getRoles().addAll(resolved);
        target.setUpdatedAt(OffsetDateTime.now());
        AdminUserResponse response = AdminUserMapper.toResponse(userRepository.save(target));

        LOG.info("Roles of user id {} replaced with {} by actor id {}", targetUserId, requested, actorUserId);
        return response;
    }

    /**
     * True when the given user is the only ACTIVE SUPER_ADMIN left. Counts
     * directly in the database (no entity loading) so the check stays cheap
     * and correct on large tables.
     */
    private boolean isLastActiveSuperAdmin(User target) {
        if (target.getRoles().stream().noneMatch(r -> SUPER_ADMIN_CODE.equals(r.getCode()))
                || !ACTIVE_STATUS.equals(target.getStatus())) {
            return false; // target is not an active SUPER_ADMIN at all
        }
        return userRepository.count(UserSpecifications.activeSuperAdmins()) <= 1;
    }
}
