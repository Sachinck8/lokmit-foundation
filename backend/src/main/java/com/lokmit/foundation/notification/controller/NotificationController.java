package com.lokmit.foundation.notification.controller;

import com.lokmit.foundation.common.api.ApiResponse;
import com.lokmit.foundation.common.api.PageResponse;
import com.lokmit.foundation.common.constants.ApiPaths;
import com.lokmit.foundation.common.pagination.PageParams;
import com.lokmit.foundation.notification.dto.NotificationResponse;
import com.lokmit.foundation.notification.service.NotificationService;
import com.lokmit.foundation.security.Permissions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin in-app notification endpoints (A7.5). All endpoints require the
 * {@code notifications:manage} permission (SUPER_ADMIN + ADMIN per V16).
 *
 * <p>Authorization does NOT broaden recipient scoping: every operation is
 * scoped to the authenticated user's own notifications, resolved
 * server-side — {@code notifications:manage} never grants access to another
 * user's personal notices.</p>
 */
@RestController
@RequestMapping(ApiPaths.ADMIN_NOTIFICATIONS)
@Tag(name = "Admin Notifications",
        description = "Recipient-scoped in-app notifications "
                + "(notifications:manage permission required)")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permissions.NOTIFICATIONS_MANAGE + "')")
    @Operation(summary = "List the authenticated user's notifications",
            description = "Newest first; optional type filter and unreadOnly filter; "
                    + "DB-side pagination (default 20, cap 100). Always scoped to the "
                    + "authenticated recipient.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> listNotifications(
            @RequestParam(required = false) String type,
            @RequestParam(name = "unreadOnly", required = false) Boolean unreadOnly,
            @Valid @ModelAttribute PageParams pageParams) {
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.listNotifications(type, unreadOnly, pageParams)));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("hasAuthority('" + Permissions.NOTIFICATIONS_MANAGE + "')")
    @Operation(summary = "Count the authenticated user's unread notifications",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Map<String, Long>>> unreadCount() {
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("unreadCount", notificationService.countUnread())));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.NOTIFICATIONS_MANAGE + "')")
    @Operation(summary = "Get one of the authenticated user's notifications",
            description = "A foreign (id, recipient) pair is a plain 404.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<NotificationResponse>> getNotification(
            @PathVariable long id) {
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.getNotification(id)));
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("hasAuthority('" + Permissions.NOTIFICATIONS_MANAGE + "')")
    @Operation(summary = "Mark one of the authenticated user's notifications as read",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<NotificationResponse>> markRead(
            @PathVariable long id) {
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.markRead(id), "Notification marked read"));
    }

    @PatchMapping("/{id}/unread")
    @PreAuthorize("hasAuthority('" + Permissions.NOTIFICATIONS_MANAGE + "')")
    @Operation(summary = "Mark one of the authenticated user's notifications as unread",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<NotificationResponse>> markUnread(
            @PathVariable long id) {
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.markUnread(id), "Notification marked unread"));
    }
}
