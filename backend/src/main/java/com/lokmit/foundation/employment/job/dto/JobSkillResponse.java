package com.lokmit.foundation.employment.job.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/** A single job-skill requirement row (V8 job_skills). */
@Getter
public class JobSkillResponse {

    private final Long jobId;
    private final Long skillId;
    private final String skillName;

    private JobSkillResponse(Builder b) {
        this.jobId = b.jobId;
        this.skillId = b.skillId;
        this.skillName = b.skillName;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long jobId;
        private Long skillId;
        private String skillName;

        public Builder jobId(Long v) { this.jobId = v; return this; }
        public Builder skillId(Long v) { this.skillId = v; return this; }
        public Builder skillName(String v) { this.skillName = v; return this; }
        public JobSkillResponse build() { return new JobSkillResponse(this); }
    }
}
