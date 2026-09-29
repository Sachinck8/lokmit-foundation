package com.lokmit.foundation.contact.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.Set;

/**
 * Maps to the existing 'contact_messages' table created in V7__communication_schema.sql.
 *
 * <p>Public website enquiry submissions are stored here with status NEW until
 * staff review them. No schema was introduced by this entity — it mirrors the
 * Flyway definition exactly.</p>
 */
@Entity
@Table(name = "contact_messages")
@Getter
@Setter
public class ContactMessage {

    /** Lifecycle status values allowed by chk_contact_messages_status. */
    public static final String STATUS_NEW = "NEW";
    public static final String STATUS_READ = "READ";
    public static final String STATUS_REPLIED = "REPLIED";
    public static final String STATUS_ARCHIVED = "ARCHIVED";

    /** All status values accepted by the admin update endpoint, matching the V7 CHECK constraint. */
    public static final Set<String> ALLOWED_STATUSES =
            Set.of(STATUS_NEW, STATUS_READ, STATUS_REPLIED, STATUS_ARCHIVED);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sender_name", nullable = false, length = 255)
    private String senderName;

    @Column(name = "sender_email", nullable = false, length = 255)
    private String senderEmail;

    @Column(name = "sender_phone", length = 50)
    private String senderPhone;

    @Column(name = "subject", length = 255)
    private String subject;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "internal_note", columnDefinition = "TEXT")
    private String internalNote;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
