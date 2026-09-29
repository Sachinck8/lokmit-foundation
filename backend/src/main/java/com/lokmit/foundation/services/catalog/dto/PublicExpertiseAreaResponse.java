package com.lokmit.foundation.services.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.OffsetDateTime;

/**
 * Public (anonymous) expertise-area representation for the A26 public
 * expertise API. Only {@code status = PUBLISHED} areas are ever mapped into
 * this DTO — no lifecycle status and no record bookkeeping beyond
 * {@code updatedAt}.
 */
@Getter
@Schema(description = "Publicly visible expertise area (published areas only)")
public class PublicExpertiseAreaResponse {

    private final Long id;
    private final String slug;
    private final String name;
    private final String description;
    private final int displayOrder;
    private final OffsetDateTime updatedAt;

    public PublicExpertiseAreaResponse(Long id, String slug, String name, String description,
                                       int displayOrder, OffsetDateTime updatedAt) {
        this.id = id;
        this.slug = slug;
        this.name = name;
        this.description = description;
        this.displayOrder = displayOrder;
        this.updatedAt = updatedAt;
    }
}
