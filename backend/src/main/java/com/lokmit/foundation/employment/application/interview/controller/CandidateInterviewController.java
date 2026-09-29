package com.lokmit.foundation.employment.application.interview.controller;

import com.lokmit.foundation.common.api.ApiResponse;
import com.lokmit.foundation.common.api.PageResponse;
import com.lokmit.foundation.common.constants.ApiPaths;
import com.lokmit.foundation.common.pagination.PageParams;
import com.lokmit.foundation.employment.application.interview.dto.CandidateInterviewResponse;
import com.lokmit.foundation.employment.application.interview.service.CandidateInterviewService;
import com.lokmit.foundation.employment.candidate.entity.Candidate;
import com.lokmit.foundation.employment.resume.service.ResumeOwnershipService;
import com.lokmit.foundation.security.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Candidate interview visibility (A14) — read-only candidate surface over
 * the interviews on the candidate's OWN applications. Ownership is
 * resolved server-side from the JWT user id via the A7.6.3 ownership
 * service; there is no candidateId request field anywhere. Foreign/unknown
 * interview ids return the identical masked 404. Read-only by design: no
 * candidate creation, editing, cancellation or rescheduling (the admin
 * {@code InterviewController} retains the full A7.4 CRUD unchanged). All
 * endpoints require the standard JWT bearer chain — no SecurityConfig
 * change.
 */
@RestController
@RequestMapping(ApiPaths.CANDIDATE_ME_INTERVIEWS)
@Tag(name = "Candidate Interviews",
        description = "Candidate visibility of interviews on their own applications "
                + "(authenticated candidate ownership required)")
public class CandidateInterviewController {

    private final CandidateInterviewService interviewService;
    private final ResumeOwnershipService ownershipService;
    private final SecurityUtils securityUtils;

    public CandidateInterviewController(CandidateInterviewService interviewService,
                                        ResumeOwnershipService ownershipService,
                                        SecurityUtils securityUtils) {
        this.interviewService = interviewService;
        this.ownershipService = ownershipService;
        this.securityUtils = securityUtils;
    }

    /**
     * Lists the interviews for one of the candidate's own applications
     * (soonest/newest scheduledAt first, DB-side pagination).
     */
    @GetMapping
    @Operation(summary = "List interviews for one of the candidate's own applications",
            description = "applicationId must reference one of the caller's own "
                    + "applications; foreign/unknown ids return the same plain 404 "
                    + "(no existence leak). Requires the applicationId query parameter.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<PageResponse<CandidateInterviewResponse>>> listOwnForApplication(
            @RequestParam(name = "applicationId") long applicationId,
            @Valid @ModelAttribute PageParams pageParams) {
        Candidate candidate = ownershipService
                .resolveOwnCandidate(securityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(
                interviewService.listOwnForApplication(
                        candidate.getId(), applicationId, pageParams)));
    }

    /** Fetches one interview that belongs to one of the candidate's own applications. */
    @GetMapping("/{interviewId}")
    @Operation(summary = "Get one interview on the candidate's own application",
            description = "Ownership flows through the interview's application to "
                    + "the server-resolved candidate; a foreign or unknown id is the "
                    + "same plain 404 (no existence leak).",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<CandidateInterviewResponse>> getOwn(
            @PathVariable long interviewId) {
        Candidate candidate = ownershipService
                .resolveOwnCandidate(securityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(
                interviewService.getOwn(candidate.getId(), interviewId)));
    }
}
