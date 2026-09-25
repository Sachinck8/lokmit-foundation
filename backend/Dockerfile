# A25 — Production backend image for the LOKMIT FOUNDATION API.
#
# Build (from the repository root):
#   docker build -f backend/Dockerfile -t lokmit-backend ./backend
#
# Design:
# - Multi-stage: Maven/Java 21 build stage, slim JRE 21 runtime stage.
# - Dependency layer caching: pom.xml is copied before src/ so dependency
#   download layers are reused across source-only changes.
# - Tests are NOT run here; CI (`.github/workflows/ci.yml`) owns `mvn
#   clean test`. Image builds stay fast and failures stay visible.
# - Non-root runtime user; no secrets are baked into the image — all
#   configuration arrives via environment variables at run time
#   (see application-prod.yml: DB_URL, DB_USERNAME, DB_PASSWORD,
#   JWT_SECRET, APP_CORS_ALLOWED_ORIGINS, ...).
# - exec-form ENTRYPOINT: the JVM is PID 1 and receives SIGTERM directly,
#   enabling Spring Boot's graceful shutdown on `docker stop`.

# ---------------------------------------------------------------- build ---
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace

# Dependency layer: resolves once per pom change, not per source change.
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# --------------------------------------------------------------- runtime ---
FROM eclipse-temurin:21-jre

# curl is used by container health checks against GET /api/v1/health.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Non-root runtime user; the app directory is the only writable location.
RUN useradd --system --uid 1001 --create-home lokmit

WORKDIR /app
RUN chown lokmit:lokmit /app

COPY --from=build --chown=lokmit:lokmit \
    /workspace/target/lokmit-foundation-backend-0.1.0-SNAPSHOT.jar app.jar

USER lokmit
EXPOSE 8080

# Configuration contract (set at run time, never baked in):
#   SPRING_PROFILES_ACTIVE=prod          activates the strict prod profile
#   DB_URL / DB_USERNAME / DB_PASSWORD   PostgreSQL connection (no defaults)
#   JWT_SECRET                           signing key, min 32 bytes (no default)
#   APP_CORS_ALLOWED_ORIGINS             exact frontend origins (no default)
#   JAVA_TOOL_OPTIONS                    optional JVM tuning (picked up natively)
ENTRYPOINT ["java", "-jar", "app.jar"]
