package com.lokmit.foundation.projects.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Creation payload for project image METADATA. The {@code imageUrl} is a
 * caller-supplied URL reference to an already-hosted image — A6 accepts no
 * binary files and implements no upload/storage (a separate future phase).
 * The owning project comes from the path, never the body.
 */
@Getter
@Setter
@Schema(description = "Add image metadata to a project. Accepts a URL reference only — no file upload.")
public class ProjectImageCreateRequest {

    @NotBlank(message = "Image URL is required")
    @Size(max = 500, message = "Image URL must not exceed 500 characters")
    @Schema(description = "URL reference to the hosted image", example = "https://cdn.example.org/images/site-visit-01.jpg")
    private String imageUrl;

    @Size(max = 255, message = "Alt text must not exceed 255 characters")
    @Schema(description = "Accessibility alt text")
    private String altText;

    @Size(max = 500, message = "Caption must not exceed 500 characters")
    @Schema(description = "Image caption")
    private String caption;

    @Min(value = 0, message = "Display order must be zero or greater")
    @Schema(description = "Display ordering within the project gallery", example = "0")
    private Integer displayOrder;
}
