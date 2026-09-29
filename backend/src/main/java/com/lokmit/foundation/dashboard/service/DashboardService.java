package com.lokmit.foundation.dashboard.service;

import com.lokmit.foundation.dashboard.dto.DashboardApplicationResponse;
import com.lokmit.foundation.dashboard.dto.DashboardEnquiryResponse;
import com.lokmit.foundation.dashboard.dto.DashboardSummaryResponse;
import com.lokmit.foundation.dashboard.dto.DashboardUserResponse;
import com.lokmit.foundation.dashboard.repository.DashboardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Assembles Admin Dashboard read data (A2).
 *
 * <p>All queries are executed by {@link DashboardRepository} with
 * database-side aggregation; this service only clamps the recent-list limit
 * (default 5, maximum 10) and maps results, so a caller-supplied
 * {@code limit=100000} can never widen the query.</p>
 */
@Service
public class DashboardService {

    /** Default number of rows for recent-activity lists. */
    public static final int DEFAULT_LIMIT = 5;

    /** Hard upper bound for recent-activity lists. */
    public static final int MAX_LIMIT = 10;

    private final DashboardRepository dashboardRepository;

    public DashboardService(DashboardRepository dashboardRepository) {
        this.dashboardRepository = dashboardRepository;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {
        return dashboardRepository.summaryCounts();
    }

    @Transactional(readOnly = true)
    public List<DashboardEnquiryResponse> getRecentEnquiries(Integer limit) {
        return dashboardRepository.recentEnquiries(clampLimit(limit));
    }

    @Transactional(readOnly = true)
    public List<DashboardUserResponse> getRecentUsers(Integer limit) {
        return dashboardRepository.recentUsers(clampLimit(limit));
    }

    @Transactional(readOnly = true)
    public List<DashboardApplicationResponse> getRecentApplications(Integer limit) {
        return dashboardRepository.recentApplications(clampLimit(limit));
    }

    /**
     * Clamps the requested limit into [1, 10]: null/0/negative → default 5,
     * above 10 → 10. Deliberately lenient (clamping rather than rejecting)
     * so a dashboard widget can never trigger a large query while keeping
     * the API forgiving for UI defaults.
     */
    private int clampLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}
