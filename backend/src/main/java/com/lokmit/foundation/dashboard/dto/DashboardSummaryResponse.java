package com.lokmit.foundation.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

/**
 * High-level platform counts for the Admin Dashboard summary view (A2).
 *
 * <p>Every value is aggregated in the database (COUNT queries against the
 * existing domain tables); no entity rows are loaded into memory. Metrics
 * that cannot be derived from the actual schema constraints are deliberately
 * omitted rather than guessed:</p>
 * <ul>
 *   <li>Jobs have no SCHEDULED or ACTIVE lifecycle state (V8:
 *       {@code chk_jobs_status IN ('DRAFT','PUBLISHED','CLOSED','ARCHIVED')}),
 *       so {@code scheduledJobs}/{@code activeJobs} do not exist.</li>
 *   <li>Users expose {@code inactiveUsers} as the count of every non-ACTIVE
 *       account (LOCKED + SUSPENDED + DELETED), matching the A1 fail-closed
 *       status model.</li>
 * </ul>
 */
@Getter
@Builder
@Schema(description = "Admin dashboard high-level counts")
public class DashboardSummaryResponse {

    // ---- Identity (users; V2 chk_users_status) ----

    @Schema(description = "All user accounts regardless of status", example = "128")
    private long totalUsers;

    @Schema(description = "Accounts with status ACTIVE", example = "121")
    private long activeUsers;

    @Schema(description = "Accounts not ACTIVE (LOCKED + SUSPENDED + DELETED)", example = "7")
    private long inactiveUsers;

    // ---- Employment (V8 tables) ----

    @Schema(description = "Candidate profiles", example = "96")
    private long totalCandidates;

    @Schema(description = "Employer profiles", example = "24")
    private long totalEmployers;

    @Schema(description = "Job postings regardless of lifecycle status", example = "57")
    private long totalJobs;

    @Schema(description = "Jobs with status PUBLISHED", example = "31")
    private long publishedJobs;

    @Schema(description = "Jobs with status DRAFT", example = "12")
    private long draftJobs;

    @Schema(description = "Jobs with status CLOSED", example = "9")
    private long closedJobs;

    @Schema(description = "Jobs with status ARCHIVED", example = "5")
    private long archivedJobs;

    @Schema(description = "Job applications regardless of status", example = "143")
    private long totalApplications;

    // ---- Communication (contact_messages; V7 chk_contact_messages_status) ----

    @Schema(description = "Enquiries with status NEW (not yet reviewed)", example = "11")
    private long newContactEnquiries;

    @Schema(description = "Enquiries with status READ", example = "4")
    private long readContactEnquiries;

    @Schema(description = "Enquiries with status REPLIED", example = "9")
    private long repliedContactEnquiries;

    @Schema(description = "Enquiries with status ARCHIVED", example = "2")
    private long archivedContactEnquiries;
}
