package com.lokmit.foundation.employment.candidate.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Candidate creation request. Validation matches the V8 column definitions;
 * availability mirrors chk_candidates_availability.
 */
@Schema(description = "Payload for creating a candidate profile")
@Getter
@Setter
public class CandidateCreateRequest {

    @Schema(description = "Linked user id", example = "42")
    @NotNull(message = "userId is required")
    private Long userId;

    @Schema(description = "Date of birth")
    private LocalDate dateOfBirth;

    @Schema(description = "Gender")
    @Pattern(regexp = "MALE|FEMALE|OTHER",
            message = "gender must be MALE, FEMALE or OTHER")
    private String gender;

    @Schema(description = "Phone")
    @Size(max = 50)
    private String phone;

    @Schema(description = "Current location")
    @Size(max = 255)
    private String currentLocation;

    @Schema(description = "Professional summary")
    @Size(max = 20000)
    private String summary;

    @Schema(description = "Expected minimum salary")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal expectedSalaryMin;

    @Schema(description = "Expected maximum salary")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal expectedSalaryMax;

    @Schema(description = "Availability", defaultValue = "ACTIVELY_LOOKING")
    @Pattern(regexp = "ACTIVELY_LOOKING|OPEN_TO_OFFERS|NOT_LOOKING",
            message = "availability must be ACTIVELY_LOOKING, OPEN_TO_OFFERS or NOT_LOOKING")
    private String availability;
}
