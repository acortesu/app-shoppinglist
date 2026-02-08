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
   - completar `GOOGLE_CLIENT_ID`
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

## Test de integración Postgres

- Test E2E con Postgres real (Testcontainers):
  - `cd backend && gradle --no-daemon test --tests com.appcompras.integration.PostgresE2EFlowTest`
- Requiere Docker disponible para el proceso de tests.

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

## CI

- Pipeline backend con tests en `.github/workflows/backend-ci.yml`.
- Corre en push/PR contra `master` y ejecuta `gradle --no-daemon test` en `backend`.

## Próximos hitos

1. Definir modelo de dominio backend y reglas de conversión.
2. Implementar API de recetas y planificación.
3. Implementar generación/edición de shopping list.
4. Montar frontend MVP.
5. Completar IaC y pipeline CI/CD.
