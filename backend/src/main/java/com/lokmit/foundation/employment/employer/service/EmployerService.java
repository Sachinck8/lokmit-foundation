package com.lokmit.foundation.employment.employer.service;

import com.lokmit.foundation.common.exception.ConflictException;
import com.lokmit.foundation.common.exception.NotFoundException;
import com.lokmit.foundation.employment.employer.dto.EmployerCreateRequest;
import com.lokmit.foundation.employment.employer.dto.EmployerResponse;
import com.lokmit.foundation.employment.employer.dto.EmployerUpdateRequest;
import com.lokmit.foundation.employment.employer.entity.Employer;
import com.lokmit.foundation.employment.employer.repository.EmployerRepository;
import com.lokmit.foundation.security.entity.User;
import com.lokmit.foundation.security.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;

/**
 * Employer profile management. Deliberately exposes NO hard-delete: the V8
 * fk_employers_user ON DELETE CASCADE would remove the owning user identity
 * (and transitively user_roles / refresh_tokens), so profile lifecycle is
 * handled through status changes only.
 */
@Service
public class EmployerService {

    private final EmployerRepository employerRepository;
    private final UserRepository userRepository;

    public EmployerService(EmployerRepository employerRepository,
                           UserRepository userRepository) {
        this.employerRepository = employerRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Page<EmployerResponse> listEmployers(String search,
                                                String verificationStatus,
                                                String status,
                                                Pageable pageable) {
        Specification<Employer> spec = Specification.where(null);
        if (StringUtils.hasText(search)) {
            String like = "%" + search.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("companyName")), like),
                    cb.like(cb.lower(root.get("contactPersonName")), like),
                    cb.like(cb.lower(root.get("contactPhone")), like)));
        }
        if (StringUtils.hasText(verificationStatus)) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("verificationStatus"), verificationStatus));
        }
        if (StringUtils.hasText(status)) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        return employerRepository.findAll(spec, pageable).map(EmployerResponse::from);
    }

    @Transactional(readOnly = true)
    public EmployerResponse getEmployer(long id) {
        return employerRepository.findById(id)
                .map(EmployerResponse::from)
                .orElseThrow(() -> new NotFoundException("Employer not found: " + id));
    }

    @Transactional
    public EmployerResponse createEmployer(EmployerCreateRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new NotFoundException("User not found: " + request.getUserId()));
        if (employerRepository.existsByUserId(user.getId())) {
            throw new ConflictException(
                    "An employer profile already exists for user " + user.getId());
        }
        Employer e = new Employer();
        e.setUser(user);
        e.setCompanyName(request.getCompanyName());
        e.setAbout(request.getAbout());
        e.setWebsiteUrl(request.getWebsiteUrl());
        e.setLogoUrl(request.getLogoUrl());
        e.setContactPersonName(request.getContactPersonName());
        e.setContactPhone(request.getContactPhone());
        e.setAddress(request.getAddress());
        e.setVerificationStatus(request.getVerificationStatus() != null
                ? request.getVerificationStatus() : Employer.VERIFICATION_UNVERIFIED);
        e.setStatus(request.getStatus() != null ? request.getStatus() : Employer.STATUS_ACTIVE);
        e.setCreatedAt(OffsetDateTime.now());
        e.setUpdatedAt(OffsetDateTime.now());
        try {
            return EmployerResponse.from(employerRepository.save(e));
        } catch (DataIntegrityViolationException ex) {
            // uq_employers_user is the final guard against concurrent creation
            throw new ConflictException(
                    "An employer profile already exists for user " + user.getId());
        }
    }

    @Transactional
    public EmployerResponse updateEmployer(long id, EmployerUpdateRequest request) {
        Employer e = employerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Employer not found: " + id));
        if (request.getCompanyName() != null) e.setCompanyName(request.getCompanyName());
        if (request.getAbout() != null) e.setAbout(request.getAbout());
        if (request.getWebsiteUrl() != null) e.setWebsiteUrl(request.getWebsiteUrl());
        if (request.getLogoUrl() != null) e.setLogoUrl(request.getLogoUrl());
        if (request.getContactPersonName() != null) e.setContactPersonName(request.getContactPersonName());
        if (request.getContactPhone() != null) e.setContactPhone(request.getContactPhone());
        if (request.getAddress() != null) e.setAddress(request.getAddress());
        if (request.getVerificationStatus() != null) e.setVerificationStatus(request.getVerificationStatus());
        if (request.getStatus() != null) e.setStatus(request.getStatus());
        e.setUpdatedAt(OffsetDateTime.now());
        return EmployerResponse.from(employerRepository.save(e));
    }
}
