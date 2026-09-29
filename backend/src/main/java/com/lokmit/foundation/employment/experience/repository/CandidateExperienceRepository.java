package com.lokmit.foundation.employment.experience.repository;

import com.lokmit.foundation.employment.experience.entity.CandidateExperience;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Repository for the existing V8 'candidate_experiences' table. */
public interface CandidateExperienceRepository
        extends JpaRepository<CandidateExperience, Long> {

    List<CandidateExperience> findByCandidateIdOrderByCreatedAtDesc(Long candidateId);

    /** Ownership-scoped lookup — the masked-404 foundation for PATCH/DELETE. */
    Optional<CandidateExperience> findByIdAndCandidateId(Long id, Long candidateId);
}
