package com.lokmit.foundation.admin.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.Set;

/**
 * Safe administrative view of a user account.
 *
 * <p>Returned only from endpoints guarded by the {@code users:manage}
 * permission. Deliberately excludes everything security-sensitive:
 * {@code password_hash}, refresh-token data, and the I-2 brute-force
 * bookkeeping columns ({@code failed_login_attempts},
 * {@code failed_login_window_started_at}, {@code locked_until}) are absent
 * from this DTO, so they can never leak into an admin response.</p>
 *
 * <p>Used for both the paginated list and the single-user detail view.</p>
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "User account as visible to authorized administrators")
public class AdminUserResponse {

    @Schema(description = "User identifier", example = "42")
    private Long id;

    @Schema(description = "Email address", example = "ramesh@example.com")
    private String email;

    @Schema(description = "Full name", example = "Ramesh Kumar")
    private String fullName;

    @Schema(description = "Platform user type", example = "STAFF",
            allowableValues = {"CANDIDATE", "EMPLOYER", "CLIENT", "STAFF"})
    private String userType;

    @Schema(description = "Account status", example = "ACTIVE",
            allowableValues = {"ACTIVE", "LOCKED", "SUSPENDED", "DELETED"})
    private String status;

    @Schema(description = "Whether the email address is verified", example = "true")
    private Boolean emailVerified;

    @Schema(description = "Assigned role codes", example = "[\"MODERATOR\"]")
    private Set<String> roles;

    @Schema(description = "When the account was created")
    private OffsetDateTime createdAt;

    @Schema(description = "When the account was last updated")
    private OffsetDateTime updatedAt;

    @Schema(description = "Last successful login, if any")
    private OffsetDateTime lastLoginAt;
}
