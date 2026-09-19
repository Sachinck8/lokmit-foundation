package com.lokmit.foundation.employment.education.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "Payload for partially updating one of the candidate's education records")
@Getter
@Setter
public class CandidateEducationUpdateRequest {

    @Schema(description = "Institution name")
    @Size(max = 255, message = "institution must not exceed 255 characters")
    private String institution;

    @Schema(description = "Degree")
    @Size(max = 255, message = "degree must not exceed 255 characters")
    private String degree;

    @Schema(description = "Field of study")
    @Size(max = 255, message = "fieldOfStudy must not exceed 255 characters")
    private String fieldOfStudy;

    @Schema(description = "Start year")
    @Min(value = 1950, message = "startYear must be 1950 or later")
    @Max(value = 2100, message = "startYear must be 2100 or earlier")
    private Integer startYear;

    @Schema(description = "End year")
    @Min(value = 1950, message = "endYear must be 1950 or later")
    @Max(value = 2100, message = "endYear must be 2100 or earlier")
    private Integer endYear;

    @Schema(description = "Grade")
    @Size(max = 100, message = "grade must not exceed 100 characters")
    private String grade;
}
