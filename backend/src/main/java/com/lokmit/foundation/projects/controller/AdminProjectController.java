package com.lokmit.foundation.projects.controller;

import com.lokmit.foundation.common.api.ApiResponse;
import com.lokmit.foundation.common.api.PageResponse;
import com.lokmit.foundation.common.constants.ApiPaths;
import com.lokmit.foundation.common.pagination.PageParams;
import com.lokmit.foundation.security.Permissions;
import com.lokmit.foundation.projects.dto.ProjectCreateRequest;
import com.lokmit.foundation.projects.dto.ProjectImageCreateRequest;
import com.lokmit.foundation.projects.dto.ProjectImageResponse;
import com.lokmit.foundation.projects.dto.ProjectImageUpdateRequest;
import com.lokmit.foundation.projects.dto.ProjectResponse;
import com.lokmit.foundation.projects.dto.ProjectUpdateRequest;
import com.lokmit.foundation.projects.service.ProjectImageService;
import com.lokmit.foundation.projects.service.ProjectService;
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
 * Administrative project and project-image endpoints (A6).
 *
 * <p>All endpoints require the {@code projects:manage} permission
 * (SUPER_ADMIN, ADMIN per the V13 grant). Image endpoints are METADATA
 * only — no binary upload exists in A6.</p>
 */
@RestController
@RequestMapping(ApiPaths.ADMIN_PROJECTS)
@Tag(name = "Admin Projects — Projects & Images",
        description = "Project management and image metadata (projects:manage permission required)")
public class AdminProjectController {

    private final ProjectService projectService;
    private final ProjectImageService projectImageService;

    public AdminProjectController(ProjectService projectService,
                                  ProjectImageService projectImageService) {
        this.projectService = projectService;
        this.projectImageService = projectImageService;
    }

    // ------------------------------------------------------------------
    // Projects
    // ------------------------------------------------------------------

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permissions.PROJECTS_MANAGE + "')")
    @Operation(summary = "List projects",
            description = "Requires authentication and the projects:manage permission. "
                    + "Supports pagination, categoryId/status/projectStatus filters and "
                    + "title/summary search.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<PageResponse<ProjectResponse>>> listProjects(
            @Valid @ModelAttribute PageParams pageParams,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String projectStatus,
            @RequestParam(required = false) String search) {
        Pageable pageable = PageRequest.of(pageParams.getPage(), pageParams.getSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ProjectResponse> page = projectService.listProjects(
                categoryId, status, projectStatus, search, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(page)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.PROJECTS_MANAGE + "')")
    @Operation(summary = "Get a project by id",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<ProjectResponse>> getProject(@PathVariable long id) {
        return ResponseEntity.ok(ApiResponse.success(projectService.getProject(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + Permissions.PROJECTS_MANAGE + "')")
    @Operation(summary = "Create a project (starts as DRAFT)",
            description = "Duplicate slug is rejected with 409; an unknown categoryId with 404; "
                    + "end-before-start dates with 400 (chk_projects_dates).",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<ProjectResponse>> createProject(
            @Valid @RequestBody ProjectCreateRequest request) {
        ProjectResponse response = projectService.createProject(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Project created"));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.PROJECTS_MANAGE + "')")
    @Operation(summary = "Update a project",
            description = "Slug is immutable; omitted fields stay unchanged; categoryId set "
                    + "explicitly to null detaches the project. Status changes use the "
                    + "dedicated lifecycle endpoints. The resulting date pair is validated "
                    + "against chk_projects_dates.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<ProjectResponse>> updateProject(
            @PathVariable long id,
            @Valid @RequestBody ProjectUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                projectService.updateProject(id, request), "Project updated"));
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('" + Permissions.PROJECTS_MANAGE + "')")
    @Operation(summary = "Publish a project (stamps published_at)",
            description = "Allowed from DRAFT; already-published and archived projects are "
                    + "rejected with 409.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<ProjectResponse>> publishProject(@PathVariable long id) {
        return ResponseEntity.ok(ApiResponse.success(
                projectService.publishProject(id), "Project published"));
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAuthority('" + Permissions.PROJECTS_MANAGE + "')")
    @Operation(summary = "Archive a project",
            description = "Allowed from DRAFT or PUBLISHED; archiving is terminal.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<ProjectResponse>> archiveProject(@PathVariable long id) {
        return ResponseEntity.ok(ApiResponse.success(
                projectService.archiveProject(id), "Project archived"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.PROJECTS_MANAGE + "')")
    @Operation(summary = "Delete a project",
            description = "Owned image metadata rows are removed by the existing FK cascade "
                    + "(the schema's designed ownership cleanup). No other table references "
                    + "projects.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Void> deleteProject(@PathVariable long id) {
        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------------
    // Project images (metadata only — no file upload in A6)
    // ------------------------------------------------------------------

    @GetMapping("/{id}/images")
    @PreAuthorize("hasAuthority('" + Permissions.PROJECTS_MANAGE + "')")
    @Operation(summary = "List a project's image metadata",
            description = "Paginated gallery for one project, lowest display order first. "
                    + "Unknown project → 404.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<PageResponse<ProjectImageResponse>>> listImages(
            @PathVariable long id,
            @Valid @ModelAttribute PageParams pageParams) {
        Pageable pageable = PageRequest.of(pageParams.getPage(), pageParams.getSize(),
                Sort.by(Sort.Direction.ASC, "displayOrder").and(Sort.by("id")));
        Page<ProjectImageResponse> page = projectImageService.listImages(id, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(page)));
    }

    @PostMapping("/{id}/images")
    @PreAuthorize("hasAuthority('" + Permissions.PROJECTS_MANAGE + "')")
    @Operation(summary = "Add image metadata to a project",
            description = "Accepts a caller-supplied URL reference only — no file upload exists "
                    + "in A6. Unknown project → 404; duplicate URL within the project's "
                    + "gallery → 409.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<ProjectImageResponse>> createImage(
            @PathVariable long id,
            @Valid @RequestBody ProjectImageCreateRequest request) {
        ProjectImageResponse response = projectImageService.createImage(id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Project image added"));
    }

    @GetMapping("/{projectId}/images/{imageId}")
    @PreAuthorize("hasAuthority('" + Permissions.PROJECTS_MANAGE + "')")
    @Operation(summary = "Get one image-metadata row",
            description = "The image must belong to the path project; a mismatched pair → 404.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<ProjectImageResponse>> getImage(
            @PathVariable long projectId, @PathVariable long imageId) {
        return ResponseEntity.ok(ApiResponse.success(
                projectImageService.getImageForProject(projectId, imageId)));
    }

    @PatchMapping("/{projectId}/images/{imageId}")
    @PreAuthorize("hasAuthority('" + Permissions.PROJECTS_MANAGE + "')")
    @Operation(summary = "Update image metadata",
            description = "The owning project is immutable; a mismatched project/image pair → 404.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<ProjectImageResponse>> updateImage(
            @PathVariable long projectId, @PathVariable long imageId,
            @Valid @RequestBody ProjectImageUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                projectImageService.updateImageForProject(projectId, imageId, request),
                "Project image updated"));
    }

    @DeleteMapping("/{projectId}/images/{imageId}")
    @PreAuthorize("hasAuthority('" + Permissions.PROJECTS_MANAGE + "')")
    @Operation(summary = "Delete an image-metadata row",
            description = "Removes the metadata row only; the owning project is never touched. "
                    + "A mismatched project/image pair → 404.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Void> deleteImage(
            @PathVariable long projectId, @PathVariable long imageId) {
        projectImageService.deleteImageForProject(projectId, imageId);
        return ResponseEntity.noContent().build();
    }
}
