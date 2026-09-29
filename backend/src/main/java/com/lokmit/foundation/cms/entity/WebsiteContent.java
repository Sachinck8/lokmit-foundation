package com.lokmit.foundation.cms.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

/**
 * Maps to the existing 'website_content' table created in
 * V3__corporate_cms_schema.sql.
 *
 * <p>One row per (page_key, section_key); the composite unique constraint
 * {@code uq_website_content_page_section} prevents duplicate section
 * records. {@code content_json} is stored as JSONB and mapped through
 * Hibernate's JSON type so the application hands it over as a String
 * containing well-formed JSON.</p>
 */
@Entity
@Table(name = "website_content")
@Getter
@Setter
public class WebsiteContent {

    /** Lifecycle statuses admitted by chk_website_content_status. */
    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_PUBLISHED = "PUBLISHED";
    public static final String STATUS_ARCHIVED = "ARCHIVED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "page_key", nullable = false, length = 100)
    private String pageKey;

    @Column(name = "section_key", nullable = false, length = 100)
    private String sectionKey;

    @Column(name = "title", length = 255)
    private String title;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content_json", columnDefinition = "jsonb")
    private String contentJson;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
