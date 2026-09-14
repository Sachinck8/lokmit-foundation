package com.lokmit.foundation.services.catalog.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Maps to the existing 'expertise_areas' table created in
 * V4__services_catalog_schema.sql.
 *
 * <p>The unique constraint {@code uq_expertise_areas_slug} prevents duplicate
 * slugs. {@code status} is constrained by chk_expertise_areas_status to
 * DRAFT/PUBLISHED/ARCHIVED.</p>
 */
@Entity
@Table(name = "expertise_areas")
@Getter
@Setter
public class ExpertiseArea {

    /** Lifecycle statuses admitted by chk_expertise_areas_status. */
    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_PUBLISHED = "PUBLISHED";
    public static final String STATUS_ARCHIVED = "ARCHIVED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String slug;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
