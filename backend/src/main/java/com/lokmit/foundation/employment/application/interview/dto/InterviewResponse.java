package com.lokmit.foundation.employment.application.interview.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.OffsetDateTime;

/**
 * Safe interview response. Contains only schema-backed scheduling data —
 * no user, candidate or employer identity beyond the owning application id,
 * and no security material of any kind.
 */
@Schema(description = "Interview scheduled for an application.")
@Getter
public class InterviewResponse {

    private final Long id;
    private final Long applicationId;
    private final OffsetDateTime scheduledAt;
    private final String mode;
    private final String location;
    private final String status;
    private final String notes;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;

    private InterviewResponse(Builder b) {
        this.id = b.id;
        this.applicationId = b.applicationId;
        this.scheduledAt = b.scheduledAt;
        this.mode = b.mode;
        this.location = b.location;
        this.status = b.status;
        this.notes = b.notes;
        this.createdAt = b.createdAt;
        this.updatedAt = b.updatedAt;
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
        private OffsetDateTime updatedAt;

        public Builder id(Long v) { this.id = v; return this; }
        public Builder applicationId(Long v) { this.applicationId = v; return this; }
        public Builder scheduledAt(OffsetDateTime v) { this.scheduledAt = v; return this; }
        public Builder mode(String v) { this.mode = v; return this; }
        public Builder location(String v) { this.location = v; return this; }
        public Builder status(String v) { this.status = v; return this; }
        public Builder notes(String v) { this.notes = v; return this; }
        public Builder createdAt(OffsetDateTime v) { this.createdAt = v; return this; }
        public Builder updatedAt(OffsetDateTime v) { this.updatedAt = v; return this; }
        public InterviewResponse build() {
            return new InterviewResponse(this);
        }
    }
}
