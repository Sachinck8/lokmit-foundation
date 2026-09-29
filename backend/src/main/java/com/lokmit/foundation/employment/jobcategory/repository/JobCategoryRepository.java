package com.lokmit.foundation.employment.jobcategory.repository;

import com.lokmit.foundation.employment.jobcategory.entity.JobCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Repository for the V8 'job_categories' table. */
public interface JobCategoryRepository
        extends JpaRepository<JobCategory, Long>, JpaSpecificationExecutor<JobCategory> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    boolean existsBySlugIgnoreCaseAndIdNot(String slug, Long id);

    boolean existsBySlugIgnoreCase(String slug);
}
