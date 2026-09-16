package com.lokmit.foundation.audit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lokmit.foundation.audit.entity.AuditLog;
import com.lokmit.foundation.audit.repository.AuditLogRepository;
import com.lokmit.foundation.common.api.PageResponse;
import com.lokmit.foundation.common.exception.BadRequestException;
import com.lokmit.foundation.common.pagination.PageParams;
import com.lokmit.foundation.common.exception.NotFoundException;
import com.lokmit.foundation.security.util.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AuditLogService} (A7.5).
 *
 * <p>Pins the two security-critical contracts: (1) the actor is always
 * resolved server-side via {@link SecurityUtils} — never from any caller
 * input — and (2) security-sensitive keys are redacted from the persisted
 * details JSON. Also covers rollback-on-unserializable-details (a corrupt
 * audit record must never be committed) and the read path.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private SecurityUtils securityUtils;

    private AuditLogService auditLogService;

    @BeforeEach
    void setUp() {
        auditLogService = new AuditLogService(auditLogRepository,
                securityUtils, new ObjectMapper());
    }

    // ------------------------------------------------------------------
    // record(): actor integrity
    // ------------------------------------------------------------------

    @Test
    @DisplayName("record resolves the actor from SecurityUtils, never from input")
    void actorComesFromSecurityUtils() {
        when(securityUtils.getCurrentUserId()).thenReturn(42L);

        auditLogService.record("START_REVIEW", "JOB_APPLICATION", 30L, Map.of("k", "v"));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();
        assertThat(saved.getActorUserId()).isEqualTo(42L);
        assertThat(saved.getAction()).isEqualTo("START_REVIEW");
        assertThat(saved.getEntityType()).isEqualTo("JOB_APPLICATION");
        assertThat(saved.getEntityId()).isEqualTo(30L);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getDetails()).isEqualTo("{\"k\":\"v\"}");
    }

    @Test
    @DisplayName("system actions persist a NULL actor")
    void systemActionHasNullActor() {
        when(securityUtils.getCurrentUserId()).thenReturn(null);

        auditLogService.record("SYSTEM_ACTION", "JOB_APPLICATION", 1L, null);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getActorUserId()).isNull();
        assertThat(captor.getValue().getDetails()).isNull();
    }

    // ------------------------------------------------------------------
    // record(): secret redaction
    // ------------------------------------------------------------------

    @Test
    @DisplayName("security-sensitive keys are redacted from the details JSON")
    void sensitiveKeysAreRedacted() {
        when(securityUtils.getCurrentUserId()).thenReturn(1L);

        auditLogService.record("ACTION", "ENTITY", 1L, Map.of(
                "password", "hunter2",
                "refreshToken", "jwt-value",
                "apiKey", "key",
                "AUTHORIZATION_HEADER", "Bearer x",
                "user_provided_secret", "s",
                "safeField", "kept"));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        String details = captor.getValue().getDetails();
        assertThat(details)
                .doesNotContain("hunter2")
                .doesNotContain("jwt-value")
                .doesNotContain("key\"")
                .doesNotContain("Bearer")
                .doesNotContain("s\"")
                .contains("safeField");
    }

    @Test
    @DisplayName("redaction is case-insensitive and separator-insensitive")
    void redactionIsCaseAndSeparatorInsensitive() {
        when(securityUtils.getCurrentUserId()).thenReturn(1L);

        auditLogService.record("ACTION", "ENTITY", 1L, Map.of(
                "PassWord", "x", "access-token", "t", "Client_Credentials", "c"));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getDetails()).isNull();
    }

    // ------------------------------------------------------------------
    // record(): serialization failure semantics
    // ------------------------------------------------------------------

    @Test
    @DisplayName("unserializable details abort with BadRequestException (transaction rolls back)")
    void unserializableDetailsAbort() {
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        Map<String, Object> poison = Map.of("bad", new Object() {
            private final Object self = this;
            @Override
            public String toString() {
                return "poison";
            }
        });

        assertThatThrownBy(() ->
                auditLogService.record("ACTION", "ENTITY", 1L, poison))
                .isInstanceOf(BadRequestException.class);
        verify(auditLogRepository, org.mockito.Mockito.never()).save(any());
    }

    // ------------------------------------------------------------------
    // read path
    // ------------------------------------------------------------------

    @Test
    @DisplayName("listAuditLogs applies filters and maps page results")
    void listAuditLogsAppliesFilters() {
        AuditLog log = new AuditLog();
        log.setId(9L);
        log.setActorUserId(2L);
        log.setAction("START_REVIEW");
        log.setEntityType("JOB_APPLICATION");
        log.setEntityId(30L);
        log.setDetails("{\"previousStatus\":\"SUBMITTED\"}");
        log.setCreatedAt(OffsetDateTime.now());
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(log)));

        PageParams params = new PageParams();
        PageResponse<com.lokmit.foundation.audit.dto.AuditLogResponse> page =
                auditLogService.listAuditLogs(
                        2L, "JOB_APPLICATION", 30L, "START_REVIEW", null, null, params);

        assertThat(page.getTotalItems()).isEqualTo(1);
        assertThat(page.getItems().get(0).getAction()).isEqualTo("START_REVIEW");
        // details JSON never contains credential material by construction
        assertThat(String.valueOf(page.getItems().get(0)))
                .doesNotContain("password");
    }

    @Test
    @DisplayName("getAuditLog returns 404 for unknown ids")
    void getAuditLogUnknownIs404() {
        when(auditLogRepository.findById(77L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> auditLogService.getAuditLog(77L))
                .isInstanceOf(NotFoundException.class);
    }
}
