package com.lokmit.foundation.audit.repository;

import com.lokmit.foundation.audit.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Repository for the V16 'audit_logs' table (read path; writes are service-only). */
public interface AuditLogRepository
        extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {
}
