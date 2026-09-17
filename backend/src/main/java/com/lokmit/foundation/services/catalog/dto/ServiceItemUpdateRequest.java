package com.lokmit.foundation.services.catalog.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Partial update of a service. The slug is deliberately absent — services
 * are identified by the path id and the slug is the URL identity. The
 * lifecycle status is intentionally NOT updatable here; transitions run
 * through the dedicated publish/archive endpoints guarded by
 * services:manage (single-tier permission).
 */
@Getter
@Setter
@Schema(description = "Partial service update. Omitted fields stay unchanged. Status changes use the dedicated lifecycle endpoints.")
public class ServiceItemUpdateRequest {

    @Size(max = 255, message = "Title must not exceed 255 characters")
    @Schema(description = "Service display title")
    private String title;

    @Size(max = 500, message = "Summary must not exceed 500 characters")
    @Schema(description = "Short summary. Set explicitly to null to clear it.")
    private String summary;

    @Schema(description = "Full service description. Set explicitly to null to clear it.")
    private String description;

    @Min(value = 1, message = "Category id must be a positive number")
    @Schema(description = "New owning category id. Set explicitly to null to detach (make uncategorized).", example = "2")
    private Long categoryId;

    @Min(value = 0, message = "Display order must be zero or greater")
    @Schema(description = "Display ordering within the catalog")
    private Integer displayOrder;

    @JsonIgnore
    @Schema(hidden = true)
    private boolean summaryProvided;

    @JsonIgnore
    @Schema(hidden = true)
    private boolean descriptionProvided;

    @JsonIgnore
    @Schema(hidden = true)
    private boolean categoryIdProvided;

    public void setSummary(String summary) {
        this.summary = summary;
        this.summaryProvided = true;
    }

    public void setDescription(String description) {
        this.description = description;
        this.descriptionProvided = true;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
        this.categoryIdProvided = true;
    }
}
