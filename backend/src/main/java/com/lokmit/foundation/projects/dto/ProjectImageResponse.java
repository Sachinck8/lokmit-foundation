package com.lokmit.foundation.projects.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

/**
 * Safe administrative representation of project image metadata. The
 * owning project is rendered as a minimal reference (id + slug + title) to
 * keep gallery responses light.
 */
@Getter
@Builder
@Schema(description = "Project image metadata (administrative view)")
public class ProjectImageResponse {

    private Long id;
    private ProjectRef project;
    private String imageUrl;
    private String altText;
    private String caption;
    private int displayOrder;
    private OffsetDateTime createdAt;

    /** Minimal owning-project reference embedded in image responses. */
    @Getter
    @Builder
    @Schema(description = "Minimal owning-project reference")
    public static class ProjectRef {
        private Long id;
        private String slug;
        private String title;
    }
}
