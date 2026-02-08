# Frontend MVP (React + Vite)

## Setup

```bash
cd /Users/alo/Documents/Code/appCompras/appCompras/frontend
npm install
npm run dev
```

App por defecto en `http://localhost:5173`.

## Variables

Crear `.env` opcional en `frontend/`:

```bash
VITE_API_BASE_URL=http://localhost:8081
```

Si no defines `VITE_API_BASE_URL`, el frontend usa proxy de Vite a `http://localhost:8080` (sin problemas de CORS en local).

## Alcance implementado

- Tab `Recetas`: listar, buscar, crear, editar y eliminar recetas.
- Tab `Planificador`: semanal/quincenal, slots breakfast/lunch/dinner y guardar plan.
- Tab `Compras`: generar draft desde plan, editar items y guardar cambios.
- Manejo base de errores desde `ApiError.error`.
- Header `X-API-Version: 1` en todas las llamadas.

## Auth

Si backend requiere auth (`APP_SECURITY_REQUIRE_AUTH=true`), la app muestra `AuthGate` al iniciar.
Pega tu `google id_token` y entra.

Alternativa por consola:

```js
localStorage.setItem('appcompras_id_token', '<google_id_token>')
```

Para limpiar token:

```js
localStorage.removeItem('appcompras_id_token')
```

Para desarrollo local rápido sin login, en `/Users/alo/Documents/Code/appCompras/appCompras/.env` usa:

```bash
APP_SECURITY_REQUIRE_AUTH=false
```

y recrea backend:

```bash
cd /Users/alo/Documents/Code/appCompras/appCompras
docker compose --profile app up -d --force-recreate backend
```

## Google Sign-In UI (Account Chooser)

Configura en `frontend/.env`:

```bash
VITE_REQUIRE_AUTH=true
VITE_GOOGLE_CLIENT_ID=tu-google-web-client-id.apps.googleusercontent.com
```

Debe coincidir con `GOOGLE_CLIENT_ID` del backend para que el token `id_token` pase validación de audiencia.
