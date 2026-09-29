package com.lokmit.foundation.employment.candidate.controller;

import com.lokmit.foundation.common.api.ApiResponse;
import com.lokmit.foundation.common.api.PageResponse;
import com.lokmit.foundation.common.constants.ApiPaths;
import com.lokmit.foundation.common.pagination.PageParams;
import com.lokmit.foundation.employment.candidate.dto.CandidateCreateRequest;
import com.lokmit.foundation.employment.candidate.dto.CandidateResponse;
import com.lokmit.foundation.employment.candidate.dto.CandidateUpdateRequest;
import com.lokmit.foundation.employment.candidate.service.CandidateService;
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
 * Admin candidate profile endpoints (A7.1).
 *
 * <p>All endpoints require the {@code candidates:manage} permission
 * (SUPER_ADMIN, ADMIN per the V14 grant). There is deliberately NO delete
 * endpoint: the V8 fk_candidates_user ON DELETE CASCADE means deleting a
 * profile row would remove the owning user identity — profile lifecycle is
 * handled through availability_status updates instead.</p>
 */
@RestController
@RequestMapping(ApiPaths.ADMIN_CANDIDATES)
@Tag(name = "Admin Candidates",
        description = "Candidate profile management (candidates:manage permission required)")
public class CandidateController {

    private final CandidateService candidateService;

    public CandidateController(CandidateService candidateService) {
        this.candidateService = candidateService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permissions.CANDIDATES_MANAGE + "')")
    @Operation(summary = "List candidates",
            description = "Requires authentication and the candidates:manage permission. "
                    + "Supports pagination, search and availability/gender filters.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<PageResponse<CandidateResponse>>> listCandidates(
            @Valid @ModelAttribute PageParams pageParams,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(required = false) String availability,
            @RequestParam(required = false) String gender) {
        Pageable pageable = PageRequest.of(pageParams.getPage(), pageParams.getSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(ApiResponse.success(
                PageResponse.of(candidateService.listCandidates(
                        search, availability, gender, pageable))));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.CANDIDATES_MANAGE + "')")
    @Operation(summary = "Get a candidate by id",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<CandidateResponse>> getCandidate(@PathVariable long id) {
        return ResponseEntity.ok(ApiResponse.success(candidateService.getCandidate(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + Permissions.CANDIDATES_MANAGE + "')")
    @Operation(summary = "Create a candidate profile for an existing user",
            description = "userId must reference an existing user; a user can own exactly one "
                    + "candidate profile (uq_candidates_user, duplicates → 409).",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<CandidateResponse>> createCandidate(
            @Valid @RequestBody CandidateCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(candidateService.createCandidate(request),
                        "Candidate created"));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.CANDIDATES_MANAGE + "')")
    @Operation(summary = "Update a candidate profile",
            description = "Partial update; the linked user is immutable. The resulting salary "
                    + "pair is validated (min ≤ max).",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<CandidateResponse>> updateCandidate(
            @PathVariable long id,
            @Valid @RequestBody CandidateUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                candidateService.updateCandidate(id, request), "Candidate updated"));
    }
}
