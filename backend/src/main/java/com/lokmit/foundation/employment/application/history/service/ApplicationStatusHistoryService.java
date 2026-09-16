package com.lokmit.foundation.employment.application.history.service;

import com.lokmit.foundation.common.exception.NotFoundException;
import com.lokmit.foundation.common.pagination.PageParams;
import com.lokmit.foundation.common.api.PageResponse;
import com.lokmit.foundation.employment.application.history.dto.ApplicationStatusHistoryResponse;
import com.lokmit.foundation.employment.application.history.entity.ApplicationStatusHistory;
import com.lokmit.foundation.employment.application.history.repository.ApplicationStatusHistoryRepository;
import com.lokmit.foundation.employment.application.repository.JobApplicationRepository;
import com.lokmit.foundation.security.util.SecurityUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * Application status history (A7.4). {@link #recordTransition} is called by
 * the A7.3 lifecycle transitions INSIDE their transaction — a failed history
 * insert rolls back the status change. The actor id comes exclusively from
 * the authenticated DB-backed principal (SecurityUtils); it is never
 * accepted from request data.
 */
@Service
public class ApplicationStatusHistoryService {

    private final ApplicationStatusHistoryRepository historyRepository;
    private final JobApplicationRepository applicationRepository;
    private final SecurityUtils securityUtils;

    public ApplicationStatusHistoryService(
            ApplicationStatusHistoryRepository historyRepository,
            JobApplicationRepository applicationRepository,
            SecurityUtils securityUtils) {
        this.historyRepository = historyRepository;
        this.applicationRepository = applicationRepository;
        this.securityUtils = securityUtils;
    }

    /**
     * Records one status transition for the given application. Runs in the
     * caller's transaction (no @Transactional of its own beyond joining the
     * existing one) so the status update and this insert commit or roll
     * back atomically.
     */
    public void recordTransition(Long applicationId,
                                 String previousStatus,
                                 String newStatus,
                                 String note) {
        ApplicationStatusHistory record = new ApplicationStatusHistory();
        record.setApplication(applicationRepository.getReferenceById(applicationId));
        record.setPreviousStatus(previousStatus);
        record.setNewStatus(newStatus);
        record.setChangedBy(securityUtils.getCurrentUserId());
        record.setChangedAt(OffsetDateTime.now());
        record.setNote(note);
        historyRepository.save(record);
    }

    @Transactional(readOnly = true)
    public PageResponse<ApplicationStatusHistoryResponse> listHistory(
            long applicationId, PageParams pageParams) {
        if (!applicationRepository.existsById(applicationId)) {
            throw new NotFoundException("Application not found: " + applicationId);
        }
        var page = historyRepository
                .findByApplicationIdOrderByChangedAtDesc(applicationId,
                        PageRequest.of(pageParams.getPage(), pageParams.getSize()))
                .map(this::toResponse);
        return PageResponse.of(page);
    }

    private ApplicationStatusHistoryResponse toResponse(ApplicationStatusHistory h) {
        return ApplicationStatusHistoryResponse.builder()
                .id(h.getId())
                .applicationId(h.getApplication().getId())
                .previousStatus(h.getPreviousStatus())
                .newStatus(h.getNewStatus())
                .changedBy(h.getChangedBy())
                .changedAt(h.getChangedAt())
                .note(h.getNote())
                .build();
    }
}
