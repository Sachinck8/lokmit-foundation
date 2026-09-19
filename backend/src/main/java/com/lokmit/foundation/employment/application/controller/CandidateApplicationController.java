package com.lokmit.foundation.employment.application.controller;

import com.lokmit.foundation.common.api.ApiResponse;
import com.lokmit.foundation.common.api.PageResponse;
import com.lokmit.foundation.common.constants.ApiPaths;
import com.lokmit.foundation.common.pagination.PageParams;
import com.lokmit.foundation.employment.application.dto.CandidateApplicationCreateRequest;
import com.lokmit.foundation.employment.application.dto.CandidateApplicationResponse;
import com.lokmit.foundation.employment.application.service.CandidateApplicationService;
import com.lokmit.foundation.employment.candidate.entity.Candidate;
import com.lokmit.foundation.employment.resume.service.ResumeOwnershipService;
import com.lokmit.foundation.security.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Candidate self-service application API (A9).
 *
 * <p>Thin HTTP surface over {@link CandidateApplicationService}. All
 * endpoints require the standard JWT bearer chain (covered by
 * {@code anyRequest().authenticated()} — no SecurityConfig change, nothing
 * is made public). The candidate profile is resolved server-side from the
 * authenticated user's id via the existing A7.6.3
 * {@link ResumeOwnershipService} — there is no candidateId request field,
 * so a candidate can never act on behalf of another. Authorization model:
 * authenticated candidate OWNERSHIP (the established platform pattern);
 * no new permission seed.</p>
 *
 * <p>A user without a candidate profile (admins, employers, clients) gets
 * the plain 404 from {@link ResumeOwnershipService#resolveOwnCandidate},
 * consistent with the resume API. Errors follow the existing error
 * envelope: 400 (missing/invalid fields), 404 (unknown job, foreign/
 * inactive resume, foreign application, no candidate profile), 409
 * (duplicate application).</p>
 */
@RestController
@RequestMapping(ApiPaths.CANDIDATE_ME_APPLICATIONS)
@Tag(name = "Candidate Applications",
        description = "Candidate self-service job applications (authenticated candidate ownership required)")
public class CandidateApplicationController {

    private final CandidateApplicationService applicationService;
    private final ResumeOwnershipService ownershipService;
    private final SecurityUtils securityUtils;

    public CandidateApplicationController(CandidateApplicationService applicationService,
                                          ResumeOwnershipService ownershipService,
                                          SecurityUtils securityUtils) {
        this.applicationService = applicationService;
        this.ownershipService = ownershipService;
        this.securityUtils = securityUtils;
    }

    /**
     * Submits an application for a published job as the authenticated
     * candidate.
     */
    @PostMapping
    @Operation(summary = "Submit an application for a published job",
            description = "Applies as the authenticated candidate. The job must be "
                    + "PUBLISHED (the A8 public-visibility rule); an optional resumeId "
                    + "must reference one of the caller's OWN ACTIVE resumes; duplicate "
                    + "applications for the same job are rejected 409. Returns 201 with "
                    + "the safe candidate-facing application DTO.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<CandidateApplicationResponse>> submit(
            @Valid @RequestBody CandidateApplicationCreateRequest request) {

        Candidate candidate = ownershipService
                .resolveOwnCandidate(securityUtils.getCurrentUserId());
        CandidateApplicationResponse response = applicationService.submit(
                candidate,
                request.getJobId(),
                request.getResumeId(),
                request.getCoverNote());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Application submitted"));
    }

    /** Lists the authenticated candidate's own applications, newest first. */
    @GetMapping
    @Operation(summary = "List the authenticated candidate's own applications",
            description = "Ownership-scoped pagination (page/size); a candidateId query "
                    + "parameter does not exist and cannot override ownership. Newest "
                    + "applications first.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<PageResponse<CandidateApplicationResponse>>> listOwn(
            @Valid PageParams pageParams) {
        Candidate candidate = ownershipService
                .resolveOwnCandidate(securityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(
                applicationService.listOwn(candidate.getId(), pageParams)));
    }

    /** Fetches one of the authenticated candidate's own applications. */
    @GetMapping("/{applicationId}")
    @Operation(summary = "Get one of the authenticated candidate's own applications",
            description = "Ownership is enforced server-side; a foreign or unknown "
                    + "application id returns the same plain 404 (no existence leak).",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<CandidateApplicationResponse>> getOwn(
            @PathVariable long applicationId) {
        Candidate candidate = ownershipService
                .resolveOwnCandidate(securityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(
                applicationService.getOwn(candidate.getId(), applicationId)));
    }

    /**
     * Withdraws one of the authenticated candidate's own applications
     * (A11). Ownership is enforced first (masked 404 for foreign/unknown
     * ids); the transition reuses the existing A7.3 workflow unchanged.
     */
    @PostMapping("/{applicationId}/withdraw")
    @Operation(summary = "Withdraw one of the authenticated candidate's own applications",
            description = "Ownership is enforced server-side; foreign/unknown ids "
                    + "return the same plain 404 (no existence leak). Reuses the "
                    + "existing lifecycle: WITHDRAWN status, A7.4 history row, audit "
                    + "record and outbox event. Decided (HIRED/REJECTED) or already- "
                    + "withdrawn applications are rejected 409 by the existing rules.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<CandidateApplicationResponse>> withdrawOwn(
            @PathVariable long applicationId) {
        Candidate candidate = ownershipService
                .resolveOwnCandidate(securityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(
                applicationService.withdrawOwn(candidate.getId(), applicationId),
                "Application withdrawn"));
    }
}
