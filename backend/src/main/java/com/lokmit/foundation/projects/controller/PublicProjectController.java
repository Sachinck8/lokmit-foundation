package com.lokmit.foundation.projects.controller;

import com.lokmit.foundation.common.api.ApiResponse;
import com.lokmit.foundation.common.api.PageResponse;
import com.lokmit.foundation.common.constants.ApiPaths;
import com.lokmit.foundation.common.pagination.PageParams;
import com.lokmit.foundation.projects.dto.PublicProjectCategoryResponse;
import com.lokmit.foundation.projects.dto.PublicProjectResponse;
import com.lokmit.foundation.projects.service.PublicProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public (anonymous) projects browsing endpoints (A26). Read-only.
 *
 * <p>Only PUBLISHED projects and ACTIVE project categories are ever
 * returned. There is no create/update/delete here — those remain the admin
 * {@code AdminProjectController} family guarded by {@code projects:manage}.
 * The security chain permits only the GET matchers on these paths
 * anonymously; any other method stays under
 * {@code anyRequest().authenticated()}.</p>
 */
@RestController
@Tag(name = "Public Projects",
        description = "Publicly visible projects and project categories (no authentication)")
public class PublicProjectController {

    private final PublicProjectService publicProjectService;

    public PublicProjectController(PublicProjectService publicProjectService) {
        this.publicProjectService = publicProjectService;
    }

    @GetMapping(ApiPaths.PUBLIC_PROJECTS)
    @Operation(summary = "List published projects",
            description = "Anonymous access. Published projects only, newest first, "
                    + "with an optional ACTIVE-category slug filter.")
    public ResponseEntity<ApiResponse<PageResponse<PublicProjectResponse>>> listProjects(
            @RequestParam(name = "category", required = false) String categorySlug,
            @Valid @ModelAttribute PageParams pageParams) {
        Pageable pageable = PageRequest.of(pageParams.getPage(), pageParams.getSize(),
                Sort.by(Sort.Direction.DESC, "publishedAt")
                        .and(Sort.by(Sort.Direction.ASC, "id")));
        return ResponseEntity.ok(ApiResponse.success(
                PageResponse.of(publicProjectService.listPublishedProjects(
                        categorySlug, pageable))));
    }

    @GetMapping(ApiPaths.PUBLIC_PROJECT_CATEGORIES)
    @Operation(summary = "List active project categories",
            description = "Anonymous access. Active project categories in display order.")
    public ResponseEntity<ApiResponse<List<PublicProjectCategoryResponse>>> listCategories() {
        return ResponseEntity.ok(ApiResponse.success(
                publicProjectService.listActiveProjectCategories()));
    }

    @GetMapping(ApiPaths.PUBLIC_PROJECT)
    @Operation(summary = "Get a published project by slug",
            description = "Anonymous access. Unknown slugs and non-published projects "
                    + "indistinguishably return 404.")
    public ResponseEntity<ApiResponse<PublicProjectResponse>> getProject(
            @PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success(
                publicProjectService.getPublishedProject(slug)));
    }
}
