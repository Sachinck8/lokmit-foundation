package com.lokmit.foundation.dashboard.controller;

import com.lokmit.foundation.common.api.ApiResponse;
import com.lokmit.foundation.common.constants.ApiPaths;
import com.lokmit.foundation.dashboard.dto.DashboardApplicationResponse;
import com.lokmit.foundation.dashboard.dto.DashboardEnquiryResponse;
import com.lokmit.foundation.dashboard.dto.DashboardSummaryResponse;
import com.lokmit.foundation.dashboard.dto.DashboardUserResponse;
import com.lokmit.foundation.dashboard.service.DashboardService;
import com.lokmit.foundation.security.Permissions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Read-only Admin Dashboard API (A2).
 *
 * <p>Every endpoint requires authentication plus the {@code dashboard:view}
 * permission (seeded by V11 to SUPER_ADMIN and ADMIN only) — enforced with
 * the A1 method-security convention so anonymous callers get 401 via the
 * authentication entry point and authenticated callers without the
 * permission get 403 from method security.</p>
 *
 * <p>The endpoints never mutate state and never expose entity internals:
 * only DTO projections defined in {@code dashboard.dto} are returned.</p>
 */
@RestController
@RequestMapping(ApiPaths.ADMIN_DASHBOARD)
@Tag(name = "Admin Dashboard", description = "Read-only dashboard summary and recent activity "
        + "for authorized administrators")
public class AdminDashboardController {

    private final DashboardService dashboardService;

    public AdminDashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('" + Permissions.DASHBOARD_VIEW + "')")
    @Operation(summary = "Dashboard summary counts",
            description = "High-level platform counts: users by status, candidate/employer "
                    + "profiles, jobs by lifecycle status, applications, and contact enquiries "
                    + "by status. Requires the dashboard:view permission.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> summary() {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getSummary()));
    }

    @GetMapping("/recent-enquiries")
    @PreAuthorize("hasAuthority('" + Permissions.DASHBOARD_VIEW + "')")
    @Operation(summary = "Recent contact enquiries",
            description = "The newest contact enquiries for dashboard display. "
                    + "Default 5 rows, maximum 10. Requires the dashboard:view permission.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<List<DashboardEnquiryResponse>>> recentEnquiries(
            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(
                ApiResponse.success(dashboardService.getRecentEnquiries(limit)));
    }

    @GetMapping("/recent-users")
    @PreAuthorize("hasAuthority('" + Permissions.DASHBOARD_VIEW + "')")
    @Operation(summary = "Recent user accounts",
            description = "The newest user accounts for dashboard display, without any "
                    + "security-sensitive fields. Default 5 rows, maximum 10. "
                    + "Requires the dashboard:view permission.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<List<DashboardUserResponse>>> recentUsers(
            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(
                ApiResponse.success(dashboardService.getRecentUsers(limit)));
    }

    @GetMapping("/recent-applications")
    @PreAuthorize("hasAuthority('" + Permissions.DASHBOARD_VIEW + "')")
    @Operation(summary = "Recent job applications",
            description = "The newest job applications with candidate and job references, "
                    + "joined in a single query. Default 5 rows, maximum 10. "
                    + "Requires the dashboard:view permission.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<List<DashboardApplicationResponse>>> recentApplications(
            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(
                ApiResponse.success(dashboardService.getRecentApplications(limit)));
    }
}
