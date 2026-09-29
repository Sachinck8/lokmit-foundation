package com.lokmit.foundation.employment.candidate.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Candidate partial update. Omitted fields stay unchanged. userId is
 * immutable — one profile per user identity (uq_candidates_user).
 */
@Schema(description = "Payload for partially updating a candidate profile")
@Getter
@Setter
public class CandidateUpdateRequest {

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

    @Schema(description = "Availability")
    @Pattern(regexp = "ACTIVELY_LOOKING|OPEN_TO_OFFERS|NOT_LOOKING",
            message = "availability must be ACTIVELY_LOOKING, OPEN_TO_OFFERS or NOT_LOOKING")
    private String availability;
}
