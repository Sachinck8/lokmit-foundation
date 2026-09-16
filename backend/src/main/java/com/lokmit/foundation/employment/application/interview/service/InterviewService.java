package com.lokmit.foundation.employment.application.interview.service;

import com.lokmit.foundation.common.exception.BadRequestException;
import com.lokmit.foundation.common.exception.ConflictException;
import com.lokmit.foundation.common.exception.NotFoundException;
import com.lokmit.foundation.common.pagination.PageParams;
import com.lokmit.foundation.common.api.PageResponse;
import com.lokmit.foundation.employment.application.entity.JobApplication;
import com.lokmit.foundation.employment.application.interview.dto.InterviewCreateRequest;
import com.lokmit.foundation.employment.application.interview.dto.InterviewResponse;
import com.lokmit.foundation.employment.application.interview.dto.InterviewUpdateRequest;
import com.lokmit.foundation.employment.application.interview.entity.Interview;
import com.lokmit.foundation.employment.application.interview.repository.InterviewRepository;
import com.lokmit.foundation.employment.application.repository.JobApplicationRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * Interview scheduling and management (A7.4) on the V15 'interviews' table.
 *
 * <p>Design rules:</p>
 * <ul>
 *   <li>All operations are scoped to one application: a mismatched
 *       (applicationId, interviewId) pair is a plain 404 — no existence
 *       leak about another application's interviews.</li>
 *   <li>Terminal applications (HIRED/REJECTED/WITHDRAWN) reject new
 *       interviews (400) — interviews are only scheduled while the review
 *       is still live; existing ones stay readable.</li>
 *   <li>New interviews always start SCHEDULED; the client cannot set status
 *       at creation. PATCH validates merged transitions conservatively:
 *       COMPLETED/NO_SHOW are terminal, CANCELLED is final, no transition
 *       out of any of them.</li>
 *   <li>DELETE is allowed only for CANCELLED interviews (409 otherwise) —
 *       scheduled/completed/no-show records are part of the scheduling
 *       record and are never silently destroyed. Deleting an interview can
 *       never delete the application (V15 FK is application → interviews
 *       ownership, not the reverse).</li>
 * </ul>
 */
@Service
public class InterviewService {

    private final InterviewRepository interviewRepository;
    private final JobApplicationRepository applicationRepository;

    public InterviewService(InterviewRepository interviewRepository,
                            JobApplicationRepository applicationRepository) {
        this.interviewRepository = interviewRepository;
        this.applicationRepository = applicationRepository;
    }

    // ------------------------------------------------------------------
    // Queries
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public PageResponse<InterviewResponse> listInterviews(long applicationId,
                                                          PageParams pageParams) {
        requireApplication(applicationId);
        var page = interviewRepository
                .findByApplicationIdOrderByScheduledAtDesc(applicationId,
                        PageRequest.of(pageParams.getPage(), pageParams.getSize()))
                .map(this::toResponse);
        return PageResponse.of(page);
    }

    @Transactional(readOnly = true)
    public InterviewResponse getInterview(long applicationId, long interviewId) {
        requireApplication(applicationId);
        Interview interview = interviewRepository
                .findByIdAndApplicationId(interviewId, applicationId)
                .orElseThrow(() -> new NotFoundException("Interview not found: " + interviewId));
        return toResponse(interview);
    }

    // ------------------------------------------------------------------
    // Mutations
    // ------------------------------------------------------------------

    @Transactional
    public InterviewResponse createInterview(long applicationId,
                                             InterviewCreateRequest request) {
        JobApplication app = requireNonTerminalApplication(applicationId, "schedule an interview");
        Interview interview = new Interview();
        interview.setApplication(app);
        interview.setScheduledAt(request.getScheduledAt());
        interview.setMode(request.getMode());
        interview.setLocation(request.getLocation());
        interview.setStatus(Interview.STATUS_SCHEDULED);
        interview.setNotes(request.getNotes());
        interview.setCreatedAt(OffsetDateTime.now());
        interview.setUpdatedAt(OffsetDateTime.now());
        return toResponse(interviewRepository.save(interview));
    }

    @Transactional
    public InterviewResponse updateInterview(long applicationId,
                                             long interviewId,
                                             InterviewUpdateRequest request) {
        requireApplication(applicationId);
        Interview interview = interviewRepository
                .findByIdAndApplicationId(interviewId, applicationId)
                .orElseThrow(() -> new NotFoundException("Interview not found: " + interviewId));

        if (request.getScheduledAt() != null) {
            interview.setScheduledAt(request.getScheduledAt());
        }
        if (request.getMode() != null) {
            interview.setMode(request.getMode());
        }
        if (request.getLocation() != null) {
            interview.setLocation(request.getLocation());
        }
        if (request.getNotes() != null) {
            interview.setNotes(request.getNotes());
        }
        if (request.getStatus() != null
                && !request.getStatus().equals(interview.getStatus())) {
            validateTransition(interview.getStatus(), request.getStatus());
            interview.setStatus(request.getStatus());
        }
        interview.setUpdatedAt(OffsetDateTime.now());
        return toResponse(interviewRepository.save(interview));
    }

    /**
     * Deletes an interview, but only if it is CANCELLED — see the class
     * javadoc. Never touches the owning application.
     */
    @Transactional
    public void deleteInterview(long applicationId, long interviewId) {
        requireApplication(applicationId);
        Interview interview = interviewRepository
                .findByIdAndApplicationId(interviewId, applicationId)
                .orElseThrow(() -> new NotFoundException("Interview not found: " + interviewId));
        if (!Interview.STATUS_CANCELLED.equals(interview.getStatus())) {
            throw new ConflictException(
                    "Only CANCELLED interviews can be deleted (current status: "
                            + interview.getStatus() + "); cancel it first");
        }
        interviewRepository.delete(interview);
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private JobApplication requireApplication(long applicationId) {
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new NotFoundException(
                        "Application not found: " + applicationId));
    }

    private JobApplication requireNonTerminalApplication(long applicationId,
                                                         String action) {
        JobApplication app = requireApplication(applicationId);
        String status = app.getStatus();
        if (JobApplication.STATUS_HIRED.equals(status)
                || JobApplication.STATUS_REJECTED.equals(status)
                || JobApplication.STATUS_WITHDRAWN.equals(status)) {
            throw new BadRequestException(
                    "Application is terminal (" + status + "); cannot " + action);
        }
        return app;
    }

    /** Conservative interview lifecycle: the three closed states are final. */
    private void validateTransition(String current, String target) {
        if (Interview.STATUS_COMPLETED.equals(current)
                || Interview.STATUS_NO_SHOW.equals(current)
                || Interview.STATUS_CANCELLED.equals(current)) {
            throw new ConflictException(
                    "Interview is " + current + " and can no longer change status");
        }
        // SCHEDULED → any of the three outcomes is allowed.
    }

    private InterviewResponse toResponse(Interview i) {
        return InterviewResponse.builder()
                .id(i.getId())
                .applicationId(i.getApplication().getId())
                .scheduledAt(i.getScheduledAt())
                .mode(i.getMode())
                .location(i.getLocation())
                .status(i.getStatus())
                .notes(i.getNotes())
                .createdAt(i.getCreatedAt())
                .updatedAt(i.getUpdatedAt())
                .build();
    }
}
