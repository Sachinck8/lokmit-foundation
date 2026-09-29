package com.lokmit.foundation.cms.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Creation payload for an SEO metadata record. One record per entity
 * (uq_seo_metadata_entity); duplicates are pre-checked in the service for a
 * clean 409.
 */
@Getter
@Setter
@Schema(description = "Create an SEO metadata record for an entity.")
public class SeoMetadataUpsertRequest {

    @NotBlank(message = "Entity type is required")
    @Size(max = 50, message = "Entity type must not exceed 50 characters")
    @Pattern(regexp = "[a-z0-9_]+", message = "Entity type may contain lowercase letters, digits and underscores")
    @Schema(description = "Referenced entity type", example = "website_content")
    private String entityType;

    @NotNull(message = "Entity id is required")
    @Schema(description = "Referenced entity id", example = "7")
    private Long entityId;

    @Size(max = 255, message = "SEO title must not exceed 255 characters")
    @Schema(description = "SEO title")
    private String seoTitle;

    @Size(max = 500, message = "SEO description must not exceed 500 characters")
    @Schema(description = "SEO description")
    private String seoDescription;

    @Size(max = 500, message = "Canonical URL must not exceed 500 characters")
    @Schema(description = "Canonical URL")
    private String canonicalUrl;

    @Size(max = 255, message = "OG title must not exceed 255 characters")
    @Schema(description = "OpenGraph title")
    private String ogTitle;

    @Size(max = 500, message = "OG description must not exceed 500 characters")
    @Schema(description = "OpenGraph description")
    private String ogDescription;

    @Size(max = 500, message = "OG image URL must not exceed 500 characters")
    @Schema(description = "OpenGraph image URL")
    private String ogImageUrl;
}
