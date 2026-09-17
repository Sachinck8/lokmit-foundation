package com.lokmit.foundation.employment.application.interview.controller;

import com.lokmit.foundation.common.api.ApiResponse;
import com.lokmit.foundation.common.api.PageResponse;
import com.lokmit.foundation.common.constants.ApiPaths;
import com.lokmit.foundation.common.pagination.PageParams;
import com.lokmit.foundation.employment.application.interview.dto.InterviewCreateRequest;
import com.lokmit.foundation.employment.application.interview.dto.InterviewResponse;
import com.lokmit.foundation.employment.application.interview.dto.InterviewUpdateRequest;
import com.lokmit.foundation.employment.application.interview.service.InterviewService;
import com.lokmit.foundation.security.Permissions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin interview scheduling (A7.4) nested under the A7.3 application
 * namespace. All endpoints require the existing {@code employment:manage}
 * permission. Interviews are scoped to their application at query level —
 * a foreign (applicationId, interviewId) pair is always a plain 404.
 */
@RestController
@Tag(name = "Admin Application Interviews",
        description = "Interview scheduling for application review "
                + "(employment:manage permission required)")
public class InterviewController {

    private final InterviewService interviewService;

    public InterviewController(InterviewService interviewService) {
        this.interviewService = interviewService;
    }

    @GetMapping(ApiPaths.ADMIN_APPLICATION_INTERVIEWS)
    @PreAuthorize("hasAuthority('" + Permissions.EMPLOYMENT_MANAGE + "')")
    @Operation(summary = "List an application's interviews",
            description = "Newest scheduled_at first; DB-side pagination. "
                    + "Terminal applications remain readable.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<PageResponse<InterviewResponse>>> listInterviews(
            @PathVariable("id") long applicationId,
            @Valid @ModelAttribute PageParams pageParams) {
        return ResponseEntity.ok(ApiResponse.success(
                interviewService.listInterviews(applicationId, pageParams)));
    }

    @PostMapping(ApiPaths.ADMIN_APPLICATION_INTERVIEWS)
    @PreAuthorize("hasAuthority('" + Permissions.EMPLOYMENT_MANAGE + "')")
    @Operation(summary = "Schedule an interview",
            description = "Only non-terminal applications accept new interviews "
                    + "(400 otherwise). New interviews always start SCHEDULED — "
                    + "status is not client-writable.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<InterviewResponse>> createInterview(
            @PathVariable("id") long applicationId,
            @Valid @RequestBody InterviewCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                interviewService.createInterview(applicationId, request),
                "Interview scheduled"));
    }

    @GetMapping(ApiPaths.ADMIN_APPLICATION_INTERVIEW)
    @PreAuthorize("hasAuthority('" + Permissions.EMPLOYMENT_MANAGE + "')")
    @Operation(summary = "Get an interview",
            description = "The interview must belong to the given application; "
                    + "a mismatched pair is a plain 404.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<InterviewResponse>> getInterview(
            @PathVariable("id") long applicationId,
            @PathVariable("interviewId") long interviewId) {
        return ResponseEntity.ok(ApiResponse.success(
                interviewService.getInterview(applicationId, interviewId)));
    }

    @PatchMapping(ApiPaths.ADMIN_APPLICATION_INTERVIEW)
    @PreAuthorize("hasAuthority('" + Permissions.EMPLOYMENT_MANAGE + "')")
    @Operation(summary = "Update an interview",
            description = "Partial update of scheduledAt/mode/location/notes and a "
                    + "conservative status transition (COMPLETED/NO_SHOW/CANCELLED "
                    + "are final → 409 on change attempts).",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<InterviewResponse>> updateInterview(
            @PathVariable("id") long applicationId,
            @PathVariable("interviewId") long interviewId,
            @Valid @RequestBody InterviewUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                interviewService.updateInterview(applicationId, interviewId, request),
                "Interview updated"));
    }

    @DeleteMapping(ApiPaths.ADMIN_APPLICATION_INTERVIEW)
    @PreAuthorize("hasAuthority('" + Permissions.EMPLOYMENT_MANAGE + "')")
    @Operation(summary = "Delete an interview",
            description = "Only CANCELLED interviews may be deleted (409 otherwise); "
                    + "the owning application is never affected.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Void> deleteInterview(
            @PathVariable("id") long applicationId,
            @PathVariable("interviewId") long interviewId) {
        interviewService.deleteInterview(applicationId, interviewId);
        return ResponseEntity.noContent().build();
    }
}
