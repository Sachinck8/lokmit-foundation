package com.lokmit.foundation.employment.application.interview.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.OffsetDateTime;

/**
 * Candidate-safe interview record (A14). Distinct from the admin
 * {@code InterviewResponse}: drops the update-timestamp bookkeeping and
 * exposes only what the candidate needs — the schedule, mode, location,
 * status, the scheduling notes the hiring team wrote for them, and their
 * own application's id. No admin internals, no other candidates' data.
 */
@Getter
public class CandidateInterviewResponse {

    private final Long id;
    private final Long applicationId;
    private final OffsetDateTime scheduledAt;
    private final String mode;
    private final String location;
    private final String status;
    private final String notes;
    private final OffsetDateTime createdAt;

    private CandidateInterviewResponse(Builder b) {
        this.id = b.id;
        this.applicationId = b.applicationId;
        this.scheduledAt = b.scheduledAt;
        this.mode = b.mode;
        this.location = b.location;
        this.status = b.status;
        this.notes = b.notes;
        this.createdAt = b.createdAt;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Long applicationId;
        private OffsetDateTime scheduledAt;
        private String mode;
        private String location;
        private String status;
        private String notes;
        private OffsetDateTime createdAt;

        public Builder id(Long v) { this.id = v; return this; }
        public Builder applicationId(Long v) { this.applicationId = v; return this; }
        public Builder scheduledAt(OffsetDateTime v) { this.scheduledAt = v; return this; }
        public Builder mode(String v) { this.mode = v; return this; }
        public Builder location(String v) { this.location = v; return this; }
        public Builder status(String v) { this.status = v; return this; }
        public Builder notes(String v) { this.notes = v; return this; }
        public Builder createdAt(OffsetDateTime v) { this.createdAt = v; return this; }
        public CandidateInterviewResponse build() {
            return new CandidateInterviewResponse(this);
        }
    }
}
