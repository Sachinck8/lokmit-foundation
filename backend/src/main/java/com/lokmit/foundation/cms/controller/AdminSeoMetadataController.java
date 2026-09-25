package com.lokmit.foundation.cms.controller;

import com.lokmit.foundation.cms.dto.SeoMetadataResponse;
import com.lokmit.foundation.cms.dto.SeoMetadataUpdateRequest;
import com.lokmit.foundation.cms.dto.SeoMetadataUpsertRequest;
import com.lokmit.foundation.cms.service.SeoMetadataService;
import com.lokmit.foundation.common.api.ApiResponse;
import com.lokmit.foundation.common.api.PageResponse;
import com.lokmit.foundation.common.constants.ApiPaths;
import com.lokmit.foundation.common.pagination.PageParams;
import com.lokmit.foundation.security.Permissions;
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
 * Administrative SEO-metadata endpoints (A4), guarded by
 * {@code content:manage} (V2 seed: SUPER_ADMIN, ADMIN, EDITOR). One record
 * per (entityType, entityId); duplicates are rejected with 409.
 */
@RestController
@RequestMapping(ApiPaths.ADMIN_CMS_SEO_METADATA)
@Tag(name = "Admin CMS — SEO Metadata",
        description = "SEO metadata management (content:manage permission required)")
public class AdminSeoMetadataController {

    private final SeoMetadataService seoMetadataService;

    public AdminSeoMetadataController(SeoMetadataService seoMetadataService) {
        this.seoMetadataService = seoMetadataService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permissions.CONTENT_MANAGE + "')")
    @Operation(summary = "List SEO metadata records",
            description = "Requires authentication and the content:manage permission. "
                    + "Supports pagination, exact entityType/entityId filters and a "
                    + "case-insensitive SEO-title search.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<PageResponse<SeoMetadataResponse>>> listMetadata(
            @Valid @ModelAttribute PageParams pageParams,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Long entityId,
            @RequestParam(required = false) String search) {
        Pageable pageable = PageRequest.of(pageParams.getPage(), pageParams.getSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<SeoMetadataResponse> page =
                seoMetadataService.listMetadata(entityType, entityId, search, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(page)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.CONTENT_MANAGE + "')")
    @Operation(summary = "Get an SEO metadata record by id",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<SeoMetadataResponse>> getMetadata(@PathVariable long id) {
        return ResponseEntity.ok(ApiResponse.success(seoMetadataService.getMetadata(id)));
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    @PreAuthorize("hasAuthority('" + Permissions.CONTENT_MANAGE + "')")
    @Operation(summary = "Get an SEO metadata record by its unique entity pair",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<SeoMetadataResponse>> getMetadataByEntity(
            @PathVariable String entityType, @PathVariable long entityId) {
        return ResponseEntity.ok(ApiResponse.success(
                seoMetadataService.getMetadataByEntity(entityType, entityId)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + Permissions.CONTENT_MANAGE + "')")
    @Operation(summary = "Create an SEO metadata record",
            description = "Requires authentication and the content:manage permission. "
                    + "Only one record may exist per (entityType, entityId); duplicates "
                    + "are rejected with 409.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<SeoMetadataResponse>> createMetadata(
            @Valid @RequestBody SeoMetadataUpsertRequest request) {
        SeoMetadataResponse response = seoMetadataService.createMetadata(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "SEO metadata created"));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.CONTENT_MANAGE + "')")
    @Operation(summary = "Update an SEO metadata record",
            description = "The (entityType, entityId) identity pair is immutable. "
                    + "Omitted fields stay unchanged; explicit nulls clear optional fields.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<SeoMetadataResponse>> updateMetadata(
            @PathVariable long id,
            @Valid @RequestBody SeoMetadataUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                seoMetadataService.updateMetadata(id, request), "SEO metadata updated"));
    }
}
