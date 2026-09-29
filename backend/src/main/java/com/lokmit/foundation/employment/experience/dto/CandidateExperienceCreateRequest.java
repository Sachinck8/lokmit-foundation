package com.lokmit.foundation.employment.experience.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Schema(description = "Payload for adding an experience record to the authenticated candidate")
@Getter
@Setter
public class CandidateExperienceCreateRequest {

    @Schema(description = "Company name", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "companyName is required")
    @Size(max = 255, message = "companyName must not exceed 255 characters")
    private String companyName;

    @Schema(description = "Job title", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "jobTitle is required")
    @Size(max = 255, message = "jobTitle must not exceed 255 characters")
    private String jobTitle;

    @Schema(description = "Description (optional)")
    @Size(max = 20000, message = "description must not exceed 20000 characters")
    private String description;

    @Schema(description = "Start date (ISO yyyy-MM-dd)", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "startDate is required")
    private LocalDate startDate;

    @Schema(description = "End date (ISO yyyy-MM-dd, optional)")
    private LocalDate endDate;
}
