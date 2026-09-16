package com.lokmit.foundation.employment.application.service;

import com.lokmit.foundation.common.exception.BadRequestException;
import com.lokmit.foundation.common.exception.ConflictException;
import com.lokmit.foundation.common.exception.NotFoundException;
import com.lokmit.foundation.employment.application.dto.ApplicationResponse;
import com.lokmit.foundation.employment.application.dto.ApplicationReviewRequest;
import com.lokmit.foundation.employment.application.entity.JobApplication;
import com.lokmit.foundation.employment.application.history.service.ApplicationStatusHistoryService;
import com.lokmit.foundation.employment.application.repository.JobApplicationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.Locale;

/**
 * Admin application review management (A7.3) on the existing V8
 * 'job_applications' table.
 *
 * <p>Design rules:</p>
 * <ul>
 *   <li>status/decided_at are controlled ONLY by the explicit review
 *       endpoints (start-review, shortlist, decide, withdraw) — generic
 *       PATCH can never change them, so the lifecycle cannot be bypassed.</li>
 *   <li>Application identity is immutable: job/candidate reassignment is
 *       not offered (the uq pair is the application's identity).</li>
 *   <li>employerId filtering is resolved with a single grouped COUNT query
 *       (idx_job_applications_candidate/join path) — no in-memory
 *       filtering, no full-table loads.</li>
 *   <li>No DELETE endpoint: applications are the project's hiring audit
 *       trail; lifecycle (WITHDRAWN/REJECTED) is the supported retirement
 *       path, and fk_job_applications has no cascade into candidates,
 *       users, employers or jobs.</li>
 *   <li>Every successful lifecycle transition also writes one row to
 *       application_status_history (A7.4) INSIDE the same transaction — a
 *       failed history insert rolls the status change back, so a status
 *       change without history can never be observed.</li>
 * </ul>
 */
@Service
public class ApplicationService {

    private final JobApplicationRepository applicationRepository;
    private final ApplicationStatusHistoryService historyService;

    public ApplicationService(JobApplicationRepository applicationRepository,
                              ApplicationStatusHistoryService historyService) {
        this.applicationRepository = applicationRepository;
        this.historyService = historyService;
    }

    // ------------------------------------------------------------------
    // Queries
    // ------------------------------------------------------------------

    /**
     * DB-side filtered/paginated list. When employerId is supplied, the
     * matching application ids are resolved through one grouped COUNT
     * query against the join path (jobs.employer_id), then applied as an
     * IN predicate — the application page itself is still one DB query.
     */
    @Transactional(readOnly = true)
    public Page<ApplicationResponse> listApplications(Long jobId,
                                                      Long candidateId,
                                                      Long employerId,
                                                      String status,
                                                      String search,
                                                      Pageable pageable) {
        Specification<JobApplication> spec = Specification.where(null);

        if (employerId != null) {
            var employerApplicationIds = applicationRepository.countByEmployerIds(
                            java.util.List.of(employerId)).stream()
                    .filter(row -> employerId.equals(((Number) row[0]).longValue()))
                    .map(row -> ((Number) row[1]).longValue())
                    .toList();
            if (employerApplicationIds.isEmpty()) {
                return Page.empty(pageable);
            }
            spec = spec.and((root, query, cb) ->
                    root.get("id").in(employerApplicationIds));
        }
        if (jobId != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("job").get("id"), jobId));
        }
        if (candidateId != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("candidate").get("id"), candidateId));
        }
        if (StringUtils.hasText(status)) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (StringUtils.hasText(search)) {
            String like = "%" + search.toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("coverNote")), like),
                    cb.like(cb.lower(root.get("employerNote")), like)));
        }
        return applicationRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ApplicationResponse getApplication(long id) {
        return applicationRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new NotFoundException("Application not found: " + id));
    }

    // ------------------------------------------------------------------
    // Review mutations (status NOT patchable)
    // ------------------------------------------------------------------

    @Transactional
    public ApplicationResponse updateReview(long id, ApplicationReviewRequest request) {
        JobApplication app = applicationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Application not found: " + id));
        if (request.getEmployerNote() != null) {
            app.setEmployerNote(request.getEmployerNote());
        }
        if (request.getResumeId() != null) {
            // fk_job_applications_resume: the DB enforces existence; invalid
            // references surface as a constraint violation → 409, never a
            // silently orphaned reference.
            app.setResumeId(request.getResumeId());
        }
        app.setUpdatedAt(OffsetDateTime.now());
        return toResponse(applicationRepository.save(app));
    }

    // ------------------------------------------------------------------
    // Review lifecycle (chk_job_applications_status values only)
    // ------------------------------------------------------------------

    @Transactional
    public ApplicationResponse startReview(long id) {
        JobApplication app = requireTransitionable(id, "start review");
        if (!JobApplication.STATUS_SUBMITTED.equals(app.getStatus())) {
            throw new BadRequestException(
                    "Only SUBMITTED applications can move to UNDER_REVIEW (current status: "
                            + app.getStatus() + ")");
        }
        app.setStatus(JobApplication.STATUS_UNDER_REVIEW);
        app.setUpdatedAt(OffsetDateTime.now());
        JobApplication saved = applicationRepository.save(app);
        historyService.recordTransition(app.getId(),
                JobApplication.STATUS_SUBMITTED, JobApplication.STATUS_UNDER_REVIEW, null);
        return toResponse(saved);
    }

    @Transactional
    public ApplicationResponse shortlist(long id) {
        JobApplication app = requireTransitionable(id, "shortlist");
        if (!JobApplication.STATUS_UNDER_REVIEW.equals(app.getStatus())) {
            throw new BadRequestException(
                    "Only UNDER_REVIEW applications can be shortlisted (current status: "
                            + app.getStatus() + ")");
        }
        app.setStatus(JobApplication.STATUS_SHORTLISTED);
        app.setUpdatedAt(OffsetDateTime.now());
        JobApplication saved = applicationRepository.save(app);
        historyService.recordTransition(app.getId(),
                JobApplication.STATUS_UNDER_REVIEW, JobApplication.STATUS_SHORTLISTED, null);
        return toResponse(saved);
    }

    /**
     * Terminal decision (HIRED or REJECTED). Allowed from SUBMITTED,
     * UNDER_REVIEW or SHORTLISTED; decided applications cannot be re-decided
     * (409) and WITHDRAWN applications are closed to review (400).
     * Stamps decided_at per the V8 column.
     */
    @Transactional
    public ApplicationResponse decide(long id, boolean hire, String decisionNote) {
        JobApplication app = requireTransitionable(id, "decide");
        String current = app.getStatus();
        if (JobApplication.STATUS_HIRED.equals(current)
                || JobApplication.STATUS_REJECTED.equals(current)) {
            throw new ConflictException(
                    "Application already decided (" + current + "); decided_at is stamped");
        }
        if (JobApplication.STATUS_WITHDRAWN.equals(current)) {
            throw new BadRequestException(
                    "WITHDRAWN applications cannot be decided");
        }
        app.setStatus(hire ? JobApplication.STATUS_HIRED : JobApplication.STATUS_REJECTED);
        if (decisionNote != null) {
            app.setEmployerNote(decisionNote);
        }
        app.setDecidedAt(OffsetDateTime.now());
        app.setUpdatedAt(OffsetDateTime.now());
        JobApplication saved = applicationRepository.save(app);
        historyService.recordTransition(app.getId(), current, saved.getStatus(), decisionNote);
        return toResponse(saved);
    }

    /**
     * Records a candidate withdrawal. Allowed from any pre-decision state;
     * decided applications are immutable (409).
     */
    @Transactional
    public ApplicationResponse withdraw(long id) {
        JobApplication app = requireTransitionable(id, "withdraw");
        String current = app.getStatus();
        if (JobApplication.STATUS_HIRED.equals(current)
                || JobApplication.STATUS_REJECTED.equals(current)) {
            throw new ConflictException(
                    "Application already decided (" + current + "); cannot be withdrawn");
        }
        if (JobApplication.STATUS_WITHDRAWN.equals(current)) {
            throw new ConflictException("Application is already withdrawn");
        }
        app.setStatus(JobApplication.STATUS_WITHDRAWN);
        app.setUpdatedAt(OffsetDateTime.now());
        JobApplication saved = applicationRepository.save(app);
        historyService.recordTransition(app.getId(), current,
                JobApplication.STATUS_WITHDRAWN, null);
        return toResponse(saved);
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private JobApplication requireTransitionable(long id, String action) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        "Application not found: " + id + " (cannot " + action + ")"));
    }

    private ApplicationResponse toResponse(JobApplication app) {
        var job = app.getJob();
        var candidate = app.getCandidate();
        var employer = job.getEmployer();
        return ApplicationResponse.builder()
                .id(app.getId())
                .job(new ApplicationResponse.JobSummary(
                        job.getId(), job.getTitle(), job.getSlug(), job.getStatus()))
                .candidate(new ApplicationResponse.CandidateSummary(
                        candidate.getId(), candidate.getPhone(),
                        candidate.getCurrentLocation(), candidate.getAvailabilityStatus()))
                .employer(new ApplicationResponse.EmployerSummary(
                        employer.getId(), employer.getCompanyName()))
                .resumeId(app.getResumeId())
                .coverNote(app.getCoverNote())
                .status(app.getStatus())
                .employerNote(app.getEmployerNote())
                .appliedAt(app.getAppliedAt())
                .decidedAt(app.getDecidedAt())
                .createdAt(app.getCreatedAt())
                .updatedAt(app.getUpdatedAt())
                .build();
    }
}
