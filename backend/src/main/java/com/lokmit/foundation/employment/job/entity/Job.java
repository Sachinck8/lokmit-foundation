package com.lokmit.foundation.employment.job.entity;

import com.lokmit.foundation.employment.employer.entity.Employer;
import com.lokmit.foundation.employment.jobcategory.entity.JobCategory;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Maps to the existing 'jobs' table created in V8__employment_schema.sql.
 * The physical schema is authoritative — no columns are invented or renamed.
 *
 * <p>Constraints honored exactly:</p>
 * <ul>
 *   <li>{@code uq_jobs_slug} — unique URL identity (immutable after creation)</li>
 *   <li>{@code fk_jobs_employer} → employers(id), NO action (no cascade, no
 *       set null) — an employer with jobs cannot be deleted at DB level</li>
 *   <li>{@code fk_jobs_category} → job_categories(id) ON DELETE SET NULL —
 *       a deleted category detaches, never deletes, jobs</li>
 *   <li>{@code chk_jobs_employment_type} FULL_TIME/PART_TIME/CONTRACT/
 *       INTERNSHIP/TEMPORARY</li>
 *   <li>{@code chk_jobs_work_mode} ONSITE/REMOTE/HYBRID</li>
 *   <li>{@code chk_jobs_status} DRAFT/PUBLISHED/CLOSED/ARCHIVED</li>
 *   <li>{@code chk_jobs_salary_range} salary_max ≥ salary_min (or either NULL)</li>
 * </ul>
 *
 * <p>{@code published_at} is stamped only by the dedicated publish transition
 * — never through PATCH, which cannot change {@code status} at all.</p>
 */
@Entity
@Table(name = "jobs")
@Getter
@Setter
public class Job {

    /** Values admitted by chk_jobs_status. */
    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_PUBLISHED = "PUBLISHED";
    public static final String STATUS_CLOSED = "CLOSED";
    public static final String STATUS_ARCHIVED = "ARCHIVED";

    /** Values admitted by chk_jobs_employment_type. */
    public static final String EMPLOYMENT_FULL_TIME = "FULL_TIME";
    public static final String EMPLOYMENT_PART_TIME = "PART_TIME";
    public static final String EMPLOYMENT_CONTRACT = "CONTRACT";
    public static final String EMPLOYMENT_INTERNSHIP = "INTERNSHIP";
    public static final String EMPLOYMENT_TEMPORARY = "TEMPORARY";

    /** Values admitted by chk_jobs_work_mode. */
    public static final String WORK_MODE_ONSITE = "ONSITE";
    public static final String WORK_MODE_REMOTE = "REMOTE";
    public static final String WORK_MODE_HYBRID = "HYBRID";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Owning employer (fk_jobs_employer, no ON DELETE action — the DB refuses
     * to delete an employer while jobs reference it). LAZY; the DTO exposes
     * only safe employer fields.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employer_id", nullable = false)
    private Employer employer;

    /** Optional category (fk_jobs_category, ON DELETE SET NULL). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private JobCategory category;

    @Column(nullable = false, length = 180)
    private String slug;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Column(columnDefinition = "text")
    private String requirements;

    @Column(name = "employment_type", nullable = false, length = 20)
    private String employmentType;

    @Column(name = "work_mode", nullable = false, length = 20)
    private String workMode;

    @Column(name = "work_location", length = 255)
    private String workLocation;

    @Column(name = "salary_min", precision = 12, scale = 2)
    private BigDecimal salaryMin;

    @Column(name = "salary_max", precision = 12, scale = 2)
    private BigDecimal salaryMax;

    @Column(name = "salary_currency", nullable = false, length = 8)
    private String salaryCurrency;

    @Column(name = "openings")
    private Integer openings;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "application_deadline")
    private LocalDate applicationDeadline;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
