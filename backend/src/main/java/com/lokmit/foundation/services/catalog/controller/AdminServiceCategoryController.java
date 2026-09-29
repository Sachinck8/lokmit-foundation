package com.lokmit.foundation.services.catalog.controller;

import com.lokmit.foundation.common.api.ApiResponse;
import com.lokmit.foundation.common.api.PageResponse;
import com.lokmit.foundation.common.constants.ApiPaths;
import com.lokmit.foundation.common.pagination.PageParams;
import com.lokmit.foundation.security.Permissions;
import com.lokmit.foundation.services.catalog.dto.ServiceCategoryCreateRequest;
import com.lokmit.foundation.services.catalog.dto.ServiceCategoryResponse;
import com.lokmit.foundation.services.catalog.dto.ServiceCategoryUpdateRequest;
import com.lokmit.foundation.services.catalog.service.ServiceCategoryService;
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
 * Administrative service-category endpoints (A5).
 *
 * <p>All endpoints require the {@code services:manage} permission
 * (SUPER_ADMIN, ADMIN per the V12 grant). Ordinary edits, status changes and
 * deletion are one tier — service categories carry no publish workflow.</p>
 */
@RestController
@RequestMapping(ApiPaths.ADMIN_SERVICE_CATEGORIES)
@Tag(name = "Admin Services — Categories",
        description = "Service category management (services:manage permission required)")
public class AdminServiceCategoryController {

    private final ServiceCategoryService serviceCategoryService;

    public AdminServiceCategoryController(ServiceCategoryService serviceCategoryService) {
        this.serviceCategoryService = serviceCategoryService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permissions.SERVICES_MANAGE + "')")
    @Operation(summary = "List service categories",
            description = "Requires authentication and the services:manage permission. "
                    + "Supports pagination and an exact status filter (ACTIVE/INACTIVE).",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<PageResponse<ServiceCategoryResponse>>> listCategories(
            @Valid @ModelAttribute PageParams pageParams,
            @RequestParam(required = false) String status) {
        Pageable pageable = PageRequest.of(pageParams.getPage(), pageParams.getSize(),
                Sort.by(Sort.Direction.ASC, "displayOrder").and(Sort.by("name")));
        Page<ServiceCategoryResponse> page = serviceCategoryService.listCategories(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(page)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.SERVICES_MANAGE + "')")
    @Operation(summary = "Get a service category by id",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<ServiceCategoryResponse>> getCategory(@PathVariable long id) {
        return ResponseEntity.ok(ApiResponse.success(serviceCategoryService.getCategory(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + Permissions.SERVICES_MANAGE + "')")
    @Operation(summary = "Create a service category (starts as ACTIVE)",
            description = "Requires authentication and the services:manage permission. "
                    + "Duplicate name or slug is rejected with 409.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<ServiceCategoryResponse>> createCategory(
            @Valid @RequestBody ServiceCategoryCreateRequest request) {
        ServiceCategoryResponse response = serviceCategoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Service category created"));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.SERVICES_MANAGE + "')")
    @Operation(summary = "Update a service category",
            description = "Slug is immutable; omitted fields stay unchanged; description "
                    + "set explicitly to null clears it. Duplicate name is rejected with 409.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<ServiceCategoryResponse>> updateCategory(
            @PathVariable long id,
            @Valid @RequestBody ServiceCategoryUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                serviceCategoryService.updateCategory(id, request), "Service category updated"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.SERVICES_MANAGE + "')")
    @Operation(summary = "Delete a service category",
            description = "Referencing services are detached (uncategorized), never "
                    + "cascade-deleted, per the existing FK design (ON DELETE SET NULL).",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Void> deleteCategory(@PathVariable long id) {
        serviceCategoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}
