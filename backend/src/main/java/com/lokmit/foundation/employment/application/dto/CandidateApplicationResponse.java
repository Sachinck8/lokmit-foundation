package com.lokmit.foundation.employment.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

/**
 * Safe candidate-facing application representation (A9). The candidate sees
 * ONLY their own applications, so no candidate or employer internals are
 * needed — the job is summarized by public identity (id/title/slug), the
 * selected resume by id only (never fileUrl/storageKey/checksum), and the
 * admin-only {@code employerNote} is deliberately omitted. Never the entity.
 */
@Getter
@Builder
public class CandidateApplicationResponse {

    /** Public job identity attached to the application. */
    @Getter
    @Builder
    public static class JobSummary {
        private final Long id;
        private final String title;
        private final String slug;
    }

    private final Long id;
    private final JobSummary job;
    private final Long resumeId;
    private final String coverNote;
    private final String status;
    private final OffsetDateTime appliedAt;
    private final OffsetDateTime decidedAt;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;
}
