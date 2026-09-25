package com.lokmit.foundation.contact.controller;

import com.lokmit.foundation.common.api.ApiResponse;
import com.lokmit.foundation.common.api.PageResponse;
import com.lokmit.foundation.common.constants.ApiPaths;
import com.lokmit.foundation.common.exception.BadRequestException;
import com.lokmit.foundation.common.pagination.PageParams;
import com.lokmit.foundation.contact.dto.ContactMessageAdminResponse;
import com.lokmit.foundation.contact.dto.ContactMessageResponse;
import com.lokmit.foundation.contact.dto.ContactMessageUpdateRequest;
import com.lokmit.foundation.contact.dto.CreateContactMessageRequest;
import com.lokmit.foundation.contact.entity.ContactMessage;
import com.lokmit.foundation.contact.service.ContactMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contact enquiry endpoints.
 *
 * <p>Creation stays public for the website contact form. Reading and managing
 * enquiries requires authentication plus the {@code messages:manage}
 * permission (granted to SUPER_ADMIN, ADMIN and MODERATOR by the V2 seed
 * data) — enforced with method security so the permission travels with the
 * endpoint.</p>
 */
@RestController
@RequestMapping(ApiPaths.CONTACT_MESSAGES)
@Tag(name = "Contact", description = "Public enquiry submissions and authenticated enquiry management")
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

    /**
     * Lists enquiries, newest first by default. Status filter and search are
     * optional; page/size follow the project's shared {@link PageParams}
     * conventions (default 20, max 100).
     */
    @GetMapping
    @PreAuthorize("hasAuthority('messages:manage')")
    @Operation(summary = "List contact enquiries",
            description = "Requires authentication and the messages:manage permission. "
                    + "Supports status filtering, search and pagination. Newest first by default.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<PageResponse<ContactMessageAdminResponse>>> listEnquiries(
            @Valid @ModelAttribute PageParams pageParams,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {

        if (status != null && !status.isBlank()
                && !ContactMessage.ALLOWED_STATUSES.contains(status.trim().toUpperCase())) {
            throw new BadRequestException("Invalid status value");
        }

        Pageable pageable = PageRequest.of(
                pageParams.getPage(), pageParams.getSize(), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ContactMessageAdminResponse> page =
                contactMessageService.listEnquiries(status, search, pageable);

        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(page)));
    }

    /**
     * Returns one enquiry with full content for authorized staff.
     * Unknown ids map to the standard 404 error envelope.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('messages:manage')")
    @Operation(summary = "Get a contact enquiry",
            description = "Requires authentication and the messages:manage permission.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<ContactMessageAdminResponse>> getEnquiry(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(contactMessageService.getEnquiry(id)));
    }

    /**
     * Partially updates the staff-manageable fields of an enquiry (status,
     * internal note). Omitted fields stay unchanged; sender identity, message
     * content and timestamps are not modifiable through this endpoint.
     */
    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('messages:manage')")
    @Operation(summary = "Update a contact enquiry",
            description = "Requires authentication and the messages:manage permission. "
                    + "Only status and internal note can be changed; omitted fields stay unchanged.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<ContactMessageAdminResponse>> updateEnquiry(
            @PathVariable Long id,
            @Valid @RequestBody ContactMessageUpdateRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(contactMessageService.updateEnquiry(id, request), "Enquiry updated"));
    }
}
