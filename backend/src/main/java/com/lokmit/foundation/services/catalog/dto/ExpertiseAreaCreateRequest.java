package com.lokmit.foundation.services.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Creation payload for an expertise area. The unique slug is enforced by the
 * database and pre-checked in the service for a clean 409. New areas always
 * start DRAFT (the schema default); publishing runs through the dedicated
 * transition endpoint.
 */
@Getter
@Setter
@Schema(description = "Create an expertise area. New areas always start as DRAFT.")
public class ExpertiseAreaCreateRequest {

    @NotBlank(message = "Slug is required")
    @Size(max = 160, message = "Slug must not exceed 160 characters")
    @Pattern(regexp = "[a-z0-9_.-]+", message = "Slug may contain lowercase letters, digits, dot, dash, underscore")
    @Schema(description = "URL-friendly identifier (unique, immutable)", example = "bim-modelling")
    private String slug;

    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    @Schema(description = "Expertise display name", example = "BIM Modelling")
    private String name;

    @Schema(description = "Full description (plain text)")
    private String description;

    @Min(value = 0, message = "Display order must be zero or greater")
    @Schema(description = "Display ordering", example = "0")
    private Integer displayOrder;
}
