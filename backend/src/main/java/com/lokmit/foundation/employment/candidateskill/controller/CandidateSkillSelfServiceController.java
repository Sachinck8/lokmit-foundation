package com.lokmit.foundation.employment.candidateskill.controller;

import com.lokmit.foundation.common.api.ApiResponse;
import com.lokmit.foundation.common.constants.ApiPaths;
import com.lokmit.foundation.common.exception.NotFoundException;
import com.lokmit.foundation.employment.candidate.entity.Candidate;
import com.lokmit.foundation.employment.candidateskill.dto.CandidateSkillRequest;
import com.lokmit.foundation.employment.candidateskill.dto.CandidateSkillResponse;
import com.lokmit.foundation.employment.candidateskill.service.CandidateSkillService;
import com.lokmit.foundation.employment.skill.dto.SkillResponse;
import com.lokmit.foundation.employment.skill.entity.Skill;
import com.lokmit.foundation.employment.skill.repository.SkillRepository;
import com.lokmit.foundation.employment.resume.service.ResumeOwnershipService;
import com.lokmit.foundation.security.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Candidate self-service skills API (A11) over the existing V8
 * 'candidate_skills' join table — no migration, no new permission.
 *
 * <p>Security invariants (same ownership pattern as /candidates/me/profile,
 * /candidates/me/resumes and /candidates/me/applications): the candidate is
 * resolved server-side from the authenticated user's id via the existing
 * A7.6.3 {@link ResumeOwnershipService}; the request contract carries no
 * candidateId field, so a candidate can never read or write another
 * candidate's skills. All endpoints require the standard JWT bearer chain
 * ({@code anyRequest().authenticated()} — no SecurityConfig change).</p>
 *
 * <p>A user without a candidate profile (admins, employers, clients) gets
 * the plain 404 from {@link ResumeOwnershipService#resolveOwnCandidate},
 * consistent with the other candidate self-service APIs. Errors follow the
 * existing error envelope: 400 (validation), 404 (unknown/INACTIVE skill,
 * non-owned assignment, no candidate profile), 409 (duplicate assignment).
 * The skill list a candidate can choose from is the backend's own ACTIVE
 * catalog — candidates cannot create skills. The admin
 * {@code CandidateSkillController} (candidates:manage) is untouched and
 * lives alongside this file in the same package.</p>
 */
@RestController
@RequestMapping(ApiPaths.CANDIDATE_ME_SKILLS)
@Tag(name = "Candidate Skills",
        description = "Candidate self-service skill management (authenticated candidate ownership required)")
public class CandidateSkillSelfServiceController {

    /** The ACTIVE catalog is bounded; still capped defensively at 500. */
    private static final int CATALOG_LIMIT = 500;

    private final CandidateSkillService candidateSkillService;
    private final ResumeOwnershipService ownershipService;
    private final SecurityUtils securityUtils;
    private final SkillRepository skillRepository;

    public CandidateSkillSelfServiceController(CandidateSkillService candidateSkillService,
                                               ResumeOwnershipService ownershipService,
                                               SecurityUtils securityUtils,
                                               SkillRepository skillRepository) {
        this.candidateSkillService = candidateSkillService;
        this.ownershipService = ownershipService;
        this.securityUtils = securityUtils;
        this.skillRepository = skillRepository;
    }

    /** Lists the authenticated candidate's own skill assignments. */
    @GetMapping
    @Operation(summary = "List the authenticated candidate's own skills",
            description = "Ownership is server-resolved; returns the caller's own "
                    + "assignments sorted by skill name.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<List<CandidateSkillResponse>>> listOwn() {
        Candidate candidate = ownershipService
                .resolveOwnCandidate(securityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(
                candidateSkillService.list(candidate.getId())));
    }

    /** Assigns a skill from the ACTIVE catalog to the authenticated candidate. */
    @PostMapping
    @Operation(summary = "Assign a catalog skill to the authenticated candidate",
            description = "skillId must reference an ACTIVE skill in the existing "
                    + "catalog (candidates cannot create skills); proficiency is "
                    + "optional (BEGINNER/INTERMEDIATE/ADVANCED/EXPERT). Duplicate "
                    + "assignment → 409; unknown or INACTIVE skill → 404.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<CandidateSkillResponse>> assignOwn(
            @Valid @RequestBody CandidateSkillRequest request) {
        Candidate candidate = ownershipService
                .resolveOwnCandidate(securityUtils.getCurrentUserId());
        // Candidate boundary rule: only ACTIVE catalog skills are selectable
        // (mirrors the ACTIVE-only catalog read below). An INACTIVE or unknown
        // skill id is the same plain 404. The admin API keeps its own semantics.
        skillRepository.findById(request.getSkillId())
                .filter(s -> Skill.STATUS_ACTIVE.equals(s.getStatus()))
                .orElseThrow(() -> new NotFoundException(
                        "Skill not found: " + request.getSkillId()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        candidateSkillService.assign(candidate.getId(), request),
                        "Skill added"));
    }

    /** Removes one of the authenticated candidate's own skill assignments. */
    @DeleteMapping("/{skillId}")
    @Operation(summary = "Remove one of the authenticated candidate's own skills",
            description = "Ownership is server-resolved; a non-owned or unknown "
                    + "assignment returns the same plain 404 (no existence leak).",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Void> removeOwn(@PathVariable Long skillId) {
        Candidate candidate = ownershipService
                .resolveOwnCandidate(securityUtils.getCurrentUserId());
        candidateSkillService.remove(candidate.getId(), skillId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Lists the backend's own ACTIVE skill catalog for the candidate to
     * choose from (safe {@link SkillResponse} DTO). This is a genuine
     * capability gap: the only catalog API today is admin-guarded. Read-only,
     * bounded, and restricted to ACTIVE entries.
     */
    @GetMapping("/catalog")
    @Operation(summary = "List the ACTIVE skill catalog",
            description = "Read-only catalog of ACTIVE skills (safe SkillResponse "
                    + "DTO) the candidate may attach to their own profile. The "
                    + "candidate cannot create skills.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<List<SkillResponse>>> catalog() {
        // resolveOwnCandidate first: non-candidates (admins/employers/clients)
        // get the same plain 404 as the other self-service endpoints.
        ownershipService.resolveOwnCandidate(securityUtils.getCurrentUserId());
        Pageable page = PageRequest.of(0, CATALOG_LIMIT,
                Sort.by(Sort.Direction.ASC, "name"));
        List<SkillResponse> skills = skillRepository
                .findAllByStatus(Skill.STATUS_ACTIVE, page)
                .map(SkillResponse::from)
                .getContent();
        return ResponseEntity.ok(ApiResponse.success(skills));
    }
}
