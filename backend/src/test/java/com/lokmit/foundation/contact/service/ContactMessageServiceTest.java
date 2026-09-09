package com.lokmit.foundation.contact.service;

import com.lokmit.foundation.contact.dto.ContactMessageResponse;
import com.lokmit.foundation.contact.dto.CreateContactMessageRequest;
import com.lokmit.foundation.contact.entity.ContactMessage;
import com.lokmit.foundation.contact.repository.ContactMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the enquiry persistence mapping (valid submission path).
 *
 * <p>The service is exercised with a mocked repository so these tests run
 * without PostgreSQL, mirroring the project's database-independent test
 * strategy. Full persistence against the real V7 table is covered by the
 * Flyway migration chain plus future repository integration tests.</p>
 */
@ExtendWith(MockitoExtension.class)
class ContactMessageServiceTest {

    @Mock
    private ContactMessageRepository contactMessageRepository;

    private ContactMessageService contactMessageService;

    @BeforeEach
    void setUp() {
        contactMessageService = new ContactMessageService(contactMessageRepository);
    }

    private CreateContactMessageRequest validRequest() {
        CreateContactMessageRequest request = new CreateContactMessageRequest();
        request.setName("  Ramesh Kumar  ");
        request.setEmail("Ramesh@Example.com");
        request.setPhone(" +91 90000 00000 ");
        request.setCategory(" consultancy ");
        request.setSubject("Skill development programme enquiry");
        request.setMessage("We would like to discuss a district-level programme.");
        return request;
    }

    @Test
    void submitEnquiry_shouldPersistTrimmedFieldsWithNewStatus() {
        CreateContactMessageRequest request = validRequest();

        when(contactMessageRepository.save(any(ContactMessage.class)))
                .thenAnswer(invocation -> {
                    ContactMessage entity = invocation.getArgument(0);
                    entity.setId(42L);
                    return entity;
                });

        ContactMessageResponse response = contactMessageService.submitEnquiry(request);

        ArgumentCaptor<ContactMessage> captor = ArgumentCaptor.forClass(ContactMessage.class);
        verify(contactMessageRepository).save(captor.capture());
        ContactMessage saved = captor.getValue();

        assertThat(saved.getSenderName()).isEqualTo("Ramesh Kumar");
        assertThat(saved.getSenderEmail()).isEqualTo("ramesh@example.com");
        assertThat(saved.getSenderPhone()).isEqualTo("+91 90000 00000");
        assertThat(saved.getSubject()).isEqualTo("Skill development programme enquiry");
        assertThat(saved.getStatus()).isEqualTo(ContactMessage.STATUS_NEW);
        assertThat(saved.getInternalNote()).isEqualTo("Enquiry category: consultancy");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();

        assertThat(response.getId()).isEqualTo(42L);
        assertThat(response.getStatus()).isEqualTo(ContactMessage.STATUS_NEW);
        assertThat(response.getReceivedAt()).isNotBlank();
    }

    @Test
    void submitEnquiry_shouldBlankOptionalFieldsToNull() {
        CreateContactMessageRequest request = validRequest();
        request.setPhone("");
        request.setCategory(null);

        when(contactMessageRepository.save(any(ContactMessage.class)))
                .thenAnswer(invocation -> {
                    ContactMessage entity = invocation.getArgument(0);
                    entity.setId(43L);
                    return entity;
                });

        contactMessageService.submitEnquiry(request);

        ArgumentCaptor<ContactMessage> captor = ArgumentCaptor.forClass(ContactMessage.class);
        verify(contactMessageRepository).save(captor.capture());
        ContactMessage saved = captor.getValue();

        assertThat(saved.getSenderPhone()).isNull();
        assertThat(saved.getInternalNote()).isNull();
    }
}
