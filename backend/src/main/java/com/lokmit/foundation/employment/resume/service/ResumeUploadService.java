package com.lokmit.foundation.employment.resume.service;

import com.lokmit.foundation.employment.candidate.repository.CandidateRepository;
import com.lokmit.foundation.employment.resume.config.UploadProperties;
import com.lokmit.foundation.employment.resume.entity.Resume;
import com.lokmit.foundation.employment.resume.repository.ResumeRepository;
import com.lokmit.foundation.audit.service.AuditLogService;
import com.lokmit.foundation.employment.resume.service.storage.FileStorage;
import com.lokmit.foundation.employment.resume.service.validation.ResumeContentValidator;
import com.lokmit.foundation.employment.resume.service.validation.ResumeFilenameValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

/**
 * Secure resume upload lifecycle (A7.6.2) — the security/validation layer
 * on top of the A7.6.1 storage foundation.
 *
 * <p>Transactionally safe lifecycle, executed in ONE transaction:</p>
 * <ol>
 *   <li>validate filename (security) and bytes (format/MIME/size)</li>
 *   <li>verify the candidate exists (clean 400, not a DB constraint error)</li>
 *   <li>deactivate any previous active resume (explicit ordering — UPDATE
 *       before INSERT — so uq_resumes_one_active_per_candidate cannot be
 *       violated by Hibernate write ordering)</li>
 *   <li>persist resume metadata to obtain the server-generated resume id</li>
 *   <li>flush so the id is assigned, then store the EXACT validated bytes
 *       under a server-generated opaque storage key
 *       ({@code resumes/{resumeId}/{uuid}}) through the A7.6.1
 *       {@link FileStorage} abstraction</li>
 *   <li>record SHA-256 checksum (of the validated bytes), size and storage
 *       key on the metadata</li>
 *   <li>commit — metadata and bytes commit or roll back together (the
 *       database storage implementation participates in this transaction)</li>
 * </ol>
 *
 * <p>Failure at any point leaves NO resume metadata row and NO blob row:
 * a validation failure never inserts anything, and a storage failure rolls
 * the whole transaction (including the metadata insert) back.</p>
 *
 * <p>Security invariants: the original filename is display metadata only
 * (never a storage path); the client MIME type is never the format
 * decision (bytes are); the storage key is server-generated and opaque;
 * no public URL is produced — the legacy {@code file_url} column keeps a
 * non-dereferenceable internal marker because V8 made it NOT NULL.</p>
 *
 * <p><b>A7.6.6 audit/outbox policy:</b> every successful upload records
 * exactly one {@code audit_logs} row via {@link AuditLogService#record},
 * written INSIDE the same transaction as the upload (REQUIRED
 * propagation) — a failed audit insert rolls the upload back. The action
 * is {@code RESUME_UPLOADED} for a first upload or {@code RESUME_REPLACED}
 * when an existing active resume was deactivated, with details limited to
 * {@code candidateId} and {@code replacedPrevious} — never bytes, storage
 * keys, checksums or any resume content.</p>
 *
 * <p><b>Outbox policy:</b> no {@code outbox_events} row is written for
 * resume lifecycle actions. The A7.5 outbox exists to materialize
 * <em>recipient-directed in-app notifications</em>; an upload/replacement
 * has no notification recipient (the only interested party is the actor
 * themselves), so routing it through the outbox would manufacture
 * notifications nobody asked for. The immutable audit row is the durable
 * lifecycle record. This mirrors the A7.3/A7.4 decision to enqueue outbox
 * events only when a status transition actually notifies a specific
 * application's candidate.</p>
 *
 * <p><b>Download auditing decision:</b> resume downloads are deliberately
 * NOT audited. The established platform policy (A7.5) audits administrative
 * state changes, not reads; a candidate viewing their own resume would
 * generate a high-volume, low-signal audit trail, and admin case-review
 * reads are already captured by admin-side state actions. Revisiting this
 * requires a product-level policy change, not a code change.</p>
 */
@Service
public class ResumeUploadService {

    /**
     * Legacy marker written to the NOT NULL V8 {@code file_url} column.
     * Deliberately non-dereferenceable (no scheme, no path) — the database
     * is the storage, so no URL exists. Documented in DATABASE.md; never
     * exposed through DTOs.
     */
    public static final String LEGACY_FILE_URL_MARKER = "internal:db-blob";

    private final ResumeContentValidator contentValidator;
    private final ResumeFilenameValidator filenameValidator;
    private final ResumeRepository resumeRepository;
    private final CandidateRepository candidateRepository;
    private final FileStorage fileStorage;
    private final UploadProperties uploadProperties;
    private final AuditLogService auditLogService;

    public ResumeUploadService(ResumeContentValidator contentValidator,
                               ResumeFilenameValidator filenameValidator,
                               ResumeRepository resumeRepository,
                               CandidateRepository candidateRepository,
                               FileStorage fileStorage,
                               UploadProperties uploadProperties,
                               AuditLogService auditLogService) {
        this.contentValidator = contentValidator;
        this.filenameValidator = filenameValidator;
        this.resumeRepository = resumeRepository;
        this.candidateRepository = candidateRepository;
        this.fileStorage = fileStorage;
        this.uploadProperties = uploadProperties;
        this.auditLogService = auditLogService;
    }

    /**
     * Validates and stores one resume upload. Returns the persisted
     * {@link Resume} metadata (never bytes, never a public URL).
     *
     * @param candidateId      the owning candidate (verified to exist)
     * @param originalFilename the client-provided filename (validated/normalized)
     * @param declaredMimeType the client-declared MIME type (cross-checked only)
     * @param content          the exact uploaded bytes (validated, stored verbatim)
     */
    @Transactional
    public Resume upload(Long candidateId, String originalFilename,
                         String declaredMimeType, byte[] content) {
        if (candidateId == null) {
            throw new com.lokmit.foundation.common.exception.BadRequestException(
                    "Candidate is required");
        }
        if (content == null || content.length == 0) {
            throw new com.lokmit.foundation.common.exception.BadRequestException(
                    "Resume file is empty");
        }

        // 0. Fast-fail on oversized uploads BEFORE any byte-level scanning
        //    (the validator re-checks its own constant as a backstop; both
        //    default to exactly 5 MiB).
        if (content.length > uploadProperties.getMaxFileSizeBytes()) {
            throw new com.lokmit.foundation.common.exception.PayloadTooLargeException(
                    "Resume file exceeds the maximum size of 5 MiB");
        }

        // 1. Validate filename (security) and bytes (size/format/MIME).
        String safeFilename = filenameValidator.validate(originalFilename);
        ResumeContentValidator.ResumeFormat format =
                contentValidator.validate(content, declaredMimeType);

        // 2. The candidate must exist — a clean domain error instead of a
        //    DB constraint violation.
        if (!candidateRepository.existsById(candidateId)) {
            throw new com.lokmit.foundation.common.exception.NotFoundException(
                    "Candidate not found: " + candidateId);
        }

        // 3. Deactivate the previous active resume BEFORE inserting the new
        //    row (explicit ordering vs. the partial unique index). The result
        //    distinguishes a first upload from a replacement for the audit
        //    trail.
        int deactivated = resumeRepository.deactivateActiveResume(candidateId);
        String auditAction = deactivated > 0 ? "RESUME_REPLACED" : "RESUME_UPLOADED";

        // 4-6. Persist metadata (id assigned on flush), then store the exact
        //       validated bytes under the server-generated key and record
        //       checksum + key + size.
        Resume resume = new Resume();
        resume.setCandidateId(candidateId);
        resume.setFileName(safeFilename);
        resume.setFileType(mimeTypeFor(format));
        resume.setFileSizeBytes((long) content.length);
        resume.setActive(true);
        resume.setCreatedAt(OffsetDateTime.now());
        resume.setFileUrl(LEGACY_FILE_URL_MARKER);
        resume = resumeRepository.saveAndFlush(resume);

        String storageKey = storageKeyFor(resume.getId());
        fileStorage.store(storageKey, content);

        resume.setStorageKey(storageKey);
        resume.setChecksumSha256(sha256Hex(content));
        Resume saved = resumeRepository.saveAndFlush(resume);

        // A7.6.6: audit trail for the resume lifecycle — one row per upload,
        // written in the SAME transaction (REQUIRED propagation). Details
        // are metadata only: no bytes, no storage key, no checksum.
        auditLogService.record(auditAction, "RESUME", saved.getId(),
                Map.of("candidateId", candidateId,
                        "replacedPrevious", deactivated > 0));

        return saved;
    }

    /** Server-generated opaque storage key — never user-controlled. */
    private String storageKeyFor(Long resumeId) {
        return "resumes/" + resumeId + "/" + UUID.randomUUID();
    }

    /** The file_type recorded from CONTENT detection, not the client claim. */
    private String mimeTypeFor(ResumeContentValidator.ResumeFormat format) {
        return switch (format) {
            case PDF -> "application/pdf";
            case DOC -> "application/msword";
            case DOCX -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        };
    }

    /**
     * Single SHA-256 implementation for the resume module — the same
     * JDK digest utility used everywhere; no duplicate checksum code.
     */
    static String sha256Hex(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is mandatory on every supported JDK — unreachable.
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }
}
