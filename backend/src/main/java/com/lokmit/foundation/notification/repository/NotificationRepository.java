package com.lokmit.foundation.notification.repository;

import com.lokmit.foundation.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

/** Repository for the V16 'notifications' table. */
public interface NotificationRepository
        extends JpaRepository<Notification, Long>, JpaSpecificationExecutor<Notification> {

    /**
     * Unread count for one recipient — backed by
     * idx_notifications_recipient_read.
     */
    long countByRecipientUserIdAndReadAtIsNull(Long recipientUserId);

    /**
     * Recipient-scoped detail lookup; a foreign (id, recipient) pair is
     * simply not found (no cross-user existence leak).
     */
    Optional<Notification> findByIdAndRecipientUserId(Long id, Long recipientUserId);

    Page<Notification> findByRecipientUserId(Long recipientUserId, Pageable pageable);
}
