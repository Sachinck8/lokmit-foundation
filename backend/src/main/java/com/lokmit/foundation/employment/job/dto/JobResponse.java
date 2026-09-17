package com.lokmit.foundation.employment.job.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Safe job representation. Relational data is embedded as safe summaries:
 * the employer exposes only company identity fields (never the linked
 * user's email, password hash, lockout or token material) and the category
 * only its display fields. No persistence internals leak.
 */
@Getter
public class JobResponse {

    /** Safe employer summary embedded in job responses. */
    @Getter
    public static class EmployerSummary {
        private final Long id;
        private final String companyName;
        private final String verificationStatus;

        public EmployerSummary(Long id, String companyName, String verificationStatus) {
            this.id = id;
            this.companyName = companyName;
            this.verificationStatus = verificationStatus;
        }
    }

    /** Safe category summary embedded in job responses. */
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

    private final Long id;
    private final EmployerSummary employer;
    private final CategorySummary category;
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
    private final String status;
    private final LocalDate applicationDeadline;
    private final OffsetDateTime publishedAt;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;

    private JobResponse(Builder b) {
        this.id = b.id;
        this.employer = b.employer;
        this.category = b.category;
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
        this.status = b.status;
        this.applicationDeadline = b.applicationDeadline;
        this.publishedAt = b.publishedAt;
        this.createdAt = b.createdAt;
        this.updatedAt = b.updatedAt;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private EmployerSummary employer;
        private CategorySummary category;
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
        private String status;
        private LocalDate applicationDeadline;
        private OffsetDateTime publishedAt;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;

        public Builder id(Long v) { this.id = v; return this; }
        public Builder employer(EmployerSummary v) { this.employer = v; return this; }
        public Builder category(CategorySummary v) { this.category = v; return this; }
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
        public Builder status(String v) { this.status = v; return this; }
        public Builder applicationDeadline(LocalDate v) { this.applicationDeadline = v; return this; }
        public Builder publishedAt(OffsetDateTime v) { this.publishedAt = v; return this; }
        public Builder createdAt(OffsetDateTime v) { this.createdAt = v; return this; }
        public Builder updatedAt(OffsetDateTime v) { this.updatedAt = v; return this; }
        public JobResponse build() { return new JobResponse(this); }
    }
}
