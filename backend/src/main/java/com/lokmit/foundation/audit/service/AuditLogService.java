package com.lokmit.foundation.audit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lokmit.foundation.audit.dto.AuditLogResponse;
import com.lokmit.foundation.audit.entity.AuditLog;
import com.lokmit.foundation.audit.repository.AuditLogRepository;
import com.lokmit.foundation.common.api.PageResponse;
import com.lokmit.foundation.common.exception.BadRequestException;
import com.lokmit.foundation.common.pagination.PageParams;
import com.lokmit.foundation.security.util.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Set;

/**
 * Backend-controlled audit trail (A7.5).
 *
 * <p>Write path: {@link #record} is called from business services inside
 * their transaction (REQUIRED propagation). The actor is always resolved
 * server-side via {@link SecurityUtils}; it is never accepted from client
 * input. {@code details} is a minimal structured map serialized to JSON
 * TEXT — keys that could carry secrets (password, token, secret,
 * credential names, authorization headers) are redacted before
 * persistence, and the payload must serialize to valid JSON or the whole
 * transaction fails (a corrupt audit record is treated as a bug, not
 * tolerated silently).</p>
 *
 * <p>Read path: DB-side filtered/paginated listing, SUPER_ADMIN-only via
 * the existing {@code users:manage} authority. Records are immutable —
 * there is deliberately no update or delete path.</p>
 */
@Service
public class AuditLogService {

    private static final Logger LOG = LoggerFactory.getLogger(AuditLogService.class);

    /** Keys that must never reach the persisted details JSON. */
    private static final Set<String> FORBIDDEN_DETAIL_KEYS = Set.of(
            "password", "passwordhash", "currentpassword", "newpassword",
            "accesstoken", "refreshtoken", "token", "jwt", "secret",
            "secretkey", "apikey", "authorization", "cookie", "credentials");

    private final AuditLogRepository auditLogRepository;
    private final SecurityUtils securityUtils;
    private final ObjectMapper objectMapper;

    public AuditLogService(AuditLogRepository auditLogRepository,
                           SecurityUtils securityUtils,
                           ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.securityUtils = securityUtils;
        this.objectMapper = objectMapper;
    }

    /**
     * Records one immutable audit entry in the caller's transaction
     * (REQUIRED propagation — joins the business transaction so the audit
     * row commits or rolls back atomically with the action).
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void record(String action, String entityType, Long entityId,
                       Map<String, ?> details) {
        AuditLog log = new AuditLog();
        log.setActorUserId(securityUtils.getCurrentUserId());
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setDetails(serializeAndRedact(details));
        log.setCreatedAt(OffsetDateTime.now());
        auditLogRepository.save(log);
    }

    // ------------------------------------------------------------------
    // read path (SUPER_ADMIN only via users:manage)
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> listAuditLogs(Long actorUserId,
                                                        String entityType,
                                                        Long entityId,
                                                        String action,
                                                        OffsetDateTime from,
                                                        OffsetDateTime to,
                                                        PageParams pageParams) {
        Pageable pageable = PageRequest.of(pageParams.getPage(), pageParams.getSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<AuditLog> spec = Specification.where(null);
        if (actorUserId != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("actorUserId"), actorUserId));
        }
        if (StringUtils.hasText(entityType)) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("entityType"), entityType));
        }
        if (entityId != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("entityId"), entityId));
        }
        if (StringUtils.hasText(action)) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("action"), action));
        }
        if (from != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("createdAt"), from));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("createdAt"), to));
        }

        return PageResponse.of(auditLogRepository.findAll(spec, pageable)
                .map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public AuditLogResponse getAuditLog(long id) {
        return auditLogRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new com.lokmit.foundation.common.exception.NotFoundException(
                        "Audit log not found: " + id));
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    /**
     * Serializes details to compact JSON TEXT. Security-sensitive keys are
     * removed (case-insensitive, substring match on the forbidden names).
     * An unserializable details map throws BadRequestException so the
     * originating transaction rolls back — a corrupt audit record must
     * never be committed half-written.
     */
    private String serializeAndRedact(Map<String, ?> details) {
        if (details == null || details.isEmpty()) {
            return null;
        }
        Map<String, Object> safe = new java.util.LinkedHashMap<>();
        details.forEach((key, value) -> {
            if (key != null && !isForbiddenKey(key)) {
                safe.put(key, value);
            }
        });
        if (safe.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(safe);
        } catch (Exception e) {
            LOG.warn("Audit details serialization failed; rolling back with 400", e);
            throw new BadRequestException("Audit details must be JSON-serializable");
        }
    }

    private boolean isForbiddenKey(String key) {
        String normalized = key.toLowerCase().replace("_", "").replace("-", "");
        return FORBIDDEN_DETAIL_KEYS.stream().anyMatch(normalized::contains);
    }

    private AuditLogResponse toResponse(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .actorUserId(log.getActorUserId())
                .action(log.getAction())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .details(log.getDetails())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
