package com.lokmit.foundation.employment.job.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Job partial update. Omitted fields stay unchanged. The slug (URL identity,
 * per project convention) and the owning employer are immutable. status is
 * deliberately absent — the lifecycle moves only through the dedicated
 * transition endpoints. The resulting salary pair is validated after merge
 * (chk_jobs_salary_range).
 */
@Schema(description = "Partially update a job. Status changes use the lifecycle endpoints.")
@Getter
@Setter
public class JobUpdateRequest {

    @Size(max = 255)
    @Schema(description = "Job title")
    private String title;

    @Schema(description = "Full job description")
    private String description;

    @Schema(description = "Requirements; explicitly null clears the field")
    private String requirements;

    @Min(value = 1, message = "Category id must be a positive number")
    @Schema(description = "Category id; explicitly null detaches the job from its category")
    private Long categoryId;

    @Pattern(regexp = "FULL_TIME|PART_TIME|CONTRACT|INTERNSHIP|TEMPORARY",
            message = "employmentType must be one of FULL_TIME, PART_TIME, CONTRACT, INTERNSHIP, TEMPORARY")
    @Schema(description = "Employment type")
    private String employmentType;

    @Pattern(regexp = "ONSITE|REMOTE|HYBRID",
            message = "workMode must be one of ONSITE, REMOTE, HYBRID")
    @Schema(description = "Work mode")
    private String workMode;

    @Size(max = 255)
    @Schema(description = "Work location; explicitly null clears the field")
    private String workLocation;

    @Digits(integer = 10, fraction = 2)
    @Schema(description = "Minimum salary; explicitly null clears the field")
    private BigDecimal salaryMin;

    @Digits(integer = 10, fraction = 2)
    @Schema(description = "Maximum salary; explicitly null clears the field")
    private BigDecimal salaryMax;

    @Size(max = 8)
    @Schema(description = "ISO currency code")
    private String salaryCurrency;

    @Min(value = 1, message = "Openings must be at least 1 when provided")
    @Schema(description = "Number of openings")
    private Integer openings;

    @Schema(description = "Application deadline; explicitly null clears the field")
    private LocalDate applicationDeadline;
}
