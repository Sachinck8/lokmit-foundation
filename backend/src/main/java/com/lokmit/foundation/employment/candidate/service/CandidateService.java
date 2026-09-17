package com.lokmit.foundation.employment.candidate.service;

import com.lokmit.foundation.common.exception.BadRequestException;
import com.lokmit.foundation.common.exception.ConflictException;
import com.lokmit.foundation.common.exception.NotFoundException;
import com.lokmit.foundation.employment.candidate.dto.CandidateCreateRequest;
import com.lokmit.foundation.employment.candidate.dto.CandidateResponse;
import com.lokmit.foundation.employment.candidate.dto.CandidateUpdateRequest;
import com.lokmit.foundation.employment.candidate.entity.Candidate;
import com.lokmit.foundation.employment.candidate.repository.CandidateRepository;
import com.lokmit.foundation.security.entity.User;
import com.lokmit.foundation.security.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Candidate profile management. Deliberately exposes NO hard-delete: the V8
 * fk_candidates_user ON DELETE CASCADE would remove the owning user identity,
 * so profile lifecycle is handled through availability_status changes only.
 */
@Service
public class CandidateService {

    private final CandidateRepository candidateRepository;
    private final UserRepository userRepository;

    public CandidateService(CandidateRepository candidateRepository,
                            UserRepository userRepository) {
        this.candidateRepository = candidateRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Page<CandidateResponse> listCandidates(String search,
                                                  String availability,
                                                  String gender,
                                                  Pageable pageable) {
        Specification<Candidate> spec = Specification.where(null);
        if (StringUtils.hasText(search)) {
            String like = "%" + search.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("phone")), like),
                    cb.like(cb.lower(root.get("currentLocation")), like),
                    cb.like(cb.lower(root.get("summary")), like)));
        }
        if (StringUtils.hasText(availability)) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("availabilityStatus"), availability));
        }
        if (StringUtils.hasText(gender)) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("gender"), gender));
        }
        return candidateRepository.findAll(spec, pageable).map(CandidateResponse::from);
    }

    @Transactional(readOnly = true)
    public CandidateResponse getCandidate(long id) {
        return candidateRepository.findById(id)
                .map(CandidateResponse::from)
                .orElseThrow(() -> new NotFoundException("Candidate not found: " + id));
    }

    @Transactional
    public CandidateResponse createCandidate(CandidateCreateRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new NotFoundException("User not found: " + request.getUserId()));
        validateSalaryRange(request.getExpectedSalaryMin(), request.getExpectedSalaryMax());
        if (candidateRepository.existsByUserId(user.getId())) {
            throw new ConflictException(
                    "A candidate profile already exists for user " + user.getId());
        }
        Candidate c = new Candidate();
        c.setUser(user);
        c.setDateOfBirth(request.getDateOfBirth());
        c.setGender(request.getGender());
        c.setPhone(request.getPhone());
        c.setCurrentLocation(request.getCurrentLocation());
        c.setSummary(request.getSummary());
        c.setExpectedSalaryMin(request.getExpectedSalaryMin());
        c.setExpectedSalaryMax(request.getExpectedSalaryMax());
        c.setAvailabilityStatus(request.getAvailability() != null
                ? request.getAvailability() : Candidate.AVAILABILITY_ACTIVELY_LOOKING);
        c.setCreatedAt(OffsetDateTime.now());
        c.setUpdatedAt(OffsetDateTime.now());
        try {
            return CandidateResponse.from(candidateRepository.save(c));
        } catch (DataIntegrityViolationException ex) {
            // uq_candidates_user is the final guard against concurrent creation
            throw new ConflictException(
                    "A candidate profile already exists for user " + user.getId());
        }
    }

    @Transactional
    public CandidateResponse updateCandidate(long id, CandidateUpdateRequest request) {
        Candidate c = candidateRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Candidate not found: " + id));
        BigDecimal min = request.getExpectedSalaryMin() != null
                ? request.getExpectedSalaryMin() : c.getExpectedSalaryMin();
        BigDecimal max = request.getExpectedSalaryMax() != null
                ? request.getExpectedSalaryMax() : c.getExpectedSalaryMax();
        validateSalaryRange(min, max);
        if (request.getDateOfBirth() != null) c.setDateOfBirth(request.getDateOfBirth());
        if (request.getGender() != null) c.setGender(request.getGender());
        if (request.getPhone() != null) c.setPhone(request.getPhone());
        if (request.getCurrentLocation() != null) c.setCurrentLocation(request.getCurrentLocation());
        if (request.getSummary() != null) c.setSummary(request.getSummary());
        if (request.getExpectedSalaryMin() != null) c.setExpectedSalaryMin(request.getExpectedSalaryMin());
        if (request.getExpectedSalaryMax() != null) c.setExpectedSalaryMax(request.getExpectedSalaryMax());
        if (request.getAvailability() != null) c.setAvailabilityStatus(request.getAvailability());
        c.setUpdatedAt(OffsetDateTime.now());
        return CandidateResponse.from(candidateRepository.save(c));
    }

    private void validateSalaryRange(BigDecimal min, BigDecimal max) {
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new BadRequestException(
                    "expectedSalaryMin must not exceed expectedSalaryMax");
        }
    }
}
