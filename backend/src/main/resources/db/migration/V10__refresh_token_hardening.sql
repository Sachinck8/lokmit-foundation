-- =====================================================================
-- V10: Refresh token rotation hardening.
--
-- Adds a lightweight token-family marker plus the consumption columns
-- needed for reuse detection and safe concurrent refresh:
--
--  family_id   Groups the tokens minted from one successful
--              authentication (login or refresh). A replayed,
--              already-consumed token lets the system revoke every
--              still-valid token in the same family — the standard
--              countermeasure for a stolen-then-rotated token.
--
--  consumed_at Timestamp recorded atomically by the refresh flow when
--              a token is traded in. A token with consumed_at set (or
--              revoked_at set) is no longer usable; presenting one is
--              treated as reuse.
--
-- No existing columns or constraints are modified.
-- =====================================================================

ALTER TABLE refresh_tokens
    ADD COLUMN family_id   UUID        NULL,
    ADD COLUMN consumed_at TIMESTAMPTZ NULL;

CREATE INDEX idx_refresh_tokens_family ON refresh_tokens (family_id);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens (expires_at);
