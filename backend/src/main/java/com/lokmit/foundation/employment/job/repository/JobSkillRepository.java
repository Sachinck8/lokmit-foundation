package com.lokmit.foundation.employment.job.repository;

import com.lokmit.foundation.employment.job.entity.JobSkill;
import com.lokmit.foundation.employment.job.entity.JobSkillId;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Repository for the V8 'job_skills' join table. */
public interface JobSkillRepository extends JpaRepository<JobSkill, JobSkillId> {

    /**
     * Skills for one job, fetched with the skill join in a single query
     * (@EntityGraph) to keep the list endpoint N+1-free.
     */
    @EntityGraph(attributePaths = "skill")
    List<JobSkill> findByJobId(Long jobId);

    boolean existsByJobIdAndSkillId(Long jobId, Long skillId);

    Optional<JobSkill> findByJobIdAndSkillId(Long jobId, Long skillId);
}
