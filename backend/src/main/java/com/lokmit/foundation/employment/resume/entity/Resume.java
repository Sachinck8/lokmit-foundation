package com.lokmit.foundation.employment.resume.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Maps to the V8 'resumes' table (A7.6.1 foundation) — one candidate
 * resume with its storage metadata. The file bytes themselves live in
 * {@link ResumeFileBlob} ('resumes_file_blobs', V17), a separate 1:1
 * companion loaded only through
 * {@link com.lokmit.foundation.employment.resume.repository.ResumeFileBlobRepository}
 * so metadata queries never drag BYTEA payloads out of the database.
 *
 * <p>Storage model (A7.6 locked decision): PostgreSQL-backed, no
 * filesystem paths and no object storage. {@code storage_key} is the
 * provider-neutral storage identity ({@code resumes/{resumeId}/{uuid}}),
 * never a public URL; {@code file_url} is LEGACY V8 metadata retained
 * only for schema compatibility — it is NOT used as an access mechanism
 * and must not be exposed through API DTOs.</p>
 *
 * <p>{@code uq_resumes_one_active_per_candidate (candidate_id) WHERE
 * is_active} (V8) enforces at most one active resume per candidate;
 * inactive rows are retained as history. {@code checksum_sha256} is
 * populated by A7.6.2 upload validation — never fabricated.</p>
 */
@Entity
@Table(name = "resumes")
@Getter
@Setter
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Owning candidate (fk_resumes_candidate ON DELETE CASCADE — personal data). */
    @Column(name = "candidate_id", nullable = false)
    private Long candidateId;

    /**
     * LEGACY V8 column (NOT NULL). Deprecated by the A7.6 database-blob
     * decision: never used to access or locate stored bytes, never exposed
     * via DTOs. Kept non-null because V8 must not be rewritten; A7.6.2
     * will document the value written for new rows.
     */
    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    /** Original filename, display-only — never used as a storage path. */
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    /** Declared MIME type (chk: none in V8; validated at upload in A7.6.2). */
    @Column(name = "file_type", length = 100)
    private String fileType;

    /** Size in bytes; blob-side CHECK (V17) bounds it to 1..5242880. */
    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    /** At most one TRUE per candidate (uq_resumes_one_active_per_candidate). */
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    /** Hex SHA-256 of the stored bytes (A7.6.2); NULL until then. */
    @Column(name = "checksum_sha256", length = 64)
    private String checksumSha256;

    /** Provider-neutral storage identity, e.g. resumes/{resumeId}/{uuid}. */
    @Column(name = "storage_key", length = 500)
    private String storageKey;
}
