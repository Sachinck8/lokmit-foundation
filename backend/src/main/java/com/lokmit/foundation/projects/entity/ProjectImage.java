package com.lokmit.foundation.projects.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Maps to the existing 'project_images' table created in
 * V5__projects_schema.sql.
 *
 * <p>Pure image METADATA: {@code image_url} is a caller-supplied URL
 * reference — A6 accepts no binary files and implements no upload,
 * storage or CDN processing (that is a separate future phase). Every row
 * is owned by a project via the {@code fk_project_images_project} FK with
 * ON DELETE CASCADE — deleting a project removes its image metadata rows,
 * while a single image can be deleted independently without touching the
 * project.</p>
 */
@Entity
@Table(name = "project_images")
@Getter
@Setter
public class ProjectImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Owning project; NOT NULL per the schema. */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    /** Caller-supplied URL reference; no file handling exists in A6. */
    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "alt_text", length = 255)
    private String altText;

    @Column(length = 500)
    private String caption;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
