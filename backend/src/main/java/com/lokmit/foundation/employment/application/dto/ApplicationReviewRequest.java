package com.lokmit.foundation.employment.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Application partial update. Only schema-mutable fields are exposed:
 * {@code employer_note} (nullable text) and the optional {@code resume_id}
 * reference (fk_job_applications_resume). Identity fields (job, candidate)
 * are immutable — reassignment is not a supported admin operation and the
 * V8 unique pair makes it meaningless. Status is NOT patchable: the review
 * lifecycle moves only through the dedicated endpoints.
 */
@Schema(description = "Partially update an application review. Status changes use the lifecycle endpoints.")
@Getter
@Setter
public class ApplicationReviewRequest {

    @Size(max = 20000)
    @Schema(description = "Employer/admin review note; explicitly null clears the field")
    private String employerNote;

    @Positive(message = "resumeId must be a positive number when provided")
    @Schema(description = "Optional resume reference; explicitly null detaches the resume")
    private Long resumeId;
}
