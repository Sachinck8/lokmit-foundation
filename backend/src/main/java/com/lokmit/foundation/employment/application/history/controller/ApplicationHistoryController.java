package com.lokmit.foundation.employment.application.history.controller;

import com.lokmit.foundation.common.api.ApiResponse;
import com.lokmit.foundation.common.api.PageResponse;
import com.lokmit.foundation.common.constants.ApiPaths;
import com.lokmit.foundation.common.pagination.PageParams;
import com.lokmit.foundation.employment.application.history.dto.ApplicationStatusHistoryResponse;
import com.lokmit.foundation.employment.application.history.service.ApplicationStatusHistoryService;
import com.lokmit.foundation.security.Permissions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin application status history (A7.4). History rows are written
 * automatically by the A7.3 lifecycle transitions; this controller only
 * exposes the read path. Requires the existing {@code employment:manage}
 * permission.
 */
@RestController
@Tag(name = "Admin Application History",
        description = "Application status-change audit (employment:manage permission required)")
public class ApplicationHistoryController {

    private final ApplicationStatusHistoryService historyService;

    public ApplicationHistoryController(ApplicationStatusHistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping(ApiPaths.ADMIN_APPLICATION_HISTORY)
    @PreAuthorize("hasAuthority('" + Permissions.EMPLOYMENT_MANAGE + "')")
    @Operation(summary = "List an application's status history",
            description = "Newest transition first; DB-side pagination. Rows are "
                    + "written automatically by the lifecycle endpoints in the same "
                    + "transaction as the status change.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<PageResponse<ApplicationStatusHistoryResponse>>> listHistory(
            @PathVariable("id") long applicationId,
            @Valid @ModelAttribute PageParams pageParams) {
        return ResponseEntity.ok(ApiResponse.success(
                historyService.listHistory(applicationId, pageParams)));
    }
}
