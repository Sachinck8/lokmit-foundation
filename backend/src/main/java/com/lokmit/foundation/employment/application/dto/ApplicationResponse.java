package com.lokmit.foundation.employment.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.OffsetDateTime;

/**
 * Safe administrative application representation. Related data is embedded
 * as safe summaries only — the employer exposes only company identity, the
 * job only posting identity. The candidate summary (A18) additionally
 * carries the linked user's review-relevant identity (full name + contact
 * email) so an administrator can actually reach the applicant; no other
 * user material (password hash, lockout/token/audit internals) is exposed.
 */
@Getter
public class ApplicationResponse {

    /** Safe job summary embedded in application responses. */
    @Getter
    public static class JobSummary {
        private final Long id;
        private final String title;
        private final String slug;
        private final String status;

        public JobSummary(Long id, String title, String slug, String status) {
            this.id = id;
            this.title = title;
            this.slug = slug;
            this.status = status;
        }
    }

    /**
     * Safe candidate summary. A18 review enrichment: candidateName and
     * candidateEmail come from the candidate's linked user identity and are
     * limited to exactly what an application reviewer needs; summary and
     * expected-salary are plain candidate columns (same class as
     * phone/location). No security material (password hash, lockout, token
     * or audit fields) is exposed.
     */
    @Getter
    public static class CandidateSummary {
        private final Long id;
        private final String candidateName;
        private final String candidateEmail;
        private final String phone;
        private final String currentLocation;
        private final String summary;
        private final java.math.BigDecimal expectedSalaryMin;
        private final java.math.BigDecimal expectedSalaryMax;
        private final String availability;

        public CandidateSummary(Long id, String candidateName, String candidateEmail,
                                String phone, String currentLocation, String summary,
                                java.math.BigDecimal expectedSalaryMin,
                                java.math.BigDecimal expectedSalaryMax,
                                String availability) {
            this.id = id;
            this.candidateName = candidateName;
            this.candidateEmail = candidateEmail;
            this.phone = phone;
            this.currentLocation = currentLocation;
            this.summary = summary;
            this.expectedSalaryMin = expectedSalaryMin;
            this.expectedSalaryMax = expectedSalaryMax;
            this.availability = availability;
        }
    }

    /** Safe employer summary (via the job's owning employer). */
    @Getter
    public static class EmployerSummary {
        private final Long id;
        private final String companyName;

        public EmployerSummary(Long id, String companyName) {
            this.id = id;
            this.companyName = companyName;
        }
    }

    private final Long id;
    private final JobSummary job;
    private final CandidateSummary candidate;
    private final EmployerSummary employer;
    private final Long resumeId;
    private final String coverNote;
    private final String status;
    private final String employerNote;
    private final OffsetDateTime appliedAt;
    private final OffsetDateTime decidedAt;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;

    private ApplicationResponse(Builder b) {
        this.id = b.id;
        this.job = b.job;
        this.candidate = b.candidate;
        this.employer = b.employer;
        this.resumeId = b.resumeId;
        this.coverNote = b.coverNote;
        this.status = b.status;
        this.employerNote = b.employerNote;
        this.appliedAt = b.appliedAt;
        this.decidedAt = b.decidedAt;
        this.createdAt = b.createdAt;
        this.updatedAt = b.updatedAt;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private JobSummary job;
        private CandidateSummary candidate;
        private EmployerSummary employer;
        private Long resumeId;
        private String coverNote;
        private String status;
        private String employerNote;
        private OffsetDateTime appliedAt;
        private OffsetDateTime decidedAt;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;

        public Builder id(Long v) { this.id = v; return this; }
        public Builder job(JobSummary v) { this.job = v; return this; }
        public Builder candidate(CandidateSummary v) { this.candidate = v; return this; }
        public Builder employer(EmployerSummary v) { this.employer = v; return this; }
        public Builder resumeId(Long v) { this.resumeId = v; return this; }
        public Builder coverNote(String v) { this.coverNote = v; return this; }
        public Builder status(String v) { this.status = v; return this; }
        public Builder employerNote(String v) { this.employerNote = v; return this; }
        public Builder appliedAt(OffsetDateTime v) { this.appliedAt = v; return this; }
        public Builder decidedAt(OffsetDateTime v) { this.decidedAt = v; return this; }
        public Builder createdAt(OffsetDateTime v) { this.createdAt = v; return this; }
        public Builder updatedAt(OffsetDateTime v) { this.updatedAt = v; return this; }
        public ApplicationResponse build() { return new ApplicationResponse(this); }
    }
}
