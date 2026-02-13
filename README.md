# appCompras - Week Meal Prep + Shopping List

MVP mobile-first para planificar comidas y generar lista de compras editable.

Objetivo principal: mantener una UX simple para el usuario final y un backend consistente para evitar duplicados, errores de unidades y problemas de ownership por usuario.

## Estado actual del proyecto

Backend y frontend están integrados en `master` con:

- Persistencia PostgreSQL para recetas, planes y drafts de compras.
- Migraciones Flyway hasta `V7`.
- Auth JWT Google opcional por entorno (`APP_SECURITY_REQUIRE_AUTH=true|false`).
- Aislamiento por usuario (`user_id`) para datos principales.
- Generación de shopping list con idempotencia (`Idempotency-Key`).
- API versionada (`X-API-Version: 1` o `v1`).
- UI React mobile-first (recetas, planificador y compras).
- CI backend con tests unitarios/aplicación + integración Postgres (Testcontainers).

## Stack técnico

- Backend: Java 17, Spring Boot 3.3, Spring Security Resource Server, Spring Data JPA, Flyway, OpenAPI (springdoc), Actuator, Micrometer Prometheus.
- DB: PostgreSQL 16.
- Frontend: React 18 + Vite 5.
- Contenedores: Docker / Docker Compose.
- CI: GitHub Actions.
- IaC (pendiente de implementación completa): Terraform en `infra/`.

## Estructura del repo

- `backend/`: API REST, dominio, seguridad, persistencia y migraciones.
- `frontend/`: SPA React/Vite mobile-first.
- `scripts/`: utilidades locales (`dev-up`, `dev-down`, smoke E2E).
- `docs/`: decisiones y handoff funcional/técnico del MVP.
- `infra/`: guía base para Terraform (pendiente bootstrap de módulos).

## Arquitectura

### Backend

Paquetes principales (`backend/src/main/java/com/appcompras`):

- `recipe`: CRUD de recetas.
- `planning`: CRUD de meal plans (`WEEK`/`FORTNIGHT`, slots `BREAKFAST/LUNCH/DINNER`).
- `shopping`: generación, lectura y edición de drafts.
- `ingredient`: catálogo + custom ingredients.
- `service`: lógica de dominio (catálogo, conversiones, agregación de compras).
- `security`: validación JWT Google y políticas de acceso.
- `config`: OpenAPI, manejo de errores, API versioning, filtros de request logging.
- `domain`: modelos de negocio.

Principios aplicados:

- Backend stateless.
- Contrato de error consistente (`ApiError` con `code`).
- IDs canónicos para ingredientes (ej. `rice`) para evitar duplicados.
- Labels amigables para UI (`preferredLabel`, aliases) sin romper canonical IDs.

### Frontend

Archivos principales (`frontend/src`):

- `App.jsx`: composición de tabs y flujos principales.
- `api.js`: cliente API, headers comunes, token auth y cache simple en memoria.
- `styles.css`: tema mobile-first.

Pantallas MVP:

- Recetas: list/search/create/update/delete.
- Planificador: vista semanal/quincenal, selección por slot, guardar plan.
- Compras: generar draft, editar items, marcar comprados, reordenar, guardar.
- AuthGate: Google Sign-In cuando auth está activo.

### Datos y reglas importantes

- El usuario puede escribir ingredientes en español; backend resuelve aliases y guarda `ingredientId` canónico.
- La UI muestra `preferredLabel` (amigable) en sugerencias y shopping list.
- Nombres visibles se normalizan a formato `Display Case` (ej. `SALSA`, `sALsa` -> `Salsa`).

## Contrato API (resumen)

Header recomendado:

- `X-API-Version: 1`

Auth (si aplica):

- `Authorization: Bearer <google_id_token>`

Grupos de endpoints:

- `GET/POST/PUT/DELETE /api/recipes`
- `GET/POST/PUT/DELETE /api/plans`
- `POST /api/shopping-lists/generate?planId=...`
- `GET /api/shopping-lists`
- `GET /api/shopping-lists/{id}`
- `PUT /api/shopping-lists/{id}`
- `DELETE /api/shopping-lists/{id}`
- `GET /api/ingredients`
- `POST /api/ingredients/custom`

Documentación runtime:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Desarrollo local

### Requisitos

- Docker / Docker Compose
- Node.js + npm
- Java 17 + Gradle
- `curl` y `jq` (para script smoke)

### Variables de entorno

1. Copiar archivo base:

```bash
cp .env.example .env
```

2. Configurar al menos:

- `APP_SECURITY_REQUIRE_AUTH` (`true` o `false`)
- `GOOGLE_CLIENT_ID` (si auth=true)

Opcional frontend (`frontend/.env`):

- `VITE_REQUIRE_AUTH=true|false`
- `VITE_GOOGLE_CLIENT_ID=<google-web-client-id>`
- `VITE_API_BASE_URL` (si no usas proxy de Vite)

### Levantar servicios

Desde la raíz del repo:

```bash
cd /Users/alo/Documents/Code/appCompras/appCompras
```

Solo Postgres:

```bash
./scripts/dev-up.sh
```

Postgres + backend dockerizado:

```bash
./scripts/dev-up.sh app
```

Apagar:

```bash
./scripts/dev-down.sh
```

Rebuild backend (cuando cambias código backend):

```bash
docker compose --profile app up -d --build backend
```

### Frontend

```bash
cd /Users/alo/Documents/Code/appCompras/appCompras/frontend
npm install
npm run dev
```

- App: `http://localhost:5173`
- Vite proxy apunta a `http://localhost:8080` para `/api`, `/actuator`, `/v3/api-docs`, `/swagger-ui`.

## Testing y calidad

Backend completo:

```bash
cd /Users/alo/Documents/Code/appCompras/appCompras/backend
gradle --no-daemon test
```

Integración Postgres (Testcontainers):

```bash
cd /Users/alo/Documents/Code/appCompras/appCompras/backend
gradle --no-daemon test --tests com.appcompras.integration.PostgresE2EFlowTest
```

Smoke E2E local (flujo funcional backend):

```bash
cd /Users/alo/Documents/Code/appCompras/appCompras
./scripts/smoke-backend-e2e.sh
```

Frontend:

```bash
cd /Users/alo/Documents/Code/appCompras/appCompras/frontend
npm run build
npm run test
```

## Docker

`docker-compose.yml` define:

- `postgres` (siempre): `postgres:16-alpine`, puerto `5432`.
- `backend` (profile `app`): build desde `backend/Dockerfile`, puerto `8080`.

Backend container variables clave:

- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- `APP_SECURITY_REQUIRE_AUTH`
- `GOOGLE_CLIENT_ID`

## CI

Workflow: `.github/workflows/backend-ci.yml`

Jobs:

- `backend-unit`: tests unitarios/aplicación por paquetes.
- `backend-integration-postgres`: `PostgresE2EFlowTest` con Testcontainers.

## Handoff para DevOps (siguiente fase)

Estado actual infra:

- `infra/` está en fase base (README), sin módulos Terraform implementados aún.

Siguientes pasos recomendados:

1. Definir módulos Terraform por entorno (`dev`/`prod`): VPC, RDS, ECS Fargate, ALB, ECR, S3, CloudFront, IAM.
2. Pipeline CI/CD: build imagen backend, push a ECR, deploy a ECS (rolling).
3. Secrets/variables en entorno gestionado (no en `.env` local).
4. Observabilidad en cloud: métricas, logs estructurados, alertas.
5. Estrategia de branch protection y release tagging.

## Handoff para otro agente AI

Si vas a continuar con otro agente, comparte esto como contexto mínimo:

- Repo: `/Users/alo/Documents/Code/appCompras/appCompras`
- Rama actual: `master`
- Backend: Spring Boot + Postgres + Flyway + JWT Google opcional.
- Frontend: React/Vite mobile-first en un solo `App.jsx` + `api.js`.
- Convención ingredientes:
  - Backend guarda `ingredientId` canónico.
  - UI muestra `preferredLabel` y aliases amigables.
- Comandos clave:
  - `./scripts/dev-up.sh app`
  - `docker compose --profile app up -d --build backend`
  - `cd frontend && npm run dev`
  - `cd backend && gradle --no-daemon test`
- Documentos clave:
  - `docs/frontend-handoff-mvp-mobile-first.md`
  - `docs/backend-mvp-dod.md`

