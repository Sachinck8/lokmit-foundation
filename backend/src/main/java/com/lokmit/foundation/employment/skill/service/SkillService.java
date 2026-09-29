package com.lokmit.foundation.employment.skill.service;

import com.lokmit.foundation.common.exception.ConflictException;
import com.lokmit.foundation.common.exception.NotFoundException;
import com.lokmit.foundation.employment.skill.dto.SkillCreateRequest;
import com.lokmit.foundation.employment.skill.dto.SkillResponse;
import com.lokmit.foundation.employment.skill.dto.SkillUpdateRequest;
import com.lokmit.foundation.employment.skill.entity.Skill;
import com.lokmit.foundation.employment.skill.repository.SkillRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;

/**
 * Skill management. Hard DELETE is intentionally exposed: V8 defines
 * candidate_skills.skill_id and job_skills.skill_id with ON DELETE CASCADE,
 * so deleting a skill also removes its assignments/requirements — a
 * documented, intentional cleanup semantic (see docs/DATABASE.md).
 */
@Service
public class SkillService {

    private final SkillRepository skillRepository;

    public SkillService(SkillRepository skillRepository) {
        this.skillRepository = skillRepository;
    }

    @Transactional(readOnly = true)
    public Page<SkillResponse> listSkills(String search, String status, Pageable pageable) {
        Specification<Skill> spec = Specification.where(null);
        if (StringUtils.hasText(search)) {
            String like = "%" + search.toLowerCase() + "%";
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("name")), like));
        }
        if (StringUtils.hasText(status)) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        return skillRepository.findAll(spec, pageable).map(SkillResponse::from);
    }

    @Transactional(readOnly = true)
    public SkillResponse getSkill(long id) {
        return skillRepository.findById(id)
                .map(SkillResponse::from)
                .orElseThrow(() -> new NotFoundException("Skill not found: " + id));
    }

    @Transactional
    public SkillResponse createSkill(SkillCreateRequest request) {
        String name = request.getName().trim();
        if (skillRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Skill already exists: " + name);
        }
        Skill s = new Skill();
        s.setName(name);
        s.setStatus(request.getStatus() != null ? request.getStatus() : Skill.STATUS_ACTIVE);
        s.setCreatedAt(OffsetDateTime.now());
        s.setUpdatedAt(OffsetDateTime.now());
        try {
            return SkillResponse.from(skillRepository.save(s));
        } catch (DataIntegrityViolationException ex) {
            // uq_skills_name is the final guard against concurrent creation
            throw new ConflictException("Skill already exists: " + name);
        }
    }

    @Transactional
    public SkillResponse updateSkill(long id, SkillUpdateRequest request) {
        Skill s = skillRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Skill not found: " + id));
        if (request.getName() != null) {
            String name = request.getName().trim();
            if (!name.equalsIgnoreCase(s.getName())
                    && skillRepository.existsByNameIgnoreCase(name)) {
                throw new ConflictException("Skill already exists: " + name);
            }
            s.setName(name);
        }
        if (request.getStatus() != null) s.setStatus(request.getStatus());
        s.setUpdatedAt(OffsetDateTime.now());
        return SkillResponse.from(skillRepository.save(s));
    }

    @Transactional
    public void deleteSkill(long id) {
        if (!skillRepository.existsById(id)) {
            throw new NotFoundException("Skill not found: " + id);
        }
        skillRepository.deleteById(id);
    }
}
