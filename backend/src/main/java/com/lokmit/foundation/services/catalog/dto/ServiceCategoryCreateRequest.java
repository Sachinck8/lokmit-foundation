package com.lokmit.foundation.services.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Creation payload for a service category. The unique name and slug are
 * enforced by the database and pre-checked in the service for a clean 409.
 * New categories always start ACTIVE (the schema default); status changes run
 * through PATCH.
 */
@Getter
@Setter
@Schema(description = "Create a service category. New categories always start as ACTIVE.")
public class ServiceCategoryCreateRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    @Schema(description = "Category display name (unique)", example = "Engineering Services")
    private String name;

    @NotBlank(message = "Slug is required")
    @Size(max = 120, message = "Slug must not exceed 120 characters")
    @Pattern(regexp = "[a-z0-9_.-]+", message = "Slug may contain lowercase letters, digits, dot, dash, underscore")
    @Schema(description = "URL-friendly identifier (unique, immutable)", example = "engineering-services")
    private String slug;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    @Schema(description = "Optional category description")
    private String description;

    @Min(value = 0, message = "Display order must be zero or greater")
    @Schema(description = "Display ordering within the catalog (lower shows first)", example = "0")
    private Integer displayOrder;
}
