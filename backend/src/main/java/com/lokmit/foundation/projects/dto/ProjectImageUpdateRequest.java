package com.lokmit.foundation.projects.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Partial update of project image metadata. The owning project is
 * immutable — images are moved between projects only by delete + recreate,
 * keeping ownership unambiguous. Omitted fields stay unchanged; nullable
 * fields set explicitly to null clear them. {@code imageUrl} IS editable
 * (it is a reference, not an identity — the row id is the identity).
 */
@Getter
@Setter
@Schema(description = "Partial image-metadata update. Omitted fields stay unchanged. The owning project is immutable.")
public class ProjectImageUpdateRequest {

    @Size(max = 500, message = "Image URL must not exceed 500 characters")
    @Schema(description = "Updated URL reference to the hosted image")
    private String imageUrl;

    @Size(max = 255, message = "Alt text must not exceed 255 characters")
    @Schema(description = "Accessibility alt text. Set explicitly to null to clear it.")
    private String altText;

    @Size(max = 500, message = "Caption must not exceed 500 characters")
    @Schema(description = "Image caption. Set explicitly to null to clear it.")
    private String caption;

    @Min(value = 0, message = "Display order must be zero or greater")
    @Schema(description = "Display ordering within the project gallery")
    private Integer displayOrder;

    @JsonIgnore
    @Schema(hidden = true)
    private boolean imageUrlProvided;

    @JsonIgnore
    @Schema(hidden = true)
    private boolean altTextProvided;

    @JsonIgnore
    @Schema(hidden = true)
    private boolean captionProvided;

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
        this.imageUrlProvided = true;
    }

    public void setAltText(String altText) {
        this.altText = altText;
        this.altTextProvided = true;
    }

    public void setCaption(String caption) {
        this.caption = caption;
        this.captionProvided = true;
    }
}
