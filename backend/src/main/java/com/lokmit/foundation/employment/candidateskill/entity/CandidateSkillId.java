package com.lokmit.foundation.employment.candidateskill.entity;

import java.io.Serializable;
import java.util.Objects;

/** Composite PK for candidate_skills (candidate_id, skill_id). */
public class CandidateSkillId implements Serializable {

    private Long candidate;
    private Long skill;

    public CandidateSkillId() {
    }

    public CandidateSkillId(Long candidate, Long skill) {
        this.candidate = candidate;
        this.skill = skill;
    }

    public Long getCandidate() { return candidate; }
    public void setCandidate(Long candidate) { this.candidate = candidate; }
    public Long getSkill() { return skill; }
    public void setSkill(Long skill) { this.skill = skill; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CandidateSkillId other)) return false;
        return Objects.equals(candidate, other.candidate)
                && Objects.equals(skill, other.skill);
    }

    @Override
    public int hashCode() {
        return Objects.hash(candidate, skill);
    }
}
