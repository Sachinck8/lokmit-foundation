package com.lokmit.foundation.employment.application.controller;

import com.lokmit.foundation.common.api.ApiResponse;
import com.lokmit.foundation.common.api.PageResponse;
import com.lokmit.foundation.common.constants.ApiPaths;
import com.lokmit.foundation.common.pagination.PageParams;
import com.lokmit.foundation.employment.application.dto.ApplicationResponse;
import com.lokmit.foundation.employment.application.dto.ApplicationReviewRequest;
import com.lokmit.foundation.employment.application.service.ApplicationService;
import com.lokmit.foundation.security.Permissions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin application review endpoints (A7.3). All endpoints require the
 * existing {@code employment:manage} permission (SUPER_ADMIN, ADMIN per the
 * V14 grant) — reviewing applications is employment administration, so no
 * new permission or migration was needed.
 *
 * <p>Lifecycle (chk_job_applications_status values only):
 * SUBMITTED → UNDER_REVIEW → SHORTLISTED → HIRED/REJECTED, with WITHDRAWN
 * reachable from any pre-decision state. Status is controlled exclusively
 * by these endpoints; PATCH cannot change it. There is deliberately NO
 * delete endpoint: applications are the hiring audit trail.</p>
 */
@RestController
@RequestMapping(ApiPaths.ADMIN_APPLICATIONS)
@Tag(name = "Admin Applications",
        description = "Job application review (employment:manage permission required)")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permissions.EMPLOYMENT_MANAGE + "')")
    @Operation(summary = "List applications",
            description = "Supports pagination, jobId/candidateId/employerId/status filters "
                    + "and cover/employer note search. Newest applications first.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<PageResponse<ApplicationResponse>>> listApplications(
            @Valid @ModelAttribute PageParams pageParams,
            @RequestParam(name = "jobId", required = false) Long jobId,
            @RequestParam(name = "candidateId", required = false) Long candidateId,
            @RequestParam(name = "employerId", required = false) Long employerId,
            @RequestParam(required = false) String status,
            @RequestParam(name = "search", required = false) String search) {
        Pageable pageable = PageRequest.of(pageParams.getPage(), pageParams.getSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(ApiResponse.success(
                PageResponse.of(applicationService.listApplications(
                        jobId, candidateId, employerId, status, search, pageable))));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.EMPLOYMENT_MANAGE + "')")
    @Operation(summary = "Get an application by id",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<ApplicationResponse>> getApplication(
            @PathVariable long id) {
        return ResponseEntity.ok(ApiResponse.success(
                applicationService.getApplication(id)));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.EMPLOYMENT_MANAGE + "')")
    @Operation(summary = "Update an application review",
            description = "Partial update of employerNote and the optional resumeId "
                    + "reference; job/candidate identity is immutable; status is not "
                    + "patchable — use the lifecycle endpoints.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<ApplicationResponse>> updateReview(
            @PathVariable long id,
            @Valid @RequestBody ApplicationReviewRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                applicationService.updateReview(id, request), "Application updated"));
    }

    @PostMapping("/{id}/start-review")
    @PreAuthorize("hasAuthority('" + Permissions.EMPLOYMENT_MANAGE + "')")
    @Operation(summary = "Move an application to UNDER_REVIEW",
            description = "Allowed from SUBMITTED only; other states → 400.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<ApplicationResponse>> startReview(
            @PathVariable long id) {
        return ResponseEntity.ok(ApiResponse.success(
                applicationService.startReview(id), "Application under review"));
    }

    @PostMapping("/{id}/shortlist")
    @PreAuthorize("hasAuthority('" + Permissions.EMPLOYMENT_MANAGE + "')")
    @Operation(summary = "Shortlist an application",
            description = "Allowed from UNDER_REVIEW only; other states → 400.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<ApplicationResponse>> shortlist(
            @PathVariable long id) {
        return ResponseEntity.ok(ApiResponse.success(
                applicationService.shortlist(id), "Application shortlisted"));
    }

    @PostMapping("/{id}/decide")
    @PreAuthorize("hasAuthority('" + Permissions.EMPLOYMENT_MANAGE + "')")
    @Operation(summary = "Record a hiring decision (HIRED or REJECTED)",
            description = "Terminal. Allowed from SUBMITTED/UNDER_REVIEW/SHORTLISTED; "
                    + "stamps decided_at; already-decided → 409; WITHDRAWN → 400. "
                    + "Body: {\"decision\":\"HIRED\"|\"REJECTED\",\"note\": optional}",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<ApplicationResponse>> decide(
            @PathVariable long id,
            @RequestBody Map<String, String> body) {
        String decision = body.getOrDefault("decision", "");
        if (!"HIRED".equalsIgnoreCase(decision) && !"REJECTED".equalsIgnoreCase(decision)) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(ApiResponse.success(
                applicationService.decide(id, "HIRED".equalsIgnoreCase(decision),
                        body.get("note")),
                "Application decided"));
    }

    @PostMapping("/{id}/withdraw")
    @PreAuthorize("hasAuthority('" + Permissions.EMPLOYMENT_MANAGE + "')")
    @Operation(summary = "Record a withdrawal",
            description = "Allowed from any pre-decision state; already-decided → 409.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<ApplicationResponse>> withdraw(
            @PathVariable long id) {
        return ResponseEntity.ok(ApiResponse.success(
                applicationService.withdraw(id), "Application withdrawn"));
    }
}
