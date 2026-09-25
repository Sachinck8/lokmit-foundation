package com.lokmit.foundation.admin.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

/**
 * Request payload for changing a user's account status via
 * {@code PATCH /api/v1/admin/users/{id}/status}.
 *
 * <p>The status is the only accepted field — this is not a generic user
 * update endpoint. Values are restricted to the existing
 * {@code chk_users_status} check-constraint domain.</p>
 */
@Getter
@Setter
@Schema(description = "Account status change. Only ACTIVE, LOCKED, SUSPENDED and DELETED exist.")
public class UpdateUserStatusRequest {

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "ACTIVE|LOCKED|SUSPENDED|DELETED",
            message = "Status must be one of: ACTIVE, LOCKED, SUSPENDED, DELETED")
    @Schema(description = "New account status", example = "SUSPENDED",
            allowableValues = {"ACTIVE", "LOCKED", "SUSPENDED", "DELETED"})
    private String status;
}
