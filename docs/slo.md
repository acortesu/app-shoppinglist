# Service-Level Objectives (SLOs)

This document defines the performance and reliability targets for appCompras.

## Service-Level Indicator (SLI) Definitions

### Latency

**Definition:** Time elapsed from request arrival to response sent (end-to-end, including network)

**Measurement:** HTTP request duration captured by `RequestLoggingFilter` in `durationMs` field

**SLI target:** P95 latency < 500ms

- P50: < 100ms (typical fast request)
- P95: < 500ms (95% of requests complete in 500ms)
- P99: < 2s (99% of requests complete in 2s, acceptable for rare slow queries)

### Availability

**Definition:** Percentage of requests that receive a successful (2xx or 3xx) response

**Measurement:** HTTP status code from response; 5xx counts as unavailability

**SLI target:** 99.5% (99.5% of requests return 2xx/3xx)

- 99.5% availability = 0.5% error budget = ~3.6 hours downtime per month

### Error Rate

**Definition:** Percentage of requests that return a 5xx error (application or infrastructure failure)

**Measurement:** HTTP status >= 500

**Alert threshold:** 5xx rate > 1% for > 5 minutes

- 1% error rate = early warning (not yet SLO violation, but investigate)
- 5% error rate = SLO violation (post-deploy, triggers automatic rollback on Cloud Run)

## Error Budget

**Monthly availability target:** 99.5%

**Error budget per month:**
- 1 - 0.995 = 0.005 = 0.5%
- 0.5% of 30 days × 24 hours × 60 minutes = 216 minutes ≈ **3.6 hours**

**Interpretation:**
- The service can be unavailable (or degraded below SLI target) for up to 3.6 hours per month
- Once this budget is exhausted, all changes freeze (no new deployments unless critical fix)
- Budget resets monthly on the 1st

## SLO Compliance Calculation

SLO compliance is calculated as:

```
Compliance % = (Requests with status < 500) / (Total Requests) × 100
Target: >= 99.5%
```

### Example Calculation

In one month:
- Total requests: 10,000,000
- Requests with 5xx: 50,000
- Successful requests: 9,950,000
- Compliance: 9,950,000 / 10,000,000 = 99.5% ✓ (meets SLO)

If 5xx errors were 100,000:
- Compliance: 9,900,000 / 10,000,000 = 99.0% ✗ (violates SLO by 0.5%, has 216 min of error budget remaining)

## Alert Policy

### Immediate Alert (< 1 min response time)

**Condition:** 5xx error rate > 5% for > 2 minutes

**Action:** Auto-rollback deployment (Cloud Run) or page on-call (AWS reference)

**Rationale:** 5% error rate is a severe failure; indicates deployment issue or catastrophic resource exhaustion

### Warning Alert (investigate within 15 min)

**Condition:** 5xx error rate > 1% for > 5 minutes

**Action:** Email alert; on-call reviews logs/traces to understand issue

**Rationale:** 1% error rate is not yet SLO violation, but indicates a problem emerging

### Performance Alert (daily review)

**Condition:** P95 latency > 1s for > 10 minutes

**Action:** Email summary; on-call checks if trend is sustained or transient

**Rationale:** Sustained latency increase may indicate scaling needed or slow query introduced

## Post-Deployment Monitoring

During the first 15 minutes after a deployment:

- **Error rate** is monitored; if > 5%, automatic rollback triggers (Cloud Run only)
- **Latency** is monitored; if P95 > 2s, alert sent to on-call
- After 15 minutes, normal alert thresholds apply

This provides a "bake-in" window to catch bad deployments quickly.

## SLO Review Cadence

- **Weekly:** Check error budget consumption; identify trends
- **Monthly:** Assess whether SLO was met; post-mortems for any misses
- **Quarterly:** Review SLO targets; adjust if needed based on business requirements

## Related Documents

- `docs/observability.md` — How metrics/logs/traces are collected
- `CLAUDE.md` — Architecture and deployment strategy
