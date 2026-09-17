package com.lokmit.foundation.employment.resume.repository;

import com.lokmit.foundation.employment.resume.entity.Resume;
import org.springframework.data.jpa.repository.JpaRepository;

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

    /** The candidate's active resume, if any (partial unique index guarantees ≤ 1). */
    Optional<Resume> findByCandidateIdAndIsActiveTrue(Long candidateId);

    /** Ownership-scoped single lookup: a foreign (id, candidate) pair is not found. */
    Optional<Resume> findByIdAndCandidateId(Long id, Long candidateId);

    /** Cheap existence check for upload/deactivate flows (A7.6.2+). */
    boolean existsById(Long id);
}
