package com.lokmit.foundation.notification.controller;

import com.lokmit.foundation.common.api.ApiResponse;
import com.lokmit.foundation.common.api.PageResponse;
import com.lokmit.foundation.common.constants.ApiPaths;
import com.lokmit.foundation.common.pagination.PageParams;
import com.lokmit.foundation.notification.dto.NotificationResponse;
import com.lokmit.foundation.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Candidate self-service notifications (A14) — a thin candidate route
 * surface over the EXISTING {@link NotificationService}, whose every method
 * already scopes to the authenticated user's database id via
 * {@code SecurityUtils} (A7.5 recipient isolation). No new service logic,
 * no repository change, no DTO change; the admin
 * {@code NotificationController} (notifications:manage) is untouched.
 *
 * <p>All endpoints require the standard JWT bearer chain
 * ({@code anyRequest().authenticated()} — no SecurityConfig change). There
 * is no candidateId or recipientId request field anywhere; a foreign
 * (notificationId, user) pair is the same plain 404 as an unknown one.
 * Mark-unread is intentionally NOT exposed to candidates (notifications
 * move forward to read; the admin API retains it).</p>
 */
@RestController
@RequestMapping(ApiPaths.CANDIDATE_ME_NOTIFICATIONS)
@Tag(name = "Candidate Notifications",
        description = "Candidate self-service in-app notifications (authenticated user ownership required)")
public class CandidateNotificationController {

    private final NotificationService notificationService;

    public CandidateNotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "List the authenticated user's own notifications",
            description = "Newest first; optional type filter and unreadOnly filter; "
                    + "DB-side pagination. Always scoped to the authenticated user — "
                    + "there is no recipientId request field.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> listOwn(
            @RequestParam(required = false) String type,
            @RequestParam(name = "unreadOnly", required = false) Boolean unreadOnly,
            @Valid @ModelAttribute PageParams pageParams) {
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.listNotifications(type, unreadOnly, pageParams)));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Count the authenticated user's own unread notifications",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Map<String, Long>>> unreadCount() {
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("unreadCount", notificationService.countUnread())));
    }

    @GetMapping("/{notificationId}")
    @Operation(summary = "Get one of the authenticated user's own notifications",
            description = "A foreign or unknown id returns the same plain 404 "
                    + "(no existence leak).",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<NotificationResponse>> getOwn(
            @PathVariable long notificationId) {
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.getNotification(notificationId)));
    }

    @PatchMapping("/{notificationId}/read")
    @Operation(summary = "Mark one of the authenticated user's own notifications as read",
            description = "Ownership is server-resolved; foreign/unknown ids return "
                    + "the same plain 404. Idempotent: an already-read notification "
                    + "keeps its original readAt timestamp.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<NotificationResponse>> markOwnRead(
            @PathVariable long notificationId) {
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.markRead(notificationId),
                "Notification marked read"));
    }
}
