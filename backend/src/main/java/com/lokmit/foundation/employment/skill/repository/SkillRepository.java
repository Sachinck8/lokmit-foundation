package com.lokmit.foundation.employment.skill.repository;

import com.lokmit.foundation.employment.skill.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Repository for the V8 'skills' table. */
public interface SkillRepository
        extends JpaRepository<Skill, Long>, JpaSpecificationExecutor<Skill> {

    boolean existsByNameIgnoreCase(String name);
}
