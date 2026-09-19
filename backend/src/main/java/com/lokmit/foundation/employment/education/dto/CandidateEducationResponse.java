package com.lokmit.foundation.employment.education.dto;

import com.lokmit.foundation.employment.education.entity.CandidateEducation;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.OffsetDateTime;

/**
 * Candidate-safe education record. No candidate internals, no audit data —
 * the candidate identity is implicit (the caller's own).
 */
@Getter
public class CandidateEducationResponse {

    private final Long id;
    private final String institution;
    private final String degree;
    private final String fieldOfStudy;
    private final Integer startYear;
    private final Integer endYear;
    private final String grade;
    private final OffsetDateTime createdAt;

    private CandidateEducationResponse(Builder b) {
        this.id = b.id;
        this.institution = b.institution;
        this.degree = b.degree;
        this.fieldOfStudy = b.fieldOfStudy;
        this.startYear = b.startYear;
        this.endYear = b.endYear;
        this.grade = b.grade;
        this.createdAt = b.createdAt;
    }

    public static Builder builder() { return new Builder(); }

    public static CandidateEducationResponse from(CandidateEducation e) {
        return builder()
                .id(e.getId())
                .institution(e.getInstitution())
                .degree(e.getDegree())
                .fieldOfStudy(e.getFieldOfStudy())
                .startYear(e.getStartYear())
                .endYear(e.getEndYear())
                .grade(e.getGrade())
                .createdAt(e.getCreatedAt())
                .build();
    }

    public static class Builder {
        private Long id;
        private String institution;
        private String degree;
        private String fieldOfStudy;
        private Integer startYear;
        private Integer endYear;
        private String grade;
        private OffsetDateTime createdAt;

        public Builder id(Long v) { this.id = v; return this; }
        public Builder institution(String v) { this.institution = v; return this; }
        public Builder degree(String v) { this.degree = v; return this; }
        public Builder fieldOfStudy(String v) { this.fieldOfStudy = v; return this; }
        public Builder startYear(Integer v) { this.startYear = v; return this; }
        public Builder endYear(Integer v) { this.endYear = v; return this; }
        public Builder grade(String v) { this.grade = v; return this; }
        public Builder createdAt(OffsetDateTime v) { this.createdAt = v; return this; }
        public CandidateEducationResponse build() {
            return new CandidateEducationResponse(this);
        }
    }
}
