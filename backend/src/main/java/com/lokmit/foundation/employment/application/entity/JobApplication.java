package com.lokmit.foundation.employment.application.entity;

import com.lokmit.foundation.employment.candidate.entity.Candidate;
import com.lokmit.foundation.employment.job.entity.Job;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Maps to the existing 'job_applications' table created in
 * V8__employment_schema.sql. The physical schema is authoritative — no
 * columns invented, no history/interview/notification/audit fields added
 * (those do not exist in V8 and belong to later phases).
 *
 * <p>Constraints honored exactly:</p>
 * <ul>
 *   <li>{@code uq_job_applications_job_candidate} — one application per
 *       (job, candidate) pair</li>
 *   <li>{@code fk_job_applications_job} → jobs(id), NO ON DELETE action —
 *       the database refuses to delete a job that has applications</li>
 *   <li>{@code fk_job_applications_candidate} → candidates(id), NO ON
 *       DELETE action</li>
 *   <li>{@code fk_job_applications_resume} → resumes(id) ON DELETE SET
 *       NULL — a removed resume detaches, never deletes, the application</li>
 *   <li>{@code chk_job_applications_status} SUBMITTED/UNDER_REVIEW/
 *       SHORTLISTED/HIRED/REJECTED/WITHDRAWN</li>
 * </ul>
 *
 * <p>{@code status} and {@code decided_at} are controlled exclusively by
 * the explicit review lifecycle endpoints — never through generic PATCH.</p>
 */
@Entity
@Table(name = "job_applications")
@Getter
@Setter
public class JobApplication {

    /** Values admitted by chk_job_applications_status. */
    public static final String STATUS_SUBMITTED = "SUBMITTED";
    public static final String STATUS_UNDER_REVIEW = "UNDER_REVIEW";
    public static final String STATUS_SHORTLISTED = "SHORTLISTED";
    public static final String STATUS_HIRED = "HIRED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_WITHDRAWN = "WITHDRAWN";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Applied-to job (fk_job_applications_job, no ON DELETE action). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    /** Applying candidate (fk_job_applications_candidate, no ON DELETE action). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    /**
     * Optional resume reference (fk_job_applications_resume ON DELETE SET
     * NULL). Mapped as a plain id: the resumes table has no JPA entity yet
     * (resume management is a later phase), and A7.3 never writes this
     * column — the database FK enforces integrity on any value set by a
     * future phase.
     */
    @Column(name = "resume_id")
    private Long resumeId;

    @Column(name = "cover_note", columnDefinition = "text")
    private String coverNote;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "employer_note", columnDefinition = "text")
    private String employerNote;

    @Column(name = "applied_at", nullable = false)
    private OffsetDateTime appliedAt;

    /** Stamped by the decide (hire/reject) lifecycle transition. */
    @Column(name = "decided_at")
    private OffsetDateTime decidedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
