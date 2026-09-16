package com.lokmit.foundation.employment.candidateskill.controller;

import com.lokmit.foundation.common.api.ApiResponse;
import com.lokmit.foundation.common.constants.ApiPaths;
import com.lokmit.foundation.employment.candidateskill.dto.CandidateSkillRequest;
import com.lokmit.foundation.employment.candidateskill.dto.CandidateSkillResponse;
import com.lokmit.foundation.employment.candidateskill.service.CandidateSkillService;
import com.lokmit.foundation.security.Permissions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin management of the candidate↔skill relationship (V8 candidate_skills,
 * composite PK candidate_id + skill_id). All endpoints require the
 * {@code candidates:manage} permission.
 */
@RestController
@Tag(name = "Admin Candidate Skills",
        description = "Candidate↔skill assignments (candidates:manage permission required)")
public class CandidateSkillController {

    private final CandidateSkillService candidateSkillService;

    public CandidateSkillController(CandidateSkillService candidateSkillService) {
        this.candidateSkillService = candidateSkillService;
    }

    @GetMapping(ApiPaths.ADMIN_CANDIDATE_SKILLS)
    @PreAuthorize("hasAuthority('" + Permissions.CANDIDATES_MANAGE + "')")
    @Operation(summary = "List skills assigned to a candidate",
            description = "Unknown candidate → 404.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<List<CandidateSkillResponse>>> listSkills(
            @PathVariable Long candidateId) {
        return ResponseEntity.ok(ApiResponse.success(
                candidateSkillService.list(candidateId)));
    }

    @PostMapping(ApiPaths.ADMIN_CANDIDATE_SKILLS)
    @PreAuthorize("hasAuthority('" + Permissions.CANDIDATES_MANAGE + "')")
    @Operation(summary = "Assign a skill to a candidate",
            description = "Unknown candidate or skill → 404; duplicate assignment → 409. "
                    + "proficiency is optional (BEGINNER/INTERMEDIATE/ADVANCED/EXPERT).",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<CandidateSkillResponse>> assignSkill(
            @PathVariable Long candidateId,
            @Valid @RequestBody CandidateSkillRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        candidateSkillService.assign(candidateId, request),
                        "Skill assigned"));
    }

    @DeleteMapping(ApiPaths.ADMIN_CANDIDATE_SKILL)
    @PreAuthorize("hasAuthority('" + Permissions.CANDIDATES_MANAGE + "')")
    @Operation(summary = "Remove a skill assignment from a candidate",
            description = "Unknown candidate/skill pair → 404.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Void> removeSkill(
            @PathVariable Long candidateId,
            @PathVariable Long skillId) {
        candidateSkillService.remove(candidateId, skillId);
        return ResponseEntity.noContent().build();
    }
}
