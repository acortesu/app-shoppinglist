# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Active plan

Full plan at `~/.claude/plans/ok-antes-de-comenzar-zesty-bee.md`. Each block is one PR; each numbered atomic task below is one commit with its own test delta, green locally (`./gradlew --no-daemon test`) before the next task. End-of-block: `./scripts/smoke-backend-e2e.sh` + full test suite.

Order: 0 → 1 → 2 → 3 → 4 → 4b → 5 → 6 → 6b → 6c → 8 → 7.

### Block 0 — Foundations
- **0.1** ~~Commit Gradle wrapper~~ — already committed at 8.10.2 (pre-existing, verified `./gradlew --version`).
- **0.2** Update all call sites to `./gradlew`: `.github/workflows/backend-ci.yml`, `docs/runbook-prod-local.md`, `docs/backend-mvp-dod.md`, `scripts/smoke-backend-e2e.sh`, `CLAUDE.md`.
- **0.3** Consolidate duplicate `Unit` enum: keep `com.appcompras.domain.Unit`, delete `com.appcompras.recipe.Unit`, fix all imports.
- **0.4** Translate Spanish → English in dev-facing docs: `docs/architecture.md`, `docs/backend-mvp-dod.md`, `docs/runbook-prod-local.md`, `docs/domain-model.md`, `frontend/README.md`, inline Java comments. Leave `seed/*.json` product data in Spanish.

### Block 1 — Conversion engine upgrade
- **1.1** Extend ingredient seed JSON schema with optional `densityRules` block; load in `IngredientCatalogService`; expose via `IngredientCatalogItem`.
- **1.2** Delete hardcoded `ingredientSpecificToBaseFactors` in `UnitConversionService.java:15-18`; `fromSpecificRule` reads from catalog item.
- **1.3** Seed density rules for common CR-market WEIGHT ingredients (flour, sugar, butter, oil-by-weight, honey, rice variants). Preserve rice/salt rules during migration.
- **1.4** De-duplicate package conversion: replace `ShoppingListService.toBaseForPackage` (L71–91) with a `UnitConversionService` call using package-only policy (reject CUP/TBSP/TSP/PINCH for packages).
- **1.5** Guard `packageBaseAmount == 0` at `ShoppingListService.java:55` (set `suggestedPackages = 0` + warn-log). Documents invariant even though `baseAmount <= 0` filter at L35 makes it unreachable.
- **1.6** Round aggregated base amounts to 1 decimal at service boundary.
- **1.7** Write `UnitConversionServiceTest`: full matrix `MeasurementType × Unit × {happy, edge}` (zero qty, TO_TASTE short-circuit, disallowed-unit, missing density rule, cross-type rejection).
- **1.8** Write `ShoppingListServiceTest`: empty list, single recipe, two-recipe mixed-unit aggregation (grams + cups), TO_TASTE filtered, floating-point stability (3×0.3 tsp), `suggestedPackages` ceiling, unknown ingredient throws.
- **1.9** Write `IngredientSeedDensityRulesTest`: seed round-trips density rules; every WEIGHT ingredient that allows CUP/TBSP/TSP has a density rule (invariant test).

### Block 2 — Service-layer unit tests
- **2.1** `RecipeServiceTest`: `toValidatedIngredient` (catalog + custom paths, unit allowance), `incrementUsageCount`, CRUD with repo mocked.
- **2.2** `MealPlanServiceTest`: `buildPlan` validation (date range, duplicate slots, unknown recipe), `applyUsageDelta` (inc/dec), `endDateFor` (WEEK +6d, FORTNIGHT +13d).
- **2.3** `ShoppingListDraftServiceTest`: each of 7 `validateItems` rules in isolation, `normalizeIdempotencyKey` (null/empty/whitespace), `DataIntegrityViolationException` retry path.
- **2.4** `IngredientCatalogServiceTest`: seed-vs-custom precedence, `search` ranking, duplicate-name conflict, alias normalization (case/accent).

### Block 3 — Repository + migration tests
- **3.1** `@DataJpaTest` for `MealPlanRepository`: user-id filtering, ordering, find-by-key.
- **3.2** `@DataJpaTest` for `ShoppingListDraftRepository`: same coverage.
- **3.3** `@DataJpaTest` for `IngredientCustomRepository`: same coverage.
- **3.4** Expand `RecipeRepositoryTest` with deleted-user + case-ordering scenarios.
- **3.5** Constraint test: V7 unique `(user_id, plan_id, idempotency_key)` rejects duplicates.
- **3.6** Constraint test: NOT NULL columns throw on missing data.
- **3.7** `FlywaySchemaHistoryTest` (integration-postgres job): asserts `flyway_schema_history` has exactly N rows — guards against silent migration edits.

### Block 4 — Security & cross-user isolation
- **4.1** Expand `OwnershipIsolationTest`: User B `DELETE /api/recipes/{id-of-A}` → 404.
- **4.2** Same for `PUT /api/plans/{id-of-A}` and `PUT /api/shopping-lists/{id-of-A}` → 404 (not 403 — avoid id enumeration).
- **4.3** New `JwtValidationTest`: expired / wrong-aud / wrong-iss / unsigned / malformed → 401. Stub `JwtDecoder`.
- **4.4** CORS rejection test: `Origin: https://evil.example.com` preflight blocked; allowed origin from env succeeds.
- **4.5** Document CORS env-var contract in `docs/runbook-prod-local.md` + `CLAUDE.md`.

### Block 4b — OIDC federation (AWS + GCP)
- **4b.1** Terraform: `aws_iam_openid_connect_provider` for GitHub.
- **4b.2** Terraform: deploy role `${project}-${env}-gh-actions-deploy` with trust scoped to `repo:<owner>/appCompras:ref:refs/heads/master`; narrow policy (ECR, ECS, CodeDeploy, TF-state, Secrets Manager, S3/CloudFront for frontend).
- **4b.3** Update `frontend-deploy-shopping.yml` + reference-stack workflows to `role-to-assume`; add `permissions: id-token: write`; remove static-key inputs.
- **4b.4** Delete `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` GH secrets.
- **4b.5** GCP: Workload Identity Pool + Provider trusting GitHub OIDC, attribute-mapped to `google.subject` + `attribute.repository`.
- **4b.6** GCP: service account `gh-actions-deploy@${project}.iam.gserviceaccount.com` with `roles/run.admin`, `iam.serviceAccountUser`, `artifactregistry.writer`, `secretmanager.secretAccessor`.
- **4b.7** Bind pool principal to SA; configure workflows with `google-github-actions/auth@v2`.
- **4b.8** Document pool ID + provider + SA email in `docs/runbook-prod-local.md`. Revoked-key exercise: rerun deploys with no static secrets.

### Block 5 — API contract & error handling
- **5.1** Create `ApiErrorCode` enum; migrate all `BusinessRuleException` call sites from string literals to enum.
- **5.2** Contract test: reflection-based check that every `BusinessRuleException` uses a code from the enum.
- **5.3** `RestExceptionHandlerTest`: each exception type → expected `ApiError` shape + `code` (`BusinessRuleException`, `MethodArgumentNotValidException`, `HttpMessageNotReadableException`, `ResponseStatusException`, catch-all).
- **5.4** `RequestLoggingFilter`: ensure `requestId` is echoed in `X-Request-Id` response header; test MDC propagation.
- **5.5** `ApiVersionFilterTest`: edge cases (empty header, malformed, v2 fallback).

### Block 6 — CI / infra polish
- **6.1** CI workflows switch to `./gradlew` (depends on Block 0).
- **6.2** Add JaCoCo plugin to `backend/build.gradle.kts`; publish HTML report as CI artifact (no gate).
- **6.3** Split `backend-unit` CI job into `backend-unit-fast` (pure unit) + `backend-unit-spring` (`@SpringBootTest` / `@DataJpaTest`).
- **6.4** `.gitignore` housekeeping: confirm `.gradle/`, `build/` not committed; remove if present.

### Block 6b — Observability
- **6b.1** Add `spring-cloud-gcp-starter-trace` to `backend/build.gradle.kts`; new `ObservabilityConfig` consolidating Micrometer common tags.
- **6b.2** Cloud Run: Terraform `infra/envs/prod/gcp/monitoring.tf` — Cloud Monitoring dashboard (req count, p95/p99 latency, instance count, CPU, cold-start count, Supabase conn errors).
- **6b.3** Cloud Run: alert policies (5xx rate, p95 > 1s sustained, post-deploy error rate > 5%) + email notification channel.
- **6b.4** AWS reference: X-Ray tracing (deps + config + sidecar daemon in task def); task role gets `AWSXRayDaemonWriteAccess`.
- **6b.5** AWS reference: `infra/envs/aws-reference/runtime/dashboard.tf` (ALB 5xx, p95, ECS CPU/memory, app req count).
- **6b.6** AWS reference: `alarms.tf` (ALB 5xx, ECS CPU > 80%) + SNS → email.
- **6b.7** Grafana Cloud: create workspace on free tier; Cloud Run scraper (grafana-agent as Cloud Run Job, remote-write); AWS CloudWatch data-source plugin.
- **6b.8** Grafana Cloud: one unified "appCompras — dual deployment" dashboard.
- **6b.9** Write `docs/slo.md` (p95 < 500ms, availability target, error budget) + `docs/observability.md`.

### Block 6c — Blue/green deploys
- **6c.1** Cloud Run: `backend-deploy.yml` deploys with `--no-traffic`, smoke-tests revision URL, then shifts 10% → 50% → 100% with sleep + alert-check.
- **6c.2** Cloud Run: auto-rollback step — if alert fires in 10 min window, `gcloud run services update-traffic --to-revisions <prev>=100`.
- **6c.3** Cloud Run: retain last 5 revisions for one-command rollback.
- **6c.4** AWS reference: Terraform switches `deployment_controller` ECS → `CODE_DEPLOY`; adds `-tg-green` target group.
- **6c.5** AWS reference: `codedeploy.tf` — app + deployment group (`ECSLinear10PercentEvery3Minutes`), auto-rollback on alarms from 6b, 10-min bake-in.
- **6c.6** AWS reference: `appspec.tmpl.yaml` + workflow step running `aws deploy create-deployment` post-apply.
- **6c.7** Verification: deploy deliberately broken image → confirm auto-rollback; deploy good image → observe linear shift.

### Block 8 — H3 migration (cutover) — 4 PRs

**Part 1 — Supabase**
- **8.1.1** Manually create Supabase project (`appcompras-prod`, closest region to us-east-1).
- **8.1.2** Verify Flyway V1–V7 applies cleanly against fresh Supabase (local boot with overridden DSN).
- **8.1.3** Decide pooler: Supavisor transaction pooler (port 6543). Document in `CLAUDE.md` + runbook.
- **8.1.4** Store Supabase DSN in GCP Secret Manager (Cloud Run) + AWS Secrets Manager (repurpose `${name_prefix}/db/credentials`).
- **8.1.5** Update `docker-compose.yml` — local dev still uses Postgres in Docker (Supabase not required offline).
- **8.1.6** Update `CLAUDE.md`, `docs/runbook-prod-local.md`, `README.md`.

**Part 2 — Move AWS runtime to reference path**
- **8.2.1** `git mv infra/envs/prod/runtime infra/envs/aws-reference/runtime`.
- **8.2.2** Update TF backend key `env/prod/runtime.tfstate` → `env/aws-reference/runtime.tfstate`; run `terraform init -migrate-state`.
- **8.2.3** Rename `prod.auto.tfvars` → `aws-reference.auto.tfvars`; set `env = "aws-reference"`; switch `api_domain` to `api-ref.acortesdev.xyz`.
- **8.2.4** Remove from Terraform: `aws_db_instance`, `aws_db_subnet_group`, `random_password.db`, `aws_security_group.rds`, private subnets + route table. Keep `aws_secretsmanager_secret.db` repurposed.
- **8.2.5** Update all path references in docs + workflows.
- **8.2.6** Delete `.github/workflows/prod-start.yml` + `prod-stop.yml`.

**Part 3 — Cloud Run primary**
- **8.3.1** New `infra/envs/prod/gcp/main.tf` + project-services enablement (run, artifactregistry, secretmanager, cloudtrace, monitoring, iam, iamcredentials).
- **8.3.2** `artifact_registry.tf` — Docker repo `appcompras-backend` in `us-east1`.
- **8.3.3** `secrets.tf` — Supabase DSN + Google OAuth client ID in Secret Manager.
- **8.3.4** `iam_wif.tf` — WIF pool/provider + deploy SA (coordinated with 4b).
- **8.3.5** `run_service.tf` — `google_cloud_run_v2_service` with `min_instance_count=0`, `max_instance_count=3`, `startup_cpu_boost=true`, `cpu=1`, `memory=512Mi`, secrets mounted as env, probe `/actuator/health`.
- **8.3.6** `google_cloud_run_domain_mapping` for `api.acortesdev.xyz`; Route53 CNAME → `ghs.googlehosted.com` + one-time TXT ownership proof.
- **8.3.7** Update `backend/Dockerfile` — AOT + JVM startup flags (`-XX:+UseSerialGC -Xshare:auto -XX:TieredStopAtLevel=1`).
- **8.3.8** New `.github/workflows/backend-deploy.yml`: WIF auth, `buildx` amd64, push to AR, `gcloud run deploy --no-traffic`, smoke, gradual shift (uses 6c.1).
- **8.3.9** Delete obsolete `.github/workflows/backend.yml`.
- **8.3.10** Verification: `curl https://api.acortesdev.xyz/actuator/health` → 200; log cold-start timing in `docs/slo.md`.

**Part 4 — On-demand AWS reference lifecycle**
- **8.4.1** New `.github/workflows/aws-reference-destroy.yml` — manual `workflow_dispatch` running `terraform destroy -auto-approve`.
- **8.4.2** New `.github/workflows/aws-reference-recreate.yml` — manual, accepts `tag` input, runs `terraform apply`, polls ALB health, external `/actuator/health` check, emits URL + time-to-recreate.
- **8.4.3** Write `docs/prod-lifecycle.md`: primary always-on, reference destroyed by default, RTO < 20 min, RPO zero (Supabase independent), when to recreate.
- **8.4.4** Run destroy → recreate end-to-end once; log timing in `docs/prod-lifecycle.md`.

### Block 7 — Frontend rewrite (separate planning cycle)

Gets its own plan at rewrite time. Scope: TS + Vite + React Query + React Hook Form + feature folders (`features/recipes|planner|shopping|auth`) + i18n (`es-CR` default, `en` for demos) + Vitest + MSW. Backend contract preserved verbatim; `/shopping-app/` base path preserved.

### Deferred (beyond this plan)

`experiments/apprunner` branch · `experiments/eks` branch · chaos engineering (AWS FIS) · load testing (k6) · OpenAPI → TypeScript codegen.

## Architectural decisions (H3 dual deployment)

- **Primary prod (runs 24/7)**: GCP **Cloud Run** (backend) + **Supabase** (Postgres) + **AWS S3 + CloudFront** (frontend). Target cost ~$0.80/mo; scale-to-zero between requests; JVM cold-start 4–8s mitigated with AOT + CPU boost + startup JVM flags.
- **Reference stack (destroyed by default, recreated for demos)**: the existing AWS ECS + ALB + Fargate stack lives at `infra/envs/aws-reference/runtime/` (post-Block 8 rename). Destroy via `aws-reference-destroy.yml`; recreate via `aws-reference-recreate.yml`. RTO < 20 min. Domain: `api-ref.acortesdev.xyz` (coexists with primary at `api.acortesdev.xyz`).
- **Portfolio narrative**: production runs hybrid for cost; full AWS enterprise stack is reproducible on demand.
- **CI auth**: keyless via OIDC — AWS IAM Role + GCP Workload Identity Federation. No static keys in GH secrets (post-Block 4b).
- **Observability**: Cloud Run native (Cloud Logging/Trace/Monitoring) for primary + CloudWatch + X-Ray for reference + one unified Grafana Cloud free-tier dashboard.
- **Blue/green**: Cloud Run native traffic split (`--no-traffic` + gradual shift) for primary; CodeDeploy-orchestrated blue/green for reference stack.

## Portfolio domain layout

`https://www.acortesdev.xyz/` is an umbrella portfolio with path-based subpages — one CloudFront distribution / S3 bucket serves multiple projects side by side:

- `/shopping-app/` — this repo's frontend (live).
- `/wordle/` — separate repo (live).
- `/` (root) — reserved for the user's personal info page (not built yet).

Implications for this repo:
- `frontend/vite.config.js` hardcodes `base: '/shopping-app/'` in prod because of this layout — preserve it in Block 7.
- CloudFront invalidations in `frontend-deploy-shopping.yml` are scoped to `/shopping-app/*`; never invalidate `/*` (would bust sibling projects' caches).
- S3 sync uses `--delete` but is scoped to the `/shopping-app` prefix for the same reason.
- The app is live but has zero real users — downtime during Block 8 migration is acceptable; no zero-downtime cutover needed.

## Language policy

English for all dev-facing artifacts (code, identifiers, comments, `README.md`, `CLAUDE.md`, `docs/*.md`, commit messages). Spanish is fine where it's product data for the Costa Rican user (ingredient seed labels, future UI copy).

## Repository layout

- `backend/` – Spring Boot 3.3 / Java 17 REST API (Gradle Kotlin DSL)
- `frontend/` – React 18 + Vite SPA
- `infra/envs/prod/runtime/` – Terraform for ECS/RDS/ALB/ECR prod runtime (moves to `infra/envs/aws-reference/runtime/` in Block 8; new `infra/envs/prod/gcp/` root added for Cloud Run)
- `scripts/` – local dev helpers (`dev-up.sh`, `dev-down.sh`, `smoke-backend-e2e.sh`)
- `docs/` – architecture, domain model, runbook, product notes
- `docker-compose.yml` – Postgres (always) + backend (profile `app`)
- `.env` / `.env.example` – single env file consumed by both postgres and backend services

## Common commands

### Local dev

```bash
./scripts/dev-up.sh        # just postgres
./scripts/dev-up.sh app    # postgres + backend container
./scripts/dev-down.sh
```

Frontend dev server (proxies `/api`, `/actuator`, `/v3/api-docs`, `/swagger-ui` to `localhost:8080`):

```bash
cd frontend && npm install && npm run dev   # http://localhost:5173
```

### Backend (Gradle wrapper committed, pinned to 8.10.2)

```bash
cd backend
./gradlew --no-daemon test                                             # all unit/app tests (H2)
./gradlew --no-daemon test --tests com.appcompras.security.ApiSecurityAuthTest   # single test
./gradlew --no-daemon test --tests "com.appcompras.integration.PostgresE2EFlowTest"  # Testcontainers Postgres
./gradlew --no-daemon bootRun                                          # run API on :8080
./gradlew --no-daemon bootJar                                          # build jar
```

Unit tests run against H2 in PostgreSQL-compat mode (`src/test/resources/application.yml`); `integration.PostgresE2EFlowTest` spins a real Postgres via Testcontainers. CI splits these into two jobs (`backend-unit` + `backend-integration-postgres`) — keep that split in mind when adding tests.

### Frontend

```bash
cd frontend
npm run dev       # vite dev server :5173 with backend proxy
npm run build     # production build (base path /shopping-app/)
npm run test      # vitest run (jsdom)
```

The production build uses `base: '/shopping-app/'` (see `vite.config.js`), because the SPA is served under `https://www.acortesdev.xyz/shopping-app/`. Dev mode uses `/`.

### End-to-end smoke test

```bash
./scripts/smoke-backend-e2e.sh   # requires docker, curl, jq; uses backend/gradlew
```

Starts postgres via compose, runs `bootRun` with auth disabled on port 8088, exercises recipe → plan → shopping-list CRUD, and cleans up. Quickest way to verify backend wiring before a deploy.

## Architecture

### Backend — modular monolith by feature

Code under `backend/src/main/java/com/appcompras/` is split by bounded context, not by technical layer. Each feature package owns its controller, service, JPA entity, mapper, repository, DTOs, and enums:

- `recipe/` – recipes CRUD (normalized ingredient references, validated units)
- `planning/` – meal plans (weekly/fortnightly slots referencing recipe IDs)
- `shopping/` – shopping-list drafts generated from a plan, editable per-item
- `ingredient/` – catalog + user-created custom ingredients
- `service/` – cross-feature services (`IngredientCatalogService`, `ShoppingListService`, `UnitConversionService`, custom-ingredient repository)
- `domain/` – shared value objects / enums used across features (`Unit`, `MealType`, `MeasurementType`, `IngredientCatalogItem`)
- `security/` – Google-ID-token JWT resource server + per-user ownership (`CurrentUserProvider`)
- `config/` – CORS, API versioning filter, request logging, standardized `ApiError` + `RestExceptionHandler`, OpenAPI

When adding behavior, prefer extending the relevant feature package. Only promote to `service/` or `domain/` when logic is genuinely shared across contexts (e.g., unit conversion, ingredient catalog lookup).

Note: `com.appcompras.recipe.Unit` is a duplicate of `com.appcompras.domain.Unit` — Block 0 deletes the recipe-package copy. Do not import the duplicate in new code.

### Error contract and API versioning

- All error responses use `config/ApiError` with a stable `code` field (e.g., `INGREDIENT_NOT_FOUND`, `PLAN_SLOT_OUT_OF_RANGE`, `VALIDATION_ERROR`). The frontend maps these codes to user-facing messages per tab (`frontend/src/App.jsx` — search `API_ERROR_MESSAGES`). Block 5 consolidates these strings into an `ApiErrorCode` enum.
- `ApiVersionFilter` enforces a non-breaking `X-API-Version: 1` header; the frontend sets it in `frontend/src/api.js`. Breaking changes must bump the version, not mutate V1.
- Shopping-list generation supports `Idempotency-Key` — retries with the same key must return the same draft. Migration `V7` enforces uniqueness via `(user_id, plan_id, idempotency_key)` index.

### Security model

- Production requires auth (`APP_SECURITY_REQUIRE_AUTH=true`). The resource server validates Google-issued ID tokens (`issuer = https://accounts.google.com`, audience = `GOOGLE_CLIENT_ID`). See `security/SecurityConfig.java`.
- All `/api/**` endpoints require auth; `/actuator/health`, `/actuator/info`, swagger, and `OPTIONS` are public.
- Per-user data isolation is enforced at the JPA layer via `user_id` columns (migration `V5__add_user_ownership.sql`). New entities that hold user data MUST include `user_id` and filter on it in repository queries — don't rely on the controller to do it.
- **CORS**: Controlled by `APP_CORS_ALLOWED_ORIGINS` env-var (comma-separated list). Only origins in the list are allowed to make cross-origin requests; preflight `OPTIONS` for disallowed origins returns `403 Forbidden`. Local dev uses `http://localhost:5173` (Vite), prod uses `https://www.acortesdev.xyz`. See `docs/runbook-prod-local.md` for examples.
- For local dev without Google login, set `APP_SECURITY_REQUIRE_AUTH=false` in root `.env` and recreate the backend container. The smoke script does this automatically.

### Database

- PostgreSQL 16 in all environments. Schema is managed exclusively by Flyway (`backend/src/main/resources/db/migration/V*.sql`); `spring.jpa.hibernate.ddl-auto=validate` — Hibernate will refuse to start if entities drift from the schema. Always add a new `V<N>__*.sql` migration rather than editing an existing one.
- H2 in PostgreSQL-compat mode is used only for the fast unit-test job; integration tests and prod use real Postgres.
- Post-Block 8: prod Postgres lives in **Supabase** (free tier, Supavisor transaction pooler on port 6543). Local dev still uses Postgres in Docker.

### Frontend

- Single-file UI in `frontend/src/App.jsx` with three tabs (recipes / planner / shopping). `api.js` is the sole HTTP client — it adds `Authorization: Bearer <google_id_token>` from `localStorage['appcompras_id_token']`, sets `X-API-Version: 1`, and caches GETs for 15s with explicit invalidation on mutations. Don't fetch the backend from components directly; extend `api.js`.
- Production build is served from `s3://<bucket>/shopping-app/` behind CloudFront; the `/shopping-app/` base path is baked into the build, so local-dev URLs and prod URLs differ.
- Block 7 rewrites the frontend from scratch (TypeScript + React Query + feature folders + i18n). Backend contract must be preserved verbatim.

### Infrastructure / CI-CD

- **Primary prod** (post-Block 8): GCP Cloud Run, Terraform root at `infra/envs/prod/gcp/`. Deploy via `.github/workflows/backend-deploy.yml` on push to `master` — builds amd64 image, pushes to Artifact Registry, `gcloud run deploy --no-traffic`, smoke-tests the revision URL, then gradual traffic shift.
- **AWS reference stack** (post-Block 8): Terraform at `infra/envs/aws-reference/runtime/`, remote state in S3 + DynamoDB. Destroyed by default; manual workflows `aws-reference-destroy.yml` / `aws-reference-recreate.yml` control lifecycle. CodeDeploy blue/green with CloudWatch-alarm auto-rollback.
- **Frontend CD**: `frontend-deploy-shopping.yml` (build → `aws s3 sync` → CloudFront invalidation) — unchanged by the H3 migration.
- **CI**: `backend-ci.yml` (unit + integration Postgres tests); Block 6 splits `backend-unit` into fast/spring and adds JaCoCo.
