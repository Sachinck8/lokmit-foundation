-- =====================================================================
-- V9: Login brute-force protection.
-- Adds persistent, account-level failed-login tracking and temporary
-- lockout state to the users table. State lives in the database so it
-- is shared across all backend instances (multi-instance safe) and
-- survives application restarts. The counter and lock are cleared by
-- successful authentication.
-- =====================================================================

ALTER TABLE users
    ADD COLUMN failed_login_attempts        INT         NOT NULL DEFAULT 0,
    ADD COLUMN failed_login_window_started_at TIMESTAMPTZ NULL,
    ADD COLUMN locked_until                 TIMESTAMPTZ NULL;

ALTER TABLE users
    ADD CONSTRAINT chk_users_failed_login_attempts
    CHECK (failed_login_attempts >= 0);
