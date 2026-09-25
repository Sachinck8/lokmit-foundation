package com.lokmit.foundation.cms.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Partial update of an SEO metadata record. The identity pair
 * (entityType, entityId) is deliberately absent — records are identified by
 * the path id and the pair is the unique key, so it must never move. Each
 * content field distinguishes "omitted" (leave unchanged) from "present with
 * JSON null" (clear the field) via binding-time provided flags, set by the
 * JSON-binding setters and invisible to clients.
 */
@Getter
@Setter
@Schema(description = "Partial SEO metadata update. Omitted fields stay unchanged.")
public class SeoMetadataUpdateRequest {

    @Size(max = 255, message = "SEO title must not exceed 255 characters")
    @Schema(description = "SEO title. Set explicitly to null to clear it.")
    private String seoTitle;

    @Size(max = 500, message = "SEO description must not exceed 500 characters")
    @Schema(description = "SEO description. Set explicitly to null to clear it.")
    private String seoDescription;

    @Size(max = 500, message = "Canonical URL must not exceed 500 characters")
    @Schema(description = "Canonical URL. Set explicitly to null to clear it.")
    private String canonicalUrl;

    @Size(max = 255, message = "OG title must not exceed 255 characters")
    @Schema(description = "OpenGraph title. Set explicitly to null to clear it.")
    private String ogTitle;

    @Size(max = 500, message = "OG description must not exceed 500 characters")
    @Schema(description = "OpenGraph description. Set explicitly to null to clear it.")
    private String ogDescription;

    @Size(max = 500, message = "OG image URL must not exceed 500 characters")
    @Schema(description = "OpenGraph image URL. Set explicitly to null to clear it.")
    private String ogImageUrl;

    @JsonIgnore
    @Schema(hidden = true)
    private boolean seoTitleProvided;

    @JsonIgnore
    @Schema(hidden = true)
    private boolean seoDescriptionProvided;

    @JsonIgnore
    @Schema(hidden = true)
    private boolean canonicalUrlProvided;

    @JsonIgnore
    @Schema(hidden = true)
    private boolean ogTitleProvided;

    @JsonIgnore
    @Schema(hidden = true)
    private boolean ogDescriptionProvided;

    @JsonIgnore
    @Schema(hidden = true)
    private boolean ogImageUrlProvided;

    public void setSeoTitle(String v) { this.seoTitle = v; this.seoTitleProvided = true; }
    public void setSeoDescription(String v) { this.seoDescription = v; this.seoDescriptionProvided = true; }
    public void setCanonicalUrl(String v) { this.canonicalUrl = v; this.canonicalUrlProvided = true; }
    public void setOgTitle(String v) { this.ogTitle = v; this.ogTitleProvided = true; }
    public void setOgDescription(String v) { this.ogDescription = v; this.ogDescriptionProvided = true; }
    public void setOgImageUrl(String v) { this.ogImageUrl = v; this.ogImageUrlProvided = true; }
}
