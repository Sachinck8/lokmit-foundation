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
  enquiry management API requires `messages:manage`.
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