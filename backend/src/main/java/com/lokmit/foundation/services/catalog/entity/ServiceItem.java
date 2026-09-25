package com.lokmit.foundation.services.catalog.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Maps to the existing 'services' table created in
 * V4__services_catalog_schema.sql.
 *
 * <p>Named {@code ServiceItem} to avoid the clash with Spring's
 * {@code @Service} stereotype. The unique constraint
 * {@code uq_services_slug} prevents duplicate slugs. {@code status} is
 * constrained by chk_services_status to DRAFT/PUBLISHED/ARCHIVED. The
 * nullable {@code category_id} FK references service_categories with
 * ON DELETE SET NULL — a service survives category deletion as an
 * uncategorized service.</p>
 */
@Entity
@Table(name = "services")
@Getter
@Setter
public class ServiceItem {

    /** Lifecycle statuses admitted by chk_services_status. */
    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_PUBLISHED = "PUBLISHED";
    public static final String STATUS_ARCHIVED = "ARCHIVED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String slug;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 500)
    private String summary;

    @Column(columnDefinition = "text")
    private String description;

    /** Nullable owning category; null means uncategorized. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    private ServiceCategory category;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
