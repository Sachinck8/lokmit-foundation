package com.lokmit.foundation.services.catalog.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

/** Safe admin view of a service category row. */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Service category as visible to authorized administrators")
public class ServiceCategoryResponse {

    @Schema(description = "Category identifier", example = "1")
    private Long id;

    @Schema(description = "Category display name", example = "Engineering Services")
    private String name;

    @Schema(description = "URL-friendly identifier", example = "engineering-services")
    private String slug;

    @Schema(description = "Category description")
    private String description;

    @Schema(description = "Display ordering within the catalog", example = "0")
    private int displayOrder;

    @Schema(description = "Lifecycle status", example = "ACTIVE",
            allowableValues = {"ACTIVE", "INACTIVE"})
    private String status;

    @Schema(description = "When the category was created")
    private OffsetDateTime createdAt;

    @Schema(description = "When the category was last updated")
    private OffsetDateTime updatedAt;
}
