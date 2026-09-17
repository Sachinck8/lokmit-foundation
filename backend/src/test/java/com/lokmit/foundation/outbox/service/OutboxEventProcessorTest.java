package com.lokmit.foundation.outbox.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lokmit.foundation.notification.entity.Notification;
import com.lokmit.foundation.notification.repository.NotificationRepository;
import com.lokmit.foundation.outbox.config.OutboxRelayConfig;
import com.lokmit.foundation.outbox.entity.OutboxEvent;
import com.lokmit.foundation.outbox.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the per-event outbox processor (A7.5).
 *
 * <p>Covers the three outcome branches of {@link OutboxEventProcessor#process}:
 * notification materialization (PROCESSED), terminal payload defects
 * (FAILED_TERMINAL, no broken notification row), and transient failures
 * (FAILED_RETRYABLE with rollback semantics at the repository boundary) —
 * plus the guarded retry bookkeeping and the PENDING recheck that makes
 * concurrent relay passes safe.</p>
 *
 * <p>{@code process} is {@code REQUIRES_NEW}; in production that boundary is
 * enforced by the Spring proxy because {@code OutboxRelay} calls it from
 * outside the bean. These unit tests invoke the raw object, which exercises
 * the same logic with the transaction boundary removed.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OutboxEventProcessorTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private NotificationRepository notificationRepository;

    private OutboxRelayConfig config;
    private OutboxEventProcessor processor;

    private OutboxEvent event(long id, String payload, int attempts) {
        OutboxEvent event = new OutboxEvent();
        event.setId(id);
        event.setAggregateType("JOB_APPLICATION");
        event.setAggregateId(30L);
        event.setEventType("APPLICATION_STATUS_CHANGED");
        event.setPayload(payload);
        event.setStatus(OutboxEvent.STATUS_PENDING);
        event.setAttempts(attempts);
        event.setAvailableAt(OffsetDateTime.now().minusSeconds(1));
        event.setCreatedAt(OffsetDateTime.now());
        return event;
    }

    @BeforeEach
    void setUp() {
        config = new OutboxRelayConfig();
        config.setMaxAttempts(3);
        config.setRetryBackoffSeconds(60);
        processor = new OutboxEventProcessor(outboxEventRepository,
                notificationRepository, config, new ObjectMapper());
    }

    // ------------------------------------------------------------------
    // process(): success
    // ------------------------------------------------------------------

    @Test
    @DisplayName("valid payload materializes the notification and marks the event PROCESSED")
    void validPayloadMaterializesNotification() {
        String payload = "{\"recipientUserId\":7,\"notificationType\":\"APPLICATION_STATUS_CHANGED\","
                + "\"title\":\"Application status updated: HIRED\",\"body\":\"Congrats\","
                + "\"entityType\":\"JOB_APPLICATION\",\"entityId\":30}";
        OutboxEvent event = event(1L, payload, 0);
        when(outboxEventRepository.acquireForProcessing(1L))
                .thenReturn(Optional.of(event));

        OutboxEventProcessor.Result result = processor.process(1L);

        assertThat(result).isEqualTo(OutboxEventProcessor.Result.PROCESSED);
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification notification = captor.getValue();
        assertThat(notification.getRecipientUserId()).isEqualTo(7L);
        assertThat(notification.getType()).isEqualTo("APPLICATION_STATUS_CHANGED");
        assertThat(notification.getTitle()).isEqualTo("Application status updated: HIRED");
        assertThat(notification.getBody()).isEqualTo("Congrats");
        assertThat(notification.getEntityType()).isEqualTo("JOB_APPLICATION");
        assertThat(notification.getEntityId()).isEqualTo(30L);
        assertThat(notification.getCreatedAt()).isNotNull();
        assertThat(event.getStatus()).isEqualTo(OutboxEvent.STATUS_PROCESSED);
        assertThat(event.getProcessedAt()).isNotNull();
        verify(outboxEventRepository, never()).markFailedIfStillPending(anyLong(), any());
    }

    @Test
    @DisplayName("string-typed numeric ids in the payload are accepted")
    void stringTypedNumericIdsAccepted() {
        String payload = "{\"recipientUserId\":\"9\",\"notificationType\":\"INTERVIEW_SCHEDULED\","
                + "\"title\":\"Interview scheduled\",\"entityId\":\"77\"}";
        OutboxEvent event = event(2L, payload, 0);
        when(outboxEventRepository.acquireForProcessing(2L))
                .thenReturn(Optional.of(event));

        OutboxEventProcessor.Result result = processor.process(2L);

        assertThat(result).isEqualTo(OutboxEventProcessor.Result.PROCESSED);
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getRecipientUserId()).isEqualTo(9L);
        assertThat(captor.getValue().getEntityId()).isEqualTo(77L);
    }

    // ------------------------------------------------------------------
    // process(): terminal payload defects → FAILED, never retried
    // ------------------------------------------------------------------

    @Test
    @DisplayName("missing recipient marks the event FAILED without creating a notification")
    void missingRecipientIsTerminal() {
        String payload = "{\"notificationType\":\"APPLICATION_STATUS_CHANGED\",\"title\":\"x\"}";
        OutboxEvent event = event(3L, payload, 0);
        when(outboxEventRepository.acquireForProcessing(3L))
                .thenReturn(Optional.of(event));

        OutboxEventProcessor.Result result = processor.process(3L);

        assertThat(result).isEqualTo(OutboxEventProcessor.Result.FAILED_TERMINAL);
        verify(notificationRepository, never()).save(any());
        verify(outboxEventRepository).markFailedIfStillPending(eq(3L), any());
    }

    @Test
    @DisplayName("missing title is a terminal defect; blank title too")
    void missingOrBlankTitleIsTerminal() {
        OutboxEvent missing = event(4L,
                "{\"recipientUserId\":7,\"notificationType\":\"T\"}", 0);
        OutboxEvent blank = event(5L,
                "{\"recipientUserId\":7,\"notificationType\":\"T\",\"title\":\"  \"}", 0);
        when(outboxEventRepository.acquireForProcessing(4L))
                .thenReturn(Optional.of(missing));
        when(outboxEventRepository.acquireForProcessing(5L))
                .thenReturn(Optional.of(blank));

        assertThat(processor.process(4L))
                .isEqualTo(OutboxEventProcessor.Result.FAILED_TERMINAL);
        assertThat(processor.process(5L))
                .isEqualTo(OutboxEventProcessor.Result.FAILED_TERMINAL);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("malformed JSON payload is terminal")
    void malformedJsonIsTerminal() {
        OutboxEvent event = event(6L, "not-json{", 0);
        when(outboxEventRepository.acquireForProcessing(6L))
                .thenReturn(Optional.of(event));

        assertThat(processor.process(6L))
                .isEqualTo(OutboxEventProcessor.Result.FAILED_TERMINAL);
        verify(notificationRepository, never()).save(any());
        verify(outboxEventRepository, never()).save(any());
    }

    // ------------------------------------------------------------------
    // process(): transient failures → retryable
    // ------------------------------------------------------------------

    @Test
    @DisplayName("a notification-save failure is retryable, not terminal")
    void saveFailureIsRetryable() {
        String payload = "{\"recipientUserId\":7,\"notificationType\":\"T\",\"title\":\"ok\"}";
        OutboxEvent event = event(7L, payload, 0);
        when(outboxEventRepository.acquireForProcessing(7L))
                .thenReturn(Optional.of(event));
        when(notificationRepository.save(any(Notification.class)))
                .thenThrow(new RuntimeException("db glitch"));

        OutboxEventProcessor.Result result = processor.process(7L);

        assertThat(result).isEqualTo(OutboxEventProcessor.Result.FAILED_RETRYABLE);
        verify(outboxEventRepository, never()).markFailedIfStillPending(anyLong(), any());
    }

    // ------------------------------------------------------------------
    // process(): PENDING recheck (multi-instance safety)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("an event already finished by a competing pass is skipped without re-processing")
    void nonPendingEventIsSkipped() {
        OutboxEvent processed = event(8L, "{\"recipientUserId\":7}", 0);
        processed.setStatus(OutboxEvent.STATUS_PROCESSED);
        when(outboxEventRepository.acquireForProcessing(8L))
                .thenReturn(Optional.of(processed));

        OutboxEventProcessor.Result result = processor.process(8L);

        assertThat(result).isEqualTo(OutboxEventProcessor.Result.PROCESSED);
        verify(notificationRepository, never()).save(any());
        verify(outboxEventRepository, never()).save(any());
    }

    // ------------------------------------------------------------------
    // scheduleRetryOrFailure(): guarded bookkeeping
    // ------------------------------------------------------------------

    @Test
    @DisplayName("below max attempts, the retry shifts available_at and increments attempts")
    void retrySchedulesBackoff() {
        OutboxEvent event = event(9L, "{}", 0); // attempts=0 → next attempt=1
        when(outboxEventRepository.findById(9L)).thenReturn(Optional.of(event));

        processor.scheduleRetryOrFailure(9L);

        verify(outboxEventRepository).scheduleRetryIfStillPending(
                eq(9L), any(OffsetDateTime.class));
        verify(outboxEventRepository, never()).markFailedIfStillPending(anyLong(), any());
    }

    @Test
    @DisplayName("at max attempts the event is marked FAILED instead of rescheduled")
    void maxAttemptsMarksFailed() {
        OutboxEvent event = event(10L, "{}", 2); // attempts=2 → next attempt=3 == maxAttempts
        when(outboxEventRepository.findById(10L)).thenReturn(Optional.of(event));

        processor.scheduleRetryOrFailure(10L);

        verify(outboxEventRepository).markFailedIfStillPending(eq(10L), any());
        verify(outboxEventRepository, never()).scheduleRetryIfStillPending(anyLong(), any());
    }

    @Test
    @DisplayName("bookkeeping swallows repository failures so the relay loop survives")
    void bookkeepingSwallowsRepositoryFailures() {
        when(outboxEventRepository.findById(11L))
                .thenThrow(new RuntimeException("db down"));

        processor.scheduleRetryOrFailure(11L);
        // no exception propagated; nothing further was attempted
        verify(outboxEventRepository, never()).scheduleRetryIfStillPending(anyLong(), any());
        verify(outboxEventRepository, never()).markFailedIfStillPending(anyLong(), any());
    }
}
