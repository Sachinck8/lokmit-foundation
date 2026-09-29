package com.lokmit.foundation.employment.resume.controller;

import com.lokmit.foundation.common.api.ApiResponse;
import com.lokmit.foundation.common.constants.ApiPaths;
import com.lokmit.foundation.employment.resume.dto.ResumeResponse;
import com.lokmit.foundation.employment.resume.repository.ResumeRepository;
import com.lokmit.foundation.employment.resume.service.ResumeOwnershipService;
import com.lokmit.foundation.security.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Candidate self-service "my resumes" listing API (A10). Completes the
 * A7.6 resume surface: the upload API (A7.6.3) replaces the active resume,
 * but until now a candidate could not see which resumes they have without
 * an admin. Ownership is resolved server-side from the authenticated
 * user's database id via the established A7.6.3
 * {@link ResumeOwnershipService}; the path has no candidateId field, so
 * there is no IDOR surface. Responses reuse the existing safe
 * {@link ResumeResponse} DTO — metadata only, never storage keys, file
 * URLs, checksums' verification semantics or blob internals.
 */
@RestController
@RequestMapping(ApiPaths.CANDIDATE_ME_RESUMES)
@Tag(name = "Candidate Resumes",
        description = "Candidate self-service resume management (authenticated candidate ownership required)")
public class CandidateResumeController {

    private final ResumeRepository resumeRepository;
    private final ResumeOwnershipService ownershipService;
    private final SecurityUtils securityUtils;

    public CandidateResumeController(ResumeRepository resumeRepository,
                                     ResumeOwnershipService ownershipService,
                                     SecurityUtils securityUtils) {
        this.resumeRepository = resumeRepository;
        this.ownershipService = ownershipService;
        this.securityUtils = securityUtils;
    }

    /**
     * Lists the authenticated candidate's own resumes, newest first
     * (metadata only; the active resume is flagged by {@code active}).
     */
    @GetMapping
    @Operation(summary = "List the authenticated candidate's own resumes",
            description = "Metadata only (id, fileName, fileType, fileSizeBytes, active, "
                    + "createdAt, checksum). Storage internals are never exposed. Newest "
                    + "resume first; the current active resume carries active=true.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<List<ResumeResponse>>> listOwn() {
        var candidate = ownershipService
                .resolveOwnCandidate(securityUtils.getCurrentUserId());
        List<ResumeResponse> resumes = resumeRepository
                .findByCandidateIdOrderByCreatedAtDesc(candidate.getId())
                .stream()
                .map(ResumeResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(resumes));
    }
}
