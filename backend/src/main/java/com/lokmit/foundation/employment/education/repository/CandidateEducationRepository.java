package com.lokmit.foundation.employment.education.repository;

import com.lokmit.foundation.employment.education.entity.CandidateEducation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Repository for the existing V8 'candidate_educations' table. */
public interface CandidateEducationRepository
        extends JpaRepository<CandidateEducation, Long> {

    List<CandidateEducation> findByCandidateIdOrderByCreatedAtDesc(Long candidateId);

    /** Ownership-scoped lookup — the masked-404 foundation for PATCH/DELETE. */
    Optional<CandidateEducation> findByIdAndCandidateId(Long id, Long candidateId);
}
