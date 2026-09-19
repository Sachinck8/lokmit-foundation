package com.lokmit.foundation.employment.candidate.controller;

import com.lokmit.foundation.common.api.ApiResponse;
import com.lokmit.foundation.common.constants.ApiPaths;
import com.lokmit.foundation.employment.candidateskill.dto.CandidateSkillResponse;
import com.lokmit.foundation.employment.candidateskill.service.CandidateSkillService;
import com.lokmit.foundation.employment.education.dto.CandidateEducationResponse;
import com.lokmit.foundation.employment.education.service.CandidateEducationService;
import com.lokmit.foundation.employment.experience.dto.CandidateExperienceResponse;
import com.lokmit.foundation.employment.experience.service.CandidateExperienceService;
import com.lokmit.foundation.security.Permissions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * A18 applications-review console — read-only candidate profile sections for
 * an administrator reviewing an application (employment/candidate detail
 * panels).
 *
 * <p>All endpoints are GET-only and require the existing
 * {@code candidates:manage} permission; the candidate id is a path variable
 * resolved server-side and no mutation surface is introduced here. Skills
 * reuse the existing {@link CandidateSkillService#list(Long)} (A7.2 admin
 * surface); educations/experiences reuse the A13 self-service services and
 * DTOs read-only, with the same unknown-candidate → 404 guard the skills
 * service already applies. No new tables, no schema change, no new
 * permission.</p>
 */
@RestController
@Tag(name = "Admin Candidate Profile",
        description = "Read-only candidate profile sections for review "
                + "(candidates:manage permission required)")
public class AdminCandidateProfileController {

    private final CandidateSkillService candidateSkillService;
    private final CandidateEducationService candidateEducationService;
    private final CandidateExperienceService candidateExperienceService;

    public AdminCandidateProfileController(CandidateSkillService candidateSkillService,
                                           CandidateEducationService candidateEducationService,
                                           CandidateExperienceService candidateExperienceService) {
        this.candidateSkillService = candidateSkillService;
        this.candidateEducationService = candidateEducationService;
        this.candidateExperienceService = candidateExperienceService;
    }

    @GetMapping(ApiPaths.ADMIN_CANDIDATE_SKILLS)
    @PreAuthorize("hasAuthority('" + Permissions.CANDIDATES_MANAGE + "')")
    @Operation(summary = "List a candidate's skills",
            description = "Read-only review surface. Unknown candidate → 404.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<List<CandidateSkillResponse>>> listSkills(
            @PathVariable Long candidateId) {
        return ResponseEntity.ok(ApiResponse.success(
                candidateSkillService.list(candidateId)));
    }

    @GetMapping(ApiPaths.ADMIN_CANDIDATE_EDUCATIONS)
    @PreAuthorize("hasAuthority('" + Permissions.CANDIDATES_MANAGE + "')")
    @Operation(summary = "List a candidate's education records",
            description = "Read-only review surface (A13 data, admin read). "
                    + "Unknown candidate → 404.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<List<CandidateEducationResponse>>> listEducations(
            @PathVariable Long candidateId) {
        return ResponseEntity.ok(ApiResponse.success(
                candidateEducationService.list(candidateId)));
    }

    @GetMapping(ApiPaths.ADMIN_CANDIDATE_EXPERIENCES)
    @PreAuthorize("hasAuthority('" + Permissions.CANDIDATES_MANAGE + "')")
    @Operation(summary = "List a candidate's experience records",
            description = "Read-only review surface (A13 data, admin read). "
                    + "Unknown candidate → 404.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<List<CandidateExperienceResponse>>> listExperiences(
            @PathVariable Long candidateId) {
        return ResponseEntity.ok(ApiResponse.success(
                candidateExperienceService.list(candidateId)));
    }
}
