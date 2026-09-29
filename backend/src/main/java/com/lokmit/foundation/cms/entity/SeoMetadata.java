package com.lokmit.foundation.cms.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Maps to the existing 'seo_metadata' table created in
 * V3__corporate_cms_schema.sql.
 *
 * <p>{@code entity_type}/{@code entity_id} is the deliberate polymorphic
 * companion documented in docs/DATABASE.md decision D5 — there is no foreign
 * key, and the {@code uq_seo_metadata_entity} unique constraint allows only
 * one SEO record per entity.</p>
 */
@Entity
@Table(name = "seo_metadata")
@Getter
@Setter
public class SeoMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "seo_title", length = 255)
    private String seoTitle;

    @Column(name = "seo_description", length = 500)
    private String seoDescription;

    @Column(name = "canonical_url", length = 500)
    private String canonicalUrl;

    @Column(name = "og_title", length = 255)
    private String ogTitle;

    @Column(name = "og_description", length = 500)
    private String ogDescription;

    @Column(name = "og_image_url", length = 500)
    private String ogImageUrl;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
