# YGC Chits — Infrastructure & Migration Guide

## ✅ What's Done in This Pass

### 1. Join Button Bug — FIXED
**Problem:** Members could click "Request to Join" on a chit they already belonged to.
**Fix:**
- `MemberController.availableChits()` now passes a `myChitStatus` map (chitId → status).
- `member/chits.html`: the OPEN badge is replaced by **"Active Member"** (green) / **"Requested"** (amber) / status badge when the user already has a membership.
- The join `<form>` is hidden and replaced with a **disabled button** ("Already a Member" / "Request Pending") for chits the user already joined.

### 2. HTTPS + Deployment
- `deploy.sh` — build / deploy / rollback / logs / status / ssl-init / ssl-renew, with health-check gated deploys and automatic rollback.
- `docker-compose.prod.yml` — app + PostgreSQL 16 + nginx, with healthchecks and named volumes.
- `nginx/nginx.conf` — TLS 1.2/1.3, HTTP/2, HSTS + security headers, gzip, SSE-aware proxy (no buffering on `/api/notifications/subscribe`), static-asset caching.
- Let's Encrypt via certbot (`./deploy.sh ssl-init` then `ssl-renew`).

### 3. CI/CD Pipeline (`.github/workflows/cicd.yml`)
- **test** → mvn verify + upload reports
- **security** → Trivy filesystem scan (CRITICAL/HIGH)
- **build** → JAR + multi-tag Docker image, GHA cache, push to Docker Hub
- **deploy** → SSH to prod, pull, `./deploy.sh deploy` (only on `main`)

Required GitHub secrets: `DOCKER_USERNAME`, `DOCKER_PASSWORD`, `PROD_HOST`, `PROD_USER`, `PROD_SSH_KEY`, `DB_PASSWORD`.

---

## 🔭 React / Spring Boot Split — IMPLEMENTED

The codebase now has a **React SPA frontend** (`ygc-web/`) and a **REST API backend** (`/api/v1/**`).

### Backend API (`/api/v1/**`)
- `AuthApiController` — login, register, refresh, change-password, me
- `MemberApiController` — member dashboard, chits, memberships, payments, bids
- `AdminApiController` — full admin operations (chits, members, payments, auctions, etc.)
- Thymeleaf MVC controllers remain for backward compatibility

### React Frontend (`ygc-web/`)
```powershell
cd ygc-web
npm install
npm run dev          # http://localhost:5173 (proxies /api → :8080)
npm run build        # output → ygc-web/dist/
```

### Production cutover
Nginx serves React from `ygc-web/dist/` at `/` and proxies `/api/**` to Spring Boot.
See `nginx/nginx.conf` and `docker-compose.prod.yml`.

---

## 🔭 Original Migration Notes (Reference)

### Phase 1 — Turn Spring Boot into a REST API (keep Thymeleaf running)
For each existing `@Controller`, add a parallel `@RestController` under `/api/v1/**` that returns JSON instead of view names. Reuse the **exact same service layer** — no business-logic changes.

```
controller/
  AdminController.java         (existing Thymeleaf — keep)
  api/
    AdminApiController.java     (new — returns JSON)
    AuthApiController.java      (JWT issue/refresh)
    MemberApiController.java
```

Add JWT auth alongside the existing session auth:
- `spring-boot-starter-oauth2-resource-server` or `jjwt`
- `POST /api/v1/auth/login` → returns `{ accessToken, refreshToken }`
- Keep SSE at `/api/notifications/subscribe` (already JSON-friendly).

### Phase 2 — Scaffold React (Vite + TypeScript)
```bash
npm create vite@latest ygc-web -- --template react-ts
cd ygc-web
npm i axios react-router-dom @tanstack/react-query react-i18next \
      vite-plugin-pwa zustand recharts
```

Suggested structure:
```
ygc-web/
  src/
    api/          axios client + endpoint wrappers
    auth/         JWT context, ProtectedRoute
    pages/        Dashboard, Chits, Members, Payments, ...
    components/   Cards, Tables, NotificationBell, Sidebar
    i18n/         en/te/hi/kn/ta JSON
    hooks/        useNotifications (SSE), useChits, ...
  vite.config.ts  (vite-plugin-pwa for installable PWA)
```

### Phase 3 — Migrate page by page
Move one screen at a time (Dashboard first). Each React page calls the new `/api/v1/**` endpoints. The Thymeleaf version stays live until its React twin is verified — zero downtime, zero big-bang risk.

### Phase 4 — Cut over
Once all pages exist in React, point nginx `/` at the React build (served as static files) and keep `/api/**` proxied to Spring Boot.

---

## 🚀 PWA / Performance (current Thymeleaf app)
- `manifest.json` + `sw.js` already present (installable, offline shell).
- nginx gzip + 30-day immutable static caching included above.
- JVM tuned: `-Xms256m -Xmx512m -XX:+UseG1GC` (lightweight, fits a 1 GB VM).
- PostgreSQL connection pooling via HikariCP (Spring Boot default).

## Local quickstart
```bash
chmod +x deploy.sh
export DOCKER_USERNAME=youruser DB_PASSWORD=secret YGC_DOMAIN=chits.example.com
./deploy.sh build      # build jar + image
./deploy.sh ssl-init   # one-time cert (DNS must point to server first)
./deploy.sh deploy     # start everything, health-gated
./deploy.sh status     # check
```
