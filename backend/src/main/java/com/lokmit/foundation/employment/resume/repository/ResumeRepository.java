package com.lokmit.foundation.employment.resume.repository;

import com.lokmit.foundation.employment.resume.entity.Resume;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repository for the V8 'resumes' table (A7.6.1 foundation). Metadata
 * only: because {@link Resume} has no association to
 * {@code ResumeFileBlob}, every query here is structurally incapable of
 * loading BYTEA content — blob access goes exclusively through
 * {@link ResumeFileBlobRepository}.
 *
 * <p>Derived queries are DB-side (no entity graph, no joins). Ownership
 * scoping helpers exist for the later candidate/admin phases (A7.6.2+):
 * a foreign (id, candidateId) pair simply does not match, the established
 * 404-masking convention.</p>
 */
public interface ResumeRepository extends JpaRepository<Resume, Long> {

    /** All resumes of one candidate, newest first (idx_resumes_candidate). */
    List<Resume> findByCandidateIdOrderByCreatedAtDesc(Long candidateId);

    /** Paginated metadata-only page for one candidate, newest first (A7.6.6 admin listing). */
    Page<Resume> findByCandidateIdOrderByCreatedAtDesc(Long candidateId, Pageable pageable);

    /** The candidate's active resume, if any (partial unique index guarantees ≤ 1). */
    Optional<Resume> findByCandidateIdAndIsActiveTrue(Long candidateId);

    /** Ownership-scoped single lookup: a foreign (id, candidate) pair is not found. */
    Optional<Resume> findByIdAndCandidateId(Long id, Long candidateId);

    /** Cheap existence check for upload/deactivate flows (A7.6.2+). */
    boolean existsById(Long id);

    /**
     * Deactivates the candidate's current active resume in place, inside
     * the caller's upload transaction. The bulk UPDATE is atomic in
     * PostgreSQL: it locks the row(s) it deactivates, so concurrent uploads
     * for the same candidate serialize on the deactivate step. Ordering is
     * explicit — DEACTIVATE (update) happens BEFORE the new INSERT, so
     * Hibernate's write ordering can never violate
     * uq_resumes_one_active_per_candidate; and in the residual case of two
     * fully concurrent first uploads (neither sees an active row), the
     * partial unique index rejects the second insert cleanly — no bad
     * state can ever be committed.
     *
     * @return the number of resumes deactivated (0 or 1)
     */
    @Modifying
    @Query("""
            UPDATE Resume r
            SET r.active = FALSE
            WHERE r.candidateId = :candidateId AND r.active = TRUE
            """)
    int deactivateActiveResume(@Param("candidateId") Long candidateId);
}
