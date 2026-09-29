package com.lokmit.foundation.employment.candidateskill.service;

import com.lokmit.foundation.common.exception.ConflictException;
import com.lokmit.foundation.common.exception.NotFoundException;
import com.lokmit.foundation.employment.candidate.repository.CandidateRepository;
import com.lokmit.foundation.employment.candidateskill.dto.CandidateSkillRequest;
import com.lokmit.foundation.employment.candidateskill.dto.CandidateSkillResponse;
import com.lokmit.foundation.employment.candidateskill.entity.CandidateSkill;
import com.lokmit.foundation.employment.candidateskill.entity.CandidateSkillId;
import com.lokmit.foundation.employment.candidateskill.repository.CandidateSkillRepository;
import com.lokmit.foundation.employment.skill.entity.Skill;
import com.lokmit.foundation.employment.skill.repository.SkillRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * Candidate-skill assignment management using the existing V8
 * candidate_skills composite relationship (candidate_id, skill_id).
 */
@Service
public class CandidateSkillService {

    private final CandidateSkillRepository candidateSkillRepository;
    private final CandidateRepository candidateRepository;
    private final SkillRepository skillRepository;

    public CandidateSkillService(CandidateSkillRepository candidateSkillRepository,
                                 CandidateRepository candidateRepository,
                                 SkillRepository skillRepository) {
        this.candidateSkillRepository = candidateSkillRepository;
        this.candidateRepository = candidateRepository;
        this.skillRepository = skillRepository;
    }

    @Transactional(readOnly = true)
    public List<CandidateSkillResponse> list(Long candidateId) {
        if (!candidateRepository.existsById(candidateId)) {
            throw new NotFoundException("Candidate not found: " + candidateId);
        }
        return candidateSkillRepository.findByCandidateId(candidateId).stream()
                .map(this::toResponse)
                .sorted(Comparator.comparing(CandidateSkillResponse::getSkillName))
                .toList();
    }

    @Transactional
    public CandidateSkillResponse assign(Long candidateId, CandidateSkillRequest req) {
        if (!candidateRepository.existsById(candidateId)) {
            throw new NotFoundException("Candidate not found: " + candidateId);
        }
        Skill skill = skillRepository.findById(req.getSkillId())
                .orElseThrow(() -> new NotFoundException("Skill not found: " + req.getSkillId()));
        if (candidateSkillRepository.existsByCandidateIdAndSkillId(candidateId, req.getSkillId())) {
            throw new ConflictException(
                    "Skill " + req.getSkillId() + " is already assigned to candidate " + candidateId);
        }
        CandidateSkill cs = new CandidateSkill();
        cs.setCandidate(candidateRepository.getReferenceById(candidateId));
        cs.setSkill(skill);
        cs.setProficiency(req.getProficiency());
        try {
            return toResponse(candidateSkillRepository.save(cs));
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException(
                    "Skill " + req.getSkillId() + " is already assigned to candidate " + candidateId);
        }
    }

    @Transactional
    public void remove(Long candidateId, Long skillId) {
        CandidateSkillId pk = new CandidateSkillId(candidateId, skillId);
        if (!candidateSkillRepository.existsById(pk)) {
            throw new NotFoundException("Candidate skill assignment not found");
        }
        candidateSkillRepository.deleteById(pk);
    }

    private CandidateSkillResponse toResponse(CandidateSkill cs) {
        return CandidateSkillResponse.builder()
                .candidateId(cs.getCandidate().getId())
                .skillId(cs.getSkill().getId())
                .skillName(cs.getSkill().getName())
                .proficiency(cs.getProficiency())
                .build();
    }
}
