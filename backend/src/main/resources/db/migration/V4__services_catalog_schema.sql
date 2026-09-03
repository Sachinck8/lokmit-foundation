-- =====================================================================
-- V4: Services catalog — service categories, service offerings,
-- areas of expertise.
-- =====================================================================

CREATE TABLE service_categories (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    slug          VARCHAR(120) NOT NULL,
    description   VARCHAR(500) NULL,
    display_order INT          NOT NULL DEFAULT 0,
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_service_categories_name UNIQUE (name),
    CONSTRAINT uq_service_categories_slug UNIQUE (slug),
    CONSTRAINT chk_service_categories_status CHECK (status IN ('ACTIVE','INACTIVE'))
);

CREATE TABLE services (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    slug          VARCHAR(160) NOT NULL,
    title         VARCHAR(255) NOT NULL,
    summary       VARCHAR(500) NULL,
    description   TEXT         NULL,
    category_id   BIGINT       NULL,
    display_order INT          NOT NULL DEFAULT 0,
    status        VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_services_slug UNIQUE (slug),
    CONSTRAINT fk_services_category FOREIGN KEY (category_id)
        REFERENCES service_categories (id) ON DELETE SET NULL,
    CONSTRAINT chk_services_status CHECK (status IN ('DRAFT','PUBLISHED','ARCHIVED'))
);

CREATE INDEX idx_services_category ON services (category_id);

CREATE TABLE expertise_areas (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    slug          VARCHAR(160) NOT NULL,
    name          VARCHAR(255) NOT NULL,
    description   TEXT         NULL,
    display_order INT          NOT NULL DEFAULT 0,
    status        VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_expertise_areas_slug UNIQUE (slug),
    CONSTRAINT chk_expertise_areas_status CHECK (status IN ('DRAFT','PUBLISHED','ARCHIVED'))
);
