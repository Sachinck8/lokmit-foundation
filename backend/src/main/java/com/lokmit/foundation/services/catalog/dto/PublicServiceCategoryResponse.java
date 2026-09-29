package com.lokmit.foundation.services.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.OffsetDateTime;

/**
 * Public (anonymous) service-category representation for the A26 public
 * services API. Only {@code status = ACTIVE} categories are ever mapped into
 * this DTO — no lifecycle status and no admin bookkeeping beyond
 * {@code updatedAt}.
 */
@Getter
@Schema(description = "Publicly visible service category (active categories only)")
public class PublicServiceCategoryResponse {

    private final Long id;
    private final String name;
    private final String slug;
    private final String description;
    private final int displayOrder;
    private final OffsetDateTime updatedAt;

    public PublicServiceCategoryResponse(Long id, String name, String slug, String description,
                                         int displayOrder, OffsetDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.displayOrder = displayOrder;
        this.updatedAt = updatedAt;
    }
}
