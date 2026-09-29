package com.lokmit.foundation.admin.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

/**
 * Request payload for replacing a user's role assignments via
 * {@code PUT /api/v1/admin/users/{id}/roles}.
 *
 * <p>The request is a full replacement: the resulting assignment is exactly
 * the supplied set; an empty set removes all roles (subject to the backend's
 * last-SUPER_ADMIN protection). Role codes are validated against the seeded
 * {@code roles} table at runtime — the per-element pattern here only rejects
 * structurally impossible values early.</p>
 */
@Getter
@Setter
@Schema(description = "Full role replacement for a user. Use an empty list to remove all roles.")
public class UpdateUserRolesRequest {

    @NotNull(message = "Roles is required")
    @Schema(description = "Role codes to assign (full replacement). An empty list removes all roles.",
            example = "[\"MODERATOR\"]")
    private Set<@Pattern(regexp = "SUPER_ADMIN|ADMIN|EDITOR|MODERATOR|CANDIDATE|EMPLOYER|CLIENT",
            message = "Unknown role code") String> roles;
}
