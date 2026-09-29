package com.lokmit.foundation.projects.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Public (anonymous) project representation for the A26 public projects API.
 *
 * <p>Only {@code status = PUBLISHED} projects are ever mapped into this DTO.
 * It is a deliberately slimmer projection than the admin
 * {@link ProjectResponse}: no editorial lifecycle {@code status}, no
 * record-management timestamps beyond {@code updatedAt}, no images
 * (the admin image metadata stays admin-only), and category data reduced to
 * the safe public identity fields. {@code projectStatus} (delivery state)
 * is part of the project's public story and stays.</p>
 */
@Getter
@Schema(description = "Publicly visible project (published projects only)")
public class PublicProjectResponse {

    private final Long id;
    private final String slug;
    private final String title;
    private final String summary;
    private final String description;
    /** Public identity of the owning project category (ACTIVE categories only). */
    private final PublicProjectCategoryResponse category;
    /** Delivery state (PLANNING/ONGOING/COMPLETED) — part of the public story. */
    private final String projectStatus;
    private final String location;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final String objectives;
    private final String impactSummary;
    private final OffsetDateTime publishedAt;
    private final OffsetDateTime updatedAt;

    public PublicProjectResponse(Long id, String slug, String title, String summary,
                                 String description, PublicProjectCategoryResponse category,
                                 String projectStatus, String location, LocalDate startDate,
                                 LocalDate endDate, String objectives, String impactSummary,
                                 OffsetDateTime publishedAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.slug = slug;
        this.title = title;
        this.summary = summary;
        this.description = description;
        this.category = category;
        this.projectStatus = projectStatus;
        this.location = location;
        this.startDate = startDate;
        this.endDate = endDate;
        this.objectives = objectives;
        this.impactSummary = impactSummary;
        this.publishedAt = publishedAt;
        this.updatedAt = updatedAt;
    }
}
