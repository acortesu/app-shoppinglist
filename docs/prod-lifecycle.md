# Production Lifecycle — Dual Deployment Model

appCompras runs on a hybrid infrastructure to balance cost, reliability, and operational flexibility:

- **Primary (always-on):** GCP Cloud Run — stateless, scales to zero when idle
- **Reference stack (on-demand):** AWS ECS + ALB — full enterprise stack, destroyed by default to save costs

This document explains the deployment model, lifecycle operations, and RTO/RPO characteristics.

---

## Architecture Overview

### Primary Prod (Cloud Run)

- **Compute:** GCP Cloud Run (fully managed, scales 0–3 instances)
- **Database:** Supabase (managed Postgres, free tier)
- **Storage:** AWS S3 + CloudFront (frontend, unchanged)
- **Cost:** ~$0.80/month (Cloud Run scales to zero; Supabase free tier)
- **Cold start:** 4–8 seconds (mitigated with AOT + JVM flags + CPU boost)
- **Domain:** `api.acortesdev.xyz`

### Reference Stack (AWS ECS)

- **Compute:** ECS Fargate (manages container orchestration, manual scaling)
- **Database:** Supabase (same managed Postgres)
- **Network:** VPC + public ALB + public subnets (no RDS-specific private networking)
- **Cost:** ~$50–100/month when running (varies with instance count)
- **Cold start:** ~30 seconds (JVM warmup + Fargate startup)
- **Domain:** `api-ref.acortesdev.xyz`
- **State:** **Destroyed by default** (manually recreated for demos/testing)

---

## Lifecycle Operations

### Destroy AWS Reference Stack

**When:** After demos, testing, or cost concerns. Saves ~$50–100/month.

**How:**

1. GitHub Actions → Workflows → `aws-reference-destroy.yml`
2. Click `Run workflow`
3. Input prompt: Type "destroy" to confirm
4. Workflow runs `terraform destroy -auto-approve`
5. All ECS, ALB, ECR resources removed (Supabase remains in GCP)

**Duration:** ~10–15 minutes

**Verification:**
```bash
aws elbv2 describe-load-balancers --region us-east-1
# Should return empty (no ALB)

aws ecs describe-clusters --region us-east-1
# Should return empty (no cluster)
```

### Recreate AWS Reference Stack

**When:** Before demos, to verify stack can be reconstructed, or for reference architecture comparison.

**How:**

1. Push a fresh Docker image tag to ECR (or use `latest`)
2. GitHub Actions → Workflows → `aws-reference-recreate.yml`
3. Click `Run workflow`
4. Input: `image_tag` (e.g., `20240430-2245-abc123f` or `latest`)
5. Workflow runs:
   - `terraform init + plan + apply` (creates VPC, ALB, ECS, etc.)
   - Waits for ALB to become healthy (~2–5 min)
   - Smoke tests `/actuator/health` endpoint
   - Outputs ALB URL and elapsed time

**Duration:** 15–25 minutes (Fargate startup + Flyway migrations on first run)

**Output:**

```
✅ AWS reference stack recreated successfully

📊 Details:
  API Base: http://appcompras-abc.elb.us-east-1.amazonaws.com
  Health check: http://appcompras-abc.elb.us-east-1.amazonaws.com/actuator/health
  Image tag: 20240430-2245-abc123f
  Elapsed time: 18m 30s
```

### Deploy to Primary (Cloud Run)

**When:** Push to `master` branch (automatic via `.github/workflows/backend-deploy.yml`)

**How:** (Automatic)

1. Code is pushed to `master`
2. CI workflow builds amd64 image
3. Image is pushed to GCP Artifact Registry
4. `backend-deploy.yml` triggers:
   - `gcloud run deploy --no-traffic` (zero-traffic revision)
   - Health check on revision URL
   - Gradual traffic shift: 10% → 50% → 100%
   - Auto-rollback on failure

**Duration:** 5–10 minutes (image build + deploy + traffic shift)

**Verification:**
```bash
curl https://api.acortesdev.xyz/actuator/health
# {"status":"UP",...}

gcloud run services describe appcompras-backend --region us-east1 --format='value(status.latestRevision)'
# Shows latest revision
```

---

## RTO & RPO

### Primary (Cloud Run)

- **RTO (Recovery Time Objective):** ~5–10 minutes (one push to master triggers automatic deployment)
- **RPO (Recovery Point Objective):** 0 (Supabase is managed, multi-AZ replication by default)
- **SLA:** Implicit (Cloud Run SLA is 99.95% uptime, per Google)
- **Failure scenarios:**
  - **Code bug:** Auto-rollback via health check failure
  - **Supabase outage:** Affects both primary and reference stacks (shared database)
  - **Cold start latency:** Mitigated but possible (4–8s on first request after idle)

### Reference Stack (AWS ECS)

- **RTO:** 15–25 minutes (manual workflow execution)
- **RPO:** 0 (same Supabase database)
- **SLA:** Manual (no automatic scale-up, no SLA guarantees)
- **Failure scenarios:**
  - **Destroyed state:** Must manually recreate (intended)
  - **Task failure:** Auto-recover via ECS service desired count (if running)
  - **ALB failure:** Terraform must be re-applied to replace

---

## Disaster Recovery Checklist

**Primary (Cloud Run) is down:**

1. Check Cloud Run service status: `gcloud run services describe appcompras-backend --region us-east1`
2. Check Cloud Run logs: `gcloud run services logs read appcompras-backend --limit 100`
3. Check Supabase status: Log in to supabase.com and verify database connectivity
4. If code issue: Fix code, push to master, `backend-deploy.yml` auto-deploys
5. If infrastructure issue: Check Terraform state, consider `terraform apply` if resources drifted

**Reference stack is needed but destroyed:**

1. Get latest image tag: `git log --oneline -1 backend/`
2. Run `aws-reference-recreate.yml` with that tag
3. Wait 15–25 minutes for stack to fully come up
4. Verify: `curl http://<ALB_DNS>/actuator/health`

**Supabase is down (affects both):**

1. Log in to supabase.com, check database status
2. If recoverable: Wait for Supabase to restore (they provide SLA)
3. If catastrophic: Restore from backup (Supabase has automated backups)
4. No code changes needed; both primary and reference will work once Supabase is back

---

## Cost Analysis

### Primary (Cloud Run)

- **Compute:** $0 (no request activity)–$0.25/mo (at 10 req/sec avg)
- **Data egress:** $0.12/GB (usually minimal for API)
- **Storage:** None
- **Supabase:** Free tier (5 GB, unlimited API calls)
- **Total:** **~$0.80/month** (with low traffic)

### Reference (ECS)

- **Fargate (if running):**
  - vCPU: 0.25 × 24h × 30d × $0.04102 ≈ $7.40/month
  - Memory: 0.512 GB × 24h × 30d × $0.00451 ≈ $5.25/month
  - ALB: ~$16–20/month
- **ECR:** ~$1–2/month (storage + bandwidth)
- **RDS:** Not used (moved to Supabase)
- **Supabase:** Free tier (same database as primary)
- **Total (when running):** **~$30–45/month**

**Cost savings by destroying reference:** $30–45/month

---

## Timeline: Typical Workflow

### Demo Day (reference stack needed)

```
Mon 09:00 — Push code to master
Mon 09:05 — Cloud Run deploy completes (api.acortesdev.xyz live)
Mon 10:00 — Run aws-reference-recreate.yml
Mon 10:30 — Reference stack up (api-ref.acortesdev.xyz live)
Mon 17:00 — Demo ends
Mon 17:15 — Run aws-reference-destroy.yml
Mon 17:30 — Reference stack destroyed, saving ~$50/month
Tue 09:00 — Cost savings apply to next invoice
```

---

## Monitoring

See `docs/observability.md` and `docs/slo.md` for:
- Metrics and dashboards (Cloud Monitoring + Grafana)
- SLO targets (P95 < 500ms, 99.5% availability)
- Alert policies (5xx > 1%, p95 > 1s)
- Cold-start timing (logged post-deployment)
