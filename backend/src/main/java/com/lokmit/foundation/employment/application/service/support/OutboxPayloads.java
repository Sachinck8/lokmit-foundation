package com.lokmit.foundation.employment.application.service.support;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builders for the compact outbox payloads produced by the A7.3/A7.4
 * integration points (A7.5). Payloads are minimal, deterministic (stable
 * key order, retry-safe) and never contain credentials or security
 * material — the relay maps them 1:1 onto in-app notification rows.
 */
public final class OutboxPayloads {

    private OutboxPayloads() {
        throw new AssertionError("Utility class must not be instantiated.");
    }

    /** Payload for APPLICATION_STATUS_CHANGED events. */
    public static Map<String, Object> applicationStatusChanged(
            Long recipientUserId, Long applicationId, String jobTitle,
            String previousStatus, String newStatus, String note) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("recipientUserId", recipientUserId);
        payload.put("notificationType", "APPLICATION_STATUS_CHANGED");
        payload.put("title", "Application status updated: " + newStatus);
        payload.put("body", "Your application for \"" + jobTitle
                + "\" moved from " + previousStatus + " to " + newStatus
                + (note != null ? (" — " + note) : ""));
        payload.put("entityType", "JOB_APPLICATION");
        payload.put("entityId", applicationId);
        return payload;
    }

    /** Payload for INTERVIEW_* events. */
    public static Map<String, Object> interviewEvent(
            String notificationType, Long recipientUserId, Long interviewId,
            Long applicationId, String scheduledAt, String mode) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("recipientUserId", recipientUserId);
        payload.put("notificationType", notificationType);
        payload.put("title", "Interview " + notificationType
                .substring("INTERVIEW_".length()).toLowerCase() + "d");
        payload.put("body", "An interview scheduled at " + scheduledAt
                + " (" + mode + ") was " + notificationType
                .substring("INTERVIEW_".length()).toLowerCase() + "d.");
        payload.put("entityType", "INTERVIEW");
        payload.put("entityId", interviewId);
        payload.put("applicationId", applicationId);
        return payload;
    }
}
