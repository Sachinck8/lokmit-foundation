package com.lokmit.foundation.projects.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.OffsetDateTime;

/**
 * Public (anonymous) project-category representation for the A26 public
 * projects API. Only {@code status = ACTIVE} categories are ever mapped into
 * this DTO — no lifecycle status and no admin bookkeeping beyond
 * {@code updatedAt}.
 */
@Getter
@Schema(description = "Publicly visible project category (active categories only)")
public class PublicProjectCategoryResponse {

    private final Long id;
    private final String name;
    private final String slug;
    private final String description;
    private final int displayOrder;
    private final OffsetDateTime updatedAt;

    public PublicProjectCategoryResponse(Long id, String name, String slug, String description,
                                         int displayOrder, OffsetDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.displayOrder = displayOrder;
        this.updatedAt = updatedAt;
    }
}
