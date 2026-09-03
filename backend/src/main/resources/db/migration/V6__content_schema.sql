-- =====================================================================
-- V6: Content schema — news/blog posts, categories, events, galleries,
-- testimonials.
-- =====================================================================

CREATE TABLE news_categories (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    slug          VARCHAR(120) NOT NULL,
    description   VARCHAR(500) NULL,
    display_order INT          NOT NULL DEFAULT 0,
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_news_categories_name UNIQUE (name),
    CONSTRAINT uq_news_categories_slug UNIQUE (slug),
    CONSTRAINT chk_news_categories_status CHECK (status IN ('ACTIVE','INACTIVE'))
);

CREATE TABLE blog_posts (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    slug              VARCHAR(160) NOT NULL,
    title             VARCHAR(255) NOT NULL,
    summary           VARCHAR(500) NULL,
    content           TEXT         NULL,
    post_type         VARCHAR(20)  NOT NULL DEFAULT 'NEWS',
    status            VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    featured_image_url VARCHAR(500) NULL,
    author_user_id    BIGINT       NULL,
    scheduled_at      TIMESTAMPTZ  NULL,
    published_at      TIMESTAMPTZ  NULL,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_blog_posts_slug UNIQUE (slug),
    CONSTRAINT fk_blog_posts_author FOREIGN KEY (author_user_id)
        REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT chk_blog_posts_type CHECK (post_type IN ('NEWS','BLOG')),
    CONSTRAINT chk_blog_posts_status CHECK (status IN ('DRAFT','SCHEDULED','PUBLISHED','ARCHIVED'))
);

CREATE INDEX idx_blog_posts_status_published ON blog_posts (status, published_at DESC);
CREATE INDEX idx_blog_posts_type ON blog_posts (post_type);

CREATE TABLE blog_post_categories (
    blog_post_id     BIGINT NOT NULL,
    news_category_id BIGINT NOT NULL,
    PRIMARY KEY (blog_post_id, news_category_id),
    CONSTRAINT fk_blog_post_categories_post FOREIGN KEY (blog_post_id)
        REFERENCES blog_posts (id) ON DELETE CASCADE,
    CONSTRAINT fk_blog_post_categories_category FOREIGN KEY (news_category_id)
        REFERENCES news_categories (id) ON DELETE CASCADE
);

CREATE INDEX idx_blog_post_categories_category ON blog_post_categories (news_category_id);

CREATE TABLE events (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    slug              VARCHAR(160) NOT NULL,
    title             VARCHAR(255) NOT NULL,
    description       TEXT         NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    event_date_start  TIMESTAMPTZ  NOT NULL,
    event_date_end    TIMESTAMPTZ  NULL,
    location          VARCHAR(255) NULL,
    registration_info TEXT         NULL,
    cover_image_url   VARCHAR(500) NULL,
    published_at      TIMESTAMPTZ  NULL,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_events_slug UNIQUE (slug),
    CONSTRAINT chk_events_status CHECK (status IN ('DRAFT','PUBLISHED','ARCHIVED')),
    CONSTRAINT chk_events_dates CHECK (event_date_end IS NULL OR event_date_end >= event_date_start)
);

CREATE INDEX idx_events_date_start ON events (event_date_start);
CREATE INDEX idx_events_status ON events (status);

CREATE TABLE event_images (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_id     BIGINT       NOT NULL,
    image_url    VARCHAR(500) NOT NULL,
    alt_text     VARCHAR(255) NULL,
    caption      VARCHAR(500) NULL,
    display_order INT         NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT fk_event_images_event FOREIGN KEY (event_id)
        REFERENCES events (id) ON DELETE CASCADE
);

CREATE INDEX idx_event_images_event ON event_images (event_id);

CREATE TABLE gallery_categories (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    slug          VARCHAR(120) NOT NULL,
    description   VARCHAR(500) NULL,
    display_order INT          NOT NULL DEFAULT 0,
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_gallery_categories_name UNIQUE (name),
    CONSTRAINT uq_gallery_categories_slug UNIQUE (slug),
    CONSTRAINT chk_gallery_categories_status CHECK (status IN ('ACTIVE','INACTIVE'))
);

CREATE TABLE galleries (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    slug        VARCHAR(160) NOT NULL,
    title       VARCHAR(255) NOT NULL,
    description TEXT         NULL,
    category_id BIGINT       NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_galleries_slug UNIQUE (slug),
    CONSTRAINT fk_galleries_category FOREIGN KEY (category_id)
        REFERENCES gallery_categories (id) ON DELETE SET NULL,
    CONSTRAINT chk_galleries_status CHECK (status IN ('DRAFT','PUBLISHED','ARCHIVED'))
);

CREATE INDEX idx_galleries_category ON galleries (category_id);

CREATE TABLE gallery_items (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    gallery_id    BIGINT       NOT NULL,
    image_url     VARCHAR(500) NOT NULL,
    caption       VARCHAR(500) NULL,
    alt_text      VARCHAR(255) NULL,
    display_order INT          NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT fk_gallery_items_gallery FOREIGN KEY (gallery_id)
        REFERENCES galleries (id) ON DELETE CASCADE
);

CREATE INDEX idx_gallery_items_gallery ON gallery_items (gallery_id);

CREATE TABLE testimonials (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    person_name      VARCHAR(255) NOT NULL,
    designation      VARCHAR(255) NULL,
    organization     VARCHAR(255) NULL,
    quote            TEXT         NOT NULL,
    photo_url        VARCHAR(500) NULL,
    display_order    INT          NOT NULL DEFAULT 0,
    consent_received BOOLEAN      NOT NULL DEFAULT FALSE,
    status           VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_testimonials_status CHECK (status IN ('DRAFT','PUBLISHED','ARCHIVED'))
);

