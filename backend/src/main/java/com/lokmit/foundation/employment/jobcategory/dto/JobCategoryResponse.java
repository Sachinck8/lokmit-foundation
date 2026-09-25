package com.lokmit.foundation.employment.jobcategory.dto;

import com.lokmit.foundation.employment.jobcategory.entity.JobCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/** Safe job category representation. */
@Getter
public class JobCategoryResponse {

    private final Long id;
    private final String name;
    private final String slug;
    private final String description;
    private final Integer displayOrder;
    private final String status;
    private final java.time.OffsetDateTime createdAt;
    private final java.time.OffsetDateTime updatedAt;

    private JobCategoryResponse(Builder b) {
        this.id = b.id;
        this.name = b.name;
        this.slug = b.slug;
        this.description = b.description;
        this.displayOrder = b.displayOrder;
        this.status = b.status;
        this.createdAt = b.createdAt;
        this.updatedAt = b.updatedAt;
    }

    public static Builder builder() { return new Builder(); }

    public static JobCategoryResponse from(JobCategory jc) {
        return builder()
                .id(jc.getId())
                .name(jc.getName())
                .slug(jc.getSlug())
                .description(jc.getDescription())
                .displayOrder(jc.getDisplayOrder())
                .status(jc.getStatus())
                .createdAt(jc.getCreatedAt())
                .updatedAt(jc.getUpdatedAt())
                .build();
    }

    public static class Builder {
        private Long id;
        private String name;
        private String slug;
        private String description;
        private Integer displayOrder;
        private String status;
        private java.time.OffsetDateTime createdAt;
        private java.time.OffsetDateTime updatedAt;

        public Builder id(Long v) { this.id = v; return this; }
        public Builder name(String v) { this.name = v; return this; }
        public Builder slug(String v) { this.slug = v; return this; }
        public Builder description(String v) { this.description = v; return this; }
        public Builder displayOrder(Integer v) { this.displayOrder = v; return this; }
        public Builder status(String v) { this.status = v; return this; }
        public Builder createdAt(java.time.OffsetDateTime v) { this.createdAt = v; return this; }
        public Builder updatedAt(java.time.OffsetDateTime v) { this.updatedAt = v; return this; }
        public JobCategoryResponse build() { return new JobCategoryResponse(this); }
    }
}
