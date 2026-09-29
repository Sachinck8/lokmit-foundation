package com.lokmit.foundation.employment.candidate.dto;

import com.lokmit.foundation.employment.candidate.entity.Candidate;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Safe candidate profile representation. Exposes the linked user only as an
 * opaque id — never email, password hash, lockout or token material.
 */
@Getter
public class CandidateResponse {

    private final Long id;
    private final Long userId;
    private final LocalDate dateOfBirth;
    private final String gender;
    private final String phone;
    private final String currentLocation;
    private final String summary;
    private final BigDecimal expectedSalaryMin;
    private final BigDecimal expectedSalaryMax;
    private final String availability;
    private final java.time.OffsetDateTime createdAt;
    private final java.time.OffsetDateTime updatedAt;

    private CandidateResponse(Builder b) {
        this.id = b.id;
        this.userId = b.userId;
        this.dateOfBirth = b.dateOfBirth;
        this.gender = b.gender;
        this.phone = b.phone;
        this.currentLocation = b.currentLocation;
        this.summary = b.summary;
        this.expectedSalaryMin = b.expectedSalaryMin;
        this.expectedSalaryMax = b.expectedSalaryMax;
        this.availability = b.availability;
        this.createdAt = b.createdAt;
        this.updatedAt = b.updatedAt;
    }

    public static Builder builder() { return new Builder(); }

    public static CandidateResponse from(Candidate c) {
        return builder()
                .id(c.getId())
                .userId(c.getUser().getId())
                .dateOfBirth(c.getDateOfBirth())
                .gender(c.getGender())
                .phone(c.getPhone())
                .currentLocation(c.getCurrentLocation())
                .summary(c.getSummary())
                .expectedSalaryMin(c.getExpectedSalaryMin())
                .expectedSalaryMax(c.getExpectedSalaryMax())
                .availability(c.getAvailabilityStatus())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }

    public static class Builder {
        private Long id;
        private Long userId;
        private LocalDate dateOfBirth;
        private String gender;
        private String phone;
        private String currentLocation;
        private String summary;
        private BigDecimal expectedSalaryMin;
        private BigDecimal expectedSalaryMax;
        private String availability;
        private java.time.OffsetDateTime createdAt;
        private java.time.OffsetDateTime updatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder userId(Long userId) { this.userId = userId; return this; }
        public Builder dateOfBirth(LocalDate v) { this.dateOfBirth = v; return this; }
        public Builder gender(String v) { this.gender = v; return this; }
        public Builder phone(String v) { this.phone = v; return this; }
        public Builder currentLocation(String v) { this.currentLocation = v; return this; }
        public Builder summary(String v) { this.summary = v; return this; }
        public Builder expectedSalaryMin(BigDecimal v) { this.expectedSalaryMin = v; return this; }
        public Builder expectedSalaryMax(BigDecimal v) { this.expectedSalaryMax = v; return this; }
        public Builder availability(String v) { this.availability = v; return this; }
        public Builder createdAt(java.time.OffsetDateTime v) { this.createdAt = v; return this; }
        public Builder updatedAt(java.time.OffsetDateTime v) { this.updatedAt = v; return this; }
        public CandidateResponse build() { return new CandidateResponse(this); }
    }
}
