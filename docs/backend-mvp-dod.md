# Backend MVP Definition of Done

Target state for the MVP backend to be considered ready for frontend and demo.

## 1) API and contract

- Base endpoints working and stable:
  - `recipes`: CRUD.
  - `plans`: CRUD.
  - `shopping-lists`: generate/get/list/update/delete.
  - `ingredients`: list/search + custom.
- Persistence in PostgreSQL with Flyway (migrations applied).
- Standardized error payload (`ApiError`) with a consistent `code`.
- Non-breaking API versioning via optional `X-API-Version` header.

## 2) Business rules

- Ingredient normalization (case/accent/alias) active on create/update.
- Per-ingredient allowed-unit validation.
- Per-user (`user_id`) isolation for recipes, plans and drafts.
- Editable shopping draft with:
  - `bought` (done/not done)
  - `note` per item
  - manual `sortOrder`
- Idempotent shopping-draft generation via `Idempotency-Key`.

## 3) Quality and tests

- Local backend tests green:
  - `cd backend && ./gradlew --no-daemon test`
- Real-Postgres integration test (Testcontainers) green:
  - `cd backend && ./gradlew --no-daemon test --tests com.appcompras.integration.PostgresE2EFlowTest`
- Local E2E smoke script green:
  - `./scripts/smoke-backend-e2e.sh`

## 4) CI and merge gates

- Workflow `.github/workflows/backend-ci.yml` with two jobs:
  - `backend-unit`
  - `backend-integration-postgres`
- GitHub branch protection (manual):
  - `Settings > Branches > Branch protection rules > master`
  - enable:
    - Require a pull request before merging
    - Require status checks to pass before merging
    - required status checks:
      - `backend-unit`
      - `backend-integration-postgres`

## 5) Minimum local operation

- `.env` configured at repo root.
- `docker compose up -d postgres` works.
- Backend boots against Postgres and exposes:
  - `/swagger-ui.html`
  - `/v3/api-docs`
  - `/actuator/health`

## 6) Minimum documentation up to date

- README updated with:
  - local setup
  - security/JWT
  - API versioning
  - troubleshooting
  - seed catalog versioning policy
