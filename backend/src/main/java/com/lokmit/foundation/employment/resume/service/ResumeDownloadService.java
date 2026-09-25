package com.lokmit.foundation.employment.resume.service;

import com.lokmit.foundation.common.exception.InternalStorageException;
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
 * Secure resume download/access service (A7.6.4). Resolves WHO may fetch
 * WHICH resume's bytes and loads them through the A7.6.1
 * {@link FileStorage} abstraction — the controller never touches storage
 * internals, blob tables or storage keys directly.
 *
 * <p>Authorization model (existing platform conventions, nothing
 * invented):</p>
 * <ul>
 *   <li><b>Candidate ownership</b> — an authenticated user with a
 *       candidate profile may access ONLY their own resume, and among
 *       their own resumes ONLY the currently ACTIVE one (the domain's
 *       established active-replacement lifecycle: inactive rows are
 *       retained history, not part of the candidate-facing surface).</li>
 *   <li><b>Admin access</b> — {@code candidates:manage} (V14 seed,
 *       SUPER_ADMIN + ADMIN), the existing employment-management
 *       permission that already governs candidate profile data. Admins
 *       may load any resume row INCLUDING inactive ones (case-review
 *       semantics); no broad new permission was created.</li>
 *   <li><b>Everyone else</b> — employers, clients, moderators: no access.
 *       Authentication alone grants nothing.</li>
 * </ul>
 *
 * <p>IDOR: the resume id is the only client input; everything else is
 * derived from the server-side principal. A foreign (id, candidate) pair
 * is masked as a plain 404 — indistinguishable from a nonexistent
 * resume.</p>
 */
@Service
public class ResumeDownloadService {

    private static final Logger LOG = LoggerFactory.getLogger(ResumeDownloadService.class);

    private final ResumeRepository resumeRepository;
    private final ResumeOwnershipService ownershipService;
    private final FileStorage fileStorage;

    public ResumeDownloadService(ResumeRepository resumeRepository,
                                 ResumeOwnershipService ownershipService,
                                 FileStorage fileStorage) {
        this.resumeRepository = resumeRepository;
        this.ownershipService = ownershipService;
        this.fileStorage = fileStorage;
    }

    /** Authorized, integrity-checked bytes of one resume. */
    public record Download(Resume resume, byte[] content, boolean active) {
    }

    /**
     * Loads an authorized resume download for the given principal.
     *
     * @param resumeId            the requested resume id (the ONLY client input)
     * @param authenticatedUserId the authenticated user's database id, or null
     * @param isAdmin             whether the principal holds candidates:manage
     * @return the resume metadata and exact stored bytes
     * @throws NotFoundException when the resume does not exist or is not
     *         authorized for this principal
     * @throws InternalStorageException when the stored bytes cannot be
     *         loaded or fail integrity verification
     */
    @Transactional(readOnly = true)
    public Download loadForPrincipal(Long resumeId, Long authenticatedUserId, boolean isAdmin) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new NotFoundException("Resume not found"));

        if (isAdmin) {
            // Existing employment-management permission governs candidate
            // data; admins may inspect any resume row, active or not.
            return loadVerified(resume);
        }

        // Non-admin: strict ownership + active-only.
        if (authenticatedUserId == null) {
            throw new NotFoundException("Resume not found");
        }
        Candidate ownCandidate = ownershipService.resolveOwnCandidate(authenticatedUserId);
        if (!resume.getCandidateId().equals(ownCandidate.getId())) {
            // Existence masking: a foreign resume is a plain 404.
            throw new NotFoundException("Resume not found");
        }
        if (!resume.isActive()) {
            // Candidates access only their CURRENT active resume; inactive
            // rows are historical lifecycle state, not a download surface.
            throw new NotFoundException("Resume not found");
        }
        return loadVerified(resume);
    }

    /**
     * Loads the bytes under the SERVER-RESOLVED storage key from the
     * authorized metadata and verifies the SHA-256 integrity recorded at
     * upload (A7.6.2, single shared checksum implementation) before
     * returning. The key never leaves this class; failures never mention
     * keys, blobs or storage internals.
     */
    private Download loadVerified(Resume resume) {
        String storageKey = resume.getStorageKey();
        if (storageKey == null || storageKey.isBlank()) {
            // Metadata exists but was never backed by stored bytes — treat
            // exactly like a missing resume (no internal detail leaks).
            throw new NotFoundException("Resume not found");
        }

        byte[] content;
        try {
            content = fileStorage.load(storageKey);
        } catch (RuntimeException e) {
            // Storage failure: safe server-error domain message; internals
            // never leak. Logged server-side with NO key material.
            LOG.error("Resume storage load failed for resume {}", resume.getId());
            throw new InternalStorageException("Resume file could not be retrieved");
        }
        if (content == null) {
            throw new NotFoundException("Resume not found");
        }

        String expected = resume.getChecksumSha256();
        if (expected == null || expected.isBlank()) {
            // Rows without a recorded checksum cannot be integrity-checked;
            // fail closed rather than serve unverified bytes.
            LOG.error("Resume {} has no recorded checksum", resume.getId());
            throw new InternalStorageException("Resume file could not be retrieved");
        }
        if (!expected.equalsIgnoreCase(ResumeUploadService.sha256Hex(content))) {
            LOG.error("Resume {} failed checksum verification", resume.getId());
            throw new InternalStorageException("Resume file could not be retrieved");
        }

        return new Download(resume, content, resume.isActive());
    }
}
