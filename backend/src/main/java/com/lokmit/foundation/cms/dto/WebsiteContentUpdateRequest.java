package com.lokmit.foundation.cms.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Partial update of a website content section. The identity keys
 * (pageKey/sectionKey) are deliberately absent — sections are identified by
 * the path id, and renaming them would silently orphan the unique pair.
 * Status is intentionally NOT updatable here; lifecycle transitions run
 * through the dedicated publish/archive endpoints guarded by
 * content:publish.
 */
@Getter
@Setter
@Schema(description = "Partial content update. Omitted fields stay unchanged. Status changes use the dedicated lifecycle endpoints.")
public class WebsiteContentUpdateRequest {

    @Size(max = 255, message = "Title must not exceed 255 characters")
    @Schema(description = "Section title. Set explicitly to null to clear it.")
    private String title;

    @Size(max = 100000, message = "Content JSON must not exceed 100000 characters")
    @Schema(description = "Section body as a JSON string. Set explicitly to null to clear it.")
    private String contentJson;

    @JsonIgnore
    @Schema(hidden = true)
    private boolean titleProvided;

    @JsonIgnore
    @Schema(hidden = true)
    private boolean contentJsonProvided;

    public void setTitle(String title) {
        this.title = title;
        this.titleProvided = true;
    }

    public void setContentJson(String contentJson) {
        this.contentJson = contentJson;
        this.contentJsonProvided = true;
    }
}
