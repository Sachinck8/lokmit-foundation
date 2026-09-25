package com.lokmit.foundation.employment.experience.controller;

import com.lokmit.foundation.common.api.ApiResponse;
import com.lokmit.foundation.common.constants.ApiPaths;
import com.lokmit.foundation.employment.candidate.entity.Candidate;
import com.lokmit.foundation.employment.experience.dto.CandidateExperienceCreateRequest;
import com.lokmit.foundation.employment.experience.dto.CandidateExperienceResponse;
import com.lokmit.foundation.employment.experience.dto.CandidateExperienceUpdateRequest;
import com.lokmit.foundation.employment.experience.service.CandidateExperienceService;
import com.lokmit.foundation.employment.resume.service.ResumeOwnershipService;
import com.lokmit.foundation.security.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Candidate self-service experience CRUD (A13) over the existing V8
 * 'candidate_experiences' table — no migration, no new permission. All
 * endpoints require the standard JWT bearer chain
 * ({@code anyRequest().authenticated()}). Ownership is resolved
 * server-side from the authenticated user's database id via the existing
 * A7.6.3 {@link ResumeOwnershipService}; the request contract carries no
 * candidateId, so a candidate can never touch another candidate's records.
 * Foreign/unknown ids return the identical masked 404. A user without a
 * candidate profile receives the same plain 404 as the other self-service
 * APIs. The request DTOs carry the field validation.
 */
@RestController
@RequestMapping(ApiPaths.CANDIDATE_ME_EXPERIENCES)
@Tag(name = "Candidate Experiences",
        description = "Candidate self-service experience records (authenticated candidate ownership required)")
public class CandidateExperienceController {

    private final CandidateExperienceService experienceService;
    private final ResumeOwnershipService ownershipService;
    private final SecurityUtils securityUtils;

    public CandidateExperienceController(CandidateExperienceService experienceService,
                                         ResumeOwnershipService ownershipService,
                                         SecurityUtils securityUtils) {
        this.experienceService = experienceService;
        this.ownershipService = ownershipService;
        this.securityUtils = securityUtils;
    }

    @GetMapping
    @Operation(summary = "List the authenticated candidate's experience records",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<List<CandidateExperienceResponse>>> listOwn() {
        Candidate candidate = ownershipService
                .resolveOwnCandidate(securityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(
                experienceService.list(candidate.getId())));
    }

    @PostMapping
    @Operation(summary = "Add an experience record to the authenticated candidate",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<CandidateExperienceResponse>> createOwn(
            @Valid @RequestBody CandidateExperienceCreateRequest request) {
        Candidate candidate = ownershipService
                .resolveOwnCandidate(securityUtils.getCurrentUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        experienceService.create(candidate.getId(), request, candidate),
                        "Experience record added"));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update one of the authenticated candidate's experience records",
            description = "Foreign/unknown ids return the same plain 404 (no existence leak).",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<CandidateExperienceResponse>> updateOwn(
            @PathVariable Long id,
            @Valid @RequestBody CandidateExperienceUpdateRequest request) {
        Candidate candidate = ownershipService
                .resolveOwnCandidate(securityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(
                experienceService.update(candidate.getId(), id, request),
                "Experience record updated"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete one of the authenticated candidate's experience records",
            description = "Foreign/unknown id return the same plain 404 (no existence leak).",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Void> deleteOwn(@PathVariable Long id) {
        Candidate candidate = ownershipService
                .resolveOwnCandidate(securityUtils.getCurrentUserId());
        experienceService.delete(candidate.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
