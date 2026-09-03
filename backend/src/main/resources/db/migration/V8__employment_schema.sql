-- =====================================================================
-- V8: Employment schema — employers, candidates, resumes, skills,
-- education/experience, job categories, jobs, applications.
-- Phase 0 decision D3: Candidate+CandidateProfile merged into `candidates`
-- and Employer+EmployerProfile merged into `employers` (1:1 tables added
-- no value at MVP scale).
-- =====================================================================

CREATE TABLE employers (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id             BIGINT       NOT NULL,
    company_name        VARCHAR(255) NOT NULL,
    about               TEXT         NULL,
    website_url         VARCHAR(500) NULL,
    logo_url            VARCHAR(500) NULL,
    contact_person_name VARCHAR(255) NULL,
    contact_phone       VARCHAR(50)  NULL,
    address             VARCHAR(500) NULL,
    verification_status VARCHAR(20)  NOT NULL DEFAULT 'UNVERIFIED',
    status              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_employers_user UNIQUE (user_id),
    CONSTRAINT fk_employers_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_employers_verification CHECK (verification_status IN ('UNVERIFIED','PENDING','VERIFIED','REJECTED')),
    CONSTRAINT chk_employers_status CHECK (status IN ('ACTIVE','SUSPENDED'))
);

CREATE TABLE candidates (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id              BIGINT       NOT NULL,
    date_of_birth        DATE         NULL,
    gender               VARCHAR(30)  NULL,
    phone                VARCHAR(50)  NULL,
    current_location     VARCHAR(255) NULL,
    summary              TEXT         NULL,
    expected_salary_min  NUMERIC(12,2) NULL,
    expected_salary_max  NUMERIC(12,2) NULL,
    availability_status  VARCHAR(30)  NOT NULL DEFAULT 'ACTIVELY_LOOKING',
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_candidates_user UNIQUE (user_id),
    CONSTRAINT fk_candidates_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_candidates_availability CHECK (availability_status IN ('ACTIVELY_LOOKING','OPEN_TO_OFFERS','NOT_LOOKING')),
    CONSTRAINT chk_candidates_salary_range CHECK (expected_salary_max IS NULL OR expected_salary_min IS NULL OR expected_salary_max >= expected_salary_min)
);

CREATE TABLE resumes (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    candidate_id    BIGINT       NOT NULL,
    file_url        VARCHAR(500) NOT NULL,
    file_name       VARCHAR(255) NOT NULL,
    file_type       VARCHAR(100) NULL,
    file_size_bytes BIGINT       NULL,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT fk_resumes_candidate FOREIGN KEY (candidate_id)
        REFERENCES candidates (id) ON DELETE CASCADE
);

CREATE INDEX idx_resumes_candidate ON resumes (candidate_id);
CREATE UNIQUE INDEX uq_resumes_one_active_per_candidate ON resumes (candidate_id) WHERE is_active;

CREATE TABLE skills (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    status     VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_skills_name UNIQUE (name),
    CONSTRAINT chk_skills_status CHECK (status IN ('ACTIVE','INACTIVE'))
);

CREATE TABLE candidate_skills (
    candidate_id      BIGINT NOT NULL,
    skill_id          BIGINT NOT NULL,
    proficiency_level VARCHAR(20) NULL,
    PRIMARY KEY (candidate_id, skill_id),
    CONSTRAINT fk_candidate_skills_candidate FOREIGN KEY (candidate_id)
        REFERENCES candidates (id) ON DELETE CASCADE,
    CONSTRAINT fk_candidate_skills_skill FOREIGN KEY (skill_id)
        REFERENCES skills (id) ON DELETE CASCADE,
    CONSTRAINT chk_candidate_skills_level CHECK (proficiency_level IS NULL OR proficiency_level IN ('BEGINNER','INTERMEDIATE','ADVANCED','EXPERT'))
);

CREATE INDEX idx_candidate_skills_skill ON candidate_skills (skill_id);

CREATE TABLE candidate_educations (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    candidate_id  BIGINT       NOT NULL,
    institution   VARCHAR(255) NOT NULL,
    degree        VARCHAR(255) NULL,
    field_of_study VARCHAR(255) NULL,
    start_year    INT          NULL,
    end_year      INT          NULL,
    grade         VARCHAR(100) NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT fk_candidate_educations_candidate FOREIGN KEY (candidate_id)
        REFERENCES candidates (id) ON DELETE CASCADE
);

CREATE INDEX idx_candidate_educations_candidate ON candidate_educations (candidate_id);

CREATE TABLE candidate_experiences (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    candidate_id BIGINT       NOT NULL,
    company_name VARCHAR(255) NOT NULL,
    job_title    VARCHAR(255) NOT NULL,
    description  TEXT         NULL,
    start_date   DATE         NULL,
    end_date     DATE         NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT fk_candidate_experiences_candidate FOREIGN KEY (candidate_id)
        REFERENCES candidates (id) ON DELETE CASCADE
);

CREATE INDEX idx_candidate_experiences_candidate ON candidate_experiences (candidate_id);

CREATE TABLE job_categories (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    slug          VARCHAR(120) NOT NULL,
    description   VARCHAR(500) NULL,
    display_order INT          NOT NULL DEFAULT 0,
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_job_categories_name UNIQUE (name),
    CONSTRAINT uq_job_categories_slug UNIQUE (slug),
    CONSTRAINT chk_job_categories_status CHECK (status IN ('ACTIVE','INACTIVE'))
);

CREATE TABLE jobs (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    employer_id          BIGINT       NOT NULL,
    category_id          BIGINT       NULL,
    slug                 VARCHAR(180) NOT NULL,
    title                VARCHAR(255) NOT NULL,
    description          TEXT         NOT NULL,
    requirements         TEXT         NULL,
    employment_type      VARCHAR(20)  NOT NULL DEFAULT 'FULL_TIME',
    work_mode            VARCHAR(20)  NOT NULL DEFAULT 'ONSITE',
    work_location        VARCHAR(255) NULL,
    salary_min           NUMERIC(12,2) NULL,
    salary_max           NUMERIC(12,2) NULL,
    salary_currency      VARCHAR(8)   NOT NULL DEFAULT 'INR',
    openings             INT          NULL,
    status               VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    application_deadline DATE         NULL,
    published_at         TIMESTAMPTZ  NULL,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_jobs_slug UNIQUE (slug),
    CONSTRAINT fk_jobs_employer FOREIGN KEY (employer_id)
        REFERENCES employers (id),
    CONSTRAINT fk_jobs_category FOREIGN KEY (category_id)
        REFERENCES job_categories (id) ON DELETE SET NULL,
    CONSTRAINT chk_jobs_employment_type CHECK (employment_type IN ('FULL_TIME','PART_TIME','CONTRACT','INTERNSHIP','TEMPORARY')),
    CONSTRAINT chk_jobs_work_mode CHECK (work_mode IN ('ONSITE','REMOTE','HYBRID')),
    CONSTRAINT chk_jobs_status CHECK (status IN ('DRAFT','PUBLISHED','CLOSED','ARCHIVED')),
    CONSTRAINT chk_jobs_salary_range CHECK (salary_max IS NULL OR salary_min IS NULL OR salary_max >= salary_min)
);

CREATE INDEX idx_jobs_employer ON jobs (employer_id);
CREATE INDEX idx_jobs_category ON jobs (category_id);
CREATE INDEX idx_jobs_status_published ON jobs (status, published_at DESC);

CREATE TABLE job_skills (
    job_id   BIGINT NOT NULL,
    skill_id BIGINT NOT NULL,
    PRIMARY KEY (job_id, skill_id),
    CONSTRAINT fk_job_skills_job FOREIGN KEY (job_id)
        REFERENCES jobs (id) ON DELETE CASCADE,
    CONSTRAINT fk_job_skills_skill FOREIGN KEY (skill_id)
        REFERENCES skills (id) ON DELETE CASCADE
);

CREATE INDEX idx_job_skills_skill ON job_skills (skill_id);

CREATE TABLE job_applications (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    job_id        BIGINT       NOT NULL,
    candidate_id  BIGINT       NOT NULL,
    resume_id     BIGINT       NULL,
    cover_note    TEXT         NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'SUBMITTED',
    employer_note TEXT         NULL,
    applied_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    decided_at    TIMESTAMPTZ  NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_job_applications_job_candidate UNIQUE (job_id, candidate_id),
    CONSTRAINT fk_job_applications_job FOREIGN KEY (job_id)
        REFERENCES jobs (id),
    CONSTRAINT fk_job_applications_candidate FOREIGN KEY (candidate_id)
        REFERENCES candidates (id),
    CONSTRAINT fk_job_applications_resume FOREIGN KEY (resume_id)
        REFERENCES resumes (id) ON DELETE SET NULL,
    CONSTRAINT chk_job_applications_status CHECK (status IN ('SUBMITTED','UNDER_REVIEW','SHORTLISTED','HIRED','REJECTED','WITHDRAWN'))
);

CREATE INDEX idx_job_applications_candidate ON job_applications (candidate_id);
CREATE INDEX idx_job_applications_job_status ON job_applications (job_id, status);

