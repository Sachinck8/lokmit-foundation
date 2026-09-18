package com.lokmit.foundation.employment.job.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Public (anonymous) job representation for the A8 public jobs browsing API.
 *
 * <p>Only legitimately published jobs are ever mapped into this DTO. It is a
 * deliberately smaller projection than the admin {@link JobResponse}: the
 * lifecycle/status machinery, publish/close/archive bookkeeping and
 * record-management timestamps are internal administration state and are not
 * exposed to anonymous visitors. The employer appears only as its public
 * company identity — never the linked user, verification workflow fields or
 * any security data.</p>
 */
@Getter
@Schema(description = "Publicly visible job posting (published jobs only)")
public class PublicJobResponse {

    private final Long id;
    private final String slug;
    private final String title;
    private final String description;
    private final String requirements;
    private final String employmentType;
    private final String workMode;
    private final String workLocation;
    private final BigDecimal salaryMin;
    private final BigDecimal salaryMax;
    private final String salaryCurrency;
    private final Integer openings;
    private final LocalDate applicationDeadline;
    private final OffsetDateTime publishedAt;
    /** Public company identity of the owning employer (name only). */
    private final String companyName;
    private final CategorySummary category;
    private final List<SkillSummary> skills;

    /** Safe category summary embedded in public job responses. */
    @Getter
    public static class CategorySummary {
        private final Long id;
        private final String name;
        private final String slug;

        public CategorySummary(Long id, String name, String slug) {
            this.id = id;
            this.name = name;
            this.slug = slug;
        }
    }

    /** Safe skill summary embedded in public job responses. */
    @Getter
    public static class SkillSummary {
        private final Long id;
        private final String name;

        public SkillSummary(Long id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    private PublicJobResponse(Builder b) {
        this.id = b.id;
        this.slug = b.slug;
        this.title = b.title;
        this.description = b.description;
        this.requirements = b.requirements;
        this.employmentType = b.employmentType;
        this.workMode = b.workMode;
        this.workLocation = b.workLocation;
        this.salaryMin = b.salaryMin;
        this.salaryMax = b.salaryMax;
        this.salaryCurrency = b.salaryCurrency;
        this.openings = b.openings;
        this.applicationDeadline = b.applicationDeadline;
        this.publishedAt = b.publishedAt;
        this.companyName = b.companyName;
        this.category = b.category;
        this.skills = b.skills;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private String slug;
        private String title;
        private String description;
        private String requirements;
        private String employmentType;
        private String workMode;
        private String workLocation;
        private BigDecimal salaryMin;
        private BigDecimal salaryMax;
        private String salaryCurrency;
        private Integer openings;
        private LocalDate applicationDeadline;
        private OffsetDateTime publishedAt;
        private String companyName;
        private CategorySummary category;
        private List<SkillSummary> skills;

        public Builder id(Long v) { this.id = v; return this; }
        public Builder slug(String v) { this.slug = v; return this; }
        public Builder title(String v) { this.title = v; return this; }
        public Builder description(String v) { this.description = v; return this; }
        public Builder requirements(String v) { this.requirements = v; return this; }
        public Builder employmentType(String v) { this.employmentType = v; return this; }
        public Builder workMode(String v) { this.workMode = v; return this; }
        public Builder workLocation(String v) { this.workLocation = v; return this; }
        public Builder salaryMin(BigDecimal v) { this.salaryMin = v; return this; }
        public Builder salaryMax(BigDecimal v) { this.salaryMax = v; return this; }
        public Builder salaryCurrency(String v) { this.salaryCurrency = v; return this; }
        public Builder openings(Integer v) { this.openings = v; return this; }
        public Builder applicationDeadline(LocalDate v) { this.applicationDeadline = v; return this; }
        public Builder publishedAt(OffsetDateTime v) { this.publishedAt = v; return this; }
        public Builder companyName(String v) { this.companyName = v; return this; }
        public Builder category(CategorySummary v) { this.category = v; return this; }
        public Builder skills(List<SkillSummary> v) { this.skills = v; return this; }
        public PublicJobResponse build() { return new PublicJobResponse(this); }
    }
}
