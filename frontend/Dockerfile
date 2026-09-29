# A25 — Production frontend image for the LOKMIT FOUNDATION web app.
#
# Build (from the repository root):
#   docker build -f frontend/Dockerfile -t lokmit-frontend ./frontend
#
# Design:
# - Multi-stage: Node 22 + Vite production build, then a small nginx image
#   that serves only the compiled static assets. No dev server, no source
#   code, no node_modules in the runtime image.
# - The API base URL follows the existing VITE_API_BASE_URL architecture:
#   it is a Vite **build-time** variable. The default build produces the
#   same-origin setup (client calls /api/v1, the reverse proxy forwards
#   /api to the backend). Override at build time only when the API lives
#   on another origin:
#     docker build --build-arg VITE_API_BASE_URL=https://api.example.org ...
# - SPA fallback: nginx routes any unknown path to index.html so React
#   Router deep links (/jobs/1, /admin-panel/...) work on hard reload.

# ---------------------------------------------------------------- build ---
FROM node:22-alpine AS build

WORKDIR /workspace

# Dependency layer: manifest first for lockfile-cached installs.
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
ARG VITE_API_BASE_URL=""
RUN if [ -n "$VITE_API_BASE_URL" ]; then \
      echo "Building with VITE_API_BASE_URL override (value not logged)" ; \
    fi \
    && npm run build

# --------------------------------------------------------------- runtime ---
FROM nginx:1.27-alpine

RUN rm /etc/nginx/conf.d/default.conf
COPY nginx-spa.conf /etc/nginx/conf.d/app.conf
COPY --from=build /workspace/dist /usr/share/nginx/html

EXPOSE 80
