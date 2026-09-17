package com.lokmit.foundation.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

/**
 * One contact enquiry as shown in the Admin Dashboard's recent-activity list
 * (A2). A deliberately minimal view — the full management payload lives in
 * the contact enquiry API ({@code messages:manage}); this projection exposes
 * only what a dashboard row needs and never includes the message body or
 * internal notes.
 */
@Getter
@Builder
@Schema(description = "Recent contact enquiry row for the dashboard")
public class DashboardEnquiryResponse {

    @Schema(description = "Enquiry identifier", example = "42")
    private Long id;

    @Schema(description = "Sender's full name", example = "Ramesh Kumar")
    private String name;

    @Schema(description = "Sender's email address", example = "ramesh@example.com")
    private String email;

    @Schema(description = "Enquiry subject", example = "Skill development programme enquiry")
    private String subject;

    @Schema(description = "Lifecycle status", example = "NEW",
            allowableValues = {"NEW", "READ", "REPLIED", "ARCHIVED"})
    private String status;

    @Schema(description = "When the enquiry was received")
    private java.time.OffsetDateTime createdAt;
}
