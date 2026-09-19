package com.lokmit.foundation.employment.experience.service;

import com.lokmit.foundation.common.exception.NotFoundException;
import com.lokmit.foundation.employment.candidate.entity.Candidate;
import com.lokmit.foundation.employment.experience.entity.CandidateExperience;
import com.lokmit.foundation.employment.experience.dto.CandidateExperienceCreateRequest;
import com.lokmit.foundation.employment.experience.dto.CandidateExperienceResponse;
import com.lokmit.foundation.employment.experience.dto.CandidateExperienceUpdateRequest;
import com.lokmit.foundation.employment.experience.repository.CandidateExperienceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Candidate self-service experience CRUD (A13) on the existing V8
 * 'candidate_experiences' table — no migration, no new permission.
 *
 * <p>Security invariants mirror the education service: every method is
 * scoped by candidateId (server-resolved from the JWT user id); the request
 * contract carries no candidateId; a foreign or unknown record id produces
 * the identical plain 404. Field-level validation lives on the DTOs.</p>
 */
@Service
public class CandidateExperienceService {

    private final CandidateExperienceRepository repository;

    public CandidateExperienceService(CandidateExperienceRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<CandidateExperienceResponse> list(Long candidateId) {
        return repository.findByCandidateIdOrderByCreatedAtDesc(candidateId).stream()
                .map(CandidateExperienceResponse::from)
                .toList();
    }

    @Transactional
    public CandidateExperienceResponse create(Long candidateId,
                                              CandidateExperienceCreateRequest request,
                                              Candidate owner) {
        CandidateExperience e = new CandidateExperience();
        e.setCandidate(owner);
        e.setCompanyName(request.getCompanyName().trim());
        e.setJobTitle(request.getJobTitle().trim());
        e.setDescription(request.getDescription());
        e.setStartDate(request.getStartDate());
        e.setEndDate(request.getEndDate());
        return CandidateExperienceResponse.from(repository.save(e));
    }

    @Transactional
    public CandidateExperienceResponse update(Long candidateId, Long id,
                                              CandidateExperienceUpdateRequest request) {
        CandidateExperience e = repository.findByIdAndCandidateId(id, candidateId)
                .orElseThrow(() -> new NotFoundException(
                        "Experience record not found: " + id));
        if (request.getCompanyName() != null) {
            e.setCompanyName(request.getCompanyName().trim());
        }
        if (request.getJobTitle() != null) {
            e.setJobTitle(request.getJobTitle().trim());
        }
        if (request.getDescription() != null) {
            e.setDescription(request.getDescription());
        }
        if (request.getStartDate() != null) {
            e.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            e.setEndDate(request.getEndDate());
        }
        return CandidateExperienceResponse.from(repository.save(e));
    }

    @Transactional
    public void delete(Long candidateId, Long id) {
        CandidateExperience e = repository.findByIdAndCandidateId(id, candidateId)
                .orElseThrow(() -> new NotFoundException(
                        "Experience record not found: " + id));
        repository.delete(e);
    }
}
