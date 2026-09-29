package com.lokmit.foundation.services.catalog.controller;

import com.lokmit.foundation.common.api.ApiResponse;
import com.lokmit.foundation.common.api.PageResponse;
import com.lokmit.foundation.common.constants.ApiPaths;
import com.lokmit.foundation.common.pagination.PageParams;
import com.lokmit.foundation.security.Permissions;
import com.lokmit.foundation.services.catalog.dto.ServiceItemCreateRequest;
import com.lokmit.foundation.services.catalog.dto.ServiceItemResponse;
import com.lokmit.foundation.services.catalog.dto.ServiceItemUpdateRequest;
import com.lokmit.foundation.services.catalog.service.ServiceItemService;
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
 * Administrative service endpoints (A5).
 *
 * <p>All endpoints require the {@code services:manage} permission
 * (SUPER_ADMIN, ADMIN per the V12 grant).</p>
 */
@RestController
@RequestMapping(ApiPaths.ADMIN_SERVICES)
@Tag(name = "Admin Services — Services",
        description = "Service management (services:manage permission required)")
public class AdminServiceItemController {

    private final ServiceItemService serviceItemService;

    public AdminServiceItemController(ServiceItemService serviceItemService) {
        this.serviceItemService = serviceItemService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permissions.SERVICES_MANAGE + "')")
    @Operation(summary = "List services",
            description = "Requires authentication and the services:manage permission. "
                    + "Supports pagination, categoryId/status filters and title/summary search.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<PageResponse<ServiceItemResponse>>> listServices(
            @Valid @ModelAttribute PageParams pageParams,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        Pageable pageable = PageRequest.of(pageParams.getPage(), pageParams.getSize(),
                Sort.by(Sort.Direction.ASC, "displayOrder").and(Sort.by("title")));
        Page<ServiceItemResponse> page = serviceItemService.listServices(
                categoryId, status, search, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(page)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.SERVICES_MANAGE + "')")
    @Operation(summary = "Get a service by id",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<ServiceItemResponse>> getService(@PathVariable long id) {
        return ResponseEntity.ok(ApiResponse.success(serviceItemService.getService(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + Permissions.SERVICES_MANAGE + "')")
    @Operation(summary = "Create a service (starts as DRAFT)",
            description = "Requires authentication and the services:manage permission. "
                    + "Duplicate slug is rejected with 409; an unknown categoryId with 404.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<ServiceItemResponse>> createService(
            @Valid @RequestBody ServiceItemCreateRequest request) {
        ServiceItemResponse response = serviceItemService.createService(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Service created"));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.SERVICES_MANAGE + "')")
    @Operation(summary = "Update a service",
            description = "Slug is immutable; omitted fields stay unchanged; categoryId set "
                    + "explicitly to null detaches the service. Status changes use the "
                    + "dedicated lifecycle endpoints.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<ServiceItemResponse>> updateService(
            @PathVariable long id,
            @Valid @RequestBody ServiceItemUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                serviceItemService.updateService(id, request), "Service updated"));
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('" + Permissions.SERVICES_MANAGE + "')")
    @Operation(summary = "Publish a service",
            description = "Allowed from DRAFT (idempotent for already-published); "
                    + "archived services cannot be re-published.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<ServiceItemResponse>> publishService(@PathVariable long id) {
        return ResponseEntity.ok(ApiResponse.success(
                serviceItemService.publishService(id), "Service published"));
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAuthority('" + Permissions.SERVICES_MANAGE + "')")
    @Operation(summary = "Archive a service",
            description = "Allowed from DRAFT or PUBLISHED; archiving is terminal.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<ServiceItemResponse>> archiveService(@PathVariable long id) {
        return ResponseEntity.ok(ApiResponse.success(
                serviceItemService.archiveService(id), "Service archived"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.SERVICES_MANAGE + "')")
    @Operation(summary = "Delete a service",
            description = "The services table is not referenced by other tables; "
                    + "deletion is self-contained.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Void> deleteService(@PathVariable long id) {
        serviceItemService.deleteService(id);
        return ResponseEntity.noContent().build();
    }
}
