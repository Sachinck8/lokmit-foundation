package com.lokmit.foundation.common.exception;

/**
 * Raised when an upload exceeds the configured application size limit.
 * Mapped to HTTP 413 (PAYLOAD_TOO_LARGE) by {@link GlobalExceptionHandler}.
 *
 * <p>This is a controlled domain error, not an uncontrolled container
 * exception: validation happens inside the service layer on the already-
 * received bytes, so the client receives the standard API error envelope.</p>
 */
public class PayloadTooLargeException extends RuntimeException {

    public PayloadTooLargeException(String message) {
        super(message);
    }
}
