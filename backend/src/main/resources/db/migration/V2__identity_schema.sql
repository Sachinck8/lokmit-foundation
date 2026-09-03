-- =====================================================================
-- V2: Identity schema — users, roles, permissions, joins, refresh tokens.
-- Phase 3 MVP database (see docs/DATABASE.md).
-- Enum-like columns use VARCHAR + CHECK constraints (see DATABASE.md, D1).
-- =====================================================================

CREATE TABLE users (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(255) NULL,
    full_name       VARCHAR(255) NOT NULL,
    user_type       VARCHAR(20)  NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    email_verified  BOOLEAN      NOT NULL DEFAULT FALSE,
    last_login_at   TIMESTAMPTZ  NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT chk_users_user_type CHECK (user_type IN ('CANDIDATE','EMPLOYER','CLIENT','STAFF')),
    CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE','LOCKED','SUSPENDED','DELETED'))
);

CREATE TABLE roles (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code        VARCHAR(50)  NOT NULL,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500) NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_roles_code UNIQUE (code)
);

CREATE TABLE permissions (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code        VARCHAR(100) NOT NULL,
    description VARCHAR(500) NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_permissions_code UNIQUE (code)
);

CREATE TABLE role_permissions (
    role_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id)
        REFERENCES roles (id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id)
        REFERENCES permissions (id) ON DELETE CASCADE
);

CREATE INDEX idx_role_permissions_permission ON role_permissions (permission_id);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id)
        REFERENCES roles (id) ON DELETE CASCADE
);

CREATE INDEX idx_user_roles_role ON user_roles (role_id);

CREATE TABLE refresh_tokens (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ  NOT NULL,
    revoked_at TIMESTAMPTZ  NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_refresh_tokens_token_hash UNIQUE (token_hash)
);

-- ---------------------------------------------------------------------
-- Seed data: roles, permissions, grants, bootstrap admin.
-- The bootstrap admin ships with password_hash NULL (cannot log in until
-- the authentication phase sets a credential from environment config).
-- ---------------------------------------------------------------------
INSERT INTO roles (code, name, description) VALUES
    ('SUPER_ADMIN', 'Super Administrator', 'Full platform control including user and role management.'),
    ('ADMIN',       'Administrator',       'Day-to-day platform administration.'),
    ('EDITOR',      'Editor',              'Creates and edits published content.'),
    ('MODERATOR',   'Moderator',           'Reviews and moderates jobs and applications.'),
    ('CANDIDATE',   'Candidate',           'Job-seeking platform user.'),
    ('EMPLOYER',    'Employer',            'Employer posting jobs on the platform.'),
    ('CLIENT',      'Client',              'Consultancy client with portal access.');

INSERT INTO permissions (code, description) VALUES
    ('content:manage',   'Create and edit website content.'),
    ('content:publish',  'Publish and unpublish website content.'),
    ('downloads:manage', 'Manage downloadable resources.'),
    ('messages:manage',  'View and manage contact messages.'),
    ('users:manage',     'Manage users, roles and permissions.'),
    ('jobs:manage',      'Create, edit and publish job postings.'),
    ('jobs:moderate',    'Review and moderate jobs and applications.'),
    ('settings:manage',  'Manage site settings and configuration.');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = ANY (ARRAY[
    'content:manage','content:publish','downloads:manage','messages:manage',
    'users:manage','jobs:manage','jobs:moderate','settings:manage'])
WHERE r.code = 'SUPER_ADMIN';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = ANY (ARRAY[
    'content:manage','content:publish','downloads:manage','messages:manage',
    'jobs:manage','settings:manage'])
WHERE r.code = 'ADMIN';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = ANY (ARRAY['content:manage','downloads:manage'])
WHERE r.code = 'EDITOR';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = ANY (ARRAY['jobs:moderate','messages:manage'])
WHERE r.code = 'MODERATOR';

-- Bootstrap SUPER_ADMIN account (placeholder email; no credential yet).
INSERT INTO users (email, password_hash, full_name, user_type, status, email_verified)
VALUES ('admin@lokmitfoundation.org', NULL, 'Platform Administrator', 'STAFF', 'ACTIVE', TRUE);

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.code = 'SUPER_ADMIN'
WHERE u.email = 'admin@lokmitfoundation.org';

