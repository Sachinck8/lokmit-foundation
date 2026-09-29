package com.lokmit.foundation.common.exception;

/**
 * Raised when stored resume bytes cannot be loaded or fail integrity
 * verification (A7.6.4). Mapped to a safe server error by
 * {@link GlobalExceptionHandler}. Deliberately carries NO cause details:
 * storage keys, blob or provider internals must never reach the client.
 */
public class InternalStorageException extends RuntimeException {

    public InternalStorageException(String message) {
        super(message);
    }
}
