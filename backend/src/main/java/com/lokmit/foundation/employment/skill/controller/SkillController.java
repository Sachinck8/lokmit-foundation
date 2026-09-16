package com.lokmit.foundation.employment.skill.controller;

import com.lokmit.foundation.common.api.ApiResponse;
import com.lokmit.foundation.common.api.PageResponse;
import com.lokmit.foundation.common.constants.ApiPaths;
import com.lokmit.foundation.common.pagination.PageParams;
import com.lokmit.foundation.employment.skill.dto.SkillCreateRequest;
import com.lokmit.foundation.employment.skill.dto.SkillResponse;
import com.lokmit.foundation.employment.skill.dto.SkillUpdateRequest;
import com.lokmit.foundation.employment.skill.service.SkillService;
import com.lokmit.foundation.security.Permissions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Admin skill endpoints (A7.1). All endpoints require the
 * {@code employment:manage} permission.
 *
 * <p>DELETE cascades into candidate_skills and job_skills per the V8
 * schema — documented and intentional (see docs/DATABASE.md).</p>
 */
@RestController
@RequestMapping(ApiPaths.ADMIN_SKILLS)
@Tag(name = "Admin Skills",
        description = "Skill management (employment:manage permission required)")
public class SkillController {

    private final SkillService skillService;

    public SkillController(SkillService skillService) {
        this.skillService = skillService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permissions.EMPLOYMENT_MANAGE + "')")
    @Operation(summary = "List skills",
            description = "Supports pagination, name search and status filter.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<PageResponse<SkillResponse>>> listSkills(
            @Valid @ModelAttribute PageParams pageParams,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(required = false) String status) {
        Pageable pageable = PageRequest.of(pageParams.getPage(), pageParams.getSize(),
                Sort.by(Sort.Direction.ASC, "name"));
        return ResponseEntity.ok(ApiResponse.success(
                PageResponse.of(skillService.listSkills(search, status, pageable))));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.EMPLOYMENT_MANAGE + "')")
    @Operation(summary = "Get a skill by id",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<SkillResponse>> getSkill(@PathVariable long id) {
        return ResponseEntity.ok(ApiResponse.success(skillService.getSkill(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + Permissions.EMPLOYMENT_MANAGE + "')")
    @Operation(summary = "Create a skill",
            description = "Duplicate names are rejected with 409.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<SkillResponse>> createSkill(
            @Valid @RequestBody SkillCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(skillService.createSkill(request), "Skill created"));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.EMPLOYMENT_MANAGE + "')")
    @Operation(summary = "Update a skill",
            description = "Partial update; duplicate names are rejected with 409.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<SkillResponse>> updateSkill(
            @PathVariable long id,
            @Valid @RequestBody SkillUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                skillService.updateSkill(id, request), "Skill updated"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.EMPLOYMENT_MANAGE + "')")
    @Operation(summary = "Delete a skill",
            description = "Also removes its candidate_skills and job_skills rows via the "
                    + "V8 ON DELETE CASCADE — an intentional, documented cleanup semantic.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Void> deleteSkill(@PathVariable long id) {
        skillService.deleteSkill(id);
        return ResponseEntity.noContent().build();
    }
}
