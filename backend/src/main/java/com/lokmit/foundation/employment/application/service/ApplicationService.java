package com.lokmit.foundation.employment.application.service;

import com.lokmit.foundation.common.exception.BadRequestException;
import com.lokmit.foundation.common.exception.ConflictException;
import com.lokmit.foundation.common.exception.NotFoundException;
import com.lokmit.foundation.employment.application.dto.ApplicationResponse;
import com.lokmit.foundation.employment.application.dto.ApplicationReviewRequest;
import com.lokmit.foundation.employment.application.entity.JobApplication;
import com.lokmit.foundation.employment.application.history.service.ApplicationStatusHistoryService;
import com.lokmit.foundation.employment.resume.entity.Resume;
import com.lokmit.foundation.employment.resume.repository.ResumeRepository;
import com.lokmit.foundation.employment.application.service.support.OutboxPayloads;
import com.lokmit.foundation.notification.entity.Notification;
import com.lokmit.foundation.audit.service.AuditLogService;
import com.lokmit.foundation.outbox.service.OutboxService;
import com.lokmit.foundation.employment.application.repository.JobApplicationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Map;

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
 *   <li>A7.5: each transition additionally writes an audit_logs row
 *       (administrative action trail) and one outbox_events row (in-app
 *       notification to the owning candidate's user), all in the SAME
 *       transaction. The outbox is only persisted here — the relay
 *       materializes the notification separately.</li>
 * </ul>
 */
@Service
public class ApplicationService {

    private final JobApplicationRepository applicationRepository;
    private final ApplicationStatusHistoryService historyService;
    private final AuditLogService auditLogService;
    private final OutboxService outboxService;
    private final ResumeRepository resumeRepository;

    public ApplicationService(JobApplicationRepository applicationRepository,
                              ApplicationStatusHistoryService historyService,
                              AuditLogService auditLogService,
                              OutboxService outboxService,
                              ResumeRepository resumeRepository) {
        this.applicationRepository = applicationRepository;
        this.historyService = historyService;
        this.auditLogService = auditLogService;
        this.outboxService = outboxService;
        this.resumeRepository = resumeRepository;
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
            // A7.6.6: the resume must EXIST and belong to the application's
            // OWN candidate — a cross-candidate resume reference is rejected
            // with the standard 404 (existence-masked, consistent with the
            // resume endpoints' ownership masking). The DB FK alone only
            // proves existence, never ownership.
            Resume resume = resumeRepository.findById(request.getResumeId())
                    .orElseThrow(() -> new NotFoundException(
                            "Resume not found: " + request.getResumeId()));
            if (!resume.getCandidateId().equals(app.getCandidate().getId())) {
                throw new NotFoundException(
                        "Resume not found: " + request.getResumeId());
            }
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
        recordSideEffects(saved, JobApplication.STATUS_SUBMITTED, "START_REVIEW", null);
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
        recordSideEffects(saved, JobApplication.STATUS_UNDER_REVIEW, "SHORTLIST", null);
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
        recordSideEffects(saved, current,
                hire ? "DECIDE_HIRED" : "DECIDE_REJECTED", decisionNote);
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
        recordSideEffects(saved, current, "WITHDRAW", null);
        return toResponse(saved);
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    /**
     * A7.5 side effects for one lifecycle transition, executed in the
     * caller's transaction: audit row (administrative action) + outbox row
     * (in-app notification to the owning candidate's linked user).
     * Application status history is written separately by the caller via
     * the A7.4 history service; nothing here duplicates it.
     */
    private void recordSideEffects(JobApplication app, String previousStatus,
                                   String auditAction, String note) {
        String newStatus = app.getStatus();
        auditLogService.record(auditAction, "JOB_APPLICATION", app.getId(),
                Map.of("previousStatus", previousStatus, "newStatus", newStatus));

        // Recipient: the candidate's linked platform user. The candidate's
        // user linkage is guaranteed by V8 (fk_candidates_user NOT NULL); if
        // it is somehow absent the outbox processing marks the event FAILED
        // rather than creating a broken notification.
        Long recipientUserId = app.getCandidate().getUser() != null
                ? app.getCandidate().getUser().getId() : null;
        outboxService.enqueue("JOB_APPLICATION", app.getId(),
                Notification.TYPE_APPLICATION_STATUS_CHANGED,
                OutboxPayloads.applicationStatusChanged(
                        recipientUserId, app.getId(), app.getJob().getTitle(),
                        previousStatus, newStatus, note));
    }

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
                        candidate.getId(),
                        candidate.getUser() != null ? candidate.getUser().getFullName() : null,
                        candidate.getUser() != null ? candidate.getUser().getEmail() : null,
                        candidate.getPhone(),
                        candidate.getCurrentLocation(),
                        candidate.getSummary(),
                        candidate.getExpectedSalaryMin(),
                        candidate.getExpectedSalaryMax(),
                        candidate.getAvailabilityStatus()))
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
