package com.lokmit.foundation.services.catalog.controller;

import com.lokmit.foundation.common.api.ApiResponse;
import com.lokmit.foundation.common.api.PageResponse;
import com.lokmit.foundation.common.constants.ApiPaths;
import com.lokmit.foundation.common.pagination.PageParams;
import com.lokmit.foundation.security.Permissions;
import com.lokmit.foundation.services.catalog.dto.ExpertiseAreaCreateRequest;
import com.lokmit.foundation.services.catalog.dto.ExpertiseAreaResponse;
import com.lokmit.foundation.services.catalog.dto.ExpertiseAreaUpdateRequest;
import com.lokmit.foundation.services.catalog.service.ExpertiseAreaService;
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
 * Administrative expertise-area endpoints (A5).
 *
 * <p>All endpoints require the {@code services:manage} permission
 * (SUPER_ADMIN, ADMIN per the V12 grant).</p>
 */
@RestController
@RequestMapping(ApiPaths.ADMIN_EXPERTISE_AREAS)
@Tag(name = "Admin Services — Expertise Areas",
        description = "Expertise area management (services:manage permission required)")
public class AdminExpertiseAreaController {

    private final ExpertiseAreaService expertiseAreaService;

    public AdminExpertiseAreaController(ExpertiseAreaService expertiseAreaService) {
        this.expertiseAreaService = expertiseAreaService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permissions.SERVICES_MANAGE + "')")
    @Operation(summary = "List expertise areas",
            description = "Requires authentication and the services:manage permission. "
                    + "Supports pagination, an exact status filter and a name search.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<PageResponse<ExpertiseAreaResponse>>> listAreas(
            @Valid @ModelAttribute PageParams pageParams,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        Pageable pageable = PageRequest.of(pageParams.getPage(), pageParams.getSize(),
                Sort.by(Sort.Direction.ASC, "displayOrder").and(Sort.by("name")));
        Page<ExpertiseAreaResponse> page = expertiseAreaService.listAreas(status, search, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(page)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.SERVICES_MANAGE + "')")
    @Operation(summary = "Get an expertise area by id",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<ExpertiseAreaResponse>> getArea(@PathVariable long id) {
        return ResponseEntity.ok(ApiResponse.success(expertiseAreaService.getArea(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + Permissions.SERVICES_MANAGE + "')")
    @Operation(summary = "Create an expertise area (starts as DRAFT)",
            description = "Requires authentication and the services:manage permission. "
                    + "Duplicate slug is rejected with 409.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<ExpertiseAreaResponse>> createArea(
            @Valid @RequestBody ExpertiseAreaCreateRequest request) {
        ExpertiseAreaResponse response = expertiseAreaService.createArea(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Expertise area created"));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.SERVICES_MANAGE + "')")
    @Operation(summary = "Update an expertise area",
            description = "Slug is immutable; omitted fields stay unchanged; description "
                    + "set explicitly to null clears it. Status changes use the dedicated "
                    + "lifecycle endpoints.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<ExpertiseAreaResponse>> updateArea(
            @PathVariable long id,
            @Valid @RequestBody ExpertiseAreaUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                expertiseAreaService.updateArea(id, request), "Expertise area updated"));
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('" + Permissions.SERVICES_MANAGE + "')")
    @Operation(summary = "Publish an expertise area",
            description = "Allowed from DRAFT (idempotent for already-published); "
                    + "archived areas cannot be re-published.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<ExpertiseAreaResponse>> publishArea(@PathVariable long id) {
        return ResponseEntity.ok(ApiResponse.success(
                expertiseAreaService.publishArea(id), "Expertise area published"));
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAuthority('" + Permissions.SERVICES_MANAGE + "')")
    @Operation(summary = "Archive an expertise area",
            description = "Allowed from DRAFT or PUBLISHED; archiving is terminal.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<ExpertiseAreaResponse>> archiveArea(@PathVariable long id) {
        return ResponseEntity.ok(ApiResponse.success(
                expertiseAreaService.archiveArea(id), "Expertise area archived"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.SERVICES_MANAGE + "')")
    @Operation(summary = "Delete an expertise area",
            description = "The expertise_areas table is not referenced by other tables; "
                    + "deletion is self-contained.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Void> deleteArea(@PathVariable long id) {
        expertiseAreaService.deleteArea(id);
        return ResponseEntity.noContent().build();
    }
}
