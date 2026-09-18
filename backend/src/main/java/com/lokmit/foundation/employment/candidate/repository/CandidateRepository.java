package com.lokmit.foundation.employment.candidate.repository;

import com.lokmit.foundation.employment.candidate.entity.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

/** Repository for the V8 'candidates' table. */
public interface CandidateRepository
        extends JpaRepository<Candidate, Long>, JpaSpecificationExecutor<Candidate> {

    boolean existsByUserId(Long userId);

    /** The candidate profile owned by the given user (uq_candidates_user guarantees ≤ 1). */
    Optional<Candidate> findByUserId(Long userId);
}
