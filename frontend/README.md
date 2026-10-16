# Vanta authentication frontend

A React and JavaScript frontend for the Spring Boot authentication service. The public auth routes are `#/login` and `#/register`; after sign-in, users are routed to `#/profile` and administrators to `#/admin/dashboard` based on the authenticated `/profile` response.

## Run locally

Start Spring Boot on port `8080`, then run from this folder:

```sh
npm install
npm run dev
```

Open the Vite URL, normally `http://localhost:5173`. Vite proxies `/auth`, `/profile`, and `/api` to the backend. Build production assets into `src/main/resources/static` with `npm run build`.

## API routes

- `POST /auth/register` — create a user account.
- `POST /auth/login` — sign in and receive a bearer token.
- `GET /profile` — return the authenticated user's name, email, role, and verification status.
- `GET /api/health` — connectivity status.
- `GET /api/admin/dashboard` — registration, verification, and successful-login counts.
- `GET /api/admin/getUsers` — administrator's account activity list.

Successful logins are counted from the point this metric is added. Existing accounts start at zero. Only users with the backend `ADMIN` role can load the admin dashboard.
