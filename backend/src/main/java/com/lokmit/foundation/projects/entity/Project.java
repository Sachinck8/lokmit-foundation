package com.lokmit.foundation.projects.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Maps to the existing 'projects' table created in
 * V5__projects_schema.sql.
 *
 * <p>The unique constraint {@code uq_projects_slug} prevents duplicate
 * slugs. {@code status} is constrained by chk_projects_status to
 * DRAFT/PUBLISHED/ARCHIVED (editorial lifecycle); {@code projectStatus} is
 * constrained by chk_projects_project_status to PLANNING/ONGOING/COMPLETED
 * (delivery state, nullable). The nullable {@code category_id} FK references
 * project_categories with ON DELETE SET NULL — a project survives category
 * deletion as an uncategorized project. {@code objectives} is a JSONB column
 * validated for well-formedness before any write. chk_projects_dates
 * guarantees {@code end_date >= start_date} whenever both are present.</p>
 */
@Entity
@Table(name = "projects")
@Getter
@Setter
public class Project {

    /** Editorial lifecycle statuses admitted by chk_projects_status. */
    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_PUBLISHED = "PUBLISHED";
    public static final String STATUS_ARCHIVED = "ARCHIVED";

    /** Delivery states admitted by chk_projects_project_status. */
    public static final String PROJECT_STATUS_PLANNING = "PLANNING";
    public static final String PROJECT_STATUS_ONGOING = "ONGOING";
    public static final String PROJECT_STATUS_COMPLETED = "COMPLETED";

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
    private ProjectCategory category;

    @Column(nullable = false, length = 20)
    private String status;

    /** Delivery state — nullable per the schema. */
    @Column(name = "project_status", length = 20)
    private String projectStatus;

    @Column(length = 255)
    private String location;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    /** Free-form objectives document; validated before any write. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String objectives;

    @Column(name = "impact_summary", columnDefinition = "text")
    private String impactSummary;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
