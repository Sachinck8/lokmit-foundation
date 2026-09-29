package com.lokmit.foundation.employment.employer.repository;

import com.lokmit.foundation.employment.employer.entity.Employer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

/**
 * Repository for the V8 'employers' table. Extends JpaSpecificationExecutor
 * so search/filter (company name, contact person, verification status,
 * lifecycle status) runs as DB-side predicates — never in-memory.
 */
public interface EmployerRepository
        extends JpaRepository<Employer, Long>, JpaSpecificationExecutor<Employer> {

    boolean existsByUserId(Long userId);

    Optional<Employer> findByUserId(Long userId);
}
