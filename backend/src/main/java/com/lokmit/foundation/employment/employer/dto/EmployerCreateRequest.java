package com.lokmit.foundation.employment.employer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Employer creation request. Validation lengths match the V8 column
 * definitions exactly; enum fields are validated with explicit pattern
 * rules mirroring the DB CHECK constraints.
 */
@Schema(description = "Payload for creating an employer profile")
@Getter
@Setter
public class EmployerCreateRequest {

    @Schema(description = "Linked user id", example = "42")
    @NotNull(message = "userId is required")
    private Long userId;

    @Schema(description = "Company name", example = "Acme Ltd")
    @NotBlank
    @Size(max = 255)
    private String companyName;

    @Schema(description = "About text (unbounded text column)")
    @Size(max = 20000)
    private String about;

    @Schema(description = "Website URL", example = "https://acme.example")
    @Size(max = 500)
    private String websiteUrl;

    @Schema(description = "Logo URL", example = "https://cdn.example/logo.png")
    @Size(max = 500)
    private String logoUrl;

    @Schema(description = "Contact person name")
    @Size(max = 255)
    private String contactPersonName;

    @Schema(description = "Contact phone")
    @Size(max = 50)
    private String contactPhone;

    @Schema(description = "Address")
    @Size(max = 500)
    private String address;

    @Schema(description = "Verification status", defaultValue = "UNVERIFIED")
    @Pattern(regexp = "UNVERIFIED|PENDING|VERIFIED|REJECTED",
            message = "verificationStatus must be one of UNVERIFIED, PENDING, VERIFIED, REJECTED")
    private String verificationStatus;

    @Schema(description = "Lifecycle status", defaultValue = "ACTIVE")
    @Pattern(regexp = "ACTIVE|SUSPENDED",
            message = "status must be ACTIVE or SUSPENDED")
    private String status;
}
