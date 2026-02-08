# Backend MVP Definition of Done

Estado objetivo para considerar el backend del MVP listo para frontend y demo.

## 1) API y contrato

- Endpoints base funcionando y estables:
  - `recipes`: CRUD.
  - `plans`: CRUD.
  - `shopping-lists`: generate/get/list/update/delete.
  - `ingredients`: list/search + custom.
- Persistencia en PostgreSQL con Flyway (migraciones aplicadas).
- Payload de errores estandarizado (`ApiError`) con `code` consistente.
- Versionado de API no rompiente con header opcional `X-API-Version`.

## 2) Reglas de negocio

- Normalización de ingredientes (case/acento/alias) activa en create/update.
- Validación de unidades permitidas por ingrediente.
- Aislamiento por usuario (`user_id`) en recetas, planes y drafts.
- Shopping draft editable con:
  - `bought` (comprado/no comprado)
  - `note` por item
  - `sortOrder` manual
- Idempotencia en generación de shopping draft con `Idempotency-Key`.

## 3) Calidad y tests

- Tests backend locales en verde:
  - `cd backend && gradle --no-daemon test`
- Test integración Postgres real (Testcontainers) en verde:
  - `cd backend && gradle --no-daemon test --tests com.appcompras.integration.PostgresE2EFlowTest`
- Smoke script E2E local en verde:
  - `./scripts/smoke-backend-e2e.sh`

## 4) CI y merge gates

- Workflow `.github/workflows/backend-ci.yml` con dos jobs:
  - `backend-unit`
  - `backend-integration-postgres`
- Branch protection en GitHub (manual):
  - `Settings > Branches > Branch protection rules > master`
  - habilitar:
    - Require a pull request before merging
    - Require status checks to pass before merging
    - status checks requeridos:
      - `backend-unit`
      - `backend-integration-postgres`

## 5) Operación local mínima

- `.env` configurado en raíz del repo.
- `docker compose up -d postgres` funciona.
- Backend levanta contra Postgres y expone:
  - `/swagger-ui.html`
  - `/v3/api-docs`
  - `/actuator/health`

## 6) Documentación mínima al día

- README actualizado con:
  - setup local
  - seguridad/JWT
  - versionado API
  - troubleshooting
  - política de versionado del catálogo seed
