package com.lokmit.foundation.employment.job.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Job creation payload. Validation mirrors the V8 jobs constraints exactly:
 * lengths, CHECK-constraint enums, salary digits, and the chk_jobs_salary_range
 * pair rule (enforced in the service on the final pair). status and
 * published_at are NOT writable — new jobs always start DRAFT.
 */
@Schema(description = "Create a job. New jobs always start as DRAFT.")
@Getter
@Setter
public class JobCreateRequest {

    @NotBlank
    @Size(max = 180)
    @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$",
            message = "slug must be lowercase alphanumeric words separated by hyphens")
    @Schema(description = "URL-friendly identifier (unique, immutable)")
    private String slug;

    @NotBlank
    @Size(max = 255)
    @Schema(description = "Job title")
    private String title;

    @NotBlank
    @Schema(description = "Full job description (text)")
    private String description;

    @Schema(description = "Requirements (text, optional)")
    private String requirements;

    @NotNull(message = "employerId is required")
    @Min(value = 1, message = "Employer id must be a positive number")
    @Schema(description = "Owning employer id (must exist)")
    private Long employerId;

    @Min(value = 1, message = "Category id must be a positive number")
    @Schema(description = "Optional category id (must exist when provided)")
    private Long categoryId;

    @NotNull
    @Pattern(regexp = "FULL_TIME|PART_TIME|CONTRACT|INTERNSHIP|TEMPORARY",
            message = "employmentType must be one of FULL_TIME, PART_TIME, CONTRACT, INTERNSHIP, TEMPORARY")
    @Schema(description = "Employment type (chk_jobs_employment_type)")
    private String employmentType;

    @NotNull
    @Pattern(regexp = "ONSITE|REMOTE|HYBRID",
            message = "workMode must be one of ONSITE, REMOTE, HYBRID")
    @Schema(description = "Work mode (chk_jobs_work_mode)")
    private String workMode;

    @Size(max = 255)
    @Schema(description = "Work location")
    private String workLocation;

    @Digits(integer = 10, fraction = 2)
    @Schema(description = "Minimum salary")
    private BigDecimal salaryMin;

    @Digits(integer = 10, fraction = 2)
    @Schema(description = "Maximum salary (must be ≥ salaryMin when both provided)")
    private BigDecimal salaryMax;

    @Size(max = 8)
    @Schema(description = "ISO currency code", defaultValue = "INR")
    private String salaryCurrency;

    @Min(value = 1, message = "Openings must be at least 1 when provided")
    @Schema(description = "Number of openings")
    private Integer openings;

    @Schema(description = "Application deadline (ISO-8601 date)")
    private LocalDate applicationDeadline;
}
