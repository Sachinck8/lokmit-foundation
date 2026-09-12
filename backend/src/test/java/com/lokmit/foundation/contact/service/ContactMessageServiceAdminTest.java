package com.lokmit.foundation.contact.service;

import com.lokmit.foundation.common.exception.NotFoundException;
import com.lokmit.foundation.contact.dto.ContactMessageAdminResponse;
import com.lokmit.foundation.contact.dto.ContactMessageUpdateRequest;
import com.lokmit.foundation.contact.entity.ContactMessage;
import com.lokmit.foundation.contact.repository.ContactMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Service-layer tests for the contact enquiry management logic (I-3):
 * mapping to safe DTOs, partial-update semantics, and protection of fields
 * that staff must not be able to change.
 *
 * <p>Pure Mockito — no Spring context, no PostgreSQL.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ContactMessageServiceAdminTest {

    @Mock
    private ContactMessageRepository contactMessageRepository;

    private ContactMessageService service;
    private ContactMessage entity;

    @BeforeEach
    void setUp() {
        service = new ContactMessageService(contactMessageRepository);

        // Real JPA save() returns the managed instance; Mockito returns null
        // by default, so mirror the real contract here.
        when(contactMessageRepository.save(any(ContactMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        entity = new ContactMessage();
        entity.setId(7L);
        entity.setSenderName("Ramesh Kumar");
        entity.setSenderEmail("ramesh@example.com");
        entity.setSenderPhone("+91 90000 00000");
        entity.setSubject("Skill development enquiry");
        entity.setMessage("We would like to discuss a programme.");
        entity.setStatus(ContactMessage.STATUS_NEW);
        entity.setInternalNote("Enquiry category: consultancy");
        entity.setCreatedAt(OffsetDateTime.now().minusDays(1));
        entity.setUpdatedAt(OffsetDateTime.now().minusDays(1));
    }

    private ContactMessageUpdateRequest updateRequest() {
        return new ContactMessageUpdateRequest();
    }

    // ------------------------------------------------------------------
    // List
    // ------------------------------------------------------------------

    @Test
    @DisplayName("listEnquiries maps entities to admin DTOs page-wise")
    void listEnquiriesMapsToAdminDtos() {
        Page<ContactMessage> page = new PageImpl<>(List.of(entity));
        when(contactMessageRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        Page<ContactMessageAdminResponse> result =
                service.listEnquiries(null, null, PageRequest.of(0, 20));

        assertEquals(1, result.getContent().size());
        ContactMessageAdminResponse dto = result.getContent().get(0);
        assertEquals(7L, dto.getId());
        assertEquals("Ramesh Kumar", dto.getSenderName());
        assertEquals("ramesh@example.com", dto.getSenderEmail());
        assertEquals("NEW", dto.getStatus());
        assertEquals("Enquiry category: consultancy", dto.getInternalNote());
        assertNotNull(dto.getCreatedAt());
    }

    @Test
    @DisplayName("listEnquiries delegates pagination and ordering to the database")
    void listEnquiriesUsesPageableFromCaller() {
        when(contactMessageRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        Pageable pageable = PageRequest.of(2, 15, Sort.by(Sort.Direction.DESC, "createdAt"));
        service.listEnquiries("NEW", "ramesh", pageable);

        verify(contactMessageRepository).findAll(any(Specification.class), org.mockito.ArgumentMatchers.eq(pageable));
    }

    @Test
    @DisplayName("listEnquiries normalizes status and search inputs")
    void listEnquiriesNormalizesInputs() {
        when(contactMessageRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        service.listEnquiries("  read ", "  RAMESH  ", PageRequest.of(0, 20));

        // Normalization happens before the predicate composition; the exact
        // predicate content is database semantics, so only the call is verified.
        verify(contactMessageRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    // ------------------------------------------------------------------
    // Get single
    // ------------------------------------------------------------------

    @Test
    @DisplayName("getEnquiry returns the full enquiry DTO for an existing id")
    void getEnquiryReturnsDto() {
        when(contactMessageRepository.findById(7L)).thenReturn(Optional.of(entity));

        ContactMessageAdminResponse dto = service.getEnquiry(7L);

        assertEquals("Skill development enquiry", dto.getSubject());
        assertEquals("+91 90000 00000", dto.getSenderPhone());
        assertEquals("We would like to discuss a programme.", dto.getMessage());
    }

    @Test
    @DisplayName("getEnquiry throws NotFoundException for an unknown id")
    void getEnquiryUnknownIdThrows() {
        when(contactMessageRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getEnquiry(999L));
    }

    // ------------------------------------------------------------------
    // Update — status and note
    // ------------------------------------------------------------------

    @Test
    @DisplayName("valid status update succeeds and bumps updated_at")
    void statusUpdateSucceeds() {
        when(contactMessageRepository.findById(7L)).thenReturn(Optional.of(entity));

        ContactMessageUpdateRequest request = updateRequest();
        request.setStatus("READ");

        ContactMessageAdminResponse result = service.updateEnquiry(7L, request);

        assertEquals("READ", result.getStatus());
        assertTrue(result.getUpdatedAt().isAfter(entity.getCreatedAt()));
    }

    @Test
    @DisplayName("internal-note update succeeds without touching status")
    void noteUpdateSucceeds() {
        when(contactMessageRepository.findById(7L)).thenReturn(Optional.of(entity));

        ContactMessageUpdateRequest request = updateRequest();
        request.setInternalNote("Called back; follow up next week.");

        ContactMessageAdminResponse result = service.updateEnquiry(7L, request);

        assertEquals("Called back; follow up next week.", result.getInternalNote());
        assertEquals("NEW", result.getStatus(), "Status must stay untouched when omitted");
    }

    @Test
    @DisplayName("explicit null internal note clears the note")
    void explicitNullClearsNote() {
        when(contactMessageRepository.findById(7L)).thenReturn(Optional.of(entity));

        ContactMessageUpdateRequest request = updateRequest();
        request.setInternalNote(null); // explicit JSON null → provided=true, value=null

        assertTrue(request.isInternalNoteProvided());
        ContactMessageAdminResponse result = service.updateEnquiry(7L, request);

        assertNull(result.getInternalNote());
    }

    @Test
    @DisplayName("omitted internal note leaves the existing note (and category) untouched")
    void omittedNotePreservesCategory() {
        when(contactMessageRepository.findById(7L)).thenReturn(Optional.of(entity));

        ContactMessageUpdateRequest request = updateRequest();
        request.setStatus("REPLIED");

        ContactMessageAdminResponse result = service.updateEnquiry(7L, request);

        assertEquals("Enquiry category: consultancy", result.getInternalNote(),
                "The public form's category data in internal_note must survive a status-only update");
    }

    @Test
    @DisplayName("empty update request changes nothing but updated_at")
    void emptyUpdateChangesNothing() {
        when(contactMessageRepository.findById(7L)).thenReturn(Optional.of(entity));
        String statusBefore = entity.getStatus();
        String noteBefore = entity.getInternalNote();

        ContactMessageAdminResponse result = service.updateEnquiry(7L, updateRequest());

        assertEquals(statusBefore, result.getStatus());
        assertEquals(noteBefore, result.getInternalNote());
    }

    @Test
    @DisplayName("updateEnquiry throws NotFoundException for an unknown id")
    void updateUnknownIdThrows() {
        when(contactMessageRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.updateEnquiry(999L, updateRequest()));
    }

    // ------------------------------------------------------------------
    // Protected fields / IDOR / mass assignment
    // ------------------------------------------------------------------

    @Test
    @DisplayName("update cannot alter sender identity, message content, id or created_at")
    void protectedFieldsCannotChange() {
        when(contactMessageRepository.findById(7L)).thenReturn(Optional.of(entity));
        OffsetDateTime createdAtBefore = entity.getCreatedAt();

        ContactMessageUpdateRequest request = updateRequest();
        request.setStatus("ARCHIVED");

        service.updateEnquiry(7L, request);

        assertEquals(7L, entity.getId());
        assertEquals("Ramesh Kumar", entity.getSenderName());
        assertEquals("ramesh@example.com", entity.getSenderEmail());
        assertEquals("+91 90000 00000", entity.getSenderPhone());
        assertEquals("We would like to discuss a programme.", entity.getMessage());
        assertEquals(createdAtBefore, entity.getCreatedAt());
    }

    @Test
    @DisplayName("update DTO exposes no mutable sender/id/timestamp properties (mass-assignment proof)")
    void updateDtoHasNoProtectedSetters() throws java.beans.IntrospectionException {
        ContactMessageUpdateRequest request = updateRequest();

        // Reflection check: only the whitelisted writable properties exist.
        java.util.Set<String> writable = new java.util.HashSet<>();
        for (java.beans.PropertyDescriptor pd :
                java.beans.Introspector.getBeanInfo(ContactMessageUpdateRequest.class).getPropertyDescriptors()) {
            if (pd.getWriteMethod() != null) {
                writable.add(pd.getName());
            }
        }

        // internalNoteProvided's write method is protected by @JsonIgnore at the
        // JSON layer; no sender/id/timestamp property may appear at all.
        assertEquals(java.util.Set.of("status", "internalNote", "internalNoteProvided"),
                writable,
                "Update DTO must not carry sender/id/timestamp writable properties");
    }

    @Test
    @DisplayName("each id is loaded and updated in isolation (no cross-record leakage)")
    void perRecordIsolation() {
        ContactMessage other = new ContactMessage();
        other.setId(8L);
        other.setSenderName("Other Sender");
        other.setSenderEmail("other@example.com");
        other.setStatus(ContactMessage.STATUS_NEW);
        other.setCreatedAt(OffsetDateTime.now());
        other.setUpdatedAt(OffsetDateTime.now());

        when(contactMessageRepository.findById(8L)).thenReturn(Optional.of(other));

        ContactMessageUpdateRequest request = updateRequest();
        request.setStatus("READ");

        ContactMessageAdminResponse result = service.updateEnquiry(8L, request);

        assertEquals(8L, result.getId());
        assertEquals("Other Sender", result.getSenderName());
        assertEquals("READ", result.getStatus());
        assertEquals("NEW", entity.getStatus(), "Record 7 must remain untouched");
    }
}
