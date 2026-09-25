package com.lokmit.foundation.employment.application.interview.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Interview partial update. Omitted fields stay unchanged. Identity
 * (interview id, owning application) and timestamps are immutable.
 * Status transitions are validated conservatively: COMPLETED/NO_SHOW are
 * terminal, CANCELLED is final, and no transition is allowed out of them.
 */
@Schema(description = "Partially update an interview.")
@Getter
@Setter
public class InterviewUpdateRequest {

    @Schema(description = "Rescheduled date-time with offset (ISO-8601)")
    private OffsetDateTime scheduledAt;

    @Pattern(regexp = "ONSITE|REMOTE|PHONE",
            message = "mode must be one of ONSITE, REMOTE, PHONE")
    @Schema(description = "Interview mode")
    private String mode;

    @Size(max = 255)
    @Schema(description = "Location or meeting link; explicitly null clears it")
    private String location;

    @Pattern(regexp = "SCHEDULED|COMPLETED|CANCELLED|NO_SHOW",
            message = "status must be one of SCHEDULED, COMPLETED, CANCELLED, NO_SHOW")
    @Schema(description = "Interview status")
    private String status;

    @Schema(description = "Notes; explicitly null clears the field")
    private String notes;
}
