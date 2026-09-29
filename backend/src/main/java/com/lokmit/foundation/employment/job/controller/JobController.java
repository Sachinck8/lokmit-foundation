package com.lokmit.foundation.employment.job.controller;

import com.lokmit.foundation.common.api.ApiResponse;
import com.lokmit.foundation.common.api.PageResponse;
import com.lokmit.foundation.common.constants.ApiPaths;
import com.lokmit.foundation.common.pagination.PageParams;
import com.lokmit.foundation.employment.job.dto.JobCreateRequest;
import com.lokmit.foundation.employment.job.dto.JobResponse;
import com.lokmit.foundation.employment.job.dto.JobUpdateRequest;
import com.lokmit.foundation.employment.job.service.JobService;
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
 * Admin job management endpoints (A7.2). All endpoints require the existing
 * {@code employment:manage} permission (SUPER_ADMIN, ADMIN per the V14
 * grant) — job postings are employment administration, so no new permission
 * or migration was needed.
 *
 * <p>Lifecycle: create always starts DRAFT; publish/close/archive move the
 * status through the dedicated transition endpoints. PATCH can never change
 * status. DELETE removes the job row — job_skills cascade away per V8, and
 * the database itself refuses deletion while job_applications reference the
 * job (fk_job_applications_job has no ON DELETE action).</p>
 */
@RestController
@RequestMapping(ApiPaths.ADMIN_JOBS)
@Tag(name = "Admin Jobs",
        description = "Job management (employment:manage permission required)")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permissions.EMPLOYMENT_MANAGE + "')")
    @Operation(summary = "List jobs",
            description = "Supports pagination, employerId/categoryId/status/employmentType/"
                    + "workMode filters and title/slug/location search. Newest first.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<PageResponse<JobResponse>>> listJobs(
            @Valid @ModelAttribute PageParams pageParams,
            @RequestParam(name = "employerId", required = false) Long employerId,
            @RequestParam(name = "categoryId", required = false) Long categoryId,
            @RequestParam(required = false) String status,
            @RequestParam(name = "employmentType", required = false) String employmentType,
            @RequestParam(name = "workMode", required = false) String workMode,
            @RequestParam(name = "search", required = false) String search) {
        Pageable pageable = PageRequest.of(pageParams.getPage(), pageParams.getSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(ApiResponse.success(
                PageResponse.of(jobService.listJobs(employerId, categoryId, status,
                        employmentType, workMode, search, pageable))));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.EMPLOYMENT_MANAGE + "')")
    @Operation(summary = "Get a job by id",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<JobResponse>> getJob(@PathVariable long id) {
        return ResponseEntity.ok(ApiResponse.success(jobService.getJob(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + Permissions.EMPLOYMENT_MANAGE + "')")
    @Operation(summary = "Create a job (always starts DRAFT)",
            description = "Duplicate slug → 409; unknown employer or category → 404; "
                    + "salaryMin > salaryMax → 400; status is not writable at creation.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<JobResponse>> createJob(
            @Valid @RequestBody JobCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(jobService.createJob(request), "Job created"));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.EMPLOYMENT_MANAGE + "')")
    @Operation(summary = "Update a job",
            description = "Partial update; slug and owning employer are immutable; categoryId "
                    + "explicitly null detaches; status is not patchable — use the lifecycle "
                    + "endpoints. The resulting salary pair is validated (min ≤ max).",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<JobResponse>> updateJob(
            @PathVariable long id,
            @Valid @RequestBody JobUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                jobService.updateJob(id, request), "Job updated"));
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('" + Permissions.EMPLOYMENT_MANAGE + "')")
    @Operation(summary = "Publish a job (stamps published_at)",
            description = "Allowed from DRAFT only; already-published → 409, any other "
                    + "non-draft state → 400.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<JobResponse>> publishJob(@PathVariable long id) {
        return ResponseEntity.ok(ApiResponse.success(
                jobService.publishJob(id), "Job published"));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAuthority('" + Permissions.EMPLOYMENT_MANAGE + "')")
    @Operation(summary = "Close a job",
            description = "Allowed from PUBLISHED only. Job data is preserved.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<JobResponse>> closeJob(@PathVariable long id) {
        return ResponseEntity.ok(ApiResponse.success(
                jobService.closeJob(id), "Job closed"));
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAuthority('" + Permissions.EMPLOYMENT_MANAGE + "')")
    @Operation(summary = "Archive a job",
            description = "Allowed from DRAFT, PUBLISHED or CLOSED; archiving is terminal. "
                    + "Job data is preserved.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<JobResponse>> archiveJob(@PathVariable long id) {
        return ResponseEntity.ok(ApiResponse.success(
                jobService.archiveJob(id), "Job archived"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.EMPLOYMENT_MANAGE + "')")
    @Operation(summary = "Delete a job",
            description = "Removes the job row; its job_skills rows cascade away per V8. "
                    + "The database refuses deletion while job applications reference the "
                    + "job (fk_job_applications_job has no ON DELETE action). Employers, "
                    + "categories, skills, candidates and users are never touched.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Void> deleteJob(@PathVariable long id) {
        jobService.deleteJob(id);
        return ResponseEntity.noContent().build();
    }
}
