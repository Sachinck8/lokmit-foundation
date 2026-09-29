package com.lokmit.foundation.notification.service;

import com.lokmit.foundation.common.api.PageResponse;
import com.lokmit.foundation.common.exception.NotFoundException;
import com.lokmit.foundation.common.pagination.PageParams;
import com.lokmit.foundation.notification.dto.NotificationResponse;
import com.lokmit.foundation.notification.entity.Notification;
import com.lokmit.foundation.notification.repository.NotificationRepository;
import com.lokmit.foundation.security.util.SecurityUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;

/**
 * Recipient-scoped in-app notification management (A7.5).
 *
 * <p>Recipient isolation is absolute: every query is scoped to the
 * authenticated user's database id resolved server-side via
 * {@link SecurityUtils} — a foreign (notificationId, recipient) pair is a
 * plain 404 with no existence leak. {@code notifications:manage} authorizes
 * the endpoints but never broadens recipient scoping; no API accepts a
 * client-supplied recipient id.</p>
 */
@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SecurityUtils securityUtils;

    public NotificationService(NotificationRepository notificationRepository,
                               SecurityUtils securityUtils) {
        this.notificationRepository = notificationRepository;
        this.securityUtils = securityUtils;
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> listNotifications(String type,
                                                                Boolean unreadOnly,
                                                                PageParams pageParams) {
        Long recipient = requireCurrentUserId();
        Pageable pageable = PageRequest.of(pageParams.getPage(), pageParams.getSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<Notification> spec = (root, query, cb) ->
                cb.equal(root.get("recipientUserId"), recipient);
        if (StringUtils.hasText(type)) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("type"), type));
        }
        if (Boolean.TRUE.equals(unreadOnly)) {
            spec = spec.and((root, query, cb) ->
                    cb.isNull(root.get("readAt")));
        }

        return PageResponse.of(notificationRepository.findAll(spec, pageable)
                .map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public NotificationResponse getNotification(long id) {
        return toResponse(findOwned(id));
    }

    @Transactional(readOnly = true)
    public long countUnread() {
        Long recipient = requireCurrentUserId();
        return notificationRepository.countByRecipientUserIdAndReadAtIsNull(recipient);
    }

    @Transactional
    public NotificationResponse markRead(long id) {
        Notification notification = findOwned(id);
        if (notification.getReadAt() == null) {
            notification.setReadAt(OffsetDateTime.now());
        }
        return toResponse(notificationRepository.save(notification));
    }

    @Transactional
    public NotificationResponse markUnread(long id) {
        Notification notification = findOwned(id);
        notification.setReadAt(null);
        return toResponse(notificationRepository.save(notification));
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private Notification findOwned(long id) {
        Long recipient = requireCurrentUserId();
        return notificationRepository.findByIdAndRecipientUserId(id, recipient)
                .orElseThrow(() -> new NotFoundException("Notification not found: " + id));
    }

    private Long requireCurrentUserId() {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) {
            throw new NotFoundException("Notification not found");
        }
        return userId;
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .type(n.getType())
                .title(n.getTitle())
                .body(n.getBody())
                .entityType(n.getEntityType())
                .entityId(n.getEntityId())
                .readAt(n.getReadAt())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
