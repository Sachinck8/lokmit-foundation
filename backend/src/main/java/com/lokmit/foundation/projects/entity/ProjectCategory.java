package com.lokmit.foundation.projects.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Maps to the existing 'project_categories' table created in
 * V5__projects_schema.sql.
 *
 * <p>Two unique business keys (name, slug) are enforced by
 * {@code uq_project_categories_name} and {@code uq_project_categories_slug}.
 * {@code status} is constrained by chk_project_categories_status to
 * ACTIVE/INACTIVE. Deleting a category is safe by schema design: projects
 * referencing it are detached (the {@code fk_projects_category} FK is
 * ON DELETE SET NULL) — never cascade-deleted.</p>
 */
@Entity
@Table(name = "project_categories")
@Getter
@Setter
public class ProjectCategory {

    /** Lifecycle statuses admitted by chk_project_categories_status. */
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
