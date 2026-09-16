# LOKMIT FOUNDATION — Digital Platform

Website and digital platform for **LOKMIT FOUNDATION** — built as a
Corporate Consultancy + Skill Development + Project Advisory + Employment +
Job Portal + Knowledge Platform.

> **Status: Phase 4 — authentication & authorization complete.**
> Foundation security layer implemented with JWT, BCrypt, RBAC, and refresh tokens.
> See Phase 0 architecture plan for the full roadmap.

---

## Architecture Overview

- **Backend:** Spring Boot 3.x (Java 21, Maven) — monolithic, package-by-feature,
  versioned REST API under `/api/v1`.
- **Frontend:** React (JavaScript, Vite) SPA — public site, job portal,
  candidate/employer portals and admin panel in later phases.
- **Database:** PostgreSQL 16+ (local dev uses PostgreSQL 18), schema managed by
  Flyway migrations (`backend/src/main/resources/db/migration`).
- **Files:** designed around a configurable storage layer (local in dev).
- **Security:** JWT + BCrypt + RBAC planned for the authentication phase. No
  secrets are hard-coded; all credentials come from environment variables.

## Tech Stack

| Layer     | Technology |
|-----------|------------|
| Language  | Java 21 (backend), JavaScript (frontend) |
| Backend   | Spring Boot 3.5.x, Spring Web, Spring Data JPA, Bean Validation, Actuator |
| API docs  | springdoc-openapi (Swagger UI / OpenAPI 3) |
| Database  | PostgreSQL, Flyway (database migrations) |
| Build     | Maven (backend), Vite (frontend) |
| Frontend  | React, React Router, Axios |
| Utilities | Lombok |

## Repository Layout

```text
lokmit-foundation/
├── backend/           Spring Boot application
│   ├── scripts/       dev helper scripts (dev-run.ps1)
│   └── src/main/resources/db/migration/   Flyway migrations
├── frontend/          React + Vite application
├── docs/              documentation (conventions, phase plans)
├── .env.example       environment variable reference (placeholders only)
└── README.md
```

## Prerequisites

- JDK 21 (LTS)
- Maven 3.9+
- Node.js 20+ (local dev machine uses Node 24)
- PostgreSQL running locally (e.g. the PostgreSQL 18 service on `localhost:5432`)

## PostgreSQL Setup

The backend uses a **dedicated database and role** — never the `postgres`
superuser.

Run the following **once**, in a PostgreSQL superuser session (psql will prompt
for the superuser password, which you should never share):

```sql
CREATE ROLE lokmit_app LOGIN PASSWORD '<generated-strong-password>'
  NOSUPERUSER NOCREATEDB NOCREATEROLE;

CREATE DATABASE lokmit_foundation OWNER lokmit_app ENCODING 'UTF8';
```

Verify ownership of the `public` schema (fresh databases already grant this to
the owner):

```sql
\c lokmit_foundation
GRANT ALL ON SCHEMA public TO lokmit_app;
```

> The generated local dev password for `lokmit_app` is recorded in the
> **gitignored** file `backend/.env`.

## Environment Configuration

Secrets and environment-specific values are never hard-coded.

| Variable | Used by | Purpose |
|----------|---------|---------|
| `DB_URL` | backend | JDBC URL (`jdbc:postgresql://localhost:5432/lokmit_foundation`) |
| `DB_USERNAME` | backend | Database role (`lokmit_app`) |
| `DB_PASSWORD` | backend | Password for the database role |
| `SERVER_PORT` | backend | HTTP port (default `8080`) |
| `JWT_SECRET` | backend | Secret key for signing JWTs (min 32 bytes) |
| `JWT_ACCESS_TOKEN_EXPIRATION` | backend | Access token expiration in ms (default `900000` = 15 min) |
| `JWT_REFRESH_TOKEN_EXPIRATION` | backend | Refresh token expiration in ms (default `604800000` = 7 days) |
| `BOOTSTRAP_ADMIN_PASSWORD` | backend | Initial admin password (only used if password_hash is NULL) |
| `APP_CORS_ALLOWED_ORIGINS` | backend | Comma-separated exact frontend origins for CORS (dev default: `http://localhost:5173,http://localhost:4173`; **required in production**) |
| `APP_HSTS_ENABLED` | backend | Emit `Strict-Transport-Security` (default `false`; `true` in production) |
| `APP_HSTS_MAX_AGE_SECONDS` | backend | HSTS max-age in seconds (default `31536000`) |
| `APP_HSTS_INCLUDE_SUBDOMAINS` | backend | HSTS `includeSubDomains` (default `true`) |
| `RATE_LIMIT_LOGIN_ENABLED` | backend | Enable login rate limiting (default `true`) |
| `RATE_LIMIT_LOGIN_CAPACITY` | backend | Login requests per window per client IP (default `10`) |
| `RATE_LIMIT_LOGIN_WINDOW_SECONDS` | backend | Login rate-limit window in seconds (default `60`) |
| `RATE_LIMIT_CONTACT_ENABLED` | backend | Enable contact-form rate limiting (default `true`) |
| `RATE_LIMIT_CONTACT_CAPACITY` | backend | Contact submissions per window per client IP (default `5`) |
| `RATE_LIMIT_CONTACT_WINDOW_SECONDS` | backend | Contact rate-limit window in seconds (default `60`) |
| `RATE_LIMIT_MAX_TRACKED_KEYS` | backend | Memory bound: max client IPs tracked per limiter (default `10000`) |
| `LOGIN_MAX_FAILED_ATTEMPTS` | backend | Failed logins before temporary lockout (default `5`) |
| `LOGIN_LOCKOUT_DURATION_MINUTES` | backend | Temporary lockout duration in minutes (default `15`) |
| `REFRESH_TOKEN_CLEANUP_INTERVAL_MINUTES` | backend | Refresh-token cleanup interval (default `60`, `0` disables) |
| `OUTBOX_RELAY_ENABLED` | backend | Enable the outbox relay that materializes in-app notifications (default `true`) |
| `OUTBOX_RELAY_POLLING_INTERVAL_SECONDS` | backend | Seconds between relay passes (default `30`, `0` disables) |
| `OUTBOX_RELAY_BATCH_SIZE` | backend | Max outbox events claimed per relay pass (default `50`) |
| `OUTBOX_RELAY_RETRY_BACKOFF_SECONDS` | backend | Base backoff after a failed processing attempt (default `60`, scaled by attempt count) |
| `OUTBOX_RELAY_MAX_ATTEMPTS` | backend | Attempts before an outbox event is marked FAILED and never retried (default `5`) |
| `VITE_API_BASE_URL` | frontend | Backend API base path (default `/api/v1`) |

- **`.env.example` files** (root, `backend/`, `frontend/`) document the variables
  with placeholder values only — they are safe to commit.
- **`.env` files** hold real local values and are **gitignored**.
- `backend/.env` exists locally with the generated dev credentials and is loaded
  automatically by `backend/scripts/dev-run.ps1`.

## Backend Setup & Run

```powershell
cd backend

# 1. Compile + run tests
mvn clean verify

# 2. Run the application (loads backend/.env into the process environment)
.\scripts\dev-run.ps1

# or manually:
#   $env:DB_PASSWORD = '<your password>'
#   mvn spring-boot:run
```

The backend starts on `http://localhost:8080`.

Health check (no authentication):

```text
GET http://localhost:8080/api/v1/health
```

## Production Configuration

The backend ships a dedicated **`prod`** Spring profile
(`backend/src/main/resources/application-prod.yml`). It is strict by design:
sensitive values have **no defaults**, so startup fails with a clear message
instead of running with insecure fallbacks.

### Activating the production profile

```bash
# Either via flag:
java -jar target/lokmit-foundation-backend-0.1.0-SNAPSHOT.jar --spring.profiles.active=prod

# Or via environment:
SPRING_PROFILES_ACTIVE=prod java -jar target/lokmit-foundation-backend-0.1.0-SNAPSHOT.jar
```

### Required environment variables (production)

| Variable | Purpose | Behavior when missing |
|----------|---------|----------------------|
| `DB_URL` | Production JDBC URL | Startup fails (`DB_URL` unresolved) |
| `DB_USERNAME` | Production database role | Startup fails |
| `DB_PASSWORD` | Production database password | Startup fails |
| `JWT_SECRET` | JWT signing key (min 32 bytes) | Startup fails fast (I-1) |
| `APP_CORS_ALLOWED_ORIGINS` | Exact frontend origins, comma-separated | Startup fails |

Set these only in the deployment secret store — never in source control,
YAML, or documentation. Placeholders live in `backend/.env.example`.

### Rate limiting (I-6)

The two public, high-risk endpoints are rate-limited **per client IP** with
instance-local, in-memory fixed windows:

| Endpoint | Default limit |
|----------|---------------|
| `POST /api/v1/auth/login` | 10 requests / 60 s / IP |
| `POST /api/v1/contact-messages` | 5 requests / 60 s / IP |

- Exceeding the limit returns **429 Too Many Requests** in the standard API
  error envelope (`RATE_LIMITED`) with a `Retry-After` header (seconds until
  the client's window resets). No bucket state is exposed.
- Only these two method+path pairs are limited — preflights (OPTIONS), the
  health check, Swagger/OpenAPI, refresh/logout, and all authenticated admin
  endpoints are untouched.
- `X-Forwarded-For` is **never** trusted (no trusted-proxy configuration
  exists); the limiter keys on the servlet remote address. Behind a reverse
  proxy, the proxy should be the only thing that can reach the backend so the
  socket address is meaningful, or set `server.forward-headers-strategy` with
  an explicit trust model.
- This complements (does not replace) the I-2 per-account login lockout:
  the limiter protects the endpoint from flooding with rotating identities;
  I-2 protects individual accounts from password guessing.
- **Instance-local limitation:** state lives in each JVM. With N instances
  the effective per-client limit is N × capacity. A distributed store can
  replace `FixedWindowRateLimiter` later without changing callers.

### Authentication & RBAC model (A1)

- **Model:** `User → roles → permissions`. Roles and permission grants live
  in the database (seeded in `V2__identity_schema.sql`); permission codes are
  mirrored as Java constants in `security/Permissions.java`.
- **Enforcement:** on every request `CustomUserDetailsService` reloads the
  user from the database and exposes `ROLE_<code>` + each granted permission
  as Spring Security authorities. JWT `roles` claims are never trusted for
  authorization — the database is authoritative.
- **Declarative checks:** management endpoints use
  `@PreAuthorize("hasAuthority('" + Permissions.X + "')")` — e.g. the contact
  enquiry management API requires `messages:manage` and the Admin Dashboard
  read APIs require `dashboard:view` (seeded by V11 to SUPER_ADMIN and
  ADMIN).
- **Access levels:** SUPER_ADMIN holds every permission; ADMIN covers
  day-to-day management (`content:*`, `downloads:manage`, `messages:manage`,
  `jobs:manage`, `settings:manage`) but **not** `users:manage`; MODERATOR is
  limited to `jobs:moderate` + `messages:manage`; CANDIDATE/EMPLOYER/CLIENT
  have no administrative permissions. Anonymous callers reach only the
  explicit public endpoints.
- **Account status is enforced per request:** only `ACTIVE` accounts are
  authenticated. `LOCKED`/`SUSPENDED`/`DELETED` accounts are rejected even
  with a still-valid access token (fail-closed), so deactivation is
  immediate.
- **Errors:** 401 `UNAUTHORIZED` for missing/invalid credentials, 403
  `FORBIDDEN` for insufficient permissions — both in the standard error
  envelope; responses never echo tokens or account internals.

### Production guarantees

- **Database:** Flyway owns the schema; Hibernate runs with
  `ddl-auto: validate` only (`create`/`update`/`create-drop` never appear in
  production). Missing database variables abort startup; values are never
  logged.
- **JWT:** the I-1 fail-fast applies unchanged — missing/blank/short
  `JWT_SECRET` aborts boot, no fallback secret exists in production.
- **CORS:** explicit allow-list only (`APP_CORS_ALLOWED_ORIGINS`),
  `allowCredentials(false)` (Bearer-token API, no cookies), methods limited
  to `GET/POST/PATCH/OPTIONS`, headers limited to `Authorization` and
  `Content-Type`. A wildcard origin is rejected at startup.
- **Security headers:** every response carries `X-Content-Type-Options:
  nosniff`, `X-Frame-Options: DENY`, a strict CSP (`default-src 'none';
  frame-ancestors 'none'`, Swagger-UI compatible), `Referrer-Policy:
  no-referrer`, and a restrictive `Permissions-Policy`; HSTS is enabled in
  production (`APP_HSTS_ENABLED`, default `true` there) and off in
  development.
- **Actuator:** exposure stays `health,info`; health details hidden
  (`show-details: never`).
- **Rate limiting:** enabled in every environment with the defaults above;
  capacities/windows overridable via the `RATE_LIMIT_*` variables.

### CORS in development

The default profile pre-allows the Vite dev server origins
(`http://localhost:5173`, `http://localhost:4173`) and remains overridable via
`APP_CORS_ALLOWED_ORIGINS`; the frontend also uses the Vite proxy for same-origin
calls, so local development needs no extra setup.

Expected response:

```json
{
  "status": "UP",
  "service": "lokmit-foundation-backend",
  "version": "0.1.0-SNAPSHOT",
  "timestamp": "2026-09-02T..."
}
```

Actuator health is also available at `http://localhost:8080/actuator/health`.

## API Documentation (Swagger / OpenAPI)

The backend generates OpenAPI 3 documentation automatically:

- JSON spec: `http://localhost:8080/api/v1/api-docs`
- Swagger UI: `http://localhost:8080/api/v1/swagger-ui.html`

## API Response & Error Handling

- Business endpoints wrap responses in a standard envelope
  (`ApiResponse<T>`): `{ success, message, data, errors, timestamp }`.
- Paginated lists use `PageResponse<T>`: `{ items, page, size, totalItems, totalPages }`
  with `page` (0-based) and `size` (default 20, max 100) query parameters.
- Errors are mapped centrally by `GlobalExceptionHandler` to stable status
  codes and machine-readable error codes (see `docs/CONVENTIONS.md`).
- `GET /api/v1/health` intentionally keeps its simple operational payload.

### Admin Dashboard API (A2)

Read-only endpoints under `/api/v1/admin/dashboard`, all guarded by the
`dashboard:view` permission (anonymous → 401, unauthorized → 403):

| Endpoint | Purpose |
|----------|---------|
| `GET /api/v1/admin/dashboard/summary` | Platform counts: users (total/active/inactive), candidate & employer profiles, jobs by lifecycle status (DRAFT/PUBLISHED/CLOSED/ARCHIVED), applications, enquiries by status (NEW/READ/REPLIED/ARCHIVED) |
| `GET /api/v1/admin/dashboard/recent-enquiries?limit=5` | Newest contact enquiries (id, name, email, subject, status, createdAt) |
| `GET /api/v1/admin/dashboard/recent-users?limit=5` | Newest accounts (no password/token/role material) |
| `GET /api/v1/admin/dashboard/recent-applications?limit=5` | Newest applications with candidate name and job title (single join, no N+1) |

- `limit` defaults to 5 and is clamped to a maximum of 10 server-side, so a
  large value can never widen the query.
- All counts aggregate in PostgreSQL (COUNT queries); no entity rows are
  loaded into memory and no dashboard tables were added.

### Admin User Management (A3)

Endpoints under `/api/v1/admin/users`, all guarded by the `users:manage`
permission (V2 seed: SUPER_ADMIN only; ADMIN/MODERATOR get 403 by design):

| Endpoint | Purpose |
|---|---|
| `GET /api/v1/admin/users?page=0&size=20&search=&status=&role=` | Paginated list, newest first. Search matches email/full name; `status` and `role` validate against the seeded domains (unknown values → 400) |
| `GET /api/v1/admin/users/{id}` | Safe detail view |
| `PATCH /api/v1/admin/users/{id}/status` | Status-only change (ACTIVE/LOCKED/SUSPENDED/DELETED) |
| `PUT /api/v1/admin/users/{id}/roles` | Full role replacement |

Server-enforced protections (never left to the frontend):

- **Self-protection** — an administrator cannot move their own account to a
  non-ACTIVE status or change their own roles (400).
- **Last-SUPER_ADMIN protection** — disabling the final active SUPER_ADMIN,
  or removing the SUPER_ADMIN role from it, is rejected (400); the system
  always keeps at least one active SUPER_ADMIN.
- **No privilege escalation** — granting SUPER_ADMIN requires the caller to
  hold `ROLE_SUPER_ADMIN` (403 otherwise), from the database-backed
  authorities, never a JWT claim.
- Moving an account out of ACTIVE revokes all its refresh tokens (I-8
  support), so sessions end immediately alongside the A1 fail-closed filter.
- Responses are DTOs; password hashes, I-2 brute-force bookkeeping and
  refresh-token material are structurally absent from every payload.

### Admin CMS Management (A4)

Endpoints under `/api/v1/admin/cms`, mapped to the existing V3 CMS tables —
no new tables, no schema changes, no new permissions (existing V2 grants
cover the tiers):

| Namespace | Permission | Endpoints |
|---|---|---|
| `/admin/cms/site-settings` | `settings:manage` (SUPER_ADMIN, ADMIN) | list (key search + pagination), get by id / unique key, PATCH value/description (key immutable) |
| `/admin/cms/website-content` | `content:manage` (SUPER_ADMIN, ADMIN, EDITOR) | list (pageKey/sectionKey/status filters), get by id / page+section key, create (always DRAFT), PATCH title/body |
| `/admin/cms/website-content/{id}/publish`, `/archive`, DELETE | `content:publish` (SUPER_ADMIN, ADMIN) | lifecycle transitions and deletion |
| `/admin/cms/seo-metadata` | `content:manage` | list (entityType/entityId/search filters), get by id / entity pair, create, PATCH |

Server-enforced rules:

- Content lifecycle mirrors the V3 check constraint: DRAFT → PUBLISHED →
  ARCHIVED (terminal). New sections always start as DRAFT; archived sections
  cannot be re-published (409). No invented publishing workflow.
- Duplicate keys are rejected with 409: unique setting_key,
  unique (page_key, section_key), unique (entity_type, entity_id) — each
  pre-checked in the service and backstopped by the database constraints
  against concurrent creation races.
- `contentJson` is validated for JSON well-formedness before it reaches the
  JSONB column (400 on malformed input).
- Identity keys never move through the API: setting keys and the SEO
  (entityType, entityId) pair are immutable.
- DTOs only; timestamps are OffsetDateTime, matching the timestamptz
  columns.

### Admin Services & Expertise Management (A5)

Endpoints under `/api/v1/admin`, mapped to the existing V4 services-catalog
tables — no new tables, one new permission:

| Namespace | Permission | Endpoints |
|---|---|---|
| `/admin/service-categories` | `services:manage` (SUPER_ADMIN, ADMIN) | list (status filter + pagination, display-order sort), get by id, create (always ACTIVE), PATCH name/description/displayOrder/status (slug immutable), DELETE (referencing services are detached, never cascade-deleted — existing FK ON DELETE SET NULL) |
| `/admin/services` | `services:manage` | list (categoryId/status filters + title/summary search + pagination), get by id, create (always DRAFT; unknown categoryId → 404), PATCH (categoryId null detaches; status changes via dedicated endpoints), `/publish`, `/archive`, DELETE |
| `/admin/expertise-areas` | `services:manage` | list (status filter + name search + pagination), get by id, create (always DRAFT), PATCH name/description/displayOrder (slug immutable), `/publish`, `/archive`, DELETE |

Server-enforced rules:

- `services:manage` was added in V12 because no V2 permission covers the
  services catalog; reusing `content:manage` would have granted EDITOR write
  access to the commercial services catalog, which the seed never intended.
- Service/expertise lifecycle mirrors the V4 check constraint: DRAFT →
  PUBLISHED → ARCHIVED (terminal). Archived records cannot change status
  (409). Category status is the simple ACTIVE/INACTIVE domain.
- Unique business keys are pre-checked for a clean 409 (category name and
  slug, service slug, expertise slug) and backstopped by the database
  constraints against concurrent races.
- Category references are validated before any write: an unknown categoryId
  returns 404 — orphaning inserts are impossible.
- Slugs are immutable through the API (URL identity); partial updates leave
  omitted fields unchanged, with explicit-null clearing where the column is
  nullable.

### Admin Projects Management (A6)

Endpoints under `/api/v1/admin`, mapped to the existing V5 projects tables —
no new tables, one new permission:

| Namespace | Permission | Endpoints |
|---|---|---|
| `/admin/project-categories` | `projects:manage` (SUPER_ADMIN, ADMIN) | list (status filter + pagination, display-order sort), get by id, create (always ACTIVE), PATCH name/description/displayOrder/status (slug immutable), DELETE (referencing projects are detached, never cascade-deleted — existing FK ON DELETE SET NULL) |
| `/admin/projects` | `projects:manage` | list (categoryId/status/projectStatus filters + title/summary search + pagination, newest first), get by id, create (always DRAFT; unknown categoryId → 404; end-before-start → 400), PATCH (categoryId null detaches; resulting date pair validated; status changes via dedicated endpoints), `/publish` (stamps published_at), `/archive`, DELETE (owned image metadata removed by the existing FK cascade) |
| `/admin/projects/{id}/images` | `projects:manage` | list (paginated gallery), POST image metadata (URL reference only — no file upload in A6; unknown project → 404; duplicate URL within the gallery → 409) |
| `/admin/projects/{projectId}/images/{imageId}` | `projects:manage` | get/PATCH/DELETE image metadata (mismatched project/image pair → 404; owning project immutable) |

Server-enforced rules:

- `projects:manage` was added in V13 because no V2 permission covers the
  projects domain; reusing `content:manage` would have granted EDITOR write
  access to the project portfolio, which the seed never intended.
- Projects carry two independent status dimensions, exactly per the V5
  constraints: the editorial lifecycle `status` (DRAFT → PUBLISHED →
  ARCHIVED, terminal; publish stamps `published_at`) and the nullable
  delivery state `projectStatus` (PLANNING/ONGOING/COMPLETED).
- `chk_projects_dates` is enforced in the service for every create and
  partial update, including patches that touch only one side of the pair —
  the API answers 400 before the database constraint would reject the row.
- `objectives` is a JSONB column; caller-supplied JSON is validated for
  well-formedness before any write (400 on malformed input).
- Unique business keys are pre-checked for a clean 409 (category name and
  slug, project slug) and backstopped by the database constraints.
- Image endpoints are METADATA only: `imageUrl` is a caller-supplied URL
  reference; binary upload, storage and processing are a separate future
  phase. A project/image mismatch is a 404 — an image is always managed
  through its owning project.

### Admin Employment Foundation (A7.1)

Endpoints under `/api/v1/admin`, mapped to the existing V8 employment
tables — no new tables, two new permissions:

| Namespace | Permission | Endpoints |
|---|---|---|
| `/admin/employers` | `employment:manage` (SUPER_ADMIN, ADMIN) | list (search + verificationStatus/status filters + pagination, newest first), get by id, create (links an EXISTING user; one profile per user, duplicates → 409; unknown user → 404), PATCH (partial; linked user immutable). **NO DELETE endpoint** |
| `/admin/candidates` | `candidates:manage` (SUPER_ADMIN, ADMIN) | list (search + availability/gender filters + pagination, newest first), get by id, create (links an EXISTING user; one profile per user), PATCH (partial; salary pair min ≤ max validated). **NO DELETE endpoint** |
| `/admin/candidates/{candidateId}/skills` | `candidates:manage` | list a candidate's skill assignments, POST assign (skillId + optional proficiency BEGINNER/INTERMEDIATE/ADVANCED/EXPERT; unknown candidate/skill → 404; duplicate assignment → 409) |
| `/admin/candidates/{candidateId}/skills/{skillId}` | `candidates:manage` | DELETE assignment (unknown pair → 404) |
| `/admin/skills` | `employment:manage` | list (name search + status filter + pagination, name-ordered), get by id, create (duplicate name → 409), PATCH (partial), DELETE (see cascade note below) |
| `/admin/job-categories` | `employment:manage` | list (name/slug search + status filter + pagination, displayOrder then name), get by id, create (duplicate name or slug → 409), PATCH (slug immutable), DELETE (jobs are detached, never deleted — existing FK ON DELETE SET NULL) |

Server-enforced rules:

- **No employer/candidate hard delete.** The existing V8 foreign keys
  `fk_employers_user` and `fk_candidates_user` are `ON DELETE CASCADE` into
  `users`; deleting a profile row would also delete the owning user identity
  (and transitively `user_roles`, `refresh_tokens`, and other user-owned
  data). The API therefore exposes create/read/update plus lifecycle fields
  only — employer `status` (ACTIVE/SUSPENDED) and
  `verificationStatus` (UNVERIFIED/PENDING/VERIFIED/REJECTED), candidate
  `availability_status` (ACTIVELY_LOOKING/OPEN_TO_OFFERS/NOT_LOOKING).
  There is no DELETED state in the schema and none was invented. If profile
  removal is ever required it must be a separate, explicitly designed
  identity/data-retention workflow.
- Profiles always link an existing user (`userId` in create); users are
  never created implicitly, and `userId` cannot be changed through PATCH.
- Skill DELETE is intentionally exposed: the V8 FKs
  `fk_candidate_skills_skill` and `fk_job_skills_skill` are
  `ON DELETE CASCADE`, so deleting a skill also removes its candidate
  assignments and job requirements — a documented cleanup semantic.
- Job-category DELETE detaches jobs (`jobs.category_id` is
  `ON DELETE SET NULL`); jobs are never deleted.
- `employment:manage` and `candidates:manage` were added in V14 because the
  V2 seed covers neither employment profiles nor skills, and reusing
  `users:manage` or `jobs:manage` would blur distinct administrative
  domains. Granted only to SUPER_ADMIN and ADMIN; MODERATOR, EDITOR,
  CANDIDATE, EMPLOYER and CLIENT deliberately receive neither.

### Admin Job Management (A7.2)

Endpoints under `/api/v1/admin/jobs`, mapped to the existing V8 `jobs` and
`job_skills` tables — no new tables, no new migration, no new permission
(reuses `employment:manage`, SUPER_ADMIN + ADMIN):

| Namespace | Permission | Endpoints |
|---|---|---|
| `/admin/jobs` | `employment:manage` | list (employerId/categoryId/status/employmentType/workMode filters + title/slug/location search + pagination, newest first), get by id, create (always DRAFT — status/publishedAt are not writable at creation; duplicate slug → 409; unknown employer/category → 404; salaryMin > salaryMax → 400), PATCH (partial; slug and owning employer immutable; categoryId null detaches; status NOT patchable; resulting salary pair validated), DELETE (job_skills cascade away per V8; the database refuses deletion while job_applications reference the job) |
| `/admin/jobs/{id}/publish` | `employment:manage` | DRAFT → PUBLISHED, stamps `published_at`; already-published → 409; any other non-draft state → 400 |
| `/admin/jobs/{id}/close` | `employment:manage` | PUBLISHED → CLOSED; job data fully preserved; other states → 400 |
| `/admin/jobs/{id}/archive` | `employment:manage` | DRAFT/PUBLISHED/CLOSED → ARCHIVED; terminal (repeat → 409); data preserved |
| `/admin/jobs/{jobId}/skills` | `employment:manage` | list a job's skill requirements, POST `{"skillId":N}` add (unknown job/skill → 404; duplicate → 409) |
| `/admin/jobs/{jobId}/skills/{skillId}` | `employment:manage` | DELETE requirement (unknown pair → 404) |

Server-enforced rules:

- The V8 lifecycle model is used exactly: `DRAFT` → `PUBLISHED` → `CLOSED`
  → `ARCHIVED` (chk_jobs_status). Publish stamps `published_at`. Status is
  controllable ONLY through the transition endpoints — PATCH cannot change
  it, so the lifecycle cannot be bypassed. Archive is terminal.
- The owning employer is immutable and must exist (404 otherwise); no
  employer or user is ever created implicitly. `fk_jobs_employer` has no
  ON DELETE action, so the database itself prevents deleting a referenced
  employer.
- Slug is the immutable URL identity (project convention); unique slug is
  pre-checked (409) and backstopped by `uq_jobs_slug` for concurrent races.
- Category is optional: `fk_jobs_category` is `ON DELETE SET NULL`, so
  deleting a category (A7.1) detaches jobs, never deletes them; responses
  safely render a null category.
- DELETE removes the job row only. Its `job_skills` rows cascade away per
  V8. `fk_job_applications_job` has NO ON DELETE action — the database
  refuses to delete a job that already has applications, so deletion is
  effectively blocked once applications exist (A7.3 will manage them).
- Employer/category data in job responses is embedded as safe summaries
  (company name / verification state, category name / slug) — no linked-user
  identity or security material is ever serialized.
- Employer/category data in job responses is embedded as safe summaries
  (company name / verification state, category name / slug) — no linked-user
  identity or security material is ever serialized.
- **A7.4+ (application history, interviews, notifications) are NOT
  implemented** — those capabilities have no API in this phase.

### Admin Application Management (A7.3)

Endpoints under `/api/v1/admin/applications`, mapped to the existing V8
`job_applications` table — no new tables, no new migration, no new
permission (reuses `employment:manage`, SUPER_ADMIN + ADMIN):

| Namespace | Permission | Endpoints |
|---|---|---|
| `/admin/applications` | `employment:manage` | list (jobId/candidateId/employerId/status filters + cover/employer-note search + pagination, newest first; employerId resolved via a single grouped COUNT query on the join path), get by id, PATCH review notes (`employerNote`, optional `resumeId` reference; job/candidate identity immutable; status NOT patchable) |
| `/admin/applications/{id}/start-review` | `employment:manage` | SUBMITTED → UNDER_REVIEW; other states → 400 |
| `/admin/applications/{id}/shortlist` | `employment:manage` | UNDER_REVIEW → SHORTLISTED; other states → 400 |
| `/admin/applications/{id}/decide` | `employment:manage` | terminal HIRED/REJECTED decision (`{"decision":"HIRED"\|"REJECTED","note":optional}`); stamps `decided_at`; already-decided → 409; WITHDRAWN → 400 |
| `/admin/applications/{id}/withdraw` | `employment:manage` | any pre-decision state → WITHDRAWN; already-decided or already-withdrawn → 409 |

Server-enforced rules:

- The V8 status model is used exactly: `chk_job_applications_status`
  (SUBMITTED/UNDER_REVIEW/SHORTLISTED/HIRED/REJECTED/WITHDRAWN). Status and
  `decided_at` are controllable ONLY through the lifecycle endpoints — PATCH
  cannot change them, so the review lifecycle cannot be bypassed.
- There is deliberately NO delete endpoint: applications are the project's
  hiring audit trail; WITHDRAWN/REJECTED lifecycle is the supported
  retirement path. The V8 FKs give applications no cascade into candidates,
  users, employers or jobs.
- Job/candidate identity is immutable (the `uq_job_applications_job_candidate`
  pair IS the application's identity); no reassignment operation exists.
- Related data is embedded as safe summaries — the candidate exposes only
  phone/location/availability, the employer only company name, the job only
  posting identity. No linked-user email, password hash, lockout or token
  material is ever serialized.
- The optional `resumeId` reference maps to the existing
  `fk_job_applications_resume` (ON DELETE SET NULL); resumes have no JPA
  entity/management API yet (a later phase) and A7.3 never creates resumes.

### Admin Application History + Interviews (A7.4)

New V15 tables (`application_status_history`, `interviews`) behind the
existing `employment:manage` permission (SUPER_ADMIN + ADMIN; no new
permission, no A7.5/A7.6 features):

| Namespace | Permission | Endpoints |
|---|---|---|
| `/admin/applications/{id}/history` | `employment:manage` | list status history — newest transition first, DB-side pagination (default 20, cap 100); unknown application → 404 |
| `/admin/applications/{id}/interviews` | `employment:manage` | list (newest `scheduled_at` first) + create (scheduledAt/mode/location/notes) |
| `/admin/applications/{id}/interviews/{interviewId}` | `employment:manage` | get, PATCH (`scheduledAt`/`mode`/`location`/`status`/`notes` partial), DELETE (CANCELLED only → 409 otherwise) |

Server-enforced rules:

- **Automatic history:** every A7.3 lifecycle transition (start-review,
  shortlist, decide, withdraw) writes one history row inside the SAME
  transaction as the status change — a failed history insert rolls the
  transition back, so a status change without history can never be observed.
  `previous_status` is NULL only for a hypothetical initial observation; no
  backfilled records exist for applications created before A7.4.
- **Actor integrity:** `changed_by` is always the authenticated admin's
  database user id (via `SecurityUtils`); it is never accepted from request
  body/query/path and is nullable by design so audit survival is not coupled
  to user retention.
- **Interview statuses** (`chk_interviews_status`): SCHEDULED →
  COMPLETED/CANCELLED/NO_SHOW; COMPLETED, CANCELLED and NO_SHOW are final —
  changing out of them → 409. New interviews always start SCHEDULED; status
  is not client-writable at creation.
- **Interview modes** (`chk_interviews_mode`): ONSITE / REMOTE / PHONE
  (reuses the V8 work-mode vocabulary).
- **Terminal applications** (HIRED/REJECTED/WITHDRAWN) reject new interviews
  with 400; existing interviews stay readable. Creating an interview never
  changes application status.
- **Cross-application protection:** a mismatched (applicationId,
  interviewId) pair is a plain 404 — no existence leak about another
  application's interviews.
- **Delete rule:** only CANCELLED interviews may be deleted (409 for
  SCHEDULED/COMPLETED/NO_SHOW); deletion never touches the application.
- **Time handling:** `scheduled_at` is TIMESTAMPTZ mapped to
  `OffsetDateTime`; malformed date-times fail with 400 and offsets are
  preserved (the API serializes instants normalized to UTC, e.g.
  `10:30+05:45` → `04:45Z`).
- History/interview responses expose only schema-backed scheduling/audit
  data — never User entities, passwords, tokens or lockout fields.
- **A7.6 — Resume/File Storage — is NOT implemented.**

### Admin Notifications + Audit Logs + Outbox (A7.5)

V16 adds three tables (`notifications`, `audit_logs`, `outbox_events`) and
the `notifications:manage` permission (SUPER_ADMIN + ADMIN). MVP delivery is
**IN-APP ONLY** — no email/SMS/WhatsApp integration and no external
messaging infrastructure:

| Namespace | Permission | Endpoints |
|---|---|---|
| `/admin/notifications` | `notifications:manage` | list own notifications (type/unreadOnly filters + pagination, newest first), unread count, get by id, PATCH mark read / mark unread |
| `/admin/audit-logs` | `users:manage` (SUPER_ADMIN only) | list audit records (actorUserId/entityType/entityId/action/from-to filters + pagination, newest first), get by id |
| — | — | the outbox has **no public API**: it is written transactionally by business services and drained by the relay |

Server-enforced rules:

- **Recipient isolation is absolute:** every notification operation is
  scoped to the authenticated user's database id resolved server-side via
  `SecurityUtils`; a foreign (id, recipient) pair is a plain 404 with no
  existence leak. `notifications:manage` authorizes the endpoints but never
  broadens scoping — no API accepts a client-supplied recipient id.
- **Audit logs are append-only and immutable:** there is deliberately no
  create/update/delete endpoint (writes 405). Rows are written by backend
  services only, in the SAME transaction as the audited action (REQUIRED
  propagation). Reads are restricted to the SUPER_ADMIN-only `users:manage`
  authority — `notifications:manage` deliberately does NOT grant audit
  access.
- **Actor integrity:** audit `actor_user_id` is always resolved server-side
  from the authenticated principal (NULL for system actions); it is never
  accepted from client input and carries no ON DELETE cascade, so audit
  survival is never coupled to user retention.
- **No secrets in audit payloads:** `details` is serialized to JSON TEXT
  with security-sensitive keys redacted (password, token, JWT, secret,
  credential, authorization — case- and separator-insensitive);
  unserializable payloads fail the transaction rather than persisting a
  corrupt record.
- **Transactional outbox:** every application status transition (A7.3) and
  interview create/update/cancel/delete (A7.4) additionally writes one
  `audit_logs` row and one `outbox_events` row inside the SAME transaction —
  a failed side effect rolls the action back, so events can never reference
  work that did not happen.
- **Relay safety:** a scheduled relay claims due PENDING events with
  `FOR UPDATE SKIP LOCKED` (safe across multiple instances), processes each
  event in its own transaction (notification insert + PROCESSED commit
  atomically), marks terminally-broken payloads FAILED, and retries
  transient failures with `attempts` + bounded `available_at` backoff up to
  `OUTBOX_RELAY_MAX_ATTEMPTS`. A poison event can never wedge the queue or
  roll back unrelated events.
- **Notification vocabulary** (`chk_notifications_type`):
  APPLICATION_STATUS_CHANGED / INTERVIEW_SCHEDULED / INTERVIEW_UPDATED /
  INTERVIEW_CANCELLED. The relay only ever creates in-app notification rows
  — no external delivery occurs in this phase.
- **No infrastructure additions:** no Kafka/RabbitMQ/Redis/WebSockets, no
  `ApplicationEventPublisher`/`@TransactionalEventListener` — direct,
  service-level transactional integration only.
- Relay behavior is configurable via `OUTBOX_RELAY_*` environment variables
  (defaults in the table above; `polling-interval-seconds=0` or
  `enabled=false` disables the relay entirely).
- **A7.6 — Resume/File Storage — is NOT implemented.**

## Frontend Setup & Run

```powershell
cd frontend

# 1. Install dependencies
npm install

# 2. Start the Vite dev server
npm run dev

# 3. Production build (outputs to frontend/dist)
npm run build
```

The dev server runs on `http://localhost:5173` and **proxies `/api` to the
backend on `http://localhost:8080`**, so no CORS configuration is needed in
development.

## Validation Checklist

Phase 1 (project skeleton):

- [x] Repo layout: `backend/`, `frontend/`, `docs/`, `.gitignore`, `README.md`
- [x] Backend compiles (`mvn clean verify`)
- [x] Health endpoint `GET /api/v1/health`
- [x] PostgreSQL connection + Flyway migration verified
- [x] Frontend installs and builds (`npm install`, `npm run build`)

Phase 2 (backend foundation):

- [x] Standard API response envelope (`ApiResponse<T>`, `ApiError`)
- [x] Global exception handling with stable error codes
- [x] Standard pagination support (`PageParams`, `PageResponse<T>`)
- [x] OpenAPI / Swagger documentation (`/api/v1/api-docs`, `/api/v1/swagger-ui.html`)
- [x] Foundation unit + web-slice tests (`mvn clean verify`)
- [x] Health endpoint regression test

Phase 3 (database & migrations):

- [x] Full MVP schema: 41 domain tables across V2–V8 (identity, CMS, services,
      projects, content, communication, employment)
- [x] Primary keys, foreign keys, unique constraints, CHECK constraints,
      indexes, NOT NULL constraints with consistent naming (`docs/DATABASE.md`)
- [x] Seed data: roles, permissions, grants, bootstrap admin (no credential)
- [x] Flyway migration verified from a clean database (integration test +
      clean-database runtime check)
- [x] Database documentation with ERD (`docs/DATABASE.md`)

Phase 4 (authentication & authorization):

- [x] Email/password authentication with BCrypt password hashing
- [x] JWT access tokens (HS256, configurable secret and expiration)
- [x] Refresh tokens with rotation and revocation (SHA-256 hashed storage)
- [x] Spring Security with stateless JWT authentication
- [x] Role-based authorization (ROLE_SUPER_ADMIN, ROLE_ADMIN, etc.)
- [x] Permission-based authorization (content:manage, users:manage, etc.)
- [x] POST /api/v1/auth/login - Authenticate and receive tokens
- [x] POST /api/v1/auth/refresh - Refresh access token with rotation
- [x] POST /api/v1/auth/logout - Revoke refresh token
- [x] GET /api/v1/auth/me - Get current user profile
- [x] Bootstrap admin credential provisioning via BOOTSTRAP_ADMIN_PASSWORD
- [x] Comprehensive test suite (JWT, password, auth, authorization)
- [x] Swagger/OpenAPI bearer authentication documentation
- [x] Environment-based security configuration

## Conventions & Docs

- `docs/CONVENTIONS.md` — coding, REST, migration, naming and Git rules.
- `docs/DATABASE.md` — database schema reference, naming conventions and ERD.
- Phase 0 architecture and requirements plan (see git history / project archive).
- Future ADRs (architecture decision records) under `docs/adr/`.