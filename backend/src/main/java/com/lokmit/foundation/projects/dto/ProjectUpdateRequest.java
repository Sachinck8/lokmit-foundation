package com.lokmit.foundation.projects.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Partial update of a project. The slug is deliberately absent — projects
 * are identified by the path id and the slug is the URL identity. The
 * editorial lifecycle status is intentionally NOT updatable here;
 * transitions run through the dedicated publish/archive endpoints.
 * Omitted fields stay unchanged; nullable fields set explicitly to null
 * clear them.
 */
@Getter
@Setter
@Schema(description = "Partial project update. Omitted fields stay unchanged. Status changes use the dedicated lifecycle endpoints.")
public class ProjectUpdateRequest {

    @Size(max = 255, message = "Title must not exceed 255 characters")
    @Schema(description = "Project display title")
    private String title;

    @Size(max = 500, message = "Summary must not exceed 500 characters")
    @Schema(description = "Short summary. Set explicitly to null to clear it.")
    private String summary;

    @Schema(description = "Full project description. Set explicitly to null to clear it.")
    private String description;

    @Min(value = 1, message = "Category id must be a positive number")
    @Schema(description = "New owning category id. Set explicitly to null to detach (make uncategorized).", example = "2")
    private Long categoryId;

    @Pattern(regexp = "PLANNING|ONGOING|COMPLETED", message = "Project status must be one of: PLANNING, ONGOING, COMPLETED")
    @Schema(description = "Delivery state. Set explicitly to null to clear it.", example = "COMPLETED")
    private String projectStatus;

    @Size(max = 255, message = "Location must not exceed 255 characters")
    @Schema(description = "Project location. Set explicitly to null to clear it.")
    private String location;

    @Schema(description = "Start date (ISO-8601). Set explicitly to null to clear it.")
    private LocalDate startDate;

    @Schema(description = "End date (ISO-8601). Must remain on or after the resulting start date. Set explicitly to null to clear it.")
    private LocalDate endDate;

    @Schema(description = "Objectives document as well-formed JSON. Set explicitly to null to clear it.")
    private String objectives;

    @Schema(description = "Impact summary. Set explicitly to null to clear it.")
    private String impactSummary;

    @JsonIgnore
    @Schema(hidden = true)
    private boolean summaryProvided;

    @JsonIgnore
    @Schema(hidden = true)
    private boolean descriptionProvided;

    @JsonIgnore
    @Schema(hidden = true)
    private boolean categoryIdProvided;

    @JsonIgnore
    @Schema(hidden = true)
    private boolean projectStatusProvided;

    @JsonIgnore
    @Schema(hidden = true)
    private boolean locationProvided;

    @JsonIgnore
    @Schema(hidden = true)
    private boolean startDateProvided;

    @JsonIgnore
    @Schema(hidden = true)
    private boolean endDateProvided;

    @JsonIgnore
    @Schema(hidden = true)
    private boolean objectivesProvided;

    @JsonIgnore
    @Schema(hidden = true)
    private boolean impactSummaryProvided;

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

    public void setProjectStatus(String projectStatus) {
        this.projectStatus = projectStatus;
        this.projectStatusProvided = true;
    }

    public void setLocation(String location) {
        this.location = location;
        this.locationProvided = true;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
        this.startDateProvided = true;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
        this.endDateProvided = true;
    }

    public void setObjectives(String objectives) {
        this.objectives = objectives;
        this.objectivesProvided = true;
    }

    public void setImpactSummary(String impactSummary) {
        this.impactSummary = impactSummary;
        this.impactSummaryProvided = true;
    }
}
