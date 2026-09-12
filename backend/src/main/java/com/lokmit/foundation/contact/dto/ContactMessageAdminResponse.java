package com.lokmit.foundation.contact.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

/**
 * Full enquiry payload for authorized staff/admin management views.
 *
 * <p>Distinct from the public {@link ContactMessageResponse}, which exposes
 * only submission confirmation data. This DTO is returned only from
 * endpoints guarded by the {@code messages:manage} permission and contains
 * everything staff need to handle an enquiry.</p>
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Contact enquiry as visible to authorized staff")
public class ContactMessageAdminResponse {

    @Schema(description = "Enquiry identifier", example = "42")
    private Long id;

    @Schema(description = "Sender's full name", example = "Ramesh Kumar")
    private String senderName;

    @Schema(description = "Sender's email address", example = "ramesh@example.com")
    private String senderEmail;

    @Schema(description = "Sender's phone number, if provided", example = "+91 90000 00000")
    private String senderPhone;

    @Schema(description = "Enquiry subject", example = "Skill development programme enquiry")
    private String subject;

    @Schema(description = "Enquiry message body")
    private String message;

    @Schema(description = "Lifecycle status", example = "NEW",
            allowableValues = {"NEW", "READ", "REPLIED", "ARCHIVED"})
    private String status;

    @Schema(description = "Internal staff note; also carries the public form's enquiry category")
    private String internalNote;

    @Schema(description = "When the enquiry was received")
    private OffsetDateTime createdAt;

    @Schema(description = "When the enquiry was last updated")
    private OffsetDateTime updatedAt;
}
