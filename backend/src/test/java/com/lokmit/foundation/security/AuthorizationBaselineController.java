package com.lokmit.foundation.security;

import com.lokmit.foundation.common.api.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A1 test-scope controller used to verify the permission-based authorization
 * baseline that every future Admin API must follow. It lives in the test
 * sources only — it never ships in the production jar and exposes no
 * permanent API surface.
 *
 * <p>Each endpoint maps one seeded permission (see
 * {@code V2__identity_schema.sql}) so the authorization matrix can be
 * exercised against real seed semantics:</p>
 * <ul>
 *   <li>{@code users:manage} — SUPER_ADMIN only (user/role administration)</li>
 *   <li>{@code messages:manage} — SUPER_ADMIN, ADMIN, MODERATOR</li>
 *   <li>{@code jobs:moderate} — SUPER_ADMIN, MODERATOR</li>
 *   <li>{@code settings:manage} — SUPER_ADMIN, ADMIN</li>
 * </ul>
 *
 * <p>The {@code @PreAuthorize} expressions intentionally use the shared
 * {@link Permissions} constant (the convention future Admin APIs must copy)
 * rather than string literals, proving the constant-based style enforces
 * identically.</p>
 */
@RestController
public class AuthorizationBaselineController {

    @GetMapping("/api/v1/security-baseline/users-manage")
    @PreAuthorize("hasAuthority('" + Permissions.USERS_MANAGE + "')")
    public ApiResponse<String> usersManage() {
        return ApiResponse.success("users:manage ok");
    }

    @GetMapping("/api/v1/security-baseline/messages-manage")
    @PreAuthorize("hasAuthority('" + Permissions.MESSAGES_MANAGE + "')")
    public ApiResponse<String> messagesManage() {
        return ApiResponse.success("messages:manage ok");
    }

    @GetMapping("/api/v1/security-baseline/jobs-moderate")
    @PreAuthorize("hasAuthority('" + Permissions.JOBS_MODERATE + "')")
    public ApiResponse<String> jobsModerate() {
        return ApiResponse.success("jobs:moderate ok");
    }

    @GetMapping("/api/v1/security-baseline/settings-manage")
    @PreAuthorize("hasAuthority('" + Permissions.SETTINGS_MANAGE + "')")
    public ApiResponse<String> settingsManage() {
        return ApiResponse.success("settings:manage ok");
    }
}
