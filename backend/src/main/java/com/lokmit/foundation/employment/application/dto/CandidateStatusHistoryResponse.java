package com.lokmit.foundation.employment.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.OffsetDateTime;

/**
 * Candidate-safe status-history record (A12). Distinct from the admin
 * {@code ApplicationStatusHistoryResponse}: exposes only the transition
 * vocabulary and timestamps — no actor identity ({@code changedBy}), no
 * database row id, and no free-text note (an internal admin channel).
 * The candidate sees their own timeline, nothing else.
 */
@Getter
public class CandidateStatusHistoryResponse {

    private final String previousStatus;
    private final String newStatus;
    private final OffsetDateTime changedAt;

    private CandidateStatusHistoryResponse(Builder b) {
        this.previousStatus = b.previousStatus;
        this.newStatus = b.newStatus;
        this.changedAt = b.changedAt;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String previousStatus;
        private String newStatus;
        private OffsetDateTime changedAt;

        public Builder previousStatus(String v) { this.previousStatus = v; return this; }
        public Builder newStatus(String v) { this.newStatus = v; return this; }
        public Builder changedAt(OffsetDateTime v) { this.changedAt = v; return this; }
        public CandidateStatusHistoryResponse build() {
            return new CandidateStatusHistoryResponse(this);
        }
    }
}
