package com.lokmit.foundation.outbox.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Maps to the 'outbox_events' table created in V16 (A7.5) — transactional
 * outbox. Business services persist an event row in the SAME transaction
 * as the originating action; the relay later claims rows atomically and
 * materializes in-app notifications.
 *
 * <p>No FK on aggregateId: aggregates span domains and must not create
 * destructive coupling. {@code payload} is TEXT containing valid JSON
 * (validated by {@link com.lokmit.foundation.outbox.service.OutboxService}
 * before persistence) — never JSONB, per the locked A7.5 schema.</p>
 */
@Entity
@Table(name = "outbox_events")
@Getter
@Setter
public class OutboxEvent {

    /** Values enforced by chk_outbox_events_status (V16). */
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PROCESSED = "PROCESSED";
    public static final String STATUS_FAILED = "FAILED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private Long aggregateId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    /** Valid JSON TEXT — deterministic enough for retry. */
    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false)
    private int attempts;

    /** Retry gate — moved forward with bounded backoff on failure. */
    @Column(name = "available_at", nullable = false)
    private OffsetDateTime availableAt;

    @Column(name = "processed_at")
    private OffsetDateTime processedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
