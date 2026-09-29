package com.lokmit.foundation.employment.job.repository;

import com.lokmit.foundation.employment.job.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Repository for the V8 'jobs' table. */
public interface JobRepository
        extends JpaRepository<Job, Long>, JpaSpecificationExecutor<Job> {

    boolean existsBySlugIgnoreCase(String slug);

    boolean existsBySlugIgnoreCaseAndIdNot(String slug, Long id);
}
