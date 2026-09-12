package com.lokmit.foundation.contact.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Request payload for updating a contact enquiry via PATCH.
 *
 * <p>All fields are optional; omitted fields are left unchanged (partial
 * update). Only staff-manageable fields are mapped — id, created_at,
 * updated_at, and sender identity fields are deliberately absent from this
 * DTO, which makes mass assignment of them structurally impossible.</p>
 *
 * <p>{@code internalNote} distinguishes "omitted" (leave unchanged) from
 * "present with JSON null" (clear the note). The {@code internalNoteProvided}
 * flag is set by the JSON-binding setter and is invisible to clients.</p>
 */
@Getter
@Setter
@Schema(description = "Partial enquiry update. Omitted fields are left unchanged.")
public class ContactMessageUpdateRequest {

    @Pattern(regexp = "NEW|READ|REPLIED|ARCHIVED",
            message = "Status must be one of: NEW, READ, REPLIED, ARCHIVED")
    @Schema(description = "New lifecycle status", example = "READ",
            allowableValues = {"NEW", "READ", "REPLIED", "ARCHIVED"})
    private String status;

    @Size(max = 10000, message = "Internal note must not exceed 10000 characters")
    @Schema(description = "Internal staff note. Set explicitly to null to clear it.",
            example = "Called back on 2026-09-12; requested follow-up next week.")
    private String internalNote;

    /** True when the JSON body contained an internalNote property at all. */
    @JsonIgnore
    @Schema(hidden = true)
    private boolean internalNoteProvided;

    /** Custom binding setter so a JSON null still counts as "provided". */
    public void setInternalNote(String internalNote) {
        this.internalNote = internalNote;
        this.internalNoteProvided = true;
    }
}
