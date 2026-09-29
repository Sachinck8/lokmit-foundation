package com.lokmit.foundation.employment.application.history.repository;

import com.lokmit.foundation.employment.application.history.entity.ApplicationStatusHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for the V15 'application_status_history' table. */
public interface ApplicationStatusHistoryRepository
        extends JpaRepository<ApplicationStatusHistory, Long> {

    /**
     * History for one application, newest transition first — DB-side
     * pagination via the Pageable; backed by
     * idx_application_status_history_application.
     */
    Page<ApplicationStatusHistory> findByApplicationIdOrderByChangedAtDesc(
            Long applicationId, Pageable pageable);
}
