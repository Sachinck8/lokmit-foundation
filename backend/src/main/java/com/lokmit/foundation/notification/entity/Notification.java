package com.lokmit.foundation.notification.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Maps to the 'notifications' table created in V16 (A7.5). One personal
 * in-app notice addressed to a single user; rows cascade with the owning
 * user identity. MVP delivery is IN-APP ONLY — no delivery-channel columns
 * exist by design.
 */
@Entity
@Table(name = "notifications")
@Getter
@Setter
public class Notification {

    /** Controlled vocabulary enforced by chk_notifications_type (V16). */
    public static final String TYPE_APPLICATION_STATUS_CHANGED =
            "APPLICATION_STATUS_CHANGED";
    public static final String TYPE_INTERVIEW_SCHEDULED = "INTERVIEW_SCHEDULED";
    public static final String TYPE_INTERVIEW_UPDATED = "INTERVIEW_UPDATED";
    public static final String TYPE_INTERVIEW_CANCELLED = "INTERVIEW_CANCELLED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Owning user (fk ON DELETE CASCADE — personal data). */
    @Column(name = "recipient_user_id", nullable = false)
    private Long recipientUserId;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "text")
    private String body;

    @Column(name = "entity_type", length = 50)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    /** NULL while unread. */
    @Column(name = "read_at")
    private OffsetDateTime readAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
