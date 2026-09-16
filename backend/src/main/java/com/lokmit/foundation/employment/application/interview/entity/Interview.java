package com.lokmit.foundation.employment.application.interview.entity;

import com.lokmit.foundation.employment.application.entity.JobApplication;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Maps to the 'interviews' table created in V15__application_history_interviews.sql.
 * Mode reuses the V8 work-mode vocabulary (ONSITE/REMOTE/PHONE); status is a
 * simple four-value lifecycle. {@code scheduledAt} is TIMESTAMPTZ and handled
 * as OffsetDateTime — timezone offsets are preserved, never discarded.
 */
@Entity
@Table(name = "interviews")
@Getter
@Setter
public class Interview {

    /** Values admitted by chk_interviews_mode. */
    public static final String MODE_ONSITE = "ONSITE";
    public static final String MODE_REMOTE = "REMOTE";
    public static final String MODE_PHONE = "PHONE";

    /** Values admitted by chk_interviews_status. */
    public static final String STATUS_SCHEDULED = "SCHEDULED";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_NO_SHOW = "NO_SHOW";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Owning application (fk ON DELETE CASCADE). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private JobApplication application;

    @Column(name = "scheduled_at", nullable = false)
    private OffsetDateTime scheduledAt;

    @Column(nullable = false, length = 20)
    private String mode;

    @Column(length = 255)
    private String location;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
