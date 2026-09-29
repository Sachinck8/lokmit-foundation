package com.lokmit.foundation.projects.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

/**
 * Safe administrative representation of a project category. DTO-only —
 * no JPA entity is ever serialized.
 */
@Getter
@Builder
@Schema(description = "Project category (administrative view)")
public class ProjectCategoryResponse {

    private Long id;
    private String name;
    private String slug;
    private String description;
    private int displayOrder;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
