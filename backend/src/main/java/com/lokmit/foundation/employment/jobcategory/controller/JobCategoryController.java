package com.lokmit.foundation.employment.jobcategory.controller;

import com.lokmit.foundation.common.api.ApiResponse;
import com.lokmit.foundation.common.api.PageResponse;
import com.lokmit.foundation.common.constants.ApiPaths;
import com.lokmit.foundation.common.pagination.PageParams;
import com.lokmit.foundation.employment.jobcategory.dto.JobCategoryCreateRequest;
import com.lokmit.foundation.employment.jobcategory.dto.JobCategoryResponse;
import com.lokmit.foundation.employment.jobcategory.dto.JobCategoryUpdateRequest;
import com.lokmit.foundation.employment.jobcategory.service.JobCategoryService;
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
 * Admin job category endpoints (A7.1). All endpoints require the
 * {@code employment:manage} permission.
 *
 * <p>DELETE is safe: V8 jobs.category_id uses ON DELETE SET NULL, so jobs
 * are detached, never deleted.</p>
 */
@RestController
@RequestMapping(ApiPaths.ADMIN_JOB_CATEGORIES)
@Tag(name = "Admin Job Categories",
        description = "Job category management (employment:manage permission required)")
public class JobCategoryController {

    private final JobCategoryService jobCategoryService;

    public JobCategoryController(JobCategoryService jobCategoryService) {
        this.jobCategoryService = jobCategoryService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permissions.EMPLOYMENT_MANAGE + "')")
    @Operation(summary = "List job categories",
            description = "Supports pagination, name/slug search and status filter. "
                    + "Ordered by displayOrder then name.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<PageResponse<JobCategoryResponse>>> listJobCategories(
            @Valid @ModelAttribute PageParams pageParams,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(required = false) String status) {
        Pageable pageable = PageRequest.of(pageParams.getPage(), pageParams.getSize(),
                Sort.by(Sort.Direction.ASC, "displayOrder")
                        .and(Sort.by(Sort.Direction.ASC, "name")));
        return ResponseEntity.ok(ApiResponse.success(
                PageResponse.of(jobCategoryService.listJobCategories(search, status, pageable))));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.EMPLOYMENT_MANAGE + "')")
    @Operation(summary = "Get a job category by id",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<JobCategoryResponse>> getJobCategory(@PathVariable long id) {
        return ResponseEntity.ok(ApiResponse.success(jobCategoryService.getJobCategory(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + Permissions.EMPLOYMENT_MANAGE + "')")
    @Operation(summary = "Create a job category",
            description = "Duplicate name or slug is rejected with 409.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<JobCategoryResponse>> createJobCategory(
            @Valid @RequestBody JobCategoryCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(jobCategoryService.createJobCategory(request),
                        "Job category created"));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.EMPLOYMENT_MANAGE + "')")
    @Operation(summary = "Update a job category",
            description = "Partial update; the slug is immutable.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<JobCategoryResponse>> updateJobCategory(
            @PathVariable long id,
            @Valid @RequestBody JobCategoryUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                jobCategoryService.updateJobCategory(id, request), "Job category updated"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.EMPLOYMENT_MANAGE + "')")
    @Operation(summary = "Delete a job category",
            description = "Jobs referencing this category are detached (category_id set to "
                    + "NULL) by the V8 ON DELETE SET NULL behavior — jobs are never deleted.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Void> deleteJobCategory(@PathVariable long id) {
        jobCategoryService.deleteJobCategory(id);
        return ResponseEntity.noContent().build();
    }
}
