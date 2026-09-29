package com.lokmit.foundation.employment.candidate.controller;

import com.lokmit.foundation.common.api.ApiResponse;
import com.lokmit.foundation.common.constants.ApiPaths;
import com.lokmit.foundation.employment.candidate.dto.CandidateResponse;
import com.lokmit.foundation.employment.candidate.dto.CandidateUpdateRequest;
import com.lokmit.foundation.employment.candidate.service.CandidateService;
import com.lokmit.foundation.employment.resume.service.ResumeOwnershipService;
import com.lokmit.foundation.security.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Candidate self-service profile API (A10) on the existing V8 'candidates'
 * table. Ownership is resolved server-side from the authenticated user's
 * database id via the established A7.6.3 {@link ResumeOwnershipService} —
 * the request contract has no candidateId field, so there is no IDOR
 * surface. A user without a candidate profile receives the same plain 404
 * as the resume API (admins/employers/clients cannot accidentally use
 * candidate self-service). Reuses the admin API's DTOs, validation
 * vocabulary and update logic — no duplicate profile implementation.
 */
@RestController
@RequestMapping(ApiPaths.CANDIDATE_ME_PROFILE)
@Tag(name = "Candidate Profile",
        description = "Candidate self-service profile (authenticated candidate ownership required)")
public class CandidateProfileController {

    private final CandidateService candidateService;
    private final ResumeOwnershipService ownershipService;
    private final SecurityUtils securityUtils;

    public CandidateProfileController(CandidateService candidateService,
                                      ResumeOwnershipService ownershipService,
                                      SecurityUtils securityUtils) {
        this.candidateService = candidateService;
        this.ownershipService = ownershipService;
        this.securityUtils = securityUtils;
    }

    /** Returns the authenticated candidate's own profile. */
    @GetMapping
    @Operation(summary = "Get the authenticated candidate's own profile",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<CandidateResponse>> getOwnProfile() {
        var candidate = ownershipService
                .resolveOwnCandidate(securityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(
                candidateService.getCandidate(candidate.getId())));
    }

    /** Partially updates the authenticated candidate's own profile. */
    @PatchMapping
    @Operation(summary = "Update the authenticated candidate's own profile",
            description = "Partial update of the same editable fields as the admin API "
                    + "(dateOfBirth, gender, phone, currentLocation, summary, expected "
                    + "salary range, availability). Ownership is server-resolved; there is "
                    + "no candidateId request field.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<CandidateResponse>> updateOwnProfile(
            @Valid @RequestBody CandidateUpdateRequest request) {
        var candidate = ownershipService
                .resolveOwnCandidate(securityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(
                candidateService.updateCandidate(candidate.getId(), request),
                "Profile updated"));
    }
}
