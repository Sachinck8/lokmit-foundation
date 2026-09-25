package com.lokmit.foundation.services.catalog.controller;

import com.lokmit.foundation.common.api.ApiResponse;
import com.lokmit.foundation.common.api.PageResponse;
import com.lokmit.foundation.common.constants.ApiPaths;
import com.lokmit.foundation.common.pagination.PageParams;
import com.lokmit.foundation.services.catalog.dto.PublicExpertiseAreaResponse;
import com.lokmit.foundation.services.catalog.dto.PublicServiceCategoryResponse;
import com.lokmit.foundation.services.catalog.dto.PublicServiceItemResponse;
import com.lokmit.foundation.services.catalog.service.PublicCatalogService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public (anonymous) services browsing endpoints (A26). Read-only.
 *
 * <p>Only PUBLISHED services/expertise areas and ACTIVE service categories
 * are ever returned. There is no create/update/delete here — those remain
 * the admin {@code AdminServiceItemController} family guarded by
 * {@code services:manage}. The security chain permits only the GET matchers
 * on these paths anonymously; any other method stays under
 * {@code anyRequest().authenticated()}.</p>
 *
 * <p>Mappings use per-method absolute paths (no class-level prefix) so each
 * public namespace lands exactly on the path SecurityConfig permits:
 * {@code /services} + {@code /services/{slug}} share a controller while
 * {@code /service-categories} and {@code /expertise-areas} stay top-level
 * (also keeping the {@code /services/{slug}} pattern unambiguous).</p>
 */
@RestController
@Tag(name = "Public Services",
        description = "Publicly visible services, categories and expertise areas (no authentication)")
public class PublicCatalogController {

    private final PublicCatalogService publicCatalogService;

    public PublicCatalogController(PublicCatalogService publicCatalogService) {
        this.publicCatalogService = publicCatalogService;
    }

    @GetMapping(ApiPaths.PUBLIC_SERVICES)
    @Operation(summary = "List published services",
            description = "Anonymous access. Published services only, ordered by "
                    + "display order then newest update.")
    public ResponseEntity<ApiResponse<PageResponse<PublicServiceItemResponse>>> listServices(
            @Valid @ModelAttribute PageParams pageParams) {
        Pageable pageable = PageRequest.of(pageParams.getPage(), pageParams.getSize(),
                Sort.by(Sort.Direction.ASC, "displayOrder")
                        .and(Sort.by(Sort.Direction.DESC, "updatedAt")));
        return ResponseEntity.ok(ApiResponse.success(
                PageResponse.of(publicCatalogService.listPublishedServices(pageable))));
    }

    @GetMapping(ApiPaths.PUBLIC_SERVICE_CATEGORIES)
    @Operation(summary = "List active service categories",
            description = "Anonymous access. Active service categories in display order.")
    public ResponseEntity<ApiResponse<List<PublicServiceCategoryResponse>>> listCategories() {
        return ResponseEntity.ok(ApiResponse.success(
                publicCatalogService.listActiveServiceCategories()));
    }

    @GetMapping(ApiPaths.PUBLIC_EXPERTISE_AREAS)
    @Operation(summary = "List published expertise areas",
            description = "Anonymous access. Published expertise areas in display order.")
    public ResponseEntity<ApiResponse<List<PublicExpertiseAreaResponse>>> listExpertiseAreas() {
        return ResponseEntity.ok(ApiResponse.success(
                publicCatalogService.listPublishedExpertiseAreas()));
    }

    @GetMapping(ApiPaths.PUBLIC_SERVICE)
    @Operation(summary = "Get a published service by slug",
            description = "Anonymous access. Unknown slugs and non-published services "
                    + "indistinguishably return 404.")
    public ResponseEntity<ApiResponse<PublicServiceItemResponse>> getService(
            @PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success(
                publicCatalogService.getPublishedService(slug)));
    }
}
