package com.lokmit.foundation.contact.service;

import com.lokmit.foundation.contact.dto.ContactMessageAdminResponse;
import com.lokmit.foundation.contact.dto.ContactMessageResponse;
import com.lokmit.foundation.contact.dto.ContactMessageUpdateRequest;
import com.lokmit.foundation.contact.dto.CreateContactMessageRequest;
import com.lokmit.foundation.contact.entity.ContactMessage;
import com.lokmit.foundation.common.exception.NotFoundException;
import com.lokmit.foundation.contact.repository.ContactMessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Business logic for public website enquiry submissions.
 *
 * <p>Validation of structure and lengths is handled by Bean Validation on the
 * request DTO; this service applies persistence concerns — mapping the public
 * payload onto the V7 schema, seeding lifecycle defaults, and never exposing
 * persistence errors to the caller.</p>
 */
@Service
@Slf4j
public class ContactMessageService {

    private static final DateTimeFormatter RECEIVED_AT_FORMAT =
            DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final ContactMessageRepository contactMessageRepository;

    public ContactMessageService(ContactMessageRepository contactMessageRepository) {
        this.contactMessageRepository = contactMessageRepository;
    }

    /**
     * Persists a public enquiry.
     *
     * @param request the validated request payload
     * @return confirmation data safe to expose publicly
     */
    @Transactional
    public ContactMessageResponse submitEnquiry(CreateContactMessageRequest request) {
        ContactMessage entity = new ContactMessage();
        entity.setSenderName(request.getName().trim());
        entity.setSenderEmail(request.getEmail().trim().toLowerCase());
        entity.setSenderPhone(request.getPhone() == null || request.getPhone().isBlank()
                ? null
                : request.getPhone().trim());
        entity.setSubject(request.getSubject().trim());
        entity.setMessage(request.getMessage().trim());
        entity.setStatus(ContactMessage.STATUS_NEW);
        entity.setInternalNote(buildInternalNote(request.getCategory()));
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setUpdatedAt(OffsetDateTime.now());

        ContactMessage saved = contactMessageRepository.save(entity);

        log.info("Contact enquiry #{} received (category: {})",
                saved.getId(), request.getCategory());

        return ContactMessageResponse.builder()
                .id(saved.getId())
                .status(saved.getStatus())
                .receivedAt(saved.getCreatedAt().format(RECEIVED_AT_FORMAT))
                .build();
    }

    /**
     * Maps the public enquiry category onto the existing nullable
     * 'internal_note' column so staff can triage enquiries without any schema
     * change. Never overrides an internal note if one already exists.
     */
    private String buildInternalNote(String category) {
        if (category == null || category.isBlank()) {
            return null;
        }
        return "Enquiry category: " + category.trim();
    }

    // =====================================================================
    // Admin management — requires the messages:manage permission (I-3)
    // =====================================================================

    /**
     * Lists enquiries for authorized staff with database-side filtering,
     * pagination and sorting.
     *
     * @param status  optional exact status filter (validated by the controller)
     * @param search  optional case-insensitive substring match over sender
     *                name, sender email and subject
     * @param pageable page/size plus ordering; callers pass the sort explicitly
     * @return one page of enquiry summaries, newest first by default
     */
    @Transactional(readOnly = true)
    public Page<ContactMessageAdminResponse> listEnquiries(String status, String search, Pageable pageable) {
        Specification<ContactMessage> spec = Specification.where(null);
        if (status != null && !status.isBlank()) {
            spec = spec.and(ContactMessageSpecifications.hasStatus(status.trim().toUpperCase()));
        }
        if (search != null && !search.isBlank()) {
            spec = spec.and(ContactMessageSpecifications.senderOrSubjectContains(search.trim()));
        }

        return contactMessageRepository.findAll(spec, pageable)
                .map(this::toAdminResponse);
    }

    /**
     * Fetches one enquiry for authorized staff.
     *
     * @throws NotFoundException when no enquiry with that id exists (mapped to 404)
     */
    @Transactional(readOnly = true)
    public ContactMessageAdminResponse getEnquiry(Long id) {
        return contactMessageRepository.findById(id)
                .map(this::toAdminResponse)
                .orElseThrow(() -> new NotFoundException("Contact message not found"));
    }

    /**
     * Partially updates an enquiry. Only the staff-manageable fields in
     * {@link ContactMessageUpdateRequest} can change — status and the internal
     * note. Sender identity, message content, id and timestamps are
     * structurally impossible to modify (absent from the update DTO).
     *
     * <p>The public form stores the enquiry category in internal_note
     * ("Enquiry category: …"); an explicit JSON null clears the note, while
     * omitting the field leaves any existing note untouched, so category
     * data is never destroyed accidentally.</p>
     *
     * @throws NotFoundException when no enquiry with that id exists (mapped to 404)
     */
    @Transactional
    public ContactMessageAdminResponse updateEnquiry(Long id, ContactMessageUpdateRequest request) {
        ContactMessage entity = contactMessageRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Contact message not found"));

        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }
        if (request.isInternalNoteProvided()) {
            entity.setInternalNote(request.getInternalNote());
        }
        entity.setUpdatedAt(OffsetDateTime.now());

        ContactMessage saved = contactMessageRepository.save(entity);
        log.info("Contact enquiry #{} updated (status: {})", saved.getId(), saved.getStatus());

        return toAdminResponse(saved);
    }

    private ContactMessageAdminResponse toAdminResponse(ContactMessage entity) {
        return ContactMessageAdminResponse.builder()
                .id(entity.getId())
                .senderName(entity.getSenderName())
                .senderEmail(entity.getSenderEmail())
                .senderPhone(entity.getSenderPhone())
                .subject(entity.getSubject())
                .message(entity.getMessage())
                .status(entity.getStatus())
                .internalNote(entity.getInternalNote())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
