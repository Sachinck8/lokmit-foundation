package com.lokmit.foundation.employment.application.history.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.OffsetDateTime;

/**
 * Safe status-history record. The actor is exposed only as an opaque
 * database user id (nullable) — no User entity, email, or security
 * material is ever serialized.
 */
@Getter
public class ApplicationStatusHistoryResponse {

    private final Long id;
    private final Long applicationId;
    private final String previousStatus;
    private final String newStatus;
    private final Long changedBy;
    private final OffsetDateTime changedAt;
    private final String note;

    private ApplicationStatusHistoryResponse(Builder b) {
        this.id = b.id;
        this.applicationId = b.applicationId;
        this.previousStatus = b.previousStatus;
        this.newStatus = b.newStatus;
        this.changedBy = b.changedBy;
        this.changedAt = b.changedAt;
        this.note = b.note;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Long applicationId;
        private String previousStatus;
        private String newStatus;
        private Long changedBy;
        private OffsetDateTime changedAt;
        private String note;

        public Builder id(Long v) { this.id = v; return this; }
        public Builder applicationId(Long v) { this.applicationId = v; return this; }
        public Builder previousStatus(String v) { this.previousStatus = v; return this; }
        public Builder newStatus(String v) { this.newStatus = v; return this; }
        public Builder changedBy(Long v) { this.changedBy = v; return this; }
        public Builder changedAt(OffsetDateTime v) { this.changedAt = v; return this; }
        public Builder note(String v) { this.note = v; return this; }
        public ApplicationStatusHistoryResponse build() {
            return new ApplicationStatusHistoryResponse(this);
        }
    }
}
