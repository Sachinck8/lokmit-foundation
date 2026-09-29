package com.lokmit.foundation.services.catalog.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Partial update of a service category. The slug is deliberately absent —
 * categories are identified by the path id, and renaming the URL identity
 * would silently break references. Omitted fields stay unchanged; set
 * description explicitly to null to clear it.
 */
@Getter
@Setter
@Schema(description = "Partial category update. Omitted fields stay unchanged.")
public class ServiceCategoryUpdateRequest {

    @Size(max = 100, message = "Name must not exceed 100 characters")
    @Schema(description = "Category display name (unique)")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    @Schema(description = "Category description. Set explicitly to null to clear it.")
    private String description;

    @Min(value = 0, message = "Display order must be zero or greater")
    @Schema(description = "Display ordering within the catalog")
    private Integer displayOrder;

    @Size(max = 20, message = "Status must not exceed 20 characters")
    @Schema(description = "Lifecycle status", example = "ACTIVE",
            allowableValues = {"ACTIVE", "INACTIVE"})
    private String status;

    @JsonIgnore
    @Schema(hidden = true)
    private boolean descriptionProvided;

    public void setDescription(String description) {
        this.description = description;
        this.descriptionProvided = true;
    }
}
