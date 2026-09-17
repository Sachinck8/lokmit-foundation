package com.lokmit.foundation.cms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

/** Safe admin view of a website content row. */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Website content as visible to authorized administrators")
public class WebsiteContentResponse {

    @Schema(description = "Content identifier", example = "7")
    private Long id;

    @Schema(description = "Page identifier key", example = "home")
    private String pageKey;

    @Schema(description = "Section identifier key within the page", example = "hero")
    private String sectionKey;

    @Schema(description = "Section title")
    private String title;

    @Schema(description = "Section body as a JSON string")
    private String contentJson;

    @Schema(description = "Lifecycle status", example = "DRAFT",
            allowableValues = {"DRAFT", "PUBLISHED", "ARCHIVED"})
    private String status;

    @Schema(description = "When the content was created")
    private OffsetDateTime createdAt;

    @Schema(description = "When the content was last updated")
    private OffsetDateTime updatedAt;
}
