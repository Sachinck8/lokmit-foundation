package com.lokmit.foundation.services.catalog.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Partial update of an expertise area. The slug is deliberately absent —
 * areas are identified by the path id and the slug is the URL identity. The
 * lifecycle status is intentionally NOT updatable here; transitions run
 * through the dedicated publish/archive endpoints.
 */
@Getter
@Setter
@Schema(description = "Partial expertise area update. Omitted fields stay unchanged. Status changes use the dedicated lifecycle endpoints.")
public class ExpertiseAreaUpdateRequest {

    @Size(max = 255, message = "Name must not exceed 255 characters")
    @Schema(description = "Expertise display name")
    private String name;

    @Schema(description = "Full description. Set explicitly to null to clear it.")
    private String description;

    @Min(value = 0, message = "Display order must be zero or greater")
    @Schema(description = "Display ordering")
    private Integer displayOrder;

    @JsonIgnore
    @Schema(hidden = true)
    private boolean descriptionProvided;

    public void setDescription(String description) {
        this.description = description;
        this.descriptionProvided = true;
    }
}
