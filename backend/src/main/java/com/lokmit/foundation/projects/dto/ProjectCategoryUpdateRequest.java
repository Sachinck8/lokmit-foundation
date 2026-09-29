package com.lokmit.foundation.projects.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Partial update of a project category. The slug is deliberately absent —
 * categories are identified by the path id and the slug is the URL identity.
 * Omitted fields stay unchanged; description set explicitly to null clears it.
 */
@Getter
@Setter
@Schema(description = "Partial project-category update. Omitted fields stay unchanged.")
public class ProjectCategoryUpdateRequest {

    @Size(max = 100, message = "Name must not exceed 100 characters")
    @Schema(description = "Category display name (unique)")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    @Schema(description = "Category description. Set explicitly to null to clear it.")
    private String description;

    @Schema(description = "Display ordering within the category list")
    private Integer displayOrder;

    @Pattern(regexp = "ACTIVE|INACTIVE", message = "Status must be one of: ACTIVE, INACTIVE")
    @Schema(description = "Category status", example = "ACTIVE")
    private String status;

    @JsonIgnore
    @Schema(hidden = true)
    private boolean descriptionProvided;

    public void setDescription(String description) {
        this.description = description;
        this.descriptionProvided = true;
    }
}
