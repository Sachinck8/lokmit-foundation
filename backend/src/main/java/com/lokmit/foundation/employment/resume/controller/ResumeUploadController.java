package com.lokmit.foundation.employment.resume.controller;

import com.lokmit.foundation.common.api.ApiResponse;
import com.lokmit.foundation.common.constants.ApiPaths;
import com.lokmit.foundation.employment.resume.dto.ResumeResponse;
import com.lokmit.foundation.employment.resume.entity.Resume;
import com.lokmit.foundation.employment.resume.service.ResumeOwnershipService;
import com.lokmit.foundation.employment.resume.service.ResumeUploadService;
import com.lokmit.foundation.security.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Candidate self-service resume upload API (A7.6.3).
 *
 * <p>Thin HTTP surface over the A7.6.2 {@link ResumeUploadService}, which
 * remains the SOLE security authority for content validation, filename
 * security, the 5 MiB limit, checksums and the transactional storage
 * lifecycle. This controller adds NO validation logic of its own beyond
 * presence checks, and never trusts client-supplied identifiers:</p>
 *
 * <ul>
 *   <li>Authentication: existing stateless JWT filter chain — anonymous
 *       requests are rejected 401 by the security filter chain before this
 *       controller runs.</li>
 *   <li>Ownership: the candidate is resolved server-side from the
 *       authenticated user's id via {@link SecurityUtils} — the request
 *       has NO candidate id parameter at all, so there is no IDOR surface.</li>
 *   <li>Authorization model: authenticated candidate OWNERSHIP (the
 *       established platform pattern — CANDIDATE/EMPLOYER/CLIENT roles
 *       carry no administrative permissions), NOT a new permission seed.</li>
 *   <li>Response: the safe {@link ResumeResponse} DTO — never the entity,
 *       never {@code storageKey}/{@code fileUrl}/blob/provider internals,
 *       never bytes.</li>
 * </ul>
 */
@RestController
@RequestMapping(ApiPaths.CANDIDATE_ME_RESUMES)
@Tag(name = "Candidate Resumes",
        description = "Candidate self-service resume upload (authenticated candidate ownership required)")
public class ResumeUploadController {

    private final ResumeUploadService uploadService;
    private final ResumeOwnershipService ownershipService;
    private final SecurityUtils securityUtils;

    public ResumeUploadController(ResumeUploadService uploadService,
                                  ResumeOwnershipService ownershipService,
                                  SecurityUtils securityUtils) {
        this.uploadService = uploadService;
        this.ownershipService = ownershipService;
        this.securityUtils = securityUtils;
    }

    /**
     * Uploads (or replaces) the authenticated candidate's resume.
     *
     * <p>Multipart field: {@code file}. The client-declared filename and
     * content type are passed through to the A7.6.2 validator untouched —
     * they are cross-check material only, never the security decision.</p>
     *
     * <p>Errors (via the standard error envelope): 400 missing/empty
     * file/filename, 404 the authenticated user owns no candidate profile
     * (or unknown candidate), 413 file above the 5 MiB limit, 400 invalid
     * PDF/DOC/DOCX content or filename or MIME contradiction (A7.6.2
     * validation vocabulary).</p>
     */
    @PostMapping
    @Operation(summary = "Upload or replace the authenticated candidate's resume",
            description = "multipart/form-data with a 'file' part. PDF/DOC/DOCX up to 5 MiB; "
                    + "content is validated from the actual bytes. The previous active resume "
                    + "is deactivated (history retained). Storage internals are never exposed.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<ResumeResponse>> upload(
            @RequestPart(value = "file", required = false) MultipartFile file) throws IOException {

        // Thin presence validation only — content/size/filename security is
        // A7.6.2's job. An absent or empty part is a clean 400 before any
        // service call; a maxFileSizeBytes backstop lives in the service.
        if (file == null || file.isEmpty()) {
            throw new com.lokmit.foundation.common.exception.BadRequestException(
                    "Multipart field 'file' is required and must not be empty");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new com.lokmit.foundation.common.exception.BadRequestException(
                    "Upload must include a filename");
        }

        Long userId = securityUtils.getCurrentUserId();
        var candidate = ownershipService.resolveOwnCandidate(userId);

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            // Controlled 400 — never leak container/storage internals.
            throw new com.lokmit.foundation.common.exception.BadRequestException(
                    "Upload could not be read");
        }

        Resume resume = uploadService.upload(
                candidate.getId(),
                originalFilename,
                file.getContentType(),
                content);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(ResumeResponse.from(resume), "Resume uploaded"));
    }
}
