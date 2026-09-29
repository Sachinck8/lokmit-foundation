package com.lokmit.foundation.employment.jobcategory.service;

import com.lokmit.foundation.common.exception.ConflictException;
import com.lokmit.foundation.common.exception.NotFoundException;
import com.lokmit.foundation.employment.jobcategory.dto.JobCategoryCreateRequest;
import com.lokmit.foundation.employment.jobcategory.dto.JobCategoryResponse;
import com.lokmit.foundation.employment.jobcategory.dto.JobCategoryUpdateRequest;
import com.lokmit.foundation.employment.jobcategory.entity.JobCategory;
import com.lokmit.foundation.employment.jobcategory.repository.JobCategoryRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;

/**
 * Job category management. Slug is immutable after creation. Deletion is
 * safe: V8 jobs.category_id uses ON DELETE SET NULL so jobs are detached,
 * never deleted.
 */
@Service
public class JobCategoryService {

    private final JobCategoryRepository jobCategoryRepository;

    public JobCategoryService(JobCategoryRepository jobCategoryRepository) {
        this.jobCategoryRepository = jobCategoryRepository;
    }

    @Transactional(readOnly = true)
    public Page<JobCategoryResponse> listJobCategories(String search, String status, Pageable pageable) {
        Specification<JobCategory> spec = Specification.where(null);
        if (StringUtils.hasText(search)) {
            String like = "%" + search.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), like),
                    cb.like(cb.lower(root.get("slug")), like)));
        }
        if (StringUtils.hasText(status)) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        return jobCategoryRepository.findAll(spec, pageable).map(JobCategoryResponse::from);
    }

    @Transactional(readOnly = true)
    public JobCategoryResponse getJobCategory(long id) {
        return jobCategoryRepository.findById(id)
                .map(JobCategoryResponse::from)
                .orElseThrow(() -> new NotFoundException("Job category not found: " + id));
    }

    @Transactional
    public JobCategoryResponse createJobCategory(JobCategoryCreateRequest request) {
        String name = request.getName().trim();
        String slug = request.getSlug().trim();
        if (jobCategoryRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Job category name already exists: " + name);
        }
        if (jobCategoryRepository.existsBySlugIgnoreCase(slug)) {
            throw new ConflictException("Job category slug already exists: " + slug);
        }
        JobCategory jc = new JobCategory();
        jc.setName(name);
        jc.setSlug(slug);
        jc.setDescription(request.getDescription());
        jc.setDisplayOrder(request.getDisplayOrder());
        jc.setStatus(request.getStatus() != null ? request.getStatus() : JobCategory.STATUS_ACTIVE);
        jc.setCreatedAt(OffsetDateTime.now());
        jc.setUpdatedAt(OffsetDateTime.now());
        try {
            return JobCategoryResponse.from(jobCategoryRepository.save(jc));
        } catch (DataIntegrityViolationException ex) {
            // uq_job_categories_name / uq_job_categories_slug guard races
            throw new ConflictException("Job category name or slug already exists");
        }
    }

    @Transactional
    public JobCategoryResponse updateJobCategory(long id, JobCategoryUpdateRequest request) {
        JobCategory jc = jobCategoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Job category not found: " + id));
        if (request.getName() != null) {
            String name = request.getName().trim();
            if (!name.equalsIgnoreCase(jc.getName())
                    && jobCategoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
                throw new ConflictException("Job category name already exists: " + name);
            }
            jc.setName(name);
        }
        if (request.getDescription() != null) jc.setDescription(request.getDescription());
        if (request.getDisplayOrder() != null) jc.setDisplayOrder(request.getDisplayOrder());
        if (request.getStatus() != null) jc.setStatus(request.getStatus());
        jc.setUpdatedAt(OffsetDateTime.now());
        return JobCategoryResponse.from(jobCategoryRepository.save(jc));
    }

    @Transactional
    public void deleteJobCategory(long id) {
        if (!jobCategoryRepository.existsById(id)) {
            throw new NotFoundException("Job category not found: " + id);
        }
        jobCategoryRepository.deleteById(id);
    }
}
