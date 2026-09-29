package com.lokmit.foundation.outbox.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lokmit.foundation.common.exception.BadRequestException;
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

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the outbox write/claim path (A7.5).
 *
 * <p>Pins the enqueue contract: PENDING seed state, valid JSON TEXT payload
 * (never JSONB), and a hard failure that aborts the caller's transaction
 * when the payload cannot be serialized.</p>
 */
@ExtendWith(MockitoExtension.class)
class OutboxServiceTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    private OutboxService outboxService;

    @BeforeEach
    void setUp() {
        outboxService = new OutboxService(outboxEventRepository,
                new OutboxRelayConfig(), new ObjectMapper());
    }

    @Test
    @DisplayName("enqueue persists a PENDING event with a JSON TEXT payload")
    void enqueuePersistsPendingEventWithJsonPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("recipientUserId", 7L);
        payload.put("title", "Hello");

        outboxService.enqueue("JOB_APPLICATION", 30L,
                "APPLICATION_STATUS_CHANGED", payload);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());
        OutboxEvent saved = captor.getValue();

        assertThat(saved.getAggregateType()).isEqualTo("JOB_APPLICATION");
        assertThat(saved.getAggregateId()).isEqualTo(30L);
        assertThat(saved.getEventType()).isEqualTo("APPLICATION_STATUS_CHANGED");
        assertThat(saved.getStatus()).isEqualTo(OutboxEvent.STATUS_PENDING);
        assertThat(saved.getAttempts()).isZero();
        assertThat(saved.getAvailableAt()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        // payload is valid JSON TEXT containing the original values
        assertThat(saved.getPayload()).contains("\"recipientUserId\":7");
        assertThat(saved.getPayload()).startsWith("{").endsWith("}");
    }

    @Test
    @DisplayName("enqueue with an unserializable payload aborts with BadRequestException")
    void enqueueRejectsUnserializablePayload() {
        Map<String, Object> payload = Map.of("bad", new Object() {
            // ObjectMapper fails on an unresolvable self-reference
            private final Object self = this;
            @Override
            public String toString() {
                return "unserializable";
            }
        });

        assertThatThrownBy(() -> outboxService.enqueue("X", 1L, "E", payload))
                .isInstanceOf(BadRequestException.class);
        verify(outboxEventRepository, org.mockito.Mockito.never()).save(any());
    }
}
