package com.lokmit.foundation.cms.controller;

import com.lokmit.foundation.cms.dto.SiteSettingResponse;
import com.lokmit.foundation.cms.dto.SiteSettingUpdateRequest;
import com.lokmit.foundation.cms.service.SiteSettingService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Administrative site-settings endpoints (A4).
 *
 * <p>All endpoints require the {@code settings:manage} permission (V2 seed:
 * SUPER_ADMIN and ADMIN), enforced with method security. Keys are immutable
 * configuration identities; only value/description are manageable.</p>
 */
@RestController
@RequestMapping(ApiPaths.ADMIN_CMS_SITE_SETTINGS)
@Tag(name = "Admin CMS — Site Settings",
        description = "Site settings management (settings:manage permission required)")
public class AdminSiteSettingController {

    private final SiteSettingService siteSettingService;

    public AdminSiteSettingController(SiteSettingService siteSettingService) {
        this.siteSettingService = siteSettingService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permissions.SETTINGS_MANAGE + "')")
    @Operation(summary = "List site settings",
            description = "Requires authentication and the settings:manage permission. "
                    + "Supports pagination and an optional case-insensitive key search.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<PageResponse<SiteSettingResponse>>> listSettings(
            @Valid @ModelAttribute PageParams pageParams,
            @RequestParam(required = false) String search) {
        Pageable pageable = PageRequest.of(pageParams.getPage(), pageParams.getSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<SiteSettingResponse> page = siteSettingService.listSettings(search, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(page)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.SETTINGS_MANAGE + "')")
    @Operation(summary = "Get a site setting by id",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<SiteSettingResponse>> getSetting(@PathVariable long id) {
        return ResponseEntity.ok(ApiResponse.success(siteSettingService.getSetting(id)));
    }

    @GetMapping("/key/{settingKey}")
    @PreAuthorize("hasAuthority('" + Permissions.SETTINGS_MANAGE + "')")
    @Operation(summary = "Get a site setting by its unique key",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<SiteSettingResponse>> getSettingByKey(
            @PathVariable String settingKey) {
        return ResponseEntity.ok(ApiResponse.success(siteSettingService.getSettingByKey(settingKey)));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.SETTINGS_MANAGE + "')")
    @Operation(summary = "Update a site setting's value/description",
            description = "The setting key is immutable. Omitted fields stay unchanged.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<SiteSettingResponse>> updateSetting(
            @PathVariable long id,
            @Valid @RequestBody SiteSettingUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                siteSettingService.updateSetting(id, request), "Site setting updated"));
    }

    @PatchMapping("/key/{settingKey}")
    @PreAuthorize("hasAuthority('" + Permissions.SETTINGS_MANAGE + "')")
    @Operation(summary = "Update a site setting addressed by its unique key",
            description = "The setting key is immutable. Omitted fields stay unchanged.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<SiteSettingResponse>> updateSettingByKey(
            @PathVariable String settingKey,
            @Valid @RequestBody SiteSettingUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                siteSettingService.updateSettingByKey(settingKey, request), "Site setting updated"));
    }
}
