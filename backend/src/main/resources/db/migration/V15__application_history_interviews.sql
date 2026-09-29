-- =====================================================================
-- V15: Application status history + interviews (A7.4).
--
-- application_status_history — persisted audit of application status
--   transitions. Rows are written by the A7.3 lifecycle endpoints in the
--   SAME transaction as the status change (a failed history insert rolls
--   back the transition — never "status changed but history missing").
--   previous_status is NULL for an application's initial observation.
--   changed_by records the acting admin's database user id; it is NULLable
--   so audit survival is never coupled to user retention (no ON DELETE
--   action — the database preserves the row's actor reference semantics
--   without cascading identity deletes).
--
-- interviews — interview scheduling attached to one application. Mode
--   vocabulary reuses the V8 jobs work-mode values (ONSITE/REMOTE/PHONE);
--   status is a simple four-value lifecycle. No participant table at MVP
--   scale (mirrors the V8 Phase-0 decision that 1:1 participant tables add
--   no value).
--
-- Both tables hang exclusively off job_applications with ON DELETE CASCADE
-- — the same ownership-cleanup semantics as V8's fk_job_skills_job. A7.3
-- deliberately exposes no application-delete endpoint, so in practice rows
-- only disappear alongside their (FK-blocked) application.
--
-- Idempotency note: pure CREATE TABLE/INDEX DDL following the V8 convention
-- (Flyway runs each versioned migration exactly once; no data seed here).
-- =====================================================================

CREATE TABLE application_status_history (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    application_id  BIGINT       NOT NULL,
    previous_status VARCHAR(20)  NULL,
    new_status      VARCHAR(20)  NOT NULL,
    changed_by      BIGINT       NULL,
    changed_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    note            VARCHAR(500) NULL,
    CONSTRAINT fk_application_status_history_application FOREIGN KEY (application_id)
        REFERENCES job_applications (id) ON DELETE CASCADE,
    CONSTRAINT fk_application_status_history_actor FOREIGN KEY (changed_by)
        REFERENCES users (id),
    CONSTRAINT chk_application_status_history_new CHECK (new_status IN
        ('SUBMITTED','UNDER_REVIEW','SHORTLISTED','HIRED','REJECTED','WITHDRAWN')),
    CONSTRAINT chk_application_status_history_previous CHECK (previous_status IS NULL OR previous_status IN
        ('SUBMITTED','UNDER_REVIEW','SHORTLISTED','HIRED','REJECTED','WITHDRAWN')),
    CONSTRAINT chk_application_status_history_transition CHECK (previous_status IS DISTINCT FROM new_status)
);

CREATE INDEX idx_application_status_history_application
    ON application_status_history (application_id, changed_at DESC);

CREATE TABLE interviews (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    application_id BIGINT       NOT NULL,
    scheduled_at   TIMESTAMPTZ  NOT NULL,
    mode           VARCHAR(20)  NOT NULL,
    location       VARCHAR(255) NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'SCHEDULED',
    notes          TEXT         NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT fk_interviews_application FOREIGN KEY (application_id)
        REFERENCES job_applications (id) ON DELETE CASCADE,
    CONSTRAINT chk_interviews_mode CHECK (mode IN ('ONSITE','REMOTE','PHONE')),
    CONSTRAINT chk_interviews_status CHECK (status IN ('SCHEDULED','COMPLETED','CANCELLED','NO_SHOW'))
);

CREATE INDEX idx_interviews_application ON interviews (application_id, scheduled_at);
