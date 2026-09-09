package com.lokmit.foundation.contact.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Request payload for a public website enquiry submission.
 *
 * <p>Field lengths mirror the V7 'contact_messages' column limits so a
 * validated request can never violate a database constraint. The enquiry
 * category is a logical routing hint for staff; it has no dedicated database
 * column and is preserved in 'internal_note' by the service layer.</p>
 */
@Getter
@Setter
public class CreateContactMessageRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 255, message = "Full name must be at most 255 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    @Size(max = 255, message = "Email must be at most 255 characters")
    private String email;

    /** Optional phone; allowed characters keep the free-text field safe. */
    @Size(max = 50, message = "Phone number must be at most 50 characters")
    @Pattern(regexp = "^[0-9+()\\-\\s]*$", message = "Phone number contains invalid characters")
    private String phone;

    /** Logical enquiry category (e.g. consultancy, general). Stored in internal_note. */
    @Size(max = 100, message = "Enquiry category is too long")
    private String category;

    @NotBlank(message = "Subject is required")
    @Size(max = 255, message = "Subject must be at most 255 characters")
    private String subject;

    @NotBlank(message = "Message is required")
    @Size(max = 5000, message = "Message must be at most 5000 characters")
    private String message;
}
