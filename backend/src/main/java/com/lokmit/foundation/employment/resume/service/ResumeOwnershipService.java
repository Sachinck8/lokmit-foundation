package com.lokmit.foundation.employment.resume.service;

import com.lokmit.foundation.common.exception.NotFoundException;
import com.lokmit.foundation.employment.candidate.entity.Candidate;
import com.lokmit.foundation.employment.candidate.repository.CandidateRepository;
import com.lokmit.foundation.employment.resume.entity.Resume;
import org.springframework.stereotype.Service;

/**
 * Resolves the authenticated user's own candidate profile for the
 * candidate self-service resume API (A7.6.3).
 *
 * <p>Security invariants:</p>
 * <ul>
 *   <li>The user id comes ONLY from the server-side security context
 *       (JWT-backed principal) — never from a request body, query
 *       parameter, path variable or multipart field.</li>
 *   <li>The candidate profile is looked up by the linked {@code user_id},
 *       so an authenticated user can only ever act on their OWN candidate
 *       account — there is no IDOR surface.</li>
 *   <li>A user without a candidate profile gets the same plain 404 as any
 *       missing resource (no existence leak, matching the established
 *       404-masking convention in this codebase).</li>
 * </ul>
 */
@Service
public class ResumeOwnershipService {

    private final CandidateRepository candidateRepository;

    public ResumeOwnershipService(CandidateRepository candidateRepository) {
        this.candidateRepository = candidateRepository;
    }

    /**
     * Resolves the candidate profile owned by the given authenticated user.
     *
     * @param userId the authenticated user's database id (from the security context)
     * @return the user's own candidate profile
     * @throws NotFoundException when the user owns no candidate profile
     */
    public Candidate resolveOwnCandidate(Long userId) {
        if (userId == null) {
            throw new NotFoundException("Candidate profile not found");
        }
        return candidateRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Candidate profile not found"));
    }

    /**
     * Guard for self-service writes: the resolved candidate must be the
     * resume's owner. Used so that any future surface reusing the upload
     * flow cannot accidentally act on another candidate's resume.
     */
    public void assertOwns(Candidate candidate, Resume resume) {
        if (resume == null || !candidate.getId().equals(resume.getCandidateId())) {
            throw new NotFoundException("Resume not found");
        }
    }
}
