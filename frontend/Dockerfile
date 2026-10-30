# Angular frontend: built with Node, served by nginx.

# ---------- build ----------
FROM node:22-alpine AS builder
WORKDIR /build

# package files first so the install layer is cached independently of the sources.
COPY package.json package-lock.json* ./
RUN npm ci || npm install

COPY . .
RUN npm run build

# ---------- runtime ----------
FROM nginx:1.27-alpine AS runtime

# The SPA needs every unknown path to fall back to index.html, and /api must reach the gateway.
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /build/dist/frontend/browser /usr/share/nginx/html

EXPOSE 80
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
    CMD wget -qO- http://localhost/ > /dev/null || exit 1
