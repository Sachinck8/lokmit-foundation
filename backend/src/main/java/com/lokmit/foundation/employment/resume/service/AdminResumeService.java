package com.lokmit.foundation.employment.resume.service;

import com.lokmit.foundation.employment.candidate.entity.Candidate;
import com.lokmit.foundation.employment.candidate.repository.CandidateRepository;
import com.lokmit.foundation.employment.resume.dto.ResumeResponse;
import com.lokmit.foundation.employment.resume.entity.Resume;
import com.lokmit.foundation.employment.resume.repository.ResumeRepository;
import com.lokmit.foundation.common.exception.NotFoundException;
import com.lokmit.foundation.common.pagination.PageParams;
import com.lokmit.foundation.common.api.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin-side resume listing service (A7.6.6, item 1). Backs
 * GET /api/v1/admin/candidates/{candidateId}/resumes with the existing
 * candidates:manage permission. Returns paginated, metadata-only pages
 * through the safe {@link ResumeResponse} DTO — never storage keys, blob
 * bytes or the legacy file_url marker.
 */
@Service
public class AdminResumeService {

    private final ResumeRepository resumeRepository;
    private final CandidateRepository candidateRepository;

    public AdminResumeService(ResumeRepository resumeRepository,
                              CandidateRepository candidateRepository) {
        this.resumeRepository = resumeRepository;
        this.candidateRepository = candidateRepository;
    }

    /**
     * Lists one candidate's resumes (active AND inactive — the admin view
     * covers the full lifecycle history, consistent with download/delete
     * admin semantics). The candidate's existence is verified first so an
     * unknown candidate is a clean 404, matching CandidateController
     * conventions.
     */
    @Transactional(readOnly = true)
    public PageResponse<ResumeResponse> listResumes(long candidateId, PageParams pageParams) {
        if (!candidateRepository.existsById(candidateId)) {
            throw new NotFoundException("Candidate not found: " + candidateId);
        }
        // Ordering is already encoded in the derived query name (createdAt DESC);
        // do not add a redundant Sort on top of it.
        Pageable pageable = PageRequest.of(pageParams.getPage(), pageParams.getSize());
        Page<Resume> page = resumeRepository
                .findByCandidateIdOrderByCreatedAtDesc(candidateId, pageable);
        return PageResponse.of(page.map(ResumeResponse::from));
    }
}
