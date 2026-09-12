package com.lokmit.foundation.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

/**
 * One user account as shown in the Admin Dashboard's recent-activity list
 * (A2). Deliberately excludes every security-sensitive field: no password
 * hash, no tokens, no role/permission material (role administration belongs
 * to the future User Management API, not the dashboard).
 */
@Getter
@Builder
@Schema(description = "Recent user account row for the dashboard")
public class DashboardUserResponse {

    @Schema(description = "User identifier", example = "17")
    private Long id;

    @Schema(description = "Account email", example = "priya.sharma@example.com")
    private String email;

    @Schema(description = "Account display name", example = "Priya Sharma")
    private String fullName;

    @Schema(description = "Platform user type", example = "CANDIDATE",
            allowableValues = {"CANDIDATE", "EMPLOYER", "CLIENT", "STAFF"})
    private String userType;

    @Schema(description = "Account status", example = "ACTIVE",
            allowableValues = {"ACTIVE", "LOCKED", "SUSPENDED", "DELETED"})
    private String status;

    @Schema(description = "When the account was created")
    private java.time.OffsetDateTime createdAt;

    @Schema(description = "Most recent successful login, if any")
    private java.time.OffsetDateTime lastLoginAt;
}
