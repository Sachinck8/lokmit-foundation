package com.lokmit.foundation.employment.candidateskill.repository;

import com.lokmit.foundation.employment.candidateskill.entity.CandidateSkill;
import com.lokmit.foundation.employment.candidateskill.entity.CandidateSkillId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Repository for the V8 'candidate_skills' join table. */
public interface CandidateSkillRepository extends JpaRepository<CandidateSkill, CandidateSkillId> {

    List<CandidateSkill> findByCandidateId(Long candidateId);

    boolean existsByCandidateIdAndSkillId(Long candidateId, Long skillId);

    Optional<CandidateSkill> findByCandidateIdAndSkillId(Long candidateId, Long skillId);
}
