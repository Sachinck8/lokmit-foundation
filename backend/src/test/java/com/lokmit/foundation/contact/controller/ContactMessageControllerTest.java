package com.lokmit.foundation.contact.controller;

import com.lokmit.foundation.common.constants.ApiPaths;
import com.lokmit.foundation.contact.dto.ContactMessageResponse;
import com.lokmit.foundation.contact.dto.CreateContactMessageRequest;
import com.lokmit.foundation.contact.service.ContactMessageService;
import com.lokmit.foundation.security.service.CustomUserDetailsService;
import com.lokmit.foundation.security.service.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ContactMessageController.class)
@AutoConfigureMockMvc(addFilters = false)
class ContactMessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ContactMessageService contactMessageService;

    // JwtAuthenticationFilter extends OncePerRequestFilter (a Filter), which
    // @WebMvcTest picks up automatically. It needs JwtTokenProvider and
    // CustomUserDetailsService, neither of which are @Service beans that
    // @WebMvcTest would scan. Provide mocks to satisfy its constructor.
    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private CreateContactMessageRequest validRequest() {
        CreateContactMessageRequest request = new CreateContactMessageRequest();
        request.setName("Ramesh Kumar");
        request.setEmail("ramesh@example.com");
        request.setPhone("+91 90000 00000");
        request.setCategory("consultancy");
        request.setSubject("Skill development programme enquiry");
        request.setMessage("We would like to discuss a district-level skill development programme.");
        return request;
    }

    @Test
    void submitEnquiry_shouldReturn201WithConfirmation() throws Exception {
        CreateContactMessageRequest request = validRequest();

        when(contactMessageService.submitEnquiry(any(CreateContactMessageRequest.class)))
                .thenReturn(ContactMessageResponse.builder()
                        .id(1L)
                        .status("NEW")
                        .receivedAt("2026-09-09T10:15:30+05:30")
                        .build());

        mockMvc.perform(post(ApiPaths.CONTACT_MESSAGES)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Enquiry received. We will respond to you shortly."))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.status").value("NEW"))
                .andExpect(jsonPath("$.data.receivedAt").isNotEmpty());

        verify(contactMessageService).submitEnquiry(any(CreateContactMessageRequest.class));
    }

    @Test
    void submitEnquiry_shouldReturn400WhenNameIsBlank() throws Exception {
        CreateContactMessageRequest request = validRequest();
        request.setName("  ");

        mockMvc.perform(post(ApiPaths.CONTACT_MESSAGES)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0].field").value("name"));
    }

    @Test
    void submitEnquiry_shouldReturn400ForInvalidEmail() throws Exception {
        CreateContactMessageRequest request = validRequest();
        request.setEmail("not-an-email");

        mockMvc.perform(post(ApiPaths.CONTACT_MESSAGES)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0].code").value("VALIDATION"));
    }

    @Test
    void submitEnquiry_shouldReturn400WhenMessageExceedsLimit() throws Exception {
        CreateContactMessageRequest request = validRequest();
        request.setMessage("x".repeat(5001));

        mockMvc.perform(post(ApiPaths.CONTACT_MESSAGES)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void submitEnquiry_shouldReturn400ForInvalidPhoneCharacters() throws Exception {
        CreateContactMessageRequest request = validRequest();
        request.setPhone("call me <script>");

        mockMvc.perform(post(ApiPaths.CONTACT_MESSAGES)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void submitEnquiry_shouldRejectMalformedBody() throws Exception {
        mockMvc.perform(post(ApiPaths.CONTACT_MESSAGES)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
