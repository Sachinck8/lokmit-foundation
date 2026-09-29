package com.lokmit.foundation.services.catalog.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

/** Safe admin view of a service row. */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Service as visible to authorized administrators")
public class ServiceItemResponse {

    @Schema(description = "Service identifier", example = "10")
    private Long id;

    @Schema(description = "URL-friendly identifier", example = "structural-design")
    private String slug;

    @Schema(description = "Service display title", example = "Structural Design & Analysis")
    private String title;

    @Schema(description = "Short summary for catalog listings")
    private String summary;

    @Schema(description = "Full service description")
    private String description;

    @Schema(description = "Owning category (null when uncategorized)")
    private ServiceCategoryResponse category;

    @Schema(description = "Display ordering within the catalog", example = "0")
    private int displayOrder;

    @Schema(description = "Lifecycle status", example = "DRAFT",
            allowableValues = {"DRAFT", "PUBLISHED", "ARCHIVED"})
    private String status;

    @Schema(description = "When the service was created")
    private OffsetDateTime createdAt;

    @Schema(description = "When the service was last updated")
    private OffsetDateTime updatedAt;
}
