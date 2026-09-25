package com.lokmit.foundation.employment.job.entity;

import java.io.Serializable;
import java.util.Objects;

/** Composite PK for job_skills (job_id, skill_id). */
public class JobSkillId implements Serializable {

    private Long job;
    private Long skill;

    public JobSkillId() {
    }

    public JobSkillId(Long job, Long skill) {
        this.job = job;
        this.skill = skill;
    }

    public Long getJob() { return job; }
    public void setJob(Long job) { this.job = job; }
    public Long getSkill() { return skill; }
    public void setSkill(Long skill) { this.skill = skill; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JobSkillId other)) return false;
        return Objects.equals(job, other.job) && Objects.equals(skill, other.skill);
    }

    @Override
    public int hashCode() {
        return Objects.hash(job, skill);
    }
}
