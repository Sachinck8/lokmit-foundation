-- =====================================================================
-- V5: Projects schema — categories, projects, project images.
-- =====================================================================

CREATE TABLE project_categories (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    slug          VARCHAR(120) NOT NULL,
    description   VARCHAR(500) NULL,
    display_order INT          NOT NULL DEFAULT 0,
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_project_categories_name UNIQUE (name),
    CONSTRAINT uq_project_categories_slug UNIQUE (slug),
    CONSTRAINT chk_project_categories_status CHECK (status IN ('ACTIVE','INACTIVE'))
);

CREATE TABLE projects (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    slug           VARCHAR(160) NOT NULL,
    title          VARCHAR(255) NOT NULL,
    summary        VARCHAR(500) NULL,
    description    TEXT         NULL,
    category_id    BIGINT       NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    project_status VARCHAR(20)  NULL,
    location       VARCHAR(255) NULL,
    start_date     DATE         NULL,
    end_date       DATE         NULL,
    objectives     JSONB        NULL,
    impact_summary TEXT         NULL,
    published_at   TIMESTAMPTZ  NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_projects_slug UNIQUE (slug),
    CONSTRAINT fk_projects_category FOREIGN KEY (category_id)
        REFERENCES project_categories (id) ON DELETE SET NULL,
    CONSTRAINT chk_projects_status CHECK (status IN ('DRAFT','PUBLISHED','ARCHIVED')),
    CONSTRAINT chk_projects_project_status CHECK (project_status IN ('PLANNING','ONGOING','COMPLETED')),
    CONSTRAINT chk_projects_dates CHECK (end_date IS NULL OR start_date IS NULL OR end_date >= start_date)
);

CREATE INDEX idx_projects_category ON projects (category_id);
CREATE INDEX idx_projects_status_published ON projects (status, published_at DESC);

CREATE TABLE project_images (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    project_id    BIGINT       NOT NULL,
    image_url     VARCHAR(500) NOT NULL,
    alt_text      VARCHAR(255) NULL,
    caption       VARCHAR(500) NULL,
    display_order INT          NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT fk_project_images_project FOREIGN KEY (project_id)
        REFERENCES projects (id) ON DELETE CASCADE
);

CREATE INDEX idx_project_images_project ON project_images (project_id);
