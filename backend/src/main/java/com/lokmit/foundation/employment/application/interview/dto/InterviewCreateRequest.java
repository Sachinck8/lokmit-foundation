package com.lokmit.foundation.employment.application.interview.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Interview scheduling payload. Only schema-backed fields are accepted;
 * status is not writable (always starts SCHEDULED). scheduledAt is an
 * ISO-8601 offset date-time — the offset is preserved (TIMESTAMPTZ), and
 * malformed input fails Jackson deserialization with the project's standard
 * 400 handling.
 */
@Schema(description = "Schedule an interview. New interviews always start SCHEDULED.")
@Getter
@Setter
public class InterviewCreateRequest {

    @NotNull(message = "scheduledAt is required")
    @Schema(description = "Scheduled date-time with offset (ISO-8601)",
            example = "2026-10-01T10:30:00+05:45")
    private OffsetDateTime scheduledAt;

    @NotNull
    @Pattern(regexp = "ONSITE|REMOTE|PHONE",
            message = "mode must be one of ONSITE, REMOTE, PHONE")
    @Schema(description = "Interview mode")
    private String mode;

    @Size(max = 255)
    @Schema(description = "Physical address or meeting link")
    private String location;

    @Schema(description = "Notes (text)")
    private String notes;
}
