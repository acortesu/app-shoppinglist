# Week Meal Prep + Shopping List

Proyecto portfolio DevOps con foco en un MVP usable para planificación de comidas y generación de lista de compras inteligente.

## Objetivo MVP

- CRUD de recetas
- Planificación semanal o quincenal (breakfast/lunch/dinner)
- Generación automática de shopping list (suma + normalización + conversión)
- Shopping list editable por el usuario

## Criterios de diseño

- Simplicidad > complejidad
- Backend con lógica de dominio fuerte y testeable
- Frontend guiado para que el usuario no piense en conversiones
- Infra declarativa con Terraform en AWS
- Sin microservicios, sin Kubernetes, sin serverless forzado

## Estructura del repositorio

- `frontend/`: React + Vite (build estático para S3 + CloudFront)
- `backend/`: Java 17 + Spring Boot REST API (stateless, dockerizable)
- `infra/`: Terraform para entornos `dev` y `prod`
- `docs/`: alcance funcional, modelo de dominio y plan de implementación

## Entorno local (docker-compose)

1. Levantar Docker Desktop.
2. Copiar variables:
   - `cp .env.example .env`
   - completar `GOOGLE_CLIENT_ID` con tu Client ID de Google
3. Desde la raíz del repo (`/Users/alo/Documents/Code/appCompras/appCompras`):
   - Solo DB: `./scripts/dev-up.sh`
   - DB + backend dockerizado: `./scripts/dev-up.sh app`
4. Verificar salud:
   - `docker compose ps`
5. Para apagar:
   - `./scripts/dev-down.sh`

Credenciales por defecto del compose:
- DB: `appcompras`
- User: `appcompras_user`
- Password: `appcompras_pass`
- Puerto: `5432`

Si corrés backend en local (no docker), usa los defaults de `backend/src/main/resources/application.yml` y conecta a esa misma DB.
El archivo `.env` debe quedar en: `/Users/alo/Documents/Code/appCompras/appCompras/.env`.

## Seguridad (Google JWT)

- Endpoints `/api/**` requieren JWT cuando `APP_SECURITY_REQUIRE_AUTH=true`.
- Header esperado: `Authorization: Bearer <google_id_token>`.
- `GOOGLE_CLIENT_ID` debe coincidir con la audiencia (`aud`) del token.
- Para desarrollo local sin login:
  - `APP_SECURITY_REQUIRE_AUTH=false`
- Endpoints públicos aunque auth esté activo:
  - `/swagger-ui.html`
  - `/v3/api-docs`
  - `/actuator/health`
  - `/actuator/info`

## Versionado de API

- Versión actual: `v1`.
- Header opcional: `X-API-Version`.
- Valores aceptados:
  - `1`
  - `v1`
- Sin header también funciona para mantener compatibilidad.
- Cualquier otro valor devuelve `400` con código `UNSUPPORTED_API_VERSION`.

## Test de integración Postgres

- Test E2E con Postgres real (Testcontainers):
  - `cd backend && gradle --no-daemon test --tests com.appcompras.integration.PostgresE2EFlowTest`
- Requiere Docker disponible para el proceso de tests.

## Tests de seguridad y ownership

- Correr todas las pruebas backend:
  - `cd backend && gradle --no-daemon test`
- Incluye:
  - `ApiSecurityAuthTest`: valida `401` sin token y acceso con JWT.
  - `OwnershipIsolationTest`: valida aislamiento de datos por `sub` (usuario autenticado).

## Shopping list UX + idempotency

- `POST /api/shopping-lists/generate` acepta header opcional `Idempotency-Key`.
- `PUT /api/shopping-lists/{id}` ahora acepta por item:
  - `bought` (boolean)
  - `note` (string)
  - `sortOrder` (integer)

## Observabilidad

- Swagger UI: `/swagger-ui.html`
- OpenAPI JSON: `/v3/api-docs`
- Health: `/actuator/health`
- Métricas Prometheus: `/actuator/prometheus`
- Logs: incluyen `requestId`, método, path, status y latencia (`durationMs`).

## Troubleshooting rápido

- `401 UNAUTHORIZED` en `/api/**`:
  - falta token o `APP_SECURITY_REQUIRE_AUTH=true` sin login.
- `invalid_token` / audience inválida:
  - revisar `GOOGLE_CLIENT_ID` y que coincida con el token real.
- Error de conexión a DB:
  - validar `docker compose ps`, credenciales del `.env`, y `DB_URL`.
- Error Flyway al arrancar:
  - revisar tabla `flyway_schema_history` y que no haya migraciones editadas luego de aplicarse.

## CI

- Pipeline backend con tests en `.github/workflows/backend-ci.yml`.
- Corre en push/PR contra `master` con dos jobs separados:
  - `backend-unit`: tests de aplicación/unidad.
  - `backend-integration-postgres`: flujo E2E con Testcontainers + PostgreSQL.
- Recomendado en GitHub branch protection:
  - marcar ambos checks (`backend-unit`, `backend-integration-postgres`) como requeridos para merge.

## Próximos hitos

1. Definir modelo de dominio backend y reglas de conversión.
2. Implementar API de recetas y planificación.
3. Implementar generación/edición de shopping list.
4. Montar frontend MVP.
5. Completar IaC y pipeline CI/CD.
