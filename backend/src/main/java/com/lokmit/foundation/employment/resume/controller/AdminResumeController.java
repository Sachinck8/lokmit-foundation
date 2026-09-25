package com.lokmit.foundation.employment.resume.controller;

import com.lokmit.foundation.common.api.ApiResponse;
import com.lokmit.foundation.common.api.PageResponse;
import com.lokmit.foundation.common.constants.ApiPaths;
import com.lokmit.foundation.common.pagination.PageParams;
import com.lokmit.foundation.employment.resume.dto.ResumeResponse;
import com.lokmit.foundation.employment.resume.service.AdminResumeService;
import com.lokmit.foundation.security.Permissions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

/**
 * Admin resume listing API (A7.6.6, item 1).
 *
 * <p>Lists one candidate's full resume history (active AND inactive rows,
 * newest first) with the same {@code candidates:manage} permission that
 * already governs candidate profiles and admin resume download/delete.
 * Metadata only: the safe {@link ResumeResponse} DTO — storage keys, blob
 * bytes and the legacy file_url marker are never exposed.</p>
 */
@RestController
@Tag(name = "Admin Resume Listing",
        description = "Candidate resume history listing (candidates:manage permission required)")
public class AdminResumeController {

    private final AdminResumeService adminResumeService;

    public AdminResumeController(AdminResumeService adminResumeService) {
        this.adminResumeService = adminResumeService;
    }

    @GetMapping(ApiPaths.ADMIN_CANDIDATE_RESUMES)
    @PreAuthorize("hasAuthority('" + Permissions.CANDIDATES_MANAGE + "')")
    @Operation(summary = "List a candidate's resumes (active and inactive)",
            description = "Requires candidates:manage. Paginated metadata only — "
                    + "no storage internals. Unknown candidate → 404.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<PageResponse<ResumeResponse>>> listResumes(
            @PathVariable long candidateId,
            @Valid @ModelAttribute PageParams pageParams) {
        return ResponseEntity.ok(ApiResponse.success(
                adminResumeService.listResumes(candidateId, pageParams)));
    }
}
