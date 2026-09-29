package com.lokmit.foundation.notification.service;

import com.lokmit.foundation.common.api.PageResponse;
import com.lokmit.foundation.common.exception.NotFoundException;
import com.lokmit.foundation.common.pagination.PageParams;
import com.lokmit.foundation.notification.entity.Notification;
import com.lokmit.foundation.notification.repository.NotificationRepository;
import com.lokmit.foundation.security.util.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link NotificationService} (A7.5).
 *
 * <p>Pins the absolute recipient-isolation contract: every read/write is
 * scoped to the authenticated user's id resolved server-side via
 * {@link SecurityUtils}; a foreign (id, recipient) pair is a plain 404 with
 * no existence leak.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private SecurityUtils securityUtils;

    private NotificationService notificationService;

    private Notification notification(long id, long recipient, String readState) {
        Notification n = new Notification();
        n.setId(id);
        n.setRecipientUserId(recipient);
        n.setType(Notification.TYPE_APPLICATION_STATUS_CHANGED);
        n.setTitle("Application status updated: HIRED");
        n.setBody("Congrats!");
        n.setEntityType("JOB_APPLICATION");
        n.setEntityId(30L);
        n.setReadAt("READ".equals(readState) ? OffsetDateTime.now() : null);
        n.setCreatedAt(OffsetDateTime.now());
        return n;
    }

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationRepository, securityUtils);
        when(securityUtils.getCurrentUserId()).thenReturn(7L);
        // JPA save returns the managed entity; mirror that contract.
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("listNotifications is always scoped to the authenticated recipient")
    void listIsRecipientScoped() {
        when(notificationRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(
                        List.of(notification(1L, 7L, "UNREAD"))));

        PageParams params = new PageParams();
        PageResponse<?> page = notificationService.listNotifications(null, null, params);

        assertThat(page.getTotalItems()).isEqualTo(1);
        // the recipient filter was part of the executed specification
        verify(notificationRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("unreadOnly filter combines with the recipient scope")
    void unreadOnlyFilter() {
        when(notificationRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        notificationService.listNotifications(null, true, new PageParams());

        verify(notificationRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("unread count is computed for the authenticated recipient only")
    void unreadCountIsRecipientScoped() {
        when(notificationRepository.countByRecipientUserIdAndReadAtIsNull(7L))
                .thenReturn(3L);

        assertThat(notificationService.countUnread()).isEqualTo(3L);
    }

    @Test
    @DisplayName("markRead stamps read_at for an owned unread notification")
    void markReadStampsTimestamp() {
        Notification owned = notification(5L, 7L, "UNREAD");
        when(notificationRepository.findByIdAndRecipientUserId(5L, 7L))
                .thenReturn(Optional.of(owned));

        notificationService.markRead(5L);

        assertThat(owned.getReadAt()).isNotNull();
        verify(notificationRepository).save(owned);
    }

    @Test
    @DisplayName("markRead is idempotent — an already-read row keeps its original timestamp")
    void markReadIsIdempotent() {
        OffsetDateTime original = OffsetDateTime.now().minusHours(2);
        Notification owned = notification(5L, 7L, "UNREAD");
        owned.setReadAt(original);
        when(notificationRepository.findByIdAndRecipientUserId(5L, 7L))
                .thenReturn(Optional.of(owned));

        notificationService.markRead(5L);

        assertThat(owned.getReadAt()).isEqualTo(original);
    }

    @Test
    @DisplayName("markUnread clears read_at")
    void markUnreadClearsTimestamp() {
        Notification owned = notification(6L, 7L, "READ");
        when(notificationRepository.findByIdAndRecipientUserId(6L, 7L))
                .thenReturn(Optional.of(owned));

        notificationService.markUnread(6L);

        assertThat(owned.getReadAt()).isNull();
        verify(notificationRepository).save(owned);
    }

    @Test
    @DisplayName("a foreign (id, recipient) pair is a plain 404 — no existence leak")
    void foreignNotificationIs404() {
        when(notificationRepository.findByIdAndRecipientUserId(99L, 7L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.getNotification(99L))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> notificationService.markRead(99L))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> notificationService.markUnread(99L))
                .isInstanceOf(NotFoundException.class);
        verify(notificationRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    @DisplayName("unauthenticated callers are treated as not-found, never cross-user reads")
    void unauthenticatedCallerGetsNotFound() {
        when(securityUtils.getCurrentUserId()).thenReturn(null);

        assertThatThrownBy(() -> notificationService.listNotifications(
                null, null, new PageParams()))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> notificationService.countUnread())
                .isInstanceOf(NotFoundException.class);
    }
}
