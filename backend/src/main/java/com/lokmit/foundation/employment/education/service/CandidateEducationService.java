package com.lokmit.foundation.employment.education.service;

import com.lokmit.foundation.common.exception.NotFoundException;
import com.lokmit.foundation.employment.candidate.entity.Candidate;
import com.lokmit.foundation.employment.education.entity.CandidateEducation;
import com.lokmit.foundation.employment.education.dto.CandidateEducationCreateRequest;
import com.lokmit.foundation.employment.education.dto.CandidateEducationResponse;
import com.lokmit.foundation.employment.education.dto.CandidateEducationUpdateRequest;
import com.lokmit.foundation.employment.candidate.repository.CandidateRepository;
import com.lokmit.foundation.employment.education.repository.CandidateEducationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Candidate self-service education CRUD (A13) on the existing V8
 * 'candidate_educations' table — no migration, no new permission.
 *
 * <p>Security invariants: every method is scoped by candidateId, which the
 * controller resolves server-side from the JWT user id via the existing
 * A7.6.3 ownership service; the request contract carries no candidateId.
 * A foreign or unknown record id produces the identical plain 404
 * (A7.6 masking convention). Field-level validation lives on the request
 * DTOs, consistent with the rest of the platform.</p>
 */
@Service
public class CandidateEducationService {

    private final CandidateEducationRepository repository;
    private final CandidateRepository candidateRepository;

    public CandidateEducationService(CandidateEducationRepository repository,
                                     CandidateRepository candidateRepository) {
        this.repository = repository;
        this.candidateRepository = candidateRepository;
    }

    @Transactional(readOnly = true)
    public List<CandidateEducationResponse> list(Long candidateId) {
        // A18: read path is now shared with the admin applications-review
        // console, so an unknown candidate must not silently return an empty
        // list — same explicit 404 guard the admin skills list already has.
        if (!candidateRepository.existsById(candidateId)) {
            throw new NotFoundException("Candidate not found: " + candidateId);
        }
        return repository.findByCandidateIdOrderByCreatedAtDesc(candidateId).stream()
                .map(CandidateEducationResponse::from)
                .toList();
    }

    @Transactional
    public CandidateEducationResponse create(Long candidateId,
                                             CandidateEducationCreateRequest request,
                                             Candidate owner) {
        CandidateEducation e = new CandidateEducation();
        e.setCandidate(owner);
        e.setInstitution(request.getInstitution().trim());
        e.setDegree(request.getDegree().trim());
        e.setFieldOfStudy(request.getFieldOfStudy());
        e.setStartYear(request.getStartYear());
        e.setEndYear(request.getEndYear());
        e.setGrade(request.getGrade());
        return CandidateEducationResponse.from(repository.save(e));
    }

    @Transactional
    public CandidateEducationResponse update(Long candidateId, Long id,
                                             CandidateEducationUpdateRequest request) {
        CandidateEducation e = repository.findByIdAndCandidateId(id, candidateId)
                .orElseThrow(() -> new NotFoundException(
                        "Education record not found: " + id));
        if (request.getInstitution() != null) {
            e.setInstitution(request.getInstitution().trim());
        }
        if (request.getDegree() != null) {
            e.setDegree(request.getDegree().trim());
        }
        if (request.getFieldOfStudy() != null) {
            e.setFieldOfStudy(request.getFieldOfStudy());
        }
        if (request.getStartYear() != null) {
            e.setStartYear(request.getStartYear());
        }
        if (request.getEndYear() != null) {
            e.setEndYear(request.getEndYear());
        }
        if (request.getGrade() != null) {
            e.setGrade(request.getGrade());
        }
        return CandidateEducationResponse.from(repository.save(e));
    }

    @Transactional
    public void delete(Long candidateId, Long id) {
        CandidateEducation e = repository.findByIdAndCandidateId(id, candidateId)
                .orElseThrow(() -> new NotFoundException(
                        "Education record not found: " + id));
        repository.delete(e);
    }
}
