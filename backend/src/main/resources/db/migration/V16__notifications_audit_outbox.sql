-- =====================================================================
-- V16: Notifications + Audit + Outbox (A7.5).
--
-- notifications — personal in-app notices addressed to one user.
--   MVP delivery is IN-APP ONLY: email/SMS/WhatsApp are explicitly out of
--   scope for this phase, so no delivery-channel columns exist. Rows
--   cascade with the owning user identity (personal data).
--
-- audit_logs — append-only administrative action trail, backend-written
--   only. There is deliberately NO updated_at: records are immutable.
--   actor_user_id is NULLable (system actions) and has NO ON DELETE
--   action so audit survival is never coupled to user retention — the
--   same rationale as V15's fk_application_status_history_actor.
--
-- outbox_events — transactional outbox. Business services persist an
--   event row in the SAME transaction as the originating action; a
--   scheduled relay claims rows atomically and materializes in-app
--   notifications. No FK on aggregate_id: aggregates span domains and
--   must not create destructive coupling. payload is TEXT (JSON), not
--   JSONB — JPA maps it trivially and the JSON contract is validated in
--   application code.
--
-- Permission seeding follows the V14 idempotent style. Audit-log reads
-- are NOT given their own permission: they are restricted to the
-- existing SUPER_ADMIN-only users:manage authority in @PreAuthorize
-- (see AuditLogController) — one sensitive cross-domain boundary, no
-- redundant second permission.
-- =====================================================================

CREATE TABLE notifications (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    recipient_user_id BIGINT       NOT NULL,
    type              VARCHAR(50)  NOT NULL,
    title             VARCHAR(255) NOT NULL,
    body              TEXT         NULL,
    entity_type       VARCHAR(50)  NULL,
    entity_id         BIGINT       NULL,
    read_at           TIMESTAMPTZ  NULL,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT fk_notifications_recipient FOREIGN KEY (recipient_user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_notifications_type CHECK (type IN
        ('APPLICATION_STATUS_CHANGED','INTERVIEW_SCHEDULED',
         'INTERVIEW_UPDATED','INTERVIEW_CANCELLED'))
);

CREATE INDEX idx_notifications_recipient_read
    ON notifications (recipient_user_id, read_at);
CREATE INDEX idx_notifications_created
    ON notifications (created_at DESC);

CREATE TABLE audit_logs (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    actor_user_id BIGINT       NULL,
    action        VARCHAR(100) NOT NULL,
    entity_type   VARCHAR(50)  NOT NULL,
    entity_id     BIGINT       NULL,
    details       TEXT         NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT fk_audit_logs_actor FOREIGN KEY (actor_user_id)
        REFERENCES users (id)
);

CREATE INDEX idx_audit_logs_created ON audit_logs (created_at DESC);
CREATE INDEX idx_audit_logs_entity ON audit_logs (entity_type, entity_id);
CREATE INDEX idx_audit_logs_actor ON audit_logs (actor_user_id);

CREATE TABLE outbox_events (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id   BIGINT      NOT NULL,
    event_type     VARCHAR(50) NOT NULL,
    payload        TEXT        NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts       INT         NOT NULL DEFAULT 0,
    available_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    processed_at   TIMESTAMPTZ NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_outbox_events_status CHECK (status IN
        ('PENDING','PROCESSED','FAILED')),
    CONSTRAINT chk_outbox_events_attempts CHECK (attempts >= 0)
);

CREATE INDEX idx_outbox_events_status_available
    ON outbox_events (status, available_at);

-- ---------------------------------------------------------------------
-- notifications:manage — guards the admin in-app notification APIs.
-- Granted only to SUPER_ADMIN and ADMIN. Recipient isolation is still
-- enforced per user in the service layer (notifications:manage does NOT
-- grant access to another user's personal notices).
-- ---------------------------------------------------------------------

INSERT INTO permissions (code, description)
SELECT 'notifications:manage',
       'View and manage in-app notifications (recipient-scoped).'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'notifications:manage');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'notifications:manage'
WHERE r.code IN ('SUPER_ADMIN', 'ADMIN')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id);
