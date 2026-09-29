package com.lokmit.foundation.services.catalog.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Maps to the existing 'service_categories' table created in
 * V4__services_catalog_schema.sql.
 *
 * <p>The unique constraints {@code uq_service_categories_name} and
 * {@code uq_service_categories_slug} prevent duplicate names and slugs.
 * {@code status} is constrained by chk_service_categories_status to
 * ACTIVE/INACTIVE.</p>
 */
@Entity
@Table(name = "service_categories")
@Getter
@Setter
public class ServiceCategory {

    /** Lifecycle statuses admitted by chk_service_categories_status. */
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_INACTIVE = "INACTIVE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 120)
    private String slug;

    @Column(length = 500)
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
