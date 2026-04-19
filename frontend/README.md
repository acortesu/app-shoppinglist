# Frontend MVP (React + Vite)

## Setup

```bash
cd /Users/alo/Documents/Code/appCompras/appCompras/frontend
npm install
npm run dev
```

App runs at `http://localhost:5173` by default.

## Environment variables

Optional `.env` under `frontend/`:

```bash
VITE_API_BASE_URL=http://localhost:8081
```

If `VITE_API_BASE_URL` is unset, the frontend uses the Vite proxy to `http://localhost:8080` (no CORS issues in local dev).

## Implemented scope

- `Recipes` tab: list, search, create, edit and delete recipes.
- `Planner` tab: weekly/fortnightly view, breakfast/lunch/dinner slots, save plan.
- `Shopping` tab: generate draft from a plan, edit items, save changes.
- Baseline error handling from `ApiError.error`.
- `X-API-Version: 1` header on every call.

## Auth

If the backend requires auth (`APP_SECURITY_REQUIRE_AUTH=true`), the app shows `AuthGate` at startup.
Paste your `google id_token` to enter.

Console alternative:

```js
localStorage.setItem('appcompras_id_token', '<google_id_token>')
```

Clear the token:

```js
localStorage.removeItem('appcompras_id_token')
```

For quick local dev without login, in `/Users/alo/Documents/Code/appCompras/appCompras/.env` set:

```bash
APP_SECURITY_REQUIRE_AUTH=false
```

Then recreate the backend:

```bash
cd /Users/alo/Documents/Code/appCompras/appCompras
docker compose --profile app up -d --force-recreate backend
```

## Google Sign-In UI (Account Chooser)

Configure in `frontend/.env`:

```bash
VITE_REQUIRE_AUTH=true
VITE_GOOGLE_CLIENT_ID=your-google-web-client-id.apps.googleusercontent.com
```

Must match the backend's `GOOGLE_CLIENT_ID` so the `id_token` passes audience validation.

Client-side validation covers:
- JWT format
- expiration (`exp`)
- `aud` against `VITE_GOOGLE_CLIENT_ID`
- `iss` from Google

## API errors (`ApiError.code`)

Context-aware mapping from business codes to UX messages for:
- auth
- recipe form
- planner
- shopping
