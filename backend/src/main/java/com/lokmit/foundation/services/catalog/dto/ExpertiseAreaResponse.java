package com.lokmit.foundation.services.catalog.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

/** Safe admin view of an expertise area row. */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Expertise area as visible to authorized administrators")
public class ExpertiseAreaResponse {

    @Schema(description = "Expertise area identifier", example = "4")
    private Long id;

    @Schema(description = "URL-friendly identifier", example = "bim-modelling")
    private String slug;

    @Schema(description = "Expertise display name", example = "BIM Modelling")
    private String name;

    @Schema(description = "Full description")
    private String description;

    @Schema(description = "Display ordering", example = "0")
    private int displayOrder;

    @Schema(description = "Lifecycle status", example = "DRAFT",
            allowableValues = {"DRAFT", "PUBLISHED", "ARCHIVED"})
    private String status;

    @Schema(description = "When the area was created")
    private OffsetDateTime createdAt;

    @Schema(description = "When the area was last updated")
    private OffsetDateTime updatedAt;
}
