package com.lokmit.foundation.employment.resume.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Maps to the V17 'resumes_file_blobs' table (A7.6.1 foundation) — the
 * raw BYTEA payload of exactly one {@link Resume}. Deliberately a
 * separate entity, NOT a lazily fetched association on {@code Resume}:
 * loading resume metadata never touches this row, so no listing query
 * can accidentally drag BYTEA payloads out of the database (a
 * {@code @OneToOne(fetch = LAZY)} with a shared PK cannot offer that
 * guarantee for the no-association side).
 *
 * <p>Lifetime is structurally bound to the resume row:
 * {@code fk_resumes_file_blobs_resume ON DELETE CASCADE} (V17), and the
 * candidate chain cascades further ({@code fk_resumes_candidate},
 * V8): Candidate → Resume → ResumeFileBlob — no orphan-prone
 * relationships.</p>
 *
 * <p>Size is bounded by the database CHECK
 * {@code chk_resumes_file_blobs_size}: 1..5242880 bytes (non-empty,
 * ≤ 5 MB). {@code resume_id} is the PRIMARY KEY — exactly one blob per
 * resume, 1:1 by construction.</p>
 */
@Entity
@Table(name = "resumes_file_blobs")
@Getter
@Setter
public class ResumeFileBlob {

    /**
     * Shared primary key with resumes.id (fk_resumes_file_blobs_resume,
     * ON DELETE CASCADE) — 1:1 by construction, no separate id column.
     */
    @Id
    @Column(name = "resume_id")
    private Long resumeId;

    /**
     * Raw resume bytes. BYTEA in PostgreSQL; {@code @Lob} + byte[] maps
     * through the existing PostgreSQL JDBC stack without new
     * dependencies.
     */
    @Lob
    @Column(name = "content", nullable = false, columnDefinition = "bytea")
    private byte[] content;
}
