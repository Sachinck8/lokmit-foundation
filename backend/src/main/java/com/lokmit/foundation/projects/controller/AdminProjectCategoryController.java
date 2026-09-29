package com.lokmit.foundation.projects.controller;

import com.lokmit.foundation.common.api.ApiResponse;
import com.lokmit.foundation.common.api.PageResponse;
import com.lokmit.foundation.common.constants.ApiPaths;
import com.lokmit.foundation.common.pagination.PageParams;
import com.lokmit.foundation.security.Permissions;
import com.lokmit.foundation.projects.dto.ProjectCategoryCreateRequest;
import com.lokmit.foundation.projects.dto.ProjectCategoryResponse;
import com.lokmit.foundation.projects.dto.ProjectCategoryUpdateRequest;
import com.lokmit.foundation.projects.service.ProjectCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Administrative project-category endpoints (A6).
 *
 * <p>All endpoints require the {@code projects:manage} permission
 * (SUPER_ADMIN, ADMIN per the V13 grant).</p>
 */
@RestController
@RequestMapping(ApiPaths.ADMIN_PROJECT_CATEGORIES)
@Tag(name = "Admin Projects — Categories",
        description = "Project category management (projects:manage permission required)")
public class AdminProjectCategoryController {

    private final ProjectCategoryService projectCategoryService;

    public AdminProjectCategoryController(ProjectCategoryService projectCategoryService) {
        this.projectCategoryService = projectCategoryService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permissions.PROJECTS_MANAGE + "')")
    @Operation(summary = "List project categories",
            description = "Requires authentication and the projects:manage permission. "
                    + "Supports pagination and a status filter (ACTIVE/INACTIVE).",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<PageResponse<ProjectCategoryResponse>>> listCategories(
            @Valid @ModelAttribute PageParams pageParams,
            @RequestParam(required = false) String status) {
        Pageable pageable = PageRequest.of(pageParams.getPage(), pageParams.getSize(),
                Sort.by(Sort.Direction.ASC, "displayOrder").and(Sort.by("name")));
        Page<ProjectCategoryResponse> page =
                projectCategoryService.listCategories(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(page)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.PROJECTS_MANAGE + "')")
    @Operation(summary = "Get a project category by id",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<ProjectCategoryResponse>> getCategory(
            @PathVariable long id) {
        return ResponseEntity.ok(ApiResponse.success(projectCategoryService.getCategory(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + Permissions.PROJECTS_MANAGE + "')")
    @Operation(summary = "Create a project category (starts as ACTIVE)",
            description = "Duplicate name or slug is rejected with 409.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<ProjectCategoryResponse>> createCategory(
            @Valid @RequestBody ProjectCategoryCreateRequest request) {
        ProjectCategoryResponse response = projectCategoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Project category created"));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.PROJECTS_MANAGE + "')")
    @Operation(summary = "Update a project category",
            description = "Slug is immutable; omitted fields stay unchanged.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<ProjectCategoryResponse>> updateCategory(
            @PathVariable long id,
            @Valid @RequestBody ProjectCategoryUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                projectCategoryService.updateCategory(id, request), "Project category updated"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.PROJECTS_MANAGE + "')")
    @Operation(summary = "Delete a project category",
            description = "Referencing projects are detached (existing FK ON DELETE SET NULL) — "
                    + "no cascade deletion of projects.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Void> deleteCategory(@PathVariable long id) {
        projectCategoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}
