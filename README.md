# LOKMIT FOUNDATION — Digital Platform

Website and digital platform for **LOKMIT FOUNDATION** — built as a
Corporate Consultancy + Skill Development + Project Advisory + Employment +
Job Portal + Knowledge Platform.

> **Status: Phase 2 — backend foundation complete.**
> No business entities, authentication, modules, or admin panel implemented yet.
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

## Conventions & Docs

- `docs/CONVENTIONS.md` — coding, REST, migration, naming and Git rules.
- `docs/DATABASE.md` — database schema reference, naming conventions and ERD.
- Phase 0 architecture and requirements plan (see git history / project archive).
- Future ADRs (architecture decision records) under `docs/adr/`.