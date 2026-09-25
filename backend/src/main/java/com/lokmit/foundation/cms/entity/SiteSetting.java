package com.lokmit.foundation.cms.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Maps to the existing 'site_settings' table created in
 * V3__corporate_cms_schema.sql.
 *
 * <p>A single row per configuration key; the {@code uq_site_settings_key}
 * unique constraint makes duplicates structurally impossible.</p>
 */
@Entity
@Table(name = "site_settings")
@Getter
@Setter
public class SiteSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "setting_key", nullable = false, unique = true, length = 100)
    private String settingKey;

    @Column(name = "setting_value", nullable = false, columnDefinition = "text")
    private String settingValue;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
