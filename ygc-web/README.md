# YGC Web — React Frontend

React + TypeScript SPA for YGC Chit Management. Talks to Spring Boot REST API at `/api/v1/**`.

## Development

**Terminal 1 — Backend:**
```powershell
cd ..
mvn spring-boot:run
```

**Terminal 2 — Frontend:**
```powershell
npm install
npm run dev
```

Open http://localhost:5173

The Vite dev server proxies `/api` to `http://127.0.0.1:8080`.

## Demo Login

```
Admin:  admin@ygc.internal / Admin@123
Member: aarav.sharma@example.com / Member@123
```

## Production Build

```powershell
npm run build
```

Output goes to `dist/`. Nginx serves this folder (see `docker-compose.prod.yml`).

## Stack

- React 19 + TypeScript
- Vite 6
- React Router 7
- TanStack Query
- Zustand (auth state)
- Axios (JWT + refresh token)
