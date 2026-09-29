package com.lokmit.foundation.outbox.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lokmit.foundation.notification.entity.Notification;
import com.lokmit.foundation.notification.repository.NotificationRepository;
import com.lokmit.foundation.outbox.config.OutboxRelayConfig;
import com.lokmit.foundation.outbox.entity.OutboxEvent;
import com.lokmit.foundation.outbox.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Per-event outbox processing and failure bookkeeping (A7.5).
 *
 * <p>This class exists so the {@code REQUIRES_NEW} boundaries are actually
 * enforced: transaction semantics only apply through the Spring proxy when
 * the method is invoked from <em>outside</em> the bean. The relay therefore
 * calls {@link #process(long)} and {@link #scheduleRetryOrFailure(long)}
 * once per claimed event from {@link OutboxRelay} — a self-invocation on the
 * same bean would silently run everything inside the caller's transaction,
 * where a single failing event would roll back the whole batch and
 * permanently wedge the queue.</p>
 *
 * <p>Each event's outcome is fully isolated:</p>
 *
 * <ul>
 *   <li>Success: notification insert + PROCESSED commit atomically.</li>
 *   <li>Terminal payload problem: event marked FAILED (never retried) — no
 *       broken notification row is ever created.</li>
 *   <li>Transient failure: transaction rolled back, then
 *       {@link #scheduleRetryOrFailure(long)} commits the retry state
 *       (attempts + bounded backoff, FAILED past maxAttempts) in its own
 *       transaction.</li>
 * </ul>
 *
 * <p>Concurrency: {@link #process(long)} re-reads the row with a blocking
 * {@code FOR UPDATE} and skips (without re-processing) if a competing pass
 * has already moved it out of PENDING; the bookkeeping UPDATEs are guarded
 * on {@code status = 'PENDING'} so they can never clobber a finished event.</p>
 */
@Component
public class OutboxEventProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(OutboxEventProcessor.class);

    private final OutboxEventRepository outboxEventRepository;
    private final NotificationRepository notificationRepository;
    private final OutboxRelayConfig config;
    private final ObjectMapper objectMapper;

    public OutboxEventProcessor(OutboxEventRepository outboxEventRepository,
                                NotificationRepository notificationRepository,
                                OutboxRelayConfig config,
                                ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.notificationRepository = notificationRepository;
        this.config = config;
        this.objectMapper = objectMapper;
    }

    /** Result of one processing attempt. */
    public enum Result {
        /** Notification materialized and the event marked PROCESSED. */
        PROCESSED,
        /** Terminal payload problem — event marked FAILED, never retried. */
        FAILED_TERMINAL,
        /** Transient failure — rolled back; schedule a retry afterwards. */
        FAILED_RETRYABLE
    }

    /**
     * Processes one event identified by {@code eventId} in a NEW transaction.
     * Never throws into the relay loop: any exception is converted to
     * {@link Result#FAILED_RETRYABLE} after the rollback.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Result process(long eventId) {
        // Blocking FOR UPDATE re-read: waits out any competing writer and
        // discards the stale state carried over from the claim pass.
        OutboxEvent event = outboxEventRepository.acquireForProcessing(eventId)
                .orElse(null);
        if (event == null || !OutboxEvent.STATUS_PENDING.equals(event.getStatus())) {
            // Already processed/failed by a competing pass — treat as done.
            return Result.PROCESSED;
        }
        try {
            materialize(event);
            event.setStatus(OutboxEvent.STATUS_PROCESSED);
            event.setProcessedAt(OffsetDateTime.now());
            outboxEventRepository.save(event);
            return Result.PROCESSED;
        } catch (TerminalPayloadException e) {
            // Valid JSON but unusable content: deterministic failure, no retry.
            LOG.warn("Outbox event {} marked FAILED: {}", eventId, e.getMessage());
            outboxEventRepository.markFailedIfStillPending(eventId, OffsetDateTime.now());
            return Result.FAILED_TERMINAL;
        } catch (Exception e) {
            // Everything else is transient: roll this transaction back
            // (releasing the row lock) and let the relay schedule the retry.
            LOG.warn("Outbox event {} processing failed (attempt {}): {}",
                    eventId, event.getAttempts() + 1, e.getMessage());
            return Result.FAILED_RETRYABLE;
        }
    }

    /**
     * Retry bookkeeping for one transiently failed event, in its own
     * transaction (called externally by the relay after the processing
     * transaction rolled back). Increments attempts and shifts available_at
     * forward (bounded backoff), or marks FAILED once maxAttempts is
     * reached. The UPDATEs are guarded on status = 'PENDING'; if neither
     * applies, a competing pass finished the event in the meantime and it
     * is left alone. Never throws into the relay loop.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void scheduleRetryOrFailure(long eventId) {
        try {
            int nextAttempt = outboxEventRepository.findById(eventId)
                    .map(event -> event.getAttempts() + 1)
                    .orElse(-1);
            if (nextAttempt < 0) {
                return; // event vanished — nothing to do
            }
            if (nextAttempt >= config.getMaxAttempts()) {
                int marked = outboxEventRepository.markFailedIfStillPending(
                        eventId, OffsetDateTime.now());
                if (marked > 0) {
                    LOG.warn("Outbox event {} marked FAILED after {} attempts",
                            eventId, nextAttempt);
                }
            } else {
                long backoffSeconds =
                        (long) config.getRetryBackoffSeconds() * nextAttempt;
                outboxEventRepository.scheduleRetryIfStillPending(eventId,
                        OffsetDateTime.now().plusSeconds(backoffSeconds));
            }
        } catch (Exception bookkeepingFailure) {
            // Event stays PENDING with its current available_at; a later
            // pass retries it. Never propagate into the relay loop.
            LOG.warn("Outbox retry bookkeeping failed for event {}: {}",
                    eventId, bookkeepingFailure.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    /**
     * Validates the payload and inserts the notification row. Payload must
     * parse to a JSON object; missing recipient/type/title raises
     * {@link TerminalPayloadException}.
     */
    private void materialize(OutboxEvent event) {
        Map<?, ?> payload = readPayload(event.getPayload());
        Long recipientUserId = toLong(payload.get("recipientUserId"));
        String type = str(payload.get("notificationType"));
        String title = str(payload.get("title"));

        if (recipientUserId == null || !StringUtils.hasText(type)
                || !StringUtils.hasText(title)) {
            throw new TerminalPayloadException(
                    "missing recipient/type/title in payload");
        }

        Notification notification = new Notification();
        notification.setRecipientUserId(recipientUserId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setBody(str(payload.get("body")));
        notification.setEntityType(str(payload.get("entityType")));
        notification.setEntityId(toLong(payload.get("entityId")));
        notification.setCreatedAt(OffsetDateTime.now());
        notificationRepository.save(notification);
    }

    private Map<?, ?> readPayload(String payload) {
        try {
            return objectMapper.readValue(payload, Map.class);
        } catch (Exception e) {
            throw new TerminalPayloadException("payload is not valid JSON");
        }
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String s && s.matches("\\d+")) {
            return Long.parseLong(s);
        }
        return null;
    }

    private String str(Object value) {
        return value instanceof String s ? s : null;
    }

    /** Deterministic, non-retryable payload defect. */
    private static class TerminalPayloadException extends RuntimeException {
        TerminalPayloadException(String message) {
            super(message);
        }
    }
}
