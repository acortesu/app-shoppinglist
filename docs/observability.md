# Observability Strategy

This document outlines the observability approach for appCompras across both primary (Cloud Run) and reference (AWS) deployments.

## Overview

Observability is built on three pillars:

1. **Metrics** — quantitative measurements (requests, latency, errors, resource usage)
2. **Logs** — structured event records with context (timestamps, requestId, user, operation)
3. **Traces** — distributed request flows across service boundaries (Cloud Trace on GCP, X-Ray on AWS)

All three are collected automatically by the application; no manual instrumentation required beyond the framework defaults.

## Metrics

### Exposure

Prometheus-format metrics are exposed at:

```
GET /actuator/prometheus
```

Available metrics include:

- **Request counts:** `http_server_requests_seconds_count` (labeled by method, path, status)
- **Latency:** `http_server_requests_seconds` (p50, p95, p99 via Micrometer's built-in percentiles)
- **JVM:** `jvm_memory_used_bytes`, `jvm_gc_memory_allocated_bytes`, `jvm_threads_live`
- **Connection pools:** `hikaricp_connections`, `hikaricp_connections_idle`

### Common Tags

All metrics include these tags for consistent filtering and grouping:

```
application: appcompras-backend
environment: {local|staging|prod}
version: 0.1.0
service: backend
```

Environment and version are configurable via application properties (`app.environment`, `app.version`) or environment variables. Common tags are registered in `com.appcompras.config.ObservabilityConfig`.

### Consumption

- **Cloud Run (Primary):** Prometheus metrics scraped by Cloud Monitoring dashboard (GCP-native)
- **AWS Reference:** CloudWatch agent scrapes `/actuator/prometheus` and publishes custom metrics
- **Grafana Cloud:** Unified dashboard queries both Cloud Monitoring and CloudWatch via data-source plugins

## Logs

### Log Format

Application logs are written to stdout in structured format:

```
2026-04-30T22:45:20.235-06:00 level=INFO logger=com.appcompras.config.RequestLoggingFilter msg="event=request method=GET path=/api/recipes status=200 durationMs=45 requestId=xyz-abc-123" requestId=xyz-abc-123 thread=http-nio-8080-exec-1
```

Each log line includes:

- **timestamp** — ISO 8601 format with timezone
- **level** — INFO, WARN, ERROR, etc.
- **logger** — fully qualified class name
- **msg** — human-readable message + structured key=value pairs
- **MDC context** — `requestId` automatically propagated by `RequestLoggingFilter`

### Request Context

The `RequestLoggingFilter` (in `com.appcompras.config.RequestLoggingFilter`) ensures every HTTP request has a unique `requestId`:

1. If client sends `X-Request-Id` header, it is used
2. Otherwise, a UUID is generated
3. The `requestId` is:
   - Added to the request context (for access in business logic)
   - Put into MDC (Mapped Diagnostic Context) for log propagation
   - Echoed in the response header `X-Request-Id`

This allows tracing a single user request across multiple logs, even across service boundaries if `X-Request-Id` is forwarded.

### Consumption

- **Cloud Run (Primary):** Logs streamed to Cloud Logging (GCP); queryable by `requestId`, status, duration
- **AWS Reference:** CloudWatch Logs; same query capability
- **Dashboards:** Grafana Cloud can query both Cloud Logging and CloudWatch Logs as data sources

## Traces

### Distributed Tracing

Cloud Trace (GCP) and X-Ray (AWS) automatically instrument Spring Boot applications when the respective starter libraries are included:

- **Cloud Run:** `spring-cloud-gcp-starter-trace` (included in `build.gradle.kts`)
- **AWS Reference:** `aws-xray-recorder-sdk-core` (to be added in Block 6b.4)

These libraries:
- Automatically instrument HTTP requests, database calls, and async operations
- Correlate traces with the `requestId` stored in MDC
- Visualize request flows (timing, latency breakdown, errors)

### Consumption

- **Cloud Run:** Cloud Trace console shows request timeline, latency breakdown, errors per span
- **AWS Reference:** X-Ray service map shows dependencies and error rates
- **Grafana Cloud:** Can integrate trace data via Tempo backend (optional, post-Block 8)

## Health & Status

### Health Endpoint

```
GET /actuator/health
```

Returns:

```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "livenessProbe": { "status": "UP" },
    "readinessProbe": { "status": "UP" }
  }
}
```

This endpoint is public (no auth required) and is used by:

- Load balancers for routing decisions (liveness/readiness probes)
- Incident response teams to verify service availability
- Uptime monitors (external health checks)

### Info Endpoint

```
GET /actuator/info
```

Returns application metadata (version, build time, git info if available):

```json
{
  "app": {
    "name": "appCompras",
    "version": "0.1.0"
  }
}
```

## Alerting

Alert policies are defined per deployment environment:

### Cloud Run (Primary) — Block 6b.3

- **5xx error rate > 1%:** indicates application or dependency failure
- **P95 latency > 1s (sustained):** indicates performance degradation or resource contention
- **Post-deploy error rate > 5%:** indicates a bad deployment; triggers automatic rollback

Alerts are sent via email to ops team.

### AWS Reference — Block 6b.6

- **ALB 5xx errors:** application returned 5xx status code
- **ECS CPU > 80%:** container nearing resource limit
- **RDS connection pool exhaustion:** not enough connections available

Alerts trigger incident response procedures.

## Dashboards

### Grafana Cloud — Block 6b.7–6b.8

One unified dashboard consolidates both deployments:

- **Primary (Cloud Run) panel:**
  - Request count (per endpoint)
  - P95 latency (last hour, last day)
  - Error rate (5xx)
  - Instance count (auto-scaled)
  - Cold-start count

- **Reference (AWS) panel:**
  - Request count (per endpoint)
  - P95 latency (last hour, last day)
  - Error rate (5xx)
  - ECS task count
  - RDS connection utilization

Each panel has alert status overlay — red highlight if threshold exceeded.

## Best Practices

### For Developers

1. **Add structured logging for domain events:**
   ```java
   log.info("event=recipe_created recipeId={} userId={} durationMs={}",
            recipeId, userId, duration);
   ```
   The `requestId` is automatically in MDC; no need to log it explicitly.

2. **Use standard HTTP status codes:**
   - 4xx for client errors (invalid input, not found, not authorized)
   - 5xx for server errors (unhandled exception, dependency failure)
   Errors automatically appear in metrics and alerts.

3. **Metrics are free:** Use `@Timed` on expensive operations or custom `MeterRegistry` calls for domain-specific counts. All metrics are collected and available.

4. **Do not log secrets:** `application.yml` masks sensitive values (passwords, tokens) in logs via Spring Boot's property name patterns.

### For Operations

1. **Correlate issues across logs and traces:**
   - Use `requestId` to find related log lines
   - Check Cloud Trace (GCP) or X-Ray (AWS) to see request path and latency breakdown

2. **Alert on SLO violations, not individual metrics:**
   - P95 latency > 500ms (SLO) is actionable; p99 > 2s is noise
   - See `docs/slo.md` for SLO definitions and error budgets

3. **Review dashboards during deployments:**
   - Watch error rate and latency during rollout
   - If alert fires, auto-rollback triggers immediately (Cloud Run) or manually (AWS reference)

## Related Documents

- `docs/slo.md` — Service-level objectives, error budget, incident thresholds
- `CLAUDE.md` — Architecture and security model (see "Observability" section)
- `.github/workflows/backend-ci.yml` — CI includes JaCoCo code coverage reports
