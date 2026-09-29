package com.lokmit.foundation.employment.job.controller;

import com.lokmit.foundation.common.api.ApiResponse;
import com.lokmit.foundation.common.api.PageResponse;
import com.lokmit.foundation.common.constants.ApiPaths;
import com.lokmit.foundation.common.pagination.PageParams;
import com.lokmit.foundation.employment.job.dto.PublicJobResponse;
import com.lokmit.foundation.employment.job.service.PublicJobService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public (anonymous) job browsing endpoints (A8). Read-only.
 *
 * <p>Only {@code status = PUBLISHED} jobs are ever returned. There is no
 * create/update/delete here — those remain the admin {@link JobController}
 * family guarded by {@code employment:manage}. The security chain permits
 * only the two GET matchers on this path anonymously; any other method is
 * still governed by {@code anyRequest().authenticated()}.</p>
 */
@RestController
@RequestMapping(ApiPaths.JOBS)
@Tag(name = "Public Jobs",
        description = "Publicly visible published job postings (no authentication)")
public class PublicJobController {

    private final PublicJobService publicJobService;

    public PublicJobController(PublicJobService publicJobService) {
        this.publicJobService = publicJobService;
    }

    @GetMapping
    @Operation(summary = "List published jobs",
            description = "Anonymous access. Supports pagination plus category, "
                    + "employmentType and workMode filters and keyword search over "
                    + "title/slug/location. Newest first.")
    public ResponseEntity<ApiResponse<PageResponse<PublicJobResponse>>> listJobs(
            @Valid @ModelAttribute PageParams pageParams,
            @RequestParam(name = "categoryId", required = false) Long categoryId,
            @RequestParam(name = "employmentType", required = false) String employmentType,
            @RequestParam(name = "workMode", required = false) String workMode,
            @RequestParam(name = "search", required = false) String search) {
        Pageable pageable = PageRequest.of(pageParams.getPage(), pageParams.getSize(),
                Sort.by(Sort.Direction.DESC, "publishedAt"));
        return ResponseEntity.ok(ApiResponse.success(
                PageResponse.of(publicJobService.listPublishedJobs(
                        categoryId, employmentType, workMode, search, pageable))));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a published job by id",
            description = "Anonymous access. Unknown ids and non-published jobs "
                    + "indistinguishably return 404.")
    public ResponseEntity<ApiResponse<PublicJobResponse>> getJob(@PathVariable long id) {
        return ResponseEntity.ok(ApiResponse.success(publicJobService.getPublishedJob(id)));
    }
}
