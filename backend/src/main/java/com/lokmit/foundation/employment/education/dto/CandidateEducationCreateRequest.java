package com.lokmit.foundation.employment.education.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "Payload for adding an education record to the authenticated candidate")
@Getter
@Setter
public class CandidateEducationCreateRequest {

    @Schema(description = "Institution name", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "institution is required")
    @Size(max = 255, message = "institution must not exceed 255 characters")
    private String institution;

    @Schema(description = "Degree", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "degree is required")
    @Size(max = 255, message = "length must not exceed 255 characters")
    private String degree;

    @Schema(description = "Field of study (optional)")
    @Size(max = 255, message = "fieldOfStudy must not exceed 255 characters")
    private String fieldOfStudy;

    @Schema(description = "Start year", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "startYear is required")
    @Min(value = 1950, message = "startYear must be 1950 or later")
    @Max(value = 2100, message = "startYear must be 2100 or earlier")
    private Integer startYear;

    @Schema(description = "End year (optional)")
    @Min(value = 1950, message = "endYear must be 1950 or later")
    @Max(value = 2100, message = "endYear must be 2100 or earlier")
    private Integer endYear;

    @Schema(description = "Grade (optional)")
    @Size(max = 100, message = "grade must not exceed 100 characters")
    private String grade;
}
