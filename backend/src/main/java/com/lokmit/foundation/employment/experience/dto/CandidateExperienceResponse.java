package com.lokmit.foundation.employment.experience.dto;

import com.lokmit.foundation.employment.experience.entity.CandidateExperience;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Candidate-safe experience record. No candidate internals, no audit data —
 * the candidate identity is implicit (the caller's own).
 */
@Getter
public class CandidateExperienceResponse {

    private final Long id;
    private final String companyName;
    private final String jobTitle;
    private final String description;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final OffsetDateTime createdAt;

    private CandidateExperienceResponse(Builder b) {
        this.id = b.id;
        this.companyName = b.companyName;
        this.jobTitle = b.jobTitle;
        this.description = b.description;
        this.startDate = b.startDate;
        this.endDate = b.endDate;
        this.createdAt = b.createdAt;
    }

    public static Builder builder() { return new Builder(); }

    public static CandidateExperienceResponse from(CandidateExperience e) {
        return builder()
                .id(e.getId())
                .companyName(e.getCompanyName())
                .jobTitle(e.getJobTitle())
                .description(e.getDescription())
                .startDate(e.getStartDate())
                .endDate(e.getEndDate())
                .createdAt(e.getCreatedAt())
                .build();
    }

    public static class Builder {
        private Long id;
        private String companyName;
        private String jobTitle;
        private String description;
        private LocalDate startDate;
        private LocalDate endDate;
        private OffsetDateTime createdAt;

        public Builder id(Long v) { this.id = v; return this; }
        public Builder companyName(String v) { this.companyName = v; return this; }
        public Builder jobTitle(String v) { this.jobTitle = v; return this; }
        public Builder description(String v) { this.description = v; return this; }
        public Builder startDate(LocalDate v) { this.startDate = v; return this; }
        public Builder endDate(LocalDate v) { this.endDate = v; return this; }
        public Builder createdAt(OffsetDateTime v) { this.createdAt = v; return this; }
        public CandidateExperienceResponse build() {
            return new CandidateExperienceResponse(this);
        }
    }
}
