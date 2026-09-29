package com.lokmit.foundation.audit.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Maps to the 'audit_logs' table created in V16 (A7.5). Append-only and
 * immutable — records are written by backend services only; there is no
 * update path and no {@code updated_at} column. {@code actorUserId} is
 * NULL for system actions and carries no cascade so audit survival is
 * never coupled to user retention.
 *
 * <p>{@code details} holds a minimal JSON object serialized to TEXT; the
 * {@link com.lokmit.foundation.audit.service.AuditLogService} validates the
 * JSON and redacts security-sensitive keys before persistence.</p>
 */
@Entity
@Table(name = "audit_logs")
@Getter
@Setter
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Acting admin's DB user id; NULL = system action (no cascade). */
    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    /** Validated JSON object serialized as TEXT. */
    @Column(columnDefinition = "text")
    private String details;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
