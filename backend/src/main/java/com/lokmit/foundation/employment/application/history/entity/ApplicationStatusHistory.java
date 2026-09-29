package com.lokmit.foundation.employment.application.history.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Maps to the 'application_status_history' table created in
 * V15__application_history_interviews.sql. Rows are written automatically
 * by the A7.3 lifecycle transitions inside the same transaction as the
 * status change. {@code previousStatus} is NULL for an application's
 * initial observation; {@code changedBy} records the acting admin's
 * database user id (never client-supplied) and is nullable by design.
 */
@Entity
@Table(name = "application_status_history")
@Getter
@Setter
public class ApplicationStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Owning application (fk ON DELETE CASCADE). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private com.lokmit.foundation.employment.application.entity.JobApplication application;

    /** NULL for the initial observation of an application. */
    @Column(name = "previous_status", length = 20)
    private String previousStatus;

    @Column(name = "new_status", nullable = false, length = 20)
    private String newStatus;

    /** Acting admin's DB user id (fk to users, NO ON DELETE action). */
    @Column(name = "changed_by")
    private Long changedBy;

    @Column(name = "changed_at", nullable = false)
    private OffsetDateTime changedAt;

    @Column(length = 500)
    private String note;
}
