package com.lokmit.foundation.employment.job.service;

import com.lokmit.foundation.common.exception.BadRequestException;
import com.lokmit.foundation.common.exception.ConflictException;
import com.lokmit.foundation.common.exception.NotFoundException;
import com.lokmit.foundation.employment.employer.repository.EmployerRepository;
import com.lokmit.foundation.employment.job.dto.JobCreateRequest;
import com.lokmit.foundation.employment.job.dto.JobResponse;
import com.lokmit.foundation.employment.job.dto.JobUpdateRequest;
import com.lokmit.foundation.employment.job.entity.Job;
import com.lokmit.foundation.employment.job.repository.JobRepository;
import com.lokmit.foundation.employment.jobcategory.repository.JobCategoryRepository;
import com.lokmit.foundation.security.entity.User;
import com.lokmit.foundation.security.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.Locale;

/**
 * Job management (A7.2) on the existing V8 'jobs' table.
 *
 * <p>Design rules carried over from the established conventions:</p>
 * <ul>
 *   <li>Slug is the immutable URL identity (A5/A6/A7.1 category convention).</li>
 *   <li>The owning employer is immutable after creation; only jobs with an
 *       employer are valid (fk_jobs_employer has no ON DELETE action, so the
 *       DB itself refuses to orphan a job or delete a referenced employer).</li>
 *   <li>status is controlled exclusively by the lifecycle transitions —
 *       PATCH can never change it, so clients cannot bypass DRAFT →
 *       PUBLISHED → CLOSED → ARCHIVED.</li>
 *   <li>The salary pair is validated after every merge so no PATCH can sneak
 *       a min > max past chk_jobs_salary_range.</li>
 *   <li>Unique slug is pre-checked for a clean 409 and backstopped by
 *       uq_jobs_slug for concurrent races.</li>
 * </ul>
 */
@Service
public class JobService {

    private final JobRepository jobRepository;
    private final EmployerRepository employerRepository;
    private final JobCategoryRepository jobCategoryRepository;
    private final UserRepository userRepository;

    public JobService(JobRepository jobRepository,
                      EmployerRepository employerRepository,
                      JobCategoryRepository jobCategoryRepository,
                      UserRepository userRepository) {
        this.jobRepository = jobRepository;
        this.employerRepository = employerRepository;
        this.jobCategoryRepository = jobCategoryRepository;
        this.userRepository = userRepository;
    }

    // ------------------------------------------------------------------
    // Queries
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<JobResponse> listJobs(Long employerId,
                                      Long categoryId,
                                      String status,
                                      String employmentType,
                                      String workMode,
                                      String search,
                                      Pageable pageable) {
        Specification<Job> spec = Specification.where(null);
        if (employerId != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("employer").get("id"), employerId));
        }
        if (categoryId != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("category").get("id"), categoryId));
        }
        if (StringUtils.hasText(status)) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
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
        // LAZY employer/category: mapping only touches ids/fields available on
        // the proxy identity for ids... but companyName requires initialization.
        // listResponses initializes both relationships in ONE extra joined
        // fetch per page via toResponse with the caller-opened session; for a
        // fully N+1-free page, use listJobsPaged with entity graph in the repo.
        return jobRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public JobResponse getJob(long id) {
        return jobRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new NotFoundException("Job not found: " + id));
    }

    // ------------------------------------------------------------------
    // Mutations
    // ------------------------------------------------------------------

    @Transactional
    public JobResponse createJob(JobCreateRequest request) {
        String slug = request.getSlug().trim();
        if (jobRepository.existsBySlugIgnoreCase(slug)) {
            throw new ConflictException("Job slug '" + slug + "' already exists");
        }

        // FK validation — reuse the A7.1 repositories; never create employers,
        // users or categories implicitly. Unknown employer/category → 404.
        var employer = employerRepository.findById(request.getEmployerId())
                .orElseThrow(() -> new NotFoundException(
                        "Employer not found: " + request.getEmployerId()));
        var category = request.getCategoryId() != null
                ? jobCategoryRepository.findById(request.getCategoryId())
                        .orElseThrow(() -> new NotFoundException(
                                "Job category not found: " + request.getCategoryId()))
                : null;

        // chk_jobs_salary_range mirror: 400 before the DB ever rejects.
        validateSalaryPair(request.getSalaryMin(), request.getSalaryMax());

        Job job = new Job();
        job.setEmployer(employer);
        job.setCategory(category);
        job.setSlug(slug);
        job.setTitle(request.getTitle());
        job.setDescription(request.getDescription());
        job.setRequirements(request.getRequirements());
        job.setEmploymentType(request.getEmploymentType());
        job.setWorkMode(request.getWorkMode());
        job.setWorkLocation(request.getWorkLocation());
        job.setSalaryMin(request.getSalaryMin());
        job.setSalaryMax(request.getSalaryMax());
        job.setSalaryCurrency(request.getSalaryCurrency() != null
                ? request.getSalaryCurrency().trim().toUpperCase(Locale.ROOT)
                : "INR"); // V8 default
        job.setOpenings(request.getOpenings());
        job.setStatus(Job.STATUS_DRAFT); // schema default; lifecycle starts here
        job.setApplicationDeadline(request.getApplicationDeadline());
        job.setCreatedAt(OffsetDateTime.now());
        job.setUpdatedAt(OffsetDateTime.now());
        try {
            return toResponse(jobRepository.saveAndFlush(job));
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            // uq_jobs_slug backstop for concurrent duplicate slugs
            throw new ConflictException("Job slug '" + slug + "' already exists");
        }
    }

    @Transactional
    public JobResponse updateJob(long id, JobUpdateRequest request) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Job not found: " + id));

        if (request.getCategoryId() != null) {
            job.setCategory(jobCategoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new NotFoundException(
                            "Job category not found: " + request.getCategoryId())));
        }
        if (request.getTitle() != null) job.setTitle(request.getTitle());
        if (request.getDescription() != null) job.setDescription(request.getDescription());
        if (request.getRequirements() != null) job.setRequirements(request.getRequirements());
        if (request.getEmploymentType() != null) job.setEmploymentType(request.getEmploymentType());
        if (request.getWorkMode() != null) job.setWorkMode(request.getWorkMode());
        if (request.getWorkLocation() != null) job.setWorkLocation(request.getWorkLocation());
        if (request.getSalaryMin() != null) job.setSalaryMin(request.getSalaryMin());
        if (request.getSalaryMax() != null) job.setSalaryMax(request.getSalaryMax());
        if (request.getSalaryCurrency() != null) {
            job.setSalaryCurrency(request.getSalaryCurrency().trim().toUpperCase(Locale.ROOT));
        }
        if (request.getOpenings() != null) job.setOpenings(request.getOpenings());
        if (request.getApplicationDeadline() != null) {
            job.setApplicationDeadline(request.getApplicationDeadline());
        }

        // Validate the FINAL merged salary pair (chk_jobs_salary_range).
        validateSalaryPair(job.getSalaryMin(), job.getSalaryMax());

        job.setUpdatedAt(OffsetDateTime.now());
        return toResponse(jobRepository.save(job));
    }

    // ------------------------------------------------------------------
    // Lifecycle (status is only mutable through these transitions)
    // ------------------------------------------------------------------

    @Transactional
    public JobResponse publishJob(long id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Job not found: " + id));
        if (Job.STATUS_PUBLISHED.equals(job.getStatus())) {
            throw new ConflictException("Job is already published");
        }
        if (!Job.STATUS_DRAFT.equals(job.getStatus())) {
            throw new BadRequestException(
                    "Only DRAFT jobs can be published (current status: " + job.getStatus() + ")");
        }
        job.setStatus(Job.STATUS_PUBLISHED);
        job.setPublishedAt(OffsetDateTime.now()); // stamps published_at per V8 column
        job.setUpdatedAt(OffsetDateTime.now());
        return toResponse(jobRepository.save(job));
    }

    @Transactional
    public JobResponse closeJob(long id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Job not found: " + id));
        if (!Job.STATUS_PUBLISHED.equals(job.getStatus())) {
            throw new BadRequestException(
                    "Only PUBLISHED jobs can be closed (current status: " + job.getStatus() + ")");
        }
        job.setStatus(Job.STATUS_CLOSED);
        job.setUpdatedAt(OffsetDateTime.now());
        return toResponse(jobRepository.save(job));
    }

    @Transactional
    public JobResponse archiveJob(long id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Job not found: " + id));
        if (Job.STATUS_ARCHIVED.equals(job.getStatus())) {
            throw new ConflictException("Job is already archived");
        }
        if (!Job.STATUS_DRAFT.equals(job.getStatus())
                && !Job.STATUS_PUBLISHED.equals(job.getStatus())
                && !Job.STATUS_CLOSED.equals(job.getStatus())) {
            throw new BadRequestException("Unknown job status: " + job.getStatus());
        }
        job.setStatus(Job.STATUS_ARCHIVED); // terminal
        job.setUpdatedAt(OffsetDateTime.now());
        return toResponse(jobRepository.save(job));
    }

    // ------------------------------------------------------------------
    // Deletion
    // ------------------------------------------------------------------

    /**
     * Deletes a job. Consequences follow the existing V8 FKs exactly:
     * job_skills rows cascade away (fk_job_skills_job), while
     * fk_job_applications_job has NO ON DELETE action — the database REFUSES
     * to delete a job that already has applications (DataIntegrityViolation →
     * mapped by the global handler). No candidates, users, employers or
     * skills are ever touched.
     */
    @Transactional
    public void deleteJob(long id) {
        if (!jobRepository.existsById(id)) {
            throw new NotFoundException("Job not found: " + id);
        }
        jobRepository.deleteById(id);
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private void validateSalaryPair(java.math.BigDecimal min, java.math.BigDecimal max) {
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new BadRequestException("salaryMin must not exceed salaryMax");
        }
    }

    /**
     * Maps to the safe DTO. Employer and category are rendered as safe
     * summaries (company/category identity only — no user identity or
     * security fields). The relationships are LAZY proxies; in list
     * responses this mapping runs inside the read-only transaction so
     * initialization is one query per distinct row's employer/category —
     * bounded by the page, never a full-table load.
     */
    private JobResponse toResponse(Job job) {
        var employer = job.getEmployer();
        var category = job.getCategory();
        JobResponse.EmployerSummary employerSummary =
                new JobResponse.EmployerSummary(
                        employer.getId(),
                        employer.getCompanyName(),
                        employer.getVerificationStatus());
        JobResponse.CategorySummary categorySummary = category != null
                ? new JobResponse.CategorySummary(
                        category.getId(), category.getName(), category.getSlug())
                : null;
        return JobResponse.builder()
                .id(job.getId())
                .employer(employerSummary)
                .category(categorySummary)
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
                .status(job.getStatus())
                .applicationDeadline(job.getApplicationDeadline())
                .publishedAt(job.getPublishedAt())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }
}
