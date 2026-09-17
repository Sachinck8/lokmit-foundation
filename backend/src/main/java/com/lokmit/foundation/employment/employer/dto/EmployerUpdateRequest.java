package com.lokmit.foundation.employment.employer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Employer partial update. Omitted fields stay unchanged; explicitly
 * supplied null clears an optional field. userId is immutable — the
 * profile is bound to exactly one user identity per uq_employers_user.
 */
@Schema(description = "Payload for partially updating an employer profile")
@Getter
@Setter
public class EmployerUpdateRequest {

    @Schema(description = "Company name")
    @Size(max = 255)
    private String companyName;

    @Schema(description = "About text")
    @Size(max = 20000)
    private String about;

    @Schema(description = "Website URL")
    @Size(max = 500)
    private String websiteUrl;

    @Schema(description = "Logo URL")
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

    @Schema(description = "Verification status")
    @Pattern(regexp = "UNVERIFIED|PENDING|VERIFIED|REJECTED",
            message = "verificationStatus must be one of UNVERIFIED, PENDING, VERIFIED, REJECTED")
    private String verificationStatus;

    @Schema(description = "Lifecycle status")
    @Pattern(regexp = "ACTIVE|SUSPENDED",
            message = "status must be ACTIVE or SUSPENDED")
    private String status;
}
