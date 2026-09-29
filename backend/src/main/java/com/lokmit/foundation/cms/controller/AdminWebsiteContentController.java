package com.lokmit.foundation.cms.controller;

import com.lokmit.foundation.cms.dto.WebsiteContentCreateRequest;
import com.lokmit.foundation.cms.dto.WebsiteContentResponse;
import com.lokmit.foundation.cms.dto.WebsiteContentUpdateRequest;
import com.lokmit.foundation.cms.service.WebsiteContentService;
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
 * Administrative website-content endpoints (A4).
 *
 * <p>Two permission tiers, straight from the V2 seed semantics:</p>
 * <ul>
 *   <li>{@code content:manage} (SUPER_ADMIN, ADMIN, EDITOR) — list, read,
 *       create (always DRAFT), edit title/body.</li>
 *   <li>{@code content:publish} (SUPER_ADMIN, ADMIN) — lifecycle transitions
 *       (publish/archive) and deletion of published material.</li>
 * </ul>
 */
@RestController
@RequestMapping(ApiPaths.ADMIN_CMS_WEBSITE_CONTENT)
@Tag(name = "Admin CMS — Website Content",
        description = "Website content management (content:manage / content:publish permissions required)")
public class AdminWebsiteContentController {

    private final WebsiteContentService websiteContentService;

    public AdminWebsiteContentController(WebsiteContentService websiteContentService) {
        this.websiteContentService = websiteContentService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permissions.CONTENT_MANAGE + "')")
    @Operation(summary = "List website content sections",
            description = "Requires authentication and the content:manage permission. "
                    + "Supports pagination and exact pageKey/sectionKey/status filters.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<PageResponse<WebsiteContentResponse>>> listContent(
            @Valid @ModelAttribute PageParams pageParams,
            @RequestParam(required = false) String pageKey,
            @RequestParam(required = false) String sectionKey,
            @RequestParam(required = false) String status) {
        Pageable pageable = PageRequest.of(pageParams.getPage(), pageParams.getSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<WebsiteContentResponse> page =
                websiteContentService.listContent(pageKey, sectionKey, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(page)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.CONTENT_MANAGE + "')")
    @Operation(summary = "Get a content section by id",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<WebsiteContentResponse>> getContent(@PathVariable long id) {
        return ResponseEntity.ok(ApiResponse.success(websiteContentService.getContent(id)));
    }

    @GetMapping("/page/{pageKey}/{sectionKey}")
    @PreAuthorize("hasAuthority('" + Permissions.CONTENT_MANAGE + "')")
    @Operation(summary = "Get a content section by its unique page/section key pair",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<WebsiteContentResponse>> getContentByKey(
            @PathVariable String pageKey, @PathVariable String sectionKey) {
        return ResponseEntity.ok(ApiResponse.success(
                websiteContentService.getContentByKey(pageKey, sectionKey)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + Permissions.CONTENT_MANAGE + "')")
    @Operation(summary = "Create a content section (starts as DRAFT)",
            description = "Requires authentication and the content:manage permission. "
                    + "Duplicate page/section pairs are rejected with 409.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<WebsiteContentResponse>> createContent(
            @Valid @RequestBody WebsiteContentCreateRequest request) {
        WebsiteContentResponse response = websiteContentService.createContent(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Content section created"));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.CONTENT_MANAGE + "')")
    @Operation(summary = "Update a content section's title/body",
            description = "Identity keys are immutable; status transitions use the "
                    + "dedicated lifecycle endpoints. Omitted fields stay unchanged.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<WebsiteContentResponse>> updateContent(
            @PathVariable long id,
            @Valid @RequestBody WebsiteContentUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                websiteContentService.updateContent(id, request), "Content section updated"));
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('" + Permissions.CONTENT_PUBLISH + "')")
    @Operation(summary = "Publish a content section",
            description = "Requires the content:publish permission. Allowed from DRAFT "
                    + "(idempotent for already-published sections); archived sections "
                    + "cannot be re-published.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<WebsiteContentResponse>> publishContent(@PathVariable long id) {
        return ResponseEntity.ok(ApiResponse.success(
                websiteContentService.publishContent(id), "Content section published"));
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAuthority('" + Permissions.CONTENT_PUBLISH + "')")
    @Operation(summary = "Archive a content section",
            description = "Requires the content:publish permission. Allowed from DRAFT "
                    + "or PUBLISHED; archiving is terminal.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<WebsiteContentResponse>> archiveContent(@PathVariable long id) {
        return ResponseEntity.ok(ApiResponse.success(
                websiteContentService.archiveContent(id), "Content section archived"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.CONTENT_PUBLISH + "')")
    @Operation(summary = "Delete a content section",
            description = "Requires the content:publish permission.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Void> deleteContent(@PathVariable long id) {
        websiteContentService.deleteContent(id);
        return ResponseEntity.noContent().build();
    }
}
