package com.lokmit.foundation.employment.employer.controller;

import com.lokmit.foundation.common.api.ApiResponse;
import com.lokmit.foundation.common.api.PageResponse;
import com.lokmit.foundation.common.constants.ApiPaths;
import com.lokmit.foundation.common.pagination.PageParams;
import com.lokmit.foundation.employment.employer.dto.EmployerCreateRequest;
import com.lokmit.foundation.employment.employer.dto.EmployerResponse;
import com.lokmit.foundation.employment.employer.dto.EmployerUpdateRequest;
import com.lokmit.foundation.employment.employer.service.EmployerService;
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
 * Admin employer profile endpoints (A7.1).
 *
 * <p>All endpoints require the {@code employment:manage} permission
 * (SUPER_ADMIN, ADMIN per the V14 grant). There is deliberately NO delete
 * endpoint: the V8 fk_employers_user ON DELETE CASCADE means deleting a
 * profile row would remove the owning user identity — profile lifecycle is
 * handled through status/verificationStatus updates instead.</p>
 */
@RestController
@RequestMapping(ApiPaths.ADMIN_EMPLOYERS)
@Tag(name = "Admin Employers",
        description = "Employer profile management (employment:manage permission required)")
public class EmployerController {

    private final EmployerService employerService;

    public EmployerController(EmployerService employerService) {
        this.employerService = employerService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permissions.EMPLOYMENT_MANAGE + "')")
    @Operation(summary = "List employers",
            description = "Requires authentication and the employment:manage permission. "
                    + "Supports pagination, search and verificationStatus/status filters.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<PageResponse<EmployerResponse>>> listEmployers(
            @Valid @ModelAttribute PageParams pageParams,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "verificationStatus", required = false) String verificationStatus,
            @RequestParam(required = false) String status) {
        Pageable pageable = PageRequest.of(pageParams.getPage(), pageParams.getSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(ApiResponse.success(
                PageResponse.of(employerService.listEmployers(
                        search, verificationStatus, status, pageable))));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.EMPLOYMENT_MANAGE + "')")
    @Operation(summary = "Get an employer by id",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<EmployerResponse>> getEmployer(@PathVariable long id) {
        return ResponseEntity.ok(ApiResponse.success(employerService.getEmployer(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + Permissions.EMPLOYMENT_MANAGE + "')")
    @Operation(summary = "Create an employer profile for an existing user",
            description = "userId must reference an existing user; a user can own exactly one "
                    + "employer profile (uq_employers_user, duplicates → 409).",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<EmployerResponse>> createEmployer(
            @Valid @RequestBody EmployerCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(employerService.createEmployer(request),
                        "Employer created"));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.EMPLOYMENT_MANAGE + "')")
    @Operation(summary = "Update an employer profile",
            description = "Partial update; the linked user is immutable. Use status/"
                    + "verificationStatus changes for lifecycle management.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<EmployerResponse>> updateEmployer(
            @PathVariable long id,
            @Valid @RequestBody EmployerUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                employerService.updateEmployer(id, request), "Employer updated"));
    }
}
