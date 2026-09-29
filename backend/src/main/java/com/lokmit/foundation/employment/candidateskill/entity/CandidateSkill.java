package com.lokmit.foundation.employment.candidateskill.entity;

import com.lokmit.foundation.employment.candidate.entity.Candidate;
import com.lokmit.foundation.employment.skill.entity.Skill;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

/**
 * Join entity for the V8 'candidate_skills' table with composite PK
 * (candidate_id, skill_id); fk_candidate_skills_skill cascades on skill
 * deletion. {@code proficiency_level} is nullable per schema and the table
 * has no timestamps.
 */
@Entity
@Table(name = "candidate_skills")
@Getter
@Setter
@IdClass(CandidateSkillId.class)
public class CandidateSkill {

    /** Proficiency values admitted by chk_candidate_skills_level. */
    public static final String BEGINNER = "BEGINNER";
    public static final String INTERMEDIATE = "INTERMEDIATE";
    public static final String ADVANCED = "ADVANCED";
    public static final String EXPERT = "EXPERT";

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @Column(name = "proficiency_level", length = 20)
    private String proficiency;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CandidateSkill other)) return false;
        return candidate != null && skill != null
                && Objects.equals(candidate.getId(), other.candidate != null ? other.candidate.getId() : null)
                && Objects.equals(skill.getId(), other.skill != null ? other.skill.getId() : null);
    }

    @Override
    public int hashCode() {
        return Objects.hash(candidate != null ? candidate.getId() : null,
                skill != null ? skill.getId() : null);
    }
}
