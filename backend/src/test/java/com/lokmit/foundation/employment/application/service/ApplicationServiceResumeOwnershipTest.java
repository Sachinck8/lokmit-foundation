package com.lokmit.foundation.employment.application.service;

import com.lokmit.foundation.audit.service.AuditLogService;
import com.lokmit.foundation.common.exception.NotFoundException;
import com.lokmit.foundation.employment.application.dto.ApplicationReviewRequest;
import com.lokmit.foundation.employment.application.entity.JobApplication;
import com.lokmit.foundation.employment.application.history.service.ApplicationStatusHistoryService;
import com.lokmit.foundation.employment.application.repository.JobApplicationRepository;
import com.lokmit.foundation.employment.candidate.entity.Candidate;
import com.lokmit.foundation.employment.employer.entity.Employer;
import com.lokmit.foundation.employment.job.entity.Job;
import com.lokmit.foundation.employment.resume.entity.Resume;
import com.lokmit.foundation.employment.resume.repository.ResumeRepository;
import com.lokmit.foundation.outbox.service.OutboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * A7.6.6 item 2: ApplicationService.updateReview resume-ownership
 * cross-check regression tests (own-candidate allowed, foreign-candidate
 * rejected, nonexistent 404, null resumeId unchanged).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ApplicationServiceResumeOwnershipTest {

    @Mock
    private JobApplicationRepository applicationRepository;
    @Mock
    private ApplicationStatusHistoryService historyService;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private OutboxService outboxService;
    @Mock
    private ResumeRepository resumeRepository;

    private ApplicationService service;

    @BeforeEach
    void setUp() {
        service = new ApplicationService(applicationRepository, historyService,
                auditLogService, outboxService, resumeRepository);

        JobApplication app = app(30L, 20L);
        when(applicationRepository.findById(30L)).thenReturn(Optional.of(app));
        when(applicationRepository.save(any(JobApplication.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    private static Candidate candidate(long id) {
        Candidate c = new Candidate();
        c.setId(id);
        c.setAvailabilityStatus(Candidate.AVAILABILITY_ACTIVELY_LOOKING);
        c.setCreatedAt(OffsetDateTime.now());
        c.setUpdatedAt(OffsetDateTime.now());
        return c;
    }

    /**
     * Minimal job/employer graph so updateReview's response mapping
     * (toResponse reads job.title/slug/status and employer.companyName)
     * never hits a null association.
     */
    private static Job job() {
        Employer employer = new Employer();
        employer.setId(7L);
        employer.setCompanyName("Acme Pvt Ltd");
        Job job = new Job();
        job.setId(3L);
        job.setEmployer(employer);
        job.setTitle("Backend Engineer");
        job.setSlug("backend-engineer");
        job.setStatus(Job.STATUS_PUBLISHED);
        return job;
    }

    private static JobApplication app(long id, long candidateId) {
        JobApplication app = new JobApplication();
        app.setId(id);
        app.setCandidate(candidate(candidateId));
        app.setJob(job());
        app.setStatus(JobApplication.STATUS_SUBMITTED);
        app.setAppliedAt(OffsetDateTime.now());
        app.setCreatedAt(OffsetDateTime.now());
        app.setUpdatedAt(OffsetDateTime.now());
        return app;
    }

    private static Resume resume(long id, long candidateId) {
        Resume r = new Resume();
        r.setId(id);
        r.setCandidateId(candidateId);
        r.setFileName("cv.pdf");
        r.setActive(true);
        r.setCreatedAt(OffsetDateTime.now());
        return r;
    }

    private static ApplicationReviewRequest request(Long resumeId) {
        ApplicationReviewRequest r = new ApplicationReviewRequest();
        r.setResumeId(resumeId);
        return r;
    }

    @Test
    @DisplayName("A: application of candidate 20 + resume 5 of candidate 20 → allowed")
    void ownCandidateResumeAllowed() {
        when(resumeRepository.findById(5L)).thenReturn(Optional.of(resume(5L, 20L)));

        service.updateReview(30L, request(5L));

        verify(resumeRepository).findById(5L);
        verify(applicationRepository).save(any(JobApplication.class));
    }

    @Test
    @DisplayName("B: application of candidate 20 + resume 6 of candidate 21 → 404, nothing saved")
    void foreignResumeRejected() {
        when(resumeRepository.findById(6L)).thenReturn(Optional.of(resume(6L, 21L)));

        assertThatThrownBy(() -> service.updateReview(30L, request(6L)))
                .isInstanceOf(NotFoundException.class);

        verify(applicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("C: nonexistent resumeId → 404, nothing saved")
    void nonexistentResume404() {
        when(resumeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateReview(30L, request(99L)))
                .isInstanceOf(NotFoundException.class);

        verify(applicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("D: null resumeId → no resume lookup, existing behavior unchanged")
    void nullResumeIdUnchanged() {
        ApplicationReviewRequest r = new ApplicationReviewRequest();
        r.setEmployerNote("note only");

        service.updateReview(30L, r);

        verifyNoInteractions(resumeRepository);
        verify(applicationRepository).save(any(JobApplication.class));
    }
}
