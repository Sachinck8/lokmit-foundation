package com.lokmit.foundation.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

/**
 * One job application as shown in the Admin Dashboard's recent-activity list
 * (A2). Built from a single joined projection (no N+1): the candidate's name
 * comes from {@code users} via {@code candidates.user_id} and the job title
 * from {@code jobs}, matching the V8 schema relationships exactly.
 */
@Getter
@Builder
@Schema(description = "Recent job application row for the dashboard")
public class DashboardApplicationResponse {

    @Schema(description = "Application identifier", example = "314")
    private Long id;

    @Schema(description = "Application lifecycle status", example = "SUBMITTED",
            allowableValues = {"SUBMITTED", "UNDER_REVIEW", "SHORTLISTED", "HIRED", "REJECTED", "WITHDRAWN"})
    private String status;

    @Schema(description = "When the candidate applied")
    private java.time.OffsetDateTime appliedAt;

    @Schema(description = "Candidate profile identifier", example = "27")
    private Long candidateId;

    @Schema(description = "Candidate display name", example = "Priya Sharma")
    private String candidateName;

    @Schema(description = "Job posting identifier", example = "9")
    private Long jobId;

    @Schema(description = "Job posting title", example = "Field Programme Coordinator")
    private String jobTitle;
}
