package com.lokmit.foundation.employment.resume.service;

import com.lokmit.foundation.common.exception.NotFoundException;
import com.lokmit.foundation.employment.candidate.entity.Candidate;
import com.lokmit.foundation.employment.resume.entity.Resume;
import com.lokmit.foundation.employment.resume.repository.ResumeRepository;
import com.lokmit.foundation.employment.resume.service.storage.FileStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Secure resume delete service (A7.6.5). Physical deletion, consistent
 * with the A7.6.1 storage lifecycle and the V17/V8 cascade design.
 *
 * <p>Authorization model — identical to A7.6.4 download:</p>
 * <ul>
 *   <li><b>Candidate ownership</b> — a candidate may delete ONLY their
 *       own resume, and only the currently ACTIVE one. Inactive rows are
 *       historical lifecycle state that replacement deactivates; they are
 *       not part of the candidate-facing surface, so a candidate deletion
 *       request against one is masked as a plain 404. Deleting the active
 *       resume leaves the candidate with zero active resumes — a valid
 *       domain state (the partial unique index constrains AT MOST one) —
 *       and the candidate can simply upload a new one.</li>
 *   <li><b>Admin access</b> — the existing {@code candidates:manage}
 *       employment permission (V14 seed: SUPER_ADMIN + ADMIN) may delete
 *       any resume row, active or inactive.</li>
 *   <li><b>Everyone else</b> — no access; authentication grants nothing.</li>
 * </ul>
 *
 * <p>Lifecycle and atomicity (the locked A7.6 database-blob decision):
 * metadata and bytes live in the same PostgreSQL database, and the V17
 * FK {@code fk_resumes_file_blobs_resume ... ON DELETE CASCADE} removes
 * the blob row in the SAME transaction as the metadata delete — there is
 * no two-system consistency problem by construction. The
 * {@link FileStorage#delete} call keeps the operation provider-neutral:
 * for {@code DatabaseFileStorage} it is an idempotent no-op when the
 * cascade has already removed the row; for a future external store it
 * would be the cleanup hook. The controller never touches blob tables
 * or storage keys directly.</p>
 *
 * <p>Repeated DELETE: the second call finds no row and returns the
 * established 404 — no second deletion state exists.</p>
 */
@Service
public class ResumeDeleteService {

    private static final Logger LOG = LoggerFactory.getLogger(ResumeDeleteService.class);

    private final ResumeRepository resumeRepository;
    private final ResumeOwnershipService ownershipService;
    private final FileStorage fileStorage;

    public ResumeDeleteService(ResumeRepository resumeRepository,
                               ResumeOwnershipService ownershipService,
                               FileStorage fileStorage) {
        this.resumeRepository = resumeRepository;
        this.ownershipService = ownershipService;
        this.fileStorage = fileStorage;
    }

    /**
     * Deletes one resume for the given principal.
     *
     * @param resumeId            the requested resume id (the ONLY client input)
     * @param authenticatedUserId the authenticated user's database id, or null
     * @param isAdmin             whether the principal holds candidates:manage
     * @throws NotFoundException when the resume does not exist or is not
     *         authorized for this principal (existence-masked 404)
     */
    @Transactional
    public void deleteForPrincipal(Long resumeId, Long authenticatedUserId, boolean isAdmin) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new NotFoundException("Resume not found"));

        if (isAdmin) {
            deleteVerified(resume);
            return;
        }

        if (authenticatedUserId == null) {
            throw new NotFoundException("Resume not found");
        }
        Candidate ownCandidate = ownershipService.resolveOwnCandidate(authenticatedUserId);
        if (!resume.getCandidateId().equals(ownCandidate.getId())) {
            // Existence masking: a foreign resume is a plain 404.
            throw new NotFoundException("Resume not found");
        }
        if (!resume.isActive()) {
            // Candidates manage only their CURRENT active resume; inactive
            // history rows are not a candidate-facing deletion surface.
            throw new NotFoundException("Resume not found");
        }
        deleteVerified(resume);
    }

    /**
     * Removes the resume metadata; the V17 ON DELETE CASCADE removes the
     * blob row in the same transaction. The provider-neutral storage
     * delete runs inside the transaction (no-op for the DB provider once
     * the cascade has removed the row; the cleanup hook for a future
     * external provider). Failures roll back the whole operation — the
     * metadata/blob state can never become silently inconsistent.
     */
    private void deleteVerified(Resume resume) {
        String storageKey = resume.getStorageKey();
        resumeRepository.delete(resume);
        resumeRepository.flush();
        if (storageKey != null && !storageKey.isBlank()) {
            // Idempotent for the DB provider; required cleanup for any
            // future external provider. Never logged, never echoed.
            fileStorage.delete(storageKey);
        }
        LOG.info("Resume {} deleted", resume.getId());
    }
}
