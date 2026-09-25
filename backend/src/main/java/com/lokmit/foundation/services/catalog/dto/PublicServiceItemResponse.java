package com.lokmit.foundation.services.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.OffsetDateTime;

/**
 * Public (anonymous) service representation for the A26 public services API.
 *
 * <p>Only {@code status = PUBLISHED} service items are ever mapped into this
 * DTO. It is a deliberately slimmer projection than the admin
 * {@link ServiceItemResponse}: no lifecycle status, no record-management
 * timestamps beyond {@code updatedAt}, and category data reduced to the safe
 * public identity fields.</p>
 */
@Getter
@Schema(description = "Publicly visible service (published services only)")
public class PublicServiceItemResponse {

    private final Long id;
    private final String slug;
    private final String title;
    private final String summary;
    private final String description;
    private final int displayOrder;
    /** Public identity of the owning service category (ACTIVE categories only). */
    private final PublicServiceCategoryResponse category;
    private final OffsetDateTime updatedAt;

    public PublicServiceItemResponse(Long id, String slug, String title, String summary,
                                     String description, int displayOrder,
                                     PublicServiceCategoryResponse category,
                                     OffsetDateTime updatedAt) {
        this.id = id;
        this.slug = slug;
        this.title = title;
        this.summary = summary;
        this.description = description;
        this.displayOrder = displayOrder;
        this.category = category;
        this.updatedAt = updatedAt;
    }
}
