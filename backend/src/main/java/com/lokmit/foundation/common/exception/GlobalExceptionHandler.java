package com.lokmit.foundation.common.exception;

import com.lokmit.foundation.common.api.ApiError;
import com.lokmit.foundation.common.api.ApiResponse;
import com.lokmit.foundation.common.api.ErrorCodes;
import com.lokmit.foundation.security.exception.AuthenticationFailedException;
import com.lokmit.foundation.security.exception.TokenException;
import com.lokmit.foundation.security.exception.UserAccountException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/**
 * Central error handling for the whole API.
 *
 * <p>Every handled exception is converted into the standard
 * {@link ApiResponse} error envelope with a stable HTTP status and a
 * machine-readable {@link ErrorCodes} code. Unexpected exceptions are logged
 * and returned as a generic 500 without leaking internals.</p>
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ErrorCodes.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(BadRequestException ex) {
        return build(HttpStatus.BAD_REQUEST, ErrorCodes.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(ConflictException ex) {
        return build(HttpStatus.CONFLICT, ErrorCodes.CONFLICT, ex.getMessage());
    }

    // Phase 4: Authentication & Authorization exception handlers

    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationFailed(AuthenticationFailedException ex) {
        return build(HttpStatus.UNAUTHORIZED, ErrorCodes.INVALID_CREDENTIALS, ex.getMessage());
    }

    @ExceptionHandler(UserAccountException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserAccountException(UserAccountException ex) {
        String code = switch (ex.getStatus()) {
            case "LOCKED" -> ErrorCodes.USER_LOCKED;
            case "SUSPENDED" -> ErrorCodes.USER_SUSPENDED;
            default -> ErrorCodes.BAD_REQUEST;
        };
        return build(HttpStatus.UNAUTHORIZED, code, ex.getMessage());
    }

    @ExceptionHandler(TokenException.class)
    public ResponseEntity<ApiResponse<Void>> handleTokenException(TokenException ex) {
        String code = ex.getMessage().contains("expired") ? ErrorCodes.EXPIRED_TOKEN : ErrorCodes.INVALID_TOKEN;
        return build(HttpStatus.UNAUTHORIZED, code, ex.getMessage());
    }

    @ExceptionHandler({AuthenticationException.class, BadCredentialsException.class})
    public ResponseEntity<ApiResponse<Void>> handleSpringAuthentication(Exception ex) {
        return build(HttpStatus.UNAUTHORIZED, ErrorCodes.UNAUTHORIZED, "Authentication required");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, ErrorCodes.FORBIDDEN, "Access denied");
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ApiResponse<Void>> handleBindValidation(BindException ex) {
        List<ApiError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> ApiError.field(
                        ErrorCodes.VALIDATION,
                        fieldError.getDefaultMessage(),
                        fieldError.getField()))
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Validation failed", errors);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodValidation(HandlerMethodValidationException ex) {
        return build(HttpStatus.BAD_REQUEST,
                "Validation failed",
                ApiError.of(ErrorCodes.VALIDATION, "Invalid request parameter"));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        List<ApiError> errors = ex.getConstraintViolations().stream()
                .map(violation -> ApiError.field(
                        ErrorCodes.VALIDATION,
                        violation.getMessage(),
                        violation.getPropertyPath().toString()))
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Validation failed", errors);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return build(HttpStatus.BAD_REQUEST,
                ErrorCodes.BAD_REQUEST,
                "Parameter '" + ex.getName() + "' has an invalid value");
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadableMessage(
            org.springframework.http.converter.HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST,
                ErrorCodes.BAD_REQUEST,
                "Request body is missing or malformed");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParameter(MissingServletRequestParameterException ex) {
        return build(HttpStatus.BAD_REQUEST,
                ErrorCodes.BAD_REQUEST,
                "Required parameter '" + ex.getParameterName() + "' is missing");
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ApiResponse<Void>> handleUnknownPath(Exception ex) {
        return build(HttpStatus.NOT_FOUND, ErrorCodes.NOT_FOUND, "Resource not found");
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotAllowed(
            org.springframework.web.HttpRequestMethodNotSupportedException ex) {
        return build(HttpStatus.METHOD_NOT_ALLOWED,
                ErrorCodes.METHOD_NOT_ALLOWED,
                "HTTP method not supported: " + ex.getMethod());
    }

    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnsupportedMediaType(
            org.springframework.web.HttpMediaTypeNotSupportedException ex) {
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                ErrorCodes.UNSUPPORTED_MEDIA_TYPE,
                "Unsupported media type: " + ex.getContentType());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorCodes.INTERNAL_ERROR,
                "An unexpected error occurred");
    }

    private ResponseEntity<ApiResponse<Void>> build(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status)
                .body(ApiResponse.error(message, ApiError.of(code, message)));
    }

        private ResponseEntity<ApiResponse<Void>> build(HttpStatus status, String message, ApiError error) {
        return build(status, message, List.of(error));
    }

    private ResponseEntity<ApiResponse<Void>> build(HttpStatus status, String message, List<ApiError> errors) {
        return ResponseEntity.status(status)
                .body(ApiResponse.error(message, errors));
    }
}