package com.lokmit.foundation.services.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Creation payload for a service. The unique slug is enforced by the
 * database and pre-checked in the service for a clean 409. categoryId is
 * validated against the service_categories table (404 when unknown). New
 * services always start DRAFT (the schema default); publishing runs through
 * the dedicated transition endpoint.
 */
@Getter
@Setter
@Schema(description = "Create a service. New services always start as DRAFT.")
public class ServiceItemCreateRequest {

    @NotBlank(message = "Slug is required")
    @Size(max = 160, message = "Slug must not exceed 160 characters")
    @Pattern(regexp = "[a-z0-9_.-]+", message = "Slug may contain lowercase letters, digits, dot, dash, underscore")
    @Schema(description = "URL-friendly identifier (unique, immutable)", example = "structural-design")
    private String slug;

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    @Schema(description = "Service display title", example = "Structural Design & Analysis")
    private String title;

    @Size(max = 500, message = "Summary must not exceed 500 characters")
    @Schema(description = "Short summary for catalog listings")
    private String summary;

    @Schema(description = "Full service description (plain text)")
    private String description;

    @Min(value = 1, message = "Category id must be a positive number")
    @Schema(description = "Owning category id. Omit or null for an uncategorized service.", example = "1")
    private Long categoryId;

    @Min(value = 0, message = "Display order must be zero or greater")
    @Schema(description = "Display ordering within the catalog", example = "0")
    private Integer displayOrder;
}
