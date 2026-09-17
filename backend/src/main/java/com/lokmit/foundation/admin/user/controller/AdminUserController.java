package com.lokmit.foundation.admin.user.controller;

import com.lokmit.foundation.admin.user.dto.AdminUserResponse;
import com.lokmit.foundation.admin.user.dto.UpdateUserRolesRequest;
import com.lokmit.foundation.admin.user.dto.UpdateUserStatusRequest;
import com.lokmit.foundation.admin.user.service.AdminUserService;
import com.lokmit.foundation.common.api.ApiResponse;
import com.lokmit.foundation.common.api.PageResponse;
import com.lokmit.foundation.common.constants.ApiPaths;
import com.lokmit.foundation.common.exception.BadRequestException;
import com.lokmit.foundation.common.pagination.PageParams;
import com.lokmit.foundation.security.Permissions;
import com.lokmit.foundation.security.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Administrative user management endpoints (A3).
 *
 * <p>Every endpoint requires authentication and the {@code users:manage}
 * permission (granted to SUPER_ADMIN by the V2 seed), enforced with method
 * security so the permission travels with the endpoint. All responses are
 * DTOs — the User entity, password hashes, refresh-token data and I-2
 * brute-force bookkeeping never leave the service layer.</p>
 */
@RestController
@RequestMapping(ApiPaths.ADMIN_USERS)
@Tag(name = "Admin Users", description = "Administrative user management (users:manage permission required)")
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final SecurityUtils securityUtils;

    public AdminUserController(AdminUserService adminUserService, SecurityUtils securityUtils) {
        this.adminUserService = adminUserService;
        this.securityUtils = securityUtils;
    }

    /**
     * Paginated user list, newest first. Optional search (email/full name),
     * status and role filters — all database-side and parameterized.
     */
    @GetMapping
    @PreAuthorize("hasAuthority('" + Permissions.USERS_MANAGE + "')")
    @Operation(summary = "List user accounts",
            description = "Requires authentication and the users:manage permission. Supports "
                    + "search (email/full name), status and role filtering, and pagination. "
                    + "Newest first by default.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<PageResponse<AdminUserResponse>>> listUsers(
            @Valid @ModelAttribute PageParams pageParams,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String role) {

        validateFilters(status, role);

        Pageable pageable = PageRequest.of(
                pageParams.getPage(), pageParams.getSize(), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AdminUserResponse> page = adminUserService.listUsers(search, status, role, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(page)));
    }

    /**
     * Safe detail view of one user. Unknown ids map to the standard 404.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.USERS_MANAGE + "')")
    @Operation(summary = "Get a user account",
            description = "Requires authentication and the users:manage permission. Returns the "
                    + "safe administrative view; no authentication secrets are included.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<AdminUserResponse>> getUser(@PathVariable long id) {
        return ResponseEntity.ok(ApiResponse.success(adminUserService.getUser(id)));
    }

    /**
     * Changes the account status (explicit whitelist — not a generic PATCH).
     * Self-demotion and last-SUPER_ADMIN disabling are rejected with 400.
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('" + Permissions.USERS_MANAGE + "')")
    @Operation(summary = "Change a user's account status",
            description = "Requires authentication and the users:manage permission. Only the "
                    + "status field changes. Administrators cannot deactivate their own "
                    + "account, and the last active SUPER_ADMIN cannot be disabled.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<AdminUserResponse>> updateStatus(
            @PathVariable long id,
            @Valid @RequestBody UpdateUserStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                adminUserService.updateStatus(id, request,
                        securityUtils.getCurrentUserId(), actorIsSuperAdmin()),
                "User status updated"));
    }

    /**
     * Full role replacement. Self-stripping, last-SUPER_ADMIN removal and
     * non-SUPER_ADMIN grants of SUPER_ADMIN are rejected with 400/403.
     */
    @PutMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('" + Permissions.USERS_MANAGE + "')")
    @Operation(summary = "Replace a user's role assignments",
            description = "Requires authentication and the users:manage permission. Performs a "
                    + "full replacement: the resulting roles are exactly the supplied set. "
                    + "Administrators cannot change their own roles; the SUPER_ADMIN role can "
                    + "only be granted by a SUPER_ADMIN; the last active SUPER_ADMIN cannot "
                    + "lose the role.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<AdminUserResponse>> updateRoles(
            @PathVariable long id,
            @Valid @RequestBody UpdateUserRolesRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                adminUserService.updateRoles(id, request,
                        securityUtils.getCurrentUserId(), actorIsSuperAdmin()),
                "User roles updated"));
    }

    /**
     * Validates optional list filters against the actual seeded domains.
     * Unknown values produce the standard 400 envelope rather than a silent
     * empty result.
     */
    private void validateFilters(String status, String role) {
        if (status != null && !status.isBlank()
                && !AdminUserService.VALID_STATUSES.contains(status.trim().toUpperCase(java.util.Locale.ROOT))) {
            throw new BadRequestException("Invalid status value");
        }
        if (role != null && !role.isBlank()
                && !AdminUserService.VALID_ROLE_CODES.contains(role.trim().toUpperCase(java.util.Locale.ROOT))) {
            throw new BadRequestException("Invalid role value");
        }
    }

    /**
     * Whether the current caller holds ROLE_SUPER_ADMIN, as established by
     * the per-request database-backed authorities (never a JWT claim).
     */
    private boolean actorIsSuperAdmin() {
        var authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_SUPER_ADMIN".equals(a.getAuthority()));
    }
}
