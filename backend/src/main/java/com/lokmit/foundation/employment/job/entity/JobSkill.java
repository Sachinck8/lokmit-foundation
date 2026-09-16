package com.lokmit.foundation.employment.job.entity;

import com.lokmit.foundation.employment.skill.entity.Skill;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

/**
 * Join entity for the V8 'job_skills' table: composite PK (job_id, skill_id),
 * both FKs ON DELETE CASCADE, no additional columns (no proficiency, no
 * timestamps — mapped exactly, nothing invented).
 */
@Entity
@Table(name = "job_skills")
@Getter
@Setter
@IdClass(JobSkillId.class)
public class JobSkill {

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JobSkill other)) return false;
        return job != null && skill != null
                && Objects.equals(job.getId(), other.job != null ? other.job.getId() : null)
                && Objects.equals(skill.getId(), other.skill != null ? other.skill.getId() : null);
    }

    @Override
    public int hashCode() {
        return Objects.hash(job != null ? job.getId() : null,
                skill != null ? skill.getId() : null);
    }
}
