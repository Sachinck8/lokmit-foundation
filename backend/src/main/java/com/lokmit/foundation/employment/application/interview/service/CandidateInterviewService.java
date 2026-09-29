package com.lokmit.foundation.employment.application.interview.service;

import com.lokmit.foundation.common.api.PageResponse;
import com.lokmit.foundation.common.exception.NotFoundException;
import com.lokmit.foundation.common.pagination.PageParams;
import com.lokmit.foundation.employment.application.entity.JobApplication;
import com.lokmit.foundation.employment.application.interview.dto.CandidateInterviewResponse;
import com.lokmit.foundation.employment.application.interview.entity.Interview;
import com.lokmit.foundation.employment.application.interview.repository.InterviewRepository;
import com.lokmit.foundation.employment.application.repository.JobApplicationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Candidate interview visibility (A14) — read-only candidate view of the
 * interviews associated with the candidate's OWN applications. Reuses the
 * existing V15 {@code interviews} table, the existing
 * {@link InterviewRepository} reads and the A7.4 scheduling semantics —
 * nothing about interview creation, editing or cancellation changes; the
 * admin {@code InterviewController} is untouched.
 *
 * <p>Security invariants: the candidate arrives from the controller,
 * resolved server-side from the JWT user id via the A7.6.3 ownership
 * service. Ownership flows Interview → application → candidate (V15 has no
 * direct candidateId on interviews), enforced at query level — a foreign or
 * unknown interview id is the same plain 404 (no existence leak). The
 * response uses the candidate-safe {@link CandidateInterviewResponse}.</p>
 */
@Service
public class CandidateInterviewService {

    private final InterviewRepository interviewRepository;
    private final JobApplicationRepository applicationRepository;

    public CandidateInterviewService(InterviewRepository interviewRepository,
                                     JobApplicationRepository applicationRepository) {
        this.interviewRepository = interviewRepository;
        this.applicationRepository = applicationRepository;
    }

    /** Lists one application's interviews, soonest/newest scheduledAt first. */
    @Transactional(readOnly = true)
    public PageResponse<CandidateInterviewResponse> listOwnForApplication(
            long candidateId, long applicationId, PageParams pageParams) {
        // Ownership gate: only the candidate's own application may be read.
        requireOwnApplication(candidateId, applicationId);
        Page<CandidateInterviewResponse> page = interviewRepository
                .findByApplicationIdOrderByScheduledAtDesc(applicationId,
                        PageRequest.of(pageParams.getPage(), pageParams.getSize()))
                .map(this::toResponse);
        return PageResponse.of(page);
    }

    /**
     * Fetches one interview ONLY when it belongs to one of the candidate's
     * own applications. Foreign/unknown ids are the identical masked 404.
     */
    @Transactional(readOnly = true)
    public CandidateInterviewResponse getOwn(long candidateId, long interviewId) {
        Interview interview = interviewRepository.findById(interviewId)
                .filter(i -> i.getApplication() != null
                        && i.getApplication().getCandidate() != null
                        && i.getApplication().getCandidate().getId() != null
                        && candidateId == i.getApplication().getCandidate().getId())
                .orElseThrow(() -> new NotFoundException(
                        "Interview not found: " + interviewId));
        return toResponse(interview);
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private JobApplication requireOwnApplication(long candidateId, long applicationId) {
        JobApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new NotFoundException(
                        "Application not found: " + applicationId));
        if (app.getCandidate() == null
                || app.getCandidate().getId() == null
                || candidateId != app.getCandidate().getId()) {
            // Same message as unknown — no existence leak.
            throw new NotFoundException("Application not found: " + applicationId);
            }
        return app;
    }

    private CandidateInterviewResponse toResponse(Interview i) {
        return CandidateInterviewResponse.builder()
                .id(i.getId())
                .applicationId(i.getApplication() != null
                        ? i.getApplication().getId() : null)
                .scheduledAt(i.getScheduledAt())
                .mode(i.getMode())
                .location(i.getLocation())
                .status(i.getStatus())
                .notes(i.getNotes())
                .createdAt(i.getCreatedAt())
                .build();
    }
}
