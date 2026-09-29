package com.lokmit.foundation.projects.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Safe administrative representation of a project. DTO-only — no JPA
 * entity is ever serialized. The owning category is rendered as the full
 * safe category DTO.
 */
@Getter
@Builder
@Schema(description = "Project (administrative view)")
public class ProjectResponse {

    private Long id;
    private String slug;
    private String title;
    private String summary;
    private String description;
    private ProjectCategoryResponse category;
    private String status;
    private String projectStatus;
    private String location;
    private LocalDate startDate;
    private LocalDate endDate;
    private String objectives;
    private String impactSummary;
    private OffsetDateTime publishedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
