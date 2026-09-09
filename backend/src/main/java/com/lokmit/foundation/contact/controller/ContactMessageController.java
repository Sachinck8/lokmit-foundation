package com.lokmit.foundation.contact.controller;

import com.lokmit.foundation.common.api.ApiResponse;
import com.lokmit.foundation.common.constants.ApiPaths;
import com.lokmit.foundation.contact.dto.ContactMessageResponse;
import com.lokmit.foundation.contact.dto.CreateContactMessageRequest;
import com.lokmit.foundation.contact.service.ContactMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public endpoint for website enquiry submissions.
 *
 * <p>Creation is the only operation exposed publicly; reading and managing
 * messages is an authenticated admin concern handled in a later phase.</p>
 */
@RestController
@RequestMapping(ApiPaths.CONTACT_MESSAGES)
@Tag(name = "Contact", description = "Public website enquiry submissions")
public class ContactMessageController {

    private final ContactMessageService contactMessageService;

    public ContactMessageController(ContactMessageService contactMessageService) {
        this.contactMessageService = contactMessageService;
    }

    @PostMapping
    @Operation(summary = "Submit a public website enquiry",
            description = "Stores the enquiry for staff review. No authentication required.")
    public ResponseEntity<ApiResponse<ContactMessageResponse>> submitEnquiry(
            @Valid @RequestBody CreateContactMessageRequest request) {
        ContactMessageResponse response = contactMessageService.submitEnquiry(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Enquiry received. We will respond to you shortly."));
    }
}
