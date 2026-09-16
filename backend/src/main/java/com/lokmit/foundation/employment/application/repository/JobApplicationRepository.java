package com.lokmit.foundation.employment.application.repository;

import com.lokmit.foundation.employment.application.entity.JobApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** Repository for the V8 'job_applications' table. */
public interface JobApplicationRepository
        extends JpaRepository<JobApplication, Long>, JpaSpecificationExecutor<JobApplication> {

    boolean existsByJobIdAndCandidateId(Long jobId, Long candidateId);

    /**
     * Count of applications for jobs owned by one employer — used to resolve
     * the employerId filter with a single grouped subquery instead of
     * loading application rows into memory.
     */
    @Query("""
            select a.job.employer.id, count(a.id)
            from JobApplication a
            where a.job.employer.id in :employerIds
            group by a.job.employer.id
            """)
    List<Object[]> countByEmployerIds(@Param("employerIds") java.util.Collection<Long> employerIds);

    /**
     * Count of applications for one specific job (jobId filter path).
     */
    long countByJobId(Long jobId);

    Page<JobApplication> findByJobId(Long jobId, Pageable pageable);
}
