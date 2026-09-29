package com.lokmit.foundation.employment.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Candidate application submission request (A9).
 *
 * <p>There is deliberately NO candidateId field: the applying candidate is
 * resolved server-side from the authenticated principal. Any candidateId
 * sent by a client is ignored by Jackson (unknown property) and can never
 * influence ownership. {@code resumeId} is optional and must reference one
 * of the caller's OWN ACTIVE resumes — enforced server-side (A7.6
 * ownership conventions); a foreign or inactive resume is rejected.</p>
 */
@Schema(description = "Submit an application for a published job as the authenticated candidate.")
@Getter
@Setter
public class CandidateApplicationCreateRequest {

    @NotNull(message = "jobId is required")
    @Positive(message = "jobId must be a positive number")
    @Schema(description = "The published job to apply for", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long jobId;

    @Positive(message = "resumeId must be a positive number when provided")
    @Schema(description = "Optional reference to one of the caller's own ACTIVE resumes")
    private Long resumeId;

    @Size(max = 20000, message = "coverNote must not exceed 20000 characters")
    @Schema(description = "Optional cover note")
    private String coverNote;
}
