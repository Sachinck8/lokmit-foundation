package com.lokmit.foundation.outbox.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lokmit.foundation.common.exception.BadRequestException;
import com.lokmit.foundation.outbox.config.OutboxRelayConfig;
import com.lokmit.foundation.outbox.entity.OutboxEvent;
import com.lokmit.foundation.outbox.repository.OutboxEventRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Transactional outbox write/claim service (A7.5).
 *
 * <p>Write path: {@link #enqueue} is called from business services inside
 * their transaction (REQUIRED propagation) — the event commits or rolls
 * back atomically with the originating action. The payload must serialize
 * to valid JSON TEXT or the transaction fails.</p>
 *
 * <p>Relay path: {@link #claimDueEventIds} runs the one short claim
 * transaction (locks due PENDING rows with {@code FOR UPDATE SKIP LOCKED}
 * and returns only their IDs). It is deliberately invoked EXTERNALLY by
 * {@link com.lokmit.foundation.outbox.relay.OutboxRelay} — calling it from
 * a sibling method inside this class would bypass the Spring proxy and run
 * the pessimistic-lock query without a transaction.</p>
 *
 * <p>Per-event processing lives in {@link OutboxEventProcessor}; retry
 * bookkeeping in {@link OutboxEventProcessor#scheduleRetryOrFailure(long)}.</p>
 */
@Service
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxRelayConfig config;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxEventRepository outboxEventRepository,
                         OutboxRelayConfig config,
                         ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.config = config;
        this.objectMapper = objectMapper;
    }

    /**
     * Persists one outbox event in the caller's transaction. The payload
     * map must be JSON-serializable; anything else aborts the originating
     * business transaction.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void enqueue(String aggregateType, Long aggregateId,
                        String eventType, Map<String, ?> payload) {
        OutboxEvent event = new OutboxEvent();
        event.setAggregateType(aggregateType);
        event.setAggregateId(aggregateId);
        event.setEventType(eventType);
        event.setPayload(toJson(payload));
        event.setStatus(OutboxEvent.STATUS_PENDING);
        event.setAttempts(0);
        event.setAvailableAt(OffsetDateTime.now());
        event.setCreatedAt(OffsetDateTime.now());
        outboxEventRepository.save(event);
    }

    /**
     * Claims the due PENDING event IDs in one short transaction. Rows are
     * locked SKIP LOCKED so concurrent instances take disjoint subsets; the
     * locks are released when this transaction commits. Must be called from
     * outside this bean (via {@code OutboxRelay}) so the transaction applies.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public List<Long> claimDueEventIds() {
        Pageable limit = PageRequest.of(0, config.getBatchSize());
        return outboxEventRepository.claimDuePendingEventIds(
                OffsetDateTime.now(), limit);
    }

    private String toJson(Map<String, ?> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new BadRequestException(
                    "Outbox payload must be JSON-serializable");
        }
    }
}
