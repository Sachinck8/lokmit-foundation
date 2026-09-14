package com.lokmit.foundation.projects.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Creation payload for a project. The unique slug is enforced by the
 * database and pre-checked for a clean 409. categoryId is validated against
 * the project_categories table (404 when unknown). New projects always start
 * DRAFT (the schema default); publishing runs through the dedicated
 * transition endpoint. chk_projects_dates guarantees end >= start whenever
 * both are provided — malformed pairs are rejected here with 400 before the
 * database ever sees them.
 */
@Getter
@Setter
@Schema(description = "Create a project. New projects always start as DRAFT.")
public class ProjectCreateRequest {

    @NotBlank(message = "Slug is required")
    @Size(max = 160, message = "Slug must not exceed 160 characters")
    @Pattern(regexp = "[a-z0-9_.-]+", message = "Slug may contain lowercase letters, digits, dot, dash, underscore")
    @Schema(description = "URL-friendly identifier (unique, immutable)", example = "rural-water-supply")
    private String slug;

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    @Schema(description = "Project display title", example = "Rural Water Supply Programme")
    private String title;

    @Size(max = 500, message = "Summary must not exceed 500 characters")
    @Schema(description = "Short summary for portfolio listings")
    private String summary;

    @Schema(description = "Full project description (plain text)")
    private String description;

    @Min(value = 1, message = "Category id must be a positive number")
    @Schema(description = "Owning category id. Omit or null for an uncategorized project.", example = "1")
    private Long categoryId;

    @Pattern(regexp = "PLANNING|ONGOING|COMPLETED", message = "Project status must be one of: PLANNING, ONGOING, COMPLETED")
    @Schema(description = "Delivery state (nullable)", example = "ONGOING")
    private String projectStatus;

    @Size(max = 255, message = "Location must not exceed 255 characters")
    @Schema(description = "Project location", example = "Kathmandu, Nepal")
    private String location;

    @Schema(description = "Start date (ISO-8601)")
    private LocalDate startDate;

    @Schema(description = "End date (ISO-8601). Must be on or after startDate when both are provided.")
    private LocalDate endDate;

    @Schema(description = "Objectives document as well-formed JSON (JSONB)")
    private String objectives;

    @Schema(description = "Impact summary (plain text)")
    private String impactSummary;
}
