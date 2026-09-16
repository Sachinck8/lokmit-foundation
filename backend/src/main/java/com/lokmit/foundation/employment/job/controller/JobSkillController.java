package com.lokmit.foundation.employment.job.controller;

import com.lokmit.foundation.common.api.ApiResponse;
import com.lokmit.foundation.common.constants.ApiPaths;
import com.lokmit.foundation.employment.job.dto.JobSkillRequest;
import com.lokmit.foundation.employment.job.dto.JobSkillResponse;
import com.lokmit.foundation.employment.job.service.JobSkillService;
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
 * Admin management of the job↔skill relationship (V8 job_skills, composite
 * PK job_id + skill_id). All endpoints require the existing
 * {@code employment:manage} permission. POST takes the skill id in the JSON
 * body — consistent with the A7.1 candidate-skill convention.
 */
@RestController
@Tag(name = "Admin Job Skills",
        description = "Job skill requirements (employment:manage permission required)")
public class JobSkillController {

    private final JobSkillService jobSkillService;

    public JobSkillController(JobSkillService jobSkillService) {
        this.jobSkillService = jobSkillService;
    }

    @GetMapping(ApiPaths.ADMIN_JOB_SKILLS)
    @PreAuthorize("hasAuthority('" + Permissions.EMPLOYMENT_MANAGE + "')")
    @Operation(summary = "List a job's skill requirements",
            description = "Unknown job → 404.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<List<JobSkillResponse>>> listSkills(
            @PathVariable Long jobId) {
        return ResponseEntity.ok(ApiResponse.success(jobSkillService.listSkills(jobId)));
    }

    @PostMapping(ApiPaths.ADMIN_JOB_SKILLS)
    @PreAuthorize("hasAuthority('" + Permissions.EMPLOYMENT_MANAGE + "')")
    @Operation(summary = "Add a skill requirement to a job",
            description = "Unknown job or skill → 404; duplicate assignment → 409.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<JobSkillResponse>> assignSkill(
            @PathVariable Long jobId,
            @Valid @RequestBody JobSkillRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        jobSkillService.assignSkill(jobId, request.getSkillId()),
                        "Skill assigned"));
    }

    @DeleteMapping(ApiPaths.ADMIN_JOB_SKILL)
    @PreAuthorize("hasAuthority('" + Permissions.EMPLOYMENT_MANAGE + "')")
    @Operation(summary = "Remove a skill requirement from a job",
            description = "Unknown job/skill pair → 404.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Void> removeSkill(
            @PathVariable Long jobId,
            @PathVariable Long skillId) {
        jobSkillService.removeSkill(jobId, skillId);
        return ResponseEntity.noContent().build();
    }
}
