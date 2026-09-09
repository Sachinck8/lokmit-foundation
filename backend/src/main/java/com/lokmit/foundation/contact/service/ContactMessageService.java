package com.lokmit.foundation.contact.service;

import com.lokmit.foundation.contact.dto.ContactMessageResponse;
import com.lokmit.foundation.contact.dto.CreateContactMessageRequest;
import com.lokmit.foundation.contact.entity.ContactMessage;
import com.lokmit.foundation.contact.repository.ContactMessageRepository;
import lombok.extern.slf4j.Slf4j;
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
}
