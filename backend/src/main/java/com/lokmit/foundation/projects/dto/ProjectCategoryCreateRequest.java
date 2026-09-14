package com.lokmit.foundation.projects.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Creation payload for a project category. Both name and slug are unique
 * business keys (uq_project_categories_name / uq_project_categories_slug),
 * pre-checked for a clean 409. New categories always start ACTIVE (the
 * schema default).
 */
@Getter
@Setter
@Schema(description = "Create a project category. New categories always start as ACTIVE.")
public class ProjectCategoryCreateRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    @Schema(description = "Category display name (unique)", example = "Infrastructure")
    private String name;

    @NotBlank(message = "Slug is required")
    @Size(max = 120, message = "Slug must not exceed 120 characters")
    @Pattern(regexp = "[a-z0-9_.-]+", message = "Slug may contain lowercase letters, digits, dot, dash, underscore")
    @Schema(description = "URL-friendly identifier (unique, immutable)", example = "infrastructure")
    private String slug;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    @Schema(description = "Optional category description")
    private String description;

    @Schema(description = "Display ordering within the category list", example = "0")
    private Integer displayOrder;
}
