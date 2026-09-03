-- =====================================================================
-- V7: Communication schema — contact messages.
-- Contact messages are never exposed publicly (see docs/DATABASE.md).
-- =====================================================================

CREATE TABLE contact_messages (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sender_name  VARCHAR(255) NOT NULL,
    sender_email VARCHAR(255) NOT NULL,
    sender_phone VARCHAR(50)  NULL,
    subject      VARCHAR(255) NULL,
    message      TEXT         NOT NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'NEW',
    internal_note TEXT        NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_contact_messages_status CHECK (status IN ('NEW','READ','REPLIED','ARCHIVED'))
);

CREATE INDEX idx_contact_messages_status_created ON contact_messages (status, created_at DESC);
