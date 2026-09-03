-- =====================================================================
-- V3: Corporate & CMS schema — site settings, website content blocks,
-- SEO metadata, team, certifications, partners, downloads, FAQs.
-- =====================================================================

CREATE TABLE site_settings (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    setting_key   VARCHAR(100) NOT NULL,
    setting_value TEXT         NOT NULL,
    description   VARCHAR(500) NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_site_settings_key UNIQUE (setting_key)
);

CREATE TABLE website_content (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    page_key     VARCHAR(100) NOT NULL,
    section_key  VARCHAR(100) NOT NULL,
    title        VARCHAR(255) NULL,
    content_json JSONB        NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_website_content_page_section UNIQUE (page_key, section_key),
    CONSTRAINT chk_website_content_status CHECK (status IN ('DRAFT','PUBLISHED','ARCHIVED'))
);

CREATE INDEX idx_website_content_page ON website_content (page_key);

CREATE TABLE seo_metadata (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    entity_type     VARCHAR(50)  NOT NULL,
    entity_id       BIGINT       NOT NULL,
    seo_title       VARCHAR(255) NULL,
    seo_description VARCHAR(500) NULL,
    canonical_url   VARCHAR(500) NULL,
    og_title        VARCHAR(255) NULL,
    og_description  VARCHAR(500) NULL,
    og_image_url    VARCHAR(500) NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_seo_metadata_entity UNIQUE (entity_type, entity_id)
);

-- entity_type/entity_id is a deliberate polymorphic reference (no FK):
-- see docs/DATABASE.md, decision D5.

CREATE TABLE team_members (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    full_name     VARCHAR(255) NOT NULL,
    designation   VARCHAR(255) NOT NULL,
    bio           TEXT         NULL,
    photo_url     VARCHAR(500) NULL,
    email_public  VARCHAR(255) NULL,
    display_order INT          NOT NULL DEFAULT 0,
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_team_members_status CHECK (status IN ('ACTIVE','INACTIVE'))
);

CREATE TABLE certifications (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title               VARCHAR(255) NOT NULL,
    issuer              VARCHAR(255) NULL,
    registration_number VARCHAR(255) NULL,
    issued_on           DATE         NULL,
    expires_on          DATE         NULL,
    document_url        VARCHAR(500) NULL,
    display_order       INT          NOT NULL DEFAULT 0,
    status              VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_certifications_status CHECK (status IN ('DRAFT','PUBLISHED','ARCHIVED'))
);

CREATE TABLE partners (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name                    VARCHAR(255) NOT NULL,
    website_url             VARCHAR(500) NULL,
    logo_url                VARCHAR(500) NULL,
    description             TEXT         NULL,
    partnership_type        VARCHAR(30)  NOT NULL DEFAULT 'PARTNER',
    logo_permission_granted BOOLEAN      NOT NULL DEFAULT FALSE,
    display_order           INT          NOT NULL DEFAULT 0,
    status                  VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_partners_type CHECK (partnership_type IN ('CLIENT','PARTNER','SPONSOR','TECHNOLOGY')),
    CONSTRAINT chk_partners_status CHECK (status IN ('ACTIVE','INACTIVE'))
);

CREATE TABLE downloads (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title           VARCHAR(255)  NOT NULL,
    description     VARCHAR(1000) NULL,
    category        VARCHAR(100)  NULL,
    file_name       VARCHAR(255)  NULL,
    file_url        VARCHAR(500)  NULL,
    file_type       VARCHAR(100)  NULL,
    file_size_bytes BIGINT        NULL,
    visibility      VARCHAR(20)   NOT NULL DEFAULT 'PUBLIC',
    download_count  BIGINT        NOT NULL DEFAULT 0,
    display_order   INT           NOT NULL DEFAULT 0,
    status          VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT chk_downloads_visibility CHECK (visibility IN ('PUBLIC','PRIVATE')),
    CONSTRAINT chk_downloads_status CHECK (status IN ('DRAFT','PUBLISHED','ARCHIVED'))
);

CREATE INDEX idx_downloads_category ON downloads (category);

CREATE TABLE faqs (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    question      TEXT         NOT NULL,
    answer        TEXT         NOT NULL,
    category      VARCHAR(100) NULL,
    display_order INT          NOT NULL DEFAULT 0,
    status        VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_faqs_status CHECK (status IN ('DRAFT','PUBLISHED','ARCHIVED'))
);

CREATE INDEX idx_faqs_category ON faqs (category);

