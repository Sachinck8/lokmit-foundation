package com.lokmit.foundation.employment.application.service.support;

import com.lokmit.foundation.notification.entity.Notification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the exact user-visible wording of the outbox payload builders. These
 * strings surface verbatim as notification titles/bodies, so the wording is
 * a contract — a regression here is immediately user-visible (e.g. the
 * "Interview scheduledd" typo fixed after the A7.5 audit).
 */
class OutboxPayloadsTest {

    // ------------------------------------------------------------------
    // interviewEvent — wording contract
    // ------------------------------------------------------------------

    @Test
    @DisplayName("interviewEvent: SCHEDULED reads \"Interview scheduled\" — no doubled d")
    void interviewScheduledWording() {
        Map<String, Object> payload = OutboxPayloads.interviewEvent(
                Notification.TYPE_INTERVIEW_SCHEDULED, 7L, 10L, 20L,
                "2026-09-20T10:00+00:00", "VIDEO");

        assertThat(payload.get("title")).isEqualTo("Interview scheduled");
        assertThat((String) payload.get("body"))
                .startsWith("An interview scheduled at 2026-09-20T10:00+00:00 (VIDEO) was scheduled.");
    }

    @Test
    @DisplayName("interviewEvent: UPDATED reads \"Interview updated\" — no doubled d")
    void interviewUpdatedWording() {
        Map<String, Object> payload = OutboxPayloads.interviewEvent(
                Notification.TYPE_INTERVIEW_UPDATED, 7L, 10L, 20L,
                "2026-09-20T10:00+00:00", "ONSITE");

        assertThat(payload.get("title")).isEqualTo("Interview updated");
        assertThat((String) payload.get("body"))
                .endsWith("was updated.");
    }

    @Test
    @DisplayName("interviewEvent: CANCELLED reads \"Interview cancelled\" — no doubled d")
    void interviewCancelledWording() {
        Map<String, Object> payload = OutboxPayloads.interviewEvent(
                Notification.TYPE_INTERVIEW_CANCELLED, 7L, 10L, 20L,
                "2026-09-20T10:00+00:00", "PHONE");

        assertThat(payload.get("title")).isEqualTo("Interview cancelled");
        assertThat((String) payload.get("body"))
                .endsWith("was cancelled.");
    }

    @Test
    @DisplayName("interviewEvent: payload shape stays minimal, typed and explicit")
    void interviewPayloadShape() {
        Map<String, Object> payload = OutboxPayloads.interviewEvent(
                Notification.TYPE_INTERVIEW_SCHEDULED, 7L, 10L, 20L,
                "2026-09-20T10:00+00:00", "VIDEO");

        assertThat(payload).containsOnlyKeys(
                "recipientUserId", "notificationType", "title", "body",
                "entityType", "entityId", "applicationId");
        assertThat(payload.get("notificationType"))
                .isEqualTo(Notification.TYPE_INTERVIEW_SCHEDULED);
        assertThat(payload.get("recipientUserId")).isEqualTo(7L);
        assertThat(payload.get("entityType")).isEqualTo("INTERVIEW");
        assertThat(payload.get("entityId")).isEqualTo(10L);
        assertThat(payload.get("applicationId")).isEqualTo(20L);
    }

    // ------------------------------------------------------------------
    // applicationStatusChanged — wording contract
    // ------------------------------------------------------------------

    @Test
    @DisplayName("applicationStatusChanged: title/body carry the status transition verbatim")
    void applicationStatusChangedWording() {
        Map<String, Object> payload = OutboxPayloads.applicationStatusChanged(
                7L, 30L, "Backend Engineer", "SUBMITTED", "UNDER_REVIEW", null);

        assertThat(payload.get("notificationType"))
                .isEqualTo(Notification.TYPE_APPLICATION_STATUS_CHANGED);
        assertThat(payload.get("title"))
                .isEqualTo("Application status updated: UNDER_REVIEW");
        assertThat((String) payload.get("body"))
                .isEqualTo("Your application for \"Backend Engineer\" "
                        + "moved from SUBMITTED to UNDER_REVIEW");
    }

    @Test
    @DisplayName("applicationStatusChanged: optional note is appended after an em dash")
    void applicationStatusChangedWithNote() {
        Map<String, Object> payload = OutboxPayloads.applicationStatusChanged(
                7L, 30L, "Backend Engineer", "UNDER_REVIEW", "HIRED", "Strong fit");

        assertThat((String) payload.get("body"))
                .isEqualTo("Your application for \"Backend Engineer\" "
                        + "moved from UNDER_REVIEW to HIRED — Strong fit");
    }

    @Test
    @DisplayName("applicationStatusChanged: payload shape stays minimal, typed and explicit")
    void applicationPayloadShape() {
        Map<String, Object> payload = OutboxPayloads.applicationStatusChanged(
                7L, 30L, "Backend Engineer", "SUBMITTED", "UNDER_REVIEW", null);

        assertThat(payload).containsOnlyKeys(
                "recipientUserId", "notificationType", "title", "body",
                "entityType", "entityId");
        assertThat(payload.get("entityType")).isEqualTo("JOB_APPLICATION");
        assertThat(payload.get("entityId")).isEqualTo(30L);
        assertThat(payload.get("recipientUserId")).isEqualTo(7L);
    }
}
