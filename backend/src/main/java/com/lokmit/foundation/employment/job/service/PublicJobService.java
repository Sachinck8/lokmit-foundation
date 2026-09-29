package com.lokmit.foundation.employment.job.service;

import com.lokmit.foundation.common.exception.NotFoundException;
import com.lokmit.foundation.employment.job.dto.PublicJobResponse;
import com.lokmit.foundation.employment.job.entity.Job;
import com.lokmit.foundation.employment.job.repository.JobRepository;
import com.lokmit.foundation.employment.job.repository.JobSkillRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

/**
 * Public (anonymous) job browsing (A8) over the existing V8 'jobs' table.
 *
 * <p>Visibility rule: only jobs in the {@link Job#STATUS_PUBLISHED} state are
 * public. Draft, closed and archived jobs behave exactly as if they do not
 * exist for anonymous callers — a draft id in the detail endpoint is a clean
 * 404 that does not leak the job's existence.</p>
 *
 * <p>This service reuses the A7.2 Specification style of {@link JobService}
 * for consistency, but is a deliberately separate read-only path: it can
 * never accidentally widen admin visibility, and its DTO projection
 * ({@link PublicJobResponse}) exposes only public fields — no status,
 * lifecycle bookkeeping or update timestamps, no employer workflow or
 * security data, no persistence internals.</p>
 */
@Service
public class PublicJobService {

    private final JobRepository jobRepository;
    private final JobSkillRepository jobSkillRepository;

    public PublicJobService(JobRepository jobRepository,
                            JobSkillRepository jobSkillRepository) {
        this.jobRepository = jobRepository;
        this.jobSkillRepository = jobSkillRepository;
    }

    /**
     * Lists published jobs, newest first. Supported filters (all fields that
     * already exist on the jobs/job_categories schema — nothing invented):
     * keyword search over title/slug/work location, category, employment
     * type and work mode.
     */
    @Transactional(readOnly = true)
    public Page<PublicJobResponse> listPublishedJobs(
            Long categoryId,
            String employmentType,
            String workMode,
            String search,
            Pageable pageable) {
        Specification<Job> spec = (root, query, cb) -> cb.equal(root.get("status"), Job.STATUS_PUBLISHED);
        if (categoryId != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("category").get("id"), categoryId));
        }
        if (StringUtils.hasText(employmentType)) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("employmentType"), employmentType));
        }
        if (StringUtils.hasText(workMode)) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("workMode"), workMode));
        }
        if (StringUtils.hasText(search)) {
            String like = "%" + search.toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("title")), like),
                    cb.like(cb.lower(root.get("slug")), like),
                    cb.like(cb.lower(root.get("workLocation")), like)));
        }
        return jobRepository.findAll(spec, pageable).map(this::toResponse);
    }

    /**
     * Fetches one published job by id for the public detail endpoint.
     * Unknown id AND known-but-not-published id both produce the identical
     * 404 (NotFoundException) — the endpoint never reveals whether an
     * unpublished job exists.
     */
    @Transactional(readOnly = true)
    public PublicJobResponse getPublishedJob(long id) {
        Job job = jobRepository.findById(id)
                .filter(j -> Job.STATUS_PUBLISHED.equals(j.getStatus()))
                .orElseThrow(() -> new NotFoundException("Job not found: " + id));
        return toResponse(job);
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    /**
     * Maps to the public DTO. The employer/category relationships are LAZY;
     * in list responses this runs inside the caller's read-only transaction
     * (bounded by the page), and the detail path initializes both plus the
     * job's skills through the @EntityGraph-backed JobSkillRepository.
     */
    private PublicJobResponse toResponse(Job job) {
        var employer = job.getEmployer();
        var category = job.getCategory();
        List<PublicJobResponse.SkillSummary> skills =
                jobSkillRepository.findByJobId(job.getId()).stream()
                        .map(js -> new PublicJobResponse.SkillSummary(
                                js.getSkill().getId(), js.getSkill().getName()))
                        .toList();
        PublicJobResponse.CategorySummary categorySummary = category != null
                ? new PublicJobResponse.CategorySummary(
                        category.getId(), category.getName(), category.getSlug())
                : null;
        return PublicJobResponse.builder()
                .id(job.getId())
                .slug(job.getSlug())
                .title(job.getTitle())
                .description(job.getDescription())
                .requirements(job.getRequirements())
                .employmentType(job.getEmploymentType())
                .workMode(job.getWorkMode())
                .workLocation(job.getWorkLocation())
                .salaryMin(job.getSalaryMin())
                .salaryMax(job.getSalaryMax())
                .salaryCurrency(job.getSalaryCurrency())
                .openings(job.getOpenings())
                .applicationDeadline(job.getApplicationDeadline())
                .publishedAt(job.getPublishedAt())
                .companyName(employer.getCompanyName())
                .category(categorySummary)
                .skills(skills)
                .build();
    }
}
