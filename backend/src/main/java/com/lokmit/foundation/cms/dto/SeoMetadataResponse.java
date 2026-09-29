package com.lokmit.foundation.cms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

/** Safe admin view of an SEO metadata row. */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "SEO metadata as visible to authorized administrators")
public class SeoMetadataResponse {

    @Schema(description = "SEO record identifier", example = "3")
    private Long id;

    @Schema(description = "Referenced entity type (polymorphic, no FK)", example = "website_content")
    private String entityType;

    @Schema(description = "Referenced entity id (polymorphic, no FK)", example = "7")
    private Long entityId;

    @Schema(description = "SEO title")
    private String seoTitle;

    @Schema(description = "SEO description")
    private String seoDescription;

    @Schema(description = "Canonical URL")
    private String canonicalUrl;

    @Schema(description = "OpenGraph title")
    private String ogTitle;

    @Schema(description = "OpenGraph description")
    private String ogDescription;

    @Schema(description = "OpenGraph image URL")
    private String ogImageUrl;

    @Schema(description = "When the record was created")
    private OffsetDateTime createdAt;

    @Schema(description = "When the record was last updated")
    private OffsetDateTime updatedAt;
}
