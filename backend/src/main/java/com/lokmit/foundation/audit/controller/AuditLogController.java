package com.lokmit.foundation.audit.controller;

import com.lokmit.foundation.audit.dto.AuditLogResponse;
import com.lokmit.foundation.audit.service.AuditLogService;
import com.lokmit.foundation.common.api.ApiResponse;
import com.lokmit.foundation.common.api.PageResponse;
import com.lokmit.foundation.common.constants.ApiPaths;
import com.lokmit.foundation.common.pagination.PageParams;
import com.lokmit.foundation.security.Permissions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

/**
 * Read-only audit-log endpoints (A7.5). Audit records are sensitive
 * cross-domain administrative data, so reads are restricted to the
 * SUPER_ADMIN-only {@code users:manage} authority (the existing
 * SUPER_ADMIN-level authorization model) rather than a new second
 * permission — {@code notifications:manage} deliberately does NOT grant
 * audit access.
 *
 * <p>There is deliberately NO create/update/delete endpoint: audit rows
 * are written by backend services only, and the table is append-only.</p>
 */
@RestController
@Tag(name = "Admin Audit Logs",
        description = "Immutable administrative action trail "
                + "(SUPER_ADMIN only, via users:manage)")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping(ApiPaths.ADMIN_AUDIT_LOGS)
    @PreAuthorize("hasAuthority('" + Permissions.USERS_MANAGE + "')")
    @Operation(summary = "List audit logs",
            description = "Newest first; DB-side filters (actorUserId, entityType, "
                    + "entityId, action, from/to) and pagination. SUPER_ADMIN only.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> listAuditLogs(
            @RequestParam(name = "actorUserId", required = false) Long actorUserId,
            @RequestParam(name = "entityType", required = false) String entityType,
            @RequestParam(name = "entityId", required = false) Long entityId,
            @RequestParam(required = false) String action,
            @RequestParam(name = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(name = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @Valid @ModelAttribute PageParams pageParams) {
        return ResponseEntity.ok(ApiResponse.success(
                auditLogService.listAuditLogs(actorUserId, entityType, entityId,
                        action, from, to, pageParams)));
    }

    @GetMapping(ApiPaths.ADMIN_AUDIT_LOGS + "/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.USERS_MANAGE + "')")
    @Operation(summary = "Get one audit log",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<AuditLogResponse>> getAuditLog(
            @PathVariable long id) {
        return ResponseEntity.ok(ApiResponse.success(
                auditLogService.getAuditLog(id)));
    }
}
