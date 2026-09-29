package com.lokmit.foundation.employment.experience.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Schema(description = "Payload for partially updating one of the candidate's experience records")
@Getter
@Setter
public class CandidateExperienceUpdateRequest {

    @Schema(description = "Company name")
    @Size(max = 255, message = "companyName must not exceed 255 characters")
    private String companyName;

    @Schema(description = "Job title")
    @Size(max = 255, message = "jobTitle must not exceed 255 characters")
    private String jobTitle;

    @Schema(description = "Description")
    @Size(max = 20000, message = "description must not exceed 20000 characters")
    private String description;

    @Schema(description = "Start date (ISO yyyy-MM-dd)")
    private LocalDate startDate;

    @Schema(description = "End date (ISO yyyy-MM-dd)")
    private LocalDate endDate;
}
