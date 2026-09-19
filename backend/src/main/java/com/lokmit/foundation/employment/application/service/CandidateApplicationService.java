package com.lokmit.foundation.employment.application.service;

import com.lokmit.foundation.audit.service.AuditLogService;
import com.lokmit.foundation.common.api.PageResponse;
import com.lokmit.foundation.common.exception.ConflictException;
import com.lokmit.foundation.common.exception.NotFoundException;
import com.lokmit.foundation.common.pagination.PageParams;
import com.lokmit.foundation.employment.application.dto.CandidateApplicationResponse;
import com.lokmit.foundation.employment.application.entity.JobApplication;
import com.lokmit.foundation.employment.application.history.service.ApplicationStatusHistoryService;
import com.lokmit.foundation.employment.application.repository.JobApplicationRepository;
import com.lokmit.foundation.employment.application.service.support.OutboxPayloads;
import com.lokmit.foundation.employment.candidate.entity.Candidate;
import com.lokmit.foundation.employment.job.entity.Job;
import com.lokmit.foundation.employment.job.repository.JobRepository;
import com.lokmit.foundation.employment.resume.entity.Resume;
import com.lokmit.foundation.employment.resume.repository.ResumeRepository;
import com.lokmit.foundation.notification.entity.Notification;
import com.lokmit.foundation.outbox.service.OutboxService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Candidate self-service application API (A9) on the existing V8
 * 'job_applications' table.
 *
 * <p>Security invariants:</p>
 * <ul>
 *   <li>The applying candidate arrives from the controller, which resolves
 *       it server-side from the authenticated user's id via the existing
 *       {@code ResumeOwnershipService} (A7.6.3) mechanism — the request
 *       contract has no candidateId field at all, so there is no IDOR
 *       surface.</li>
 *   <li>Applications may target only jobs in the {@link Job#STATUS_PUBLISHED}
 *       state — exactly the A8 public-visibility rule. Unknown AND
 *       non-published jobs produce the identical 404, so the endpoint never
 *       reveals whether a hidden job exists.</li>
 *   <li>An optional {@code resumeId} must be one of the caller's OWN ACTIVE
 *       resumes (ownership-scoped lookup, A7.6 404-masking convention); a
 *       foreign or inactive resume is a plain 404.</li>
 *   <li>Duplicates are rejected per the existing
 *       {@code uq_job_applications_job_candidate} business rule (409) — no
 *       new duplicate policy was invented.</li>
 *   <li>New applications use the existing initial {@code SUBMITTED} status
 *       and write their history/audit/outbox side effects with the A7.4/
 *       A7.5 services INSIDE the same transaction — no partial creation.
 *       Outbox follows the existing
 *       {@link Notification#TYPE_APPLICATION_STATUS_CHANGED} vocabulary
 *       (null → SUBMITTED is a status change); no new notification type is
 *       seeded.</li>
 * </ul>
 */
@Service
public class CandidateApplicationService {

    private final JobApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final ResumeRepository resumeRepository;
    private final ApplicationStatusHistoryService historyService;
    private final AuditLogService auditLogService;
    private final OutboxService outboxService;
    private final ApplicationService applicationService;

    public CandidateApplicationService(
            JobApplicationRepository applicationRepository,
            JobRepository jobRepository,
            ResumeRepository resumeRepository,
            ApplicationStatusHistoryService historyService,
            AuditLogService auditLogService,
            OutboxService outboxService,
            ApplicationService applicationService) {
        this.applicationRepository = applicationRepository;
        this.jobRepository = jobRepository;
        this.resumeRepository = resumeRepository;
        this.historyService = historyService;
        this.auditLogService = auditLogService;
        this.outboxService = outboxService;
        this.applicationService = applicationService;
    }

    // ------------------------------------------------------------------
    // Submission
    // ------------------------------------------------------------------

    /**
     * Submits one application for the given published job as the given
     * (server-resolved) candidate. Runs in one transaction: application
     * row + initial history row + audit row + outbox event commit or roll
     * back together.
     */
    @Transactional
    public CandidateApplicationResponse submit(Candidate candidate, Long jobId,
                                               Long resumeId, String coverNote) {
        long candidateId = candidate.getId();

        // 1. Job visibility — exactly the A8 public rule, existence-masked.
        Job job = jobRepository.findById(jobId)
                .filter(j -> Job.STATUS_PUBLISHED.equals(j.getStatus()))
                .orElseThrow(() -> new NotFoundException("Job not found: " + jobId));

        // 2. Duplicate — the existing uq_job_applications_job_candidate rule.
        if (applicationRepository.existsByJobIdAndCandidateId(jobId, candidateId)) {
            throw new ConflictException("You have already applied to this job");
        }

        // 3. Optional resume — must be the caller's OWN ACTIVE resume.
        Long attachedResumeId = null;
        if (resumeId != null) {
            Resume resume = resumeRepository.findByIdAndCandidateId(resumeId, candidateId)
                    .filter(Resume::isActive)
                    .orElseThrow(() -> new NotFoundException(
                            "Resume not found: " + resumeId));
            attachedResumeId = resume.getId();
        }

        // 4. Persist with the existing initial status.
        OffsetDateTime now = OffsetDateTime.now();
        JobApplication app = new JobApplication();
        app.setJob(job);
        app.setCandidate(candidate);
        app.setResumeId(attachedResumeId);
        app.setCoverNote(coverNote);
        app.setStatus(JobApplication.STATUS_SUBMITTED);
        app.setAppliedAt(now);
        app.setCreatedAt(now);
        app.setUpdatedAt(now);
        JobApplication saved = applicationRepository.save(app);

        // 5. A7.4 history: the initial observation (previousStatus NULL,
        //    documented nullable-by-design for exactly this case).
        historyService.recordTransition(saved.getId(), null,
                JobApplication.STATUS_SUBMITTED, null);

        // 6. A7.5 side effects in the SAME transaction. The candidate's
        //    user linkage is guaranteed by V8 (fk_candidates_user NOT NULL);
        //    if it is somehow absent the outbox event carries a null
        //    recipient and fails safely downstream, exactly like A7.3.
        Long recipientUserId = candidate.getUser() != null
                ? candidate.getUser().getId() : null;
        auditLogService.record("SUBMIT", "JOB_APPLICATION", saved.getId(),
                Map.of("jobId", job.getId(), "candidateId", candidateId));
        outboxService.enqueue("JOB_APPLICATION", saved.getId(),
                Notification.TYPE_APPLICATION_STATUS_CHANGED,
                OutboxPayloads.applicationStatusChanged(
                        recipientUserId, saved.getId(), job.getTitle(),
                        null, JobApplication.STATUS_SUBMITTED, null));

        return toResponse(saved);
    }

    // ------------------------------------------------------------------
    // Read APIs (ownership-scoped)
    // ------------------------------------------------------------------

    /** Lists only the given candidate's applications, newest first. */
    @Transactional(readOnly = true)
    public PageResponse<CandidateApplicationResponse> listOwn(
            long candidateId, PageParams pageParams) {
        Page<JobApplication> page = applicationRepository
                .findByCandidateIdOrderByCreatedAtDesc(candidateId,
                        PageRequest.of(pageParams.getPage(), pageParams.getSize()));
        return PageResponse.of(page.map(this::toResponse));
    }

    /**
     * Fetches one application ONLY when it belongs to the given candidate.
     * A foreign application is the same plain 404 as a nonexistent one —
     * no existence leak (A7.6 masking convention).
     */
    @Transactional(readOnly = true)
    public CandidateApplicationResponse getOwn(long candidateId, long applicationId) {
        JobApplication app = applicationRepository
                .findByIdAndCandidateId(applicationId, candidateId)
                .orElseThrow(() -> new NotFoundException(
                        "Application not found: " + applicationId));
        return toResponse(app);
    }

    // ------------------------------------------------------------------
    // Withdrawal (A11)
    // ------------------------------------------------------------------

    /**
     * Withdraws one of the given candidate's OWN applications (A11).
     *
     * <p>Ownership is enforced FIRST via the ownership-scoped lookup — a
     * foreign or unknown application id produces the identical plain 404,
     * so the endpoint never reveals whether someone else's application
     * exists. The transition itself is delegated to the existing A7.3
     * {@link ApplicationService#withdraw}: the WITHDRAWN status, the
     * decided/already-withdrawn 409 rules, the A7.4 history row, the audit
     * record and the outbox event are that method's existing behavior —
     * nothing here duplicates the workflow. The response is re-read through
     * the candidate-safe DTO, so the admin-only fields of
     * {@code ApplicationResponse} never cross this boundary.</p>
     */
    @Transactional
    public CandidateApplicationResponse withdrawOwn(long candidateId, long applicationId) {
        // 1. Ownership gate (masked 404 for foreign/unknown ids).
        JobApplication own = applicationRepository
                .findByIdAndCandidateId(applicationId, candidateId)
                .orElseThrow(() -> new NotFoundException(
                        "Application not found: " + applicationId));

        // 2. Delegate to the existing workflow (same transaction).
        applicationService.withdraw(own.getId());

        // 3. Candidate-facing response via the safe DTO.
        return getOwn(candidateId, applicationId);
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private CandidateApplicationResponse toResponse(JobApplication app) {
        var job = app.getJob();
        return CandidateApplicationResponse.builder()
                .id(app.getId())
                .job(CandidateApplicationResponse.JobSummary.builder()
                        .id(job.getId())
                        .title(job.getTitle())
                        .slug(job.getSlug())
                        .build())
                .resumeId(app.getResumeId())
                .coverNote(app.getCoverNote())
                .status(app.getStatus())
                .appliedAt(app.getAppliedAt())
                .decidedAt(app.getDecidedAt())
                .createdAt(app.getCreatedAt())
                .updatedAt(app.getUpdatedAt())
                .build();
    }
}
