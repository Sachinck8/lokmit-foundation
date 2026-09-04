# LOKMIT FOUNDATION — Development Conventions

These conventions apply to the `backend/` (Java/Spring Boot) and `frontend/`
(React/Vite) applications. They are derived from the Phase 0 architecture plan
and take effect from Phase 1 onward.

---

## 1. Java Package Naming

- Root package: **`com.lokmit.foundation`** (backend base is
  `com.lokmit.foundation`).
- **Package-by-feature** — each business capability owns a vertical slice
  (`controller`, `service`, `repository`, `dto`, ...) inside its own package.
- Shared, non-business code lives under `com.lokmit.foundation.common`
  (`common.constants`, `common.health`, `common.api` for the response
  envelope, `common.exception` for the global handler, `common.pagination`).
- Technical config belongs to `com.lokmit.foundation.config`; security to
  `com.lokmit.foundation.security`.
- Package names are always lowercase; class names follow UpperCamelCase.

## 2. REST API Naming

- Resources use **plural nouns** (`/projects`, `/services`, `/news`).
- **Nouns, not verbs**, in URLs (`/projects/{slug}` — not `/getProject`).
- Actions are expressed with HTTP methods: `GET` (read), `POST` (create),
  `PUT` (full update), `PATCH` (partial update), `DELETE` (delete/archive).
- Sub-resources nest under their parent where ownership is clear
  (`/employer/jobs/{jobId}/applications`).
- Nested levels beyond two are avoided; prefer query parameters.

## 3. `/api/v1` Convention

- Every endpoint is mounted under **`/api/v1`**.
- Path constants are centralized in `ApiPaths.java` (backend) and
  `apiEndpoints.js` (frontend) — literals are not repeated in controllers.
- Public routes stay under `/api/v1/...` (e.g. `/api/v1/health`).
- Backward-incompatible changes require a new root (`/api/v2`);
  additive changes are made within the current major version.
- The current concrete example: `GET /api/v1/health`.

## 4. DTO Convention

- **DTOs (Data Transfer Objects) define the API contract**; persistence
  entities are never exposed directly to callers.
- Naming:
  - Request: `CreateXxxRequest`, `UpdateXxxRequest`, `XxxSearchRequest`
  - Response: `XxxResponse`, `XxxSummaryResponse`
- Validation annotations belong on request DTOs (Bean Validation).
- DTO mapping lives in the service layer (or a dedicated mapper class);
  repositories and controllers do not know about each other's DTOs.

## 5. Service / Controller Responsibilities

- **Controllers are thin:** parameter binding + validation, security
  annotations, call exactly one service method, return a response object.
  No business logic in controllers.
- **Services contain business logic:** validation orchestration, permission /
  ownership checks, transaction boundaries (`@Transactional`), DTO mapping.
- Services communicate with repositories only; they never call other
  controllers.
- Controllers depend on services via interfaces where a second implementation
  exists; otherwise concrete classes are acceptable for this project size.

## 6. Repository Responsibilities

- Repositories handle **persistence only**: find/save/delete queries.
- Naming follows Spring Data JPA conventions (`findBy...`, `existsBy...`,
  `countBy...`).
- Complex aggregation/reporting queries that cannot stay readable in derived
  queries are written as JPQL or native SQL with `@Query`, reviewed for
  PostgreSQL dialect compatibility.
- Bulk operations use `@Modifying` only in real bulk cases; the default path
  prefers entity lifecycle through the persistence context.

## 7. Flyway Migration Naming

- One schema version, one migration, in
  `backend/src/main/resources/db/migration`.
- Format: `V<version>__<snake_case_description>.sql`
  - Examples: `V1__baseline.sql`, `V2__create_users_table.sql`
- Version numbers are **global increments** — never reuse or renumber a
  published migration.
- **Migrations are immutable once committed.** Corrections go in a new
  migration.
- Schema design, naming conventions, and ERD: see `docs/DATABASE.md`.
- For future phases: `R__` repeatable migrations are allowed only for
  genuinely idempotent data views/reference seed data.

## 8. Frontend Naming Conventions

- Folders/files: `kebab-case` for folders; JSX components in `PascalCase.jsx`.
- Example: `src/pages/public/Home/Home.jsx`,
  `src/layouts/PublicLayout/PublicLayout.jsx`.
- Non-component modules: `camelCase.js` (`apiClient`, `healthService`).
- Component naming: functional components, exported as default, PascalCase.
- Hooks follow the `use` prefix (`useAuth`, `useApi`).
- Environment variables used in the browser must be prefixed with
  `VITE_` (Vite requirement).
- All endpooints are imported from `constants/apiEndpoints.js`, never inline.
## 9. Environment Variable Rules

- **Never commit secrets** (database passwords, JWT keys, API keys, tokens).
- Express config via variables: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`,
  `SERVER_PORT`, `JWT_SECRET`, `JWT_ACCESS_TOKEN_EXPIRATION`,
  `JWT_REFRESH_TOKEN_EXPIRATION`, `BOOTSTRAP_ADMIN_PASSWORD`,
  later `MAIL_*`, `STORAGE_*`.
- `application.yml` reads them with safe, non-secret defaults:
  `${DB_USERNAME:lokmit_app}`, `${DB_PASSWORD:}` etc.
- `.env.example` files contain placeholders only and are committed.
- `.env` files contain real values, are gitignored, and are never committed.
- Any new secret must be added to the env tables in this doc, plus the
  corresponding `.env.example` — as a placeholder.

## 10. Security Rules

- Passwords are hashed with BCrypt (never plain text, never logged).
- JWT secrets and private keys are provided exclusively via environment
  variables — never in source or configuration files.
- Public APIs must be explicitly allowed; everything else is authenticated
  (enforced in the authentication phase via the SecurityFilterChain).
- Server-side authorization is mandatory — the frontend UI is never the
  enforcement point.
- Input validation is applied on every public-facing request DTO.
- Logs must not contain passwords, tokens, or personal data.
- File uploads are validated for type, size, and allowed extensions before
  storage.

## 11. Git Commit Conventions

- Commits are **atomic**: one logical change per commit.
- Format:

  ```text
  <type>(<scope>): <short imperative summary>

  [optional body explaining WHY, not WHAT]
  ```

- Types: `feat`, `fix`, `docs`, `refactor`, `test`, `build`, `chore`,
  `style`, `perf`.
- Examples:
  - `feat(backend): add health endpoint skeleton`
  - `chore: add gitignore and env templates`
  - `docs: document REST naming conventions`
- Branch naming: `feature/<phase>-<name>`, `fix/<short-name>`,
  `chore/<short-name>`.
- Never commit: `.env`, build outputs (`target/`, `node_modules/`,
  `dist/`), logs, or IDE folders.
## 12. API Response Envelope

- Every business API response uses the **`ApiResponse<T>`** envelope:
  `{ "success": bool, "message": string, "data": T|null, "errors": [...], "timestamp": ISO-8601 }`.
- Success responses use `ApiResponse.success(data)`; failures carry
  `errors: [ApiError]` (`{ code, message, field? }`).
- `data` and `errors` are omitted when empty (`@JsonInclude(NON_NULL)`).
- Paginated endpoints return `PageResponse<T>`:
  `{ items, page, size, totalItems, totalPages }`.
- Operational endpoints (`/api/v1/health`) also use the envelope, with the
  operational payload carried in `data` and a plain-language `message`.

## 13. Error Handling & Error Codes

- Domain failures use the common exceptions:
  `NotFoundException` (404), `BadRequestException` (400), `ConflictException` (409).
- `GlobalExceptionHandler` (`@RestControllerAdvice` in `common.exception`)
  maps domain and framework exceptions to the envelope. Controllers never
  catch exceptions to build error responses.
- Machine-readable codes live in `common.api.ErrorCodes`: `NOT_FOUND`,
  `BAD_REQUEST`, `VALIDATION`, `CONFLICT`, `METHOD_NOT_ALLOWED`,
  `UNSUPPORTED_MEDIA_TYPE`, `INTERNAL_ERROR`.
- The handler never leaks internals in 500 responses — the generic handler
  logs the exception and returns a fixed generic message.

## 14. Pagination

- List endpoints bind `page` (zero-based, default `0`) and `size`
  (default `20`, max `100`) into `common.pagination.PageParams`
  (`@Valid @ModelAttribute`).
- `PageParams.toPageable()` produces the Spring Data `Pageable` consumed by
  repositories; repositories stay Spring-Data-native.

## 15. API Documentation

- OpenAPI is generated by springdoc (`springdoc-openapi-starter-webmvc-ui`).
- JSON spec: `/api/v1/api-docs`; UI: `/api/v1/swagger-ui.html`.
- Global metadata lives in `config/OpenApiConfig` (`@OpenAPIDefinition` on a
  Spring-managed bean).
- Controllers may annotate operations/parameters with
  `@Operation`/`@Parameter`/`@Response` for clarity.

## 16. Testing Conventions

- JUnit 5 + AssertJ; Spring Boot test slices.
- Pure logic (envelope, pagination, params) → plain JUnit tests.
- MVC behavior → `@WebMvcTest(controllers = Xxx.class)` + MockMvc, asserting
  status codes and envelope JSON shape.
- Web slices never connect to PostgreSQL or run Flyway.
- Run `mvn clean verify` before finishing every phase.