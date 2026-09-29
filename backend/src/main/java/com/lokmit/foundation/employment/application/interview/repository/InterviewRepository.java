package com.lokmit.foundation.employment.application.interview.repository;

import com.lokmit.foundation.employment.application.interview.entity.Interview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Repository for the V15 'interviews' table. */
public interface InterviewRepository extends JpaRepository<Interview, Long> {

    /** One application's interviews, soonest/newest scheduled_at first. */
    Page<Interview> findByApplicationIdOrderByScheduledAtDesc(
            Long applicationId, Pageable pageable);

    /** Detail lookup scoped to the owning application — enforces the
     * application/interview pairing at query level (a mismatched pair is
     * simply not found). */
    Optional<Interview> findByIdAndApplicationId(Long id, Long applicationId);
}
