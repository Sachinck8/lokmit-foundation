package com.lokmit.foundation.employment.job.service;

import com.lokmit.foundation.common.exception.ConflictException;
import com.lokmit.foundation.common.exception.NotFoundException;
import com.lokmit.foundation.employment.job.dto.JobSkillResponse;
import com.lokmit.foundation.employment.job.entity.JobSkill;
import com.lokmit.foundation.employment.job.entity.JobSkillId;
import com.lokmit.foundation.employment.job.repository.JobRepository;
import com.lokmit.foundation.employment.job.repository.JobSkillRepository;
import com.lokmit.foundation.employment.skill.entity.Skill;
import com.lokmit.foundation.employment.skill.repository.SkillRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * Job ↔ skill requirement management using the existing V8 'job_skills'
 * join table (composite PK job_id + skill_id, no extra columns). Available
 * in every lifecycle state — skill requirements are catalog-like metadata,
 * consistent with the A7.1 candidate-skill design; archived jobs remain
 * readable but are excluded from mutation-sensitive flows elsewhere.
 */
@Service
public class JobSkillService {

    private final JobSkillRepository jobSkillRepository;
    private final JobRepository jobRepository;
    private final SkillRepository skillRepository;

    public JobSkillService(JobSkillRepository jobSkillRepository,
                           JobRepository jobRepository,
                           SkillRepository skillRepository) {
        this.jobSkillRepository = jobSkillRepository;
        this.jobRepository = jobRepository;
        this.skillRepository = skillRepository;
    }

    @Transactional(readOnly = true)
    public List<JobSkillResponse> listSkills(long jobId) {
        if (!jobRepository.existsById(jobId)) {
            throw new NotFoundException("Job not found: " + jobId);
        }
        return jobSkillRepository.findByJobId(jobId).stream()
                .map(this::toResponse)
                .sorted(Comparator.comparing(JobSkillResponse::getSkillName))
                .toList();
    }

    @Transactional
    public JobSkillResponse assignSkill(long jobId, Long skillId) {
        if (!jobRepository.existsById(jobId)) {
            throw new NotFoundException("Job not found: " + jobId);
        }
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new NotFoundException("Skill not found: " + skillId));
        if (jobSkillRepository.existsByJobIdAndSkillId(jobId, skillId)) {
            throw new ConflictException(
                    "Skill " + skillId + " is already assigned to job " + jobId);
        }
        JobSkill js = new JobSkill();
        js.setJob(jobRepository.getReferenceById(jobId));
        js.setSkill(skill);
        try {
            return toResponse(jobSkillRepository.saveAndFlush(js));
        } catch (DataIntegrityViolationException ex) {
            // composite-PK backstop against concurrent duplicate assignments
            throw new ConflictException(
                    "Skill " + skillId + " is already assigned to job " + jobId);
        }
    }

    @Transactional
    public void removeSkill(long jobId, long skillId) {
        JobSkillId pk = new JobSkillId(jobId, skillId);
        if (!jobSkillRepository.existsById(pk)) {
            throw new NotFoundException("Job skill assignment not found");
        }
        jobSkillRepository.deleteById(pk);
    }

    private JobSkillResponse toResponse(JobSkill js) {
        return JobSkillResponse.builder()
                .jobId(js.getJob().getId())
                .skillId(js.getSkill().getId())
                .skillName(js.getSkill().getName())
                .build();
    }
}
