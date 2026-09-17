package com.lokmit.foundation.employment.candidateskill.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/** A single candidate-skill assignment. */
@Getter
public class CandidateSkillResponse {

    private final Long candidateId;
    private final Long skillId;
    private final String skillName;
    private final String proficiency;

    private CandidateSkillResponse(Builder b) {
        this.candidateId = b.candidateId;
        this.skillId = b.skillId;
        this.skillName = b.skillName;
        this.proficiency = b.proficiency;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long candidateId;
        private Long skillId;
        private String skillName;
        private String proficiency;

        public Builder candidateId(Long v) { this.candidateId = v; return this; }
        public Builder skillId(Long v) { this.skillId = v; return this; }
        public Builder skillName(String v) { this.skillName = v; return this; }
        public Builder proficiency(String v) { this.proficiency = v; return this; }
        public CandidateSkillResponse build() { return new CandidateSkillResponse(this); }
    }
}
