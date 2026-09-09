package com.lokmit.foundation.contact.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

/**
 * Response payload returned after a successful public enquiry submission.
 *
 * <p>Only non-sensitive confirmation data is exposed — message content and
 * internal fields are never echoed back.</p>
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContactMessageResponse {

    private Long id;

    private String status;

    private String receivedAt;
}
