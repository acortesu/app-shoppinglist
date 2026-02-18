

# appCompras – DevOps Portfolio Project

This project is part of my DevOps portfolio and focuses on designing, deploying, and operating a real-world container-based cloud architecture using AWS, Terraform, and GitHub Actions.

The application is a personal shopping and meal planning app composed of:

- A React + Vite frontend deployed as a static website
- A Spring Boot backend deployed on ECS Fargate
- A PostgreSQL database hosted on AWS RDS

The primary goal of this project is learning and showcasing DevOps practices, infrastructure decisions, CI/CD workflows, and cost-aware cloud architecture — not just building the application.

---

## Architecture (Current State)

```text
User
 ↓
Browser
 ↓
CloudFront (HTTPS)
 ↓
AWS S3 (Static Hosting)
 ↓
React + Vite Frontend (/shopping-app)
 ↓
HTTPS (REST)
 ↓
Application Load Balancer (ALB)
 ↓
ECS Fargate (Spring Boot Container)
 ↓
AWS RDS (PostgreSQL)
```

---

## Architecture Diagram

See `docs/architecture.png` for the full infrastructure diagram.

---

## Tech Stack

### Frontend

- React 18
- Vite
- Tailwind CSS
- AWS S3 (static hosting)
- AWS CloudFront (CDN + HTTPS)
- Google OAuth (Authentication)

### Backend

- Java 17
- Spring Boot
- Docker
- AWS ECS (Fargate)
- AWS Application Load Balancer
- AWS RDS (PostgreSQL)

### Infrastructure / DevOps

- Terraform (Infrastructure as Code)
- AWS S3 (Terraform remote state)
- DynamoDB (state locking)
- GitHub Actions (CI/CD)
- AWS IAM (least-privilege access)
- ECS + RDS start/stop automation for cost optimization

---

## Key DevOps Decisions

### Static Frontend Deployment (S3 + CloudFront)

The frontend is deployed as a static SPA to:

- Reduce infrastructure cost
- Decouple frontend and backend deployments
- Improve global performance via CDN
- Simplify CI/CD

The frontend is served under:

```
https://www.acortesdev.xyz/shopping-app/
```

This follows a path-based routing strategy to host multiple projects under a single domain.

---

### Containerized Backend (ECS Fargate)

The backend runs as a Docker container on ECS Fargate:

- No EC2 management
- Managed scaling
- Clean separation between infrastructure and application
- Production-grade architecture (ALB + private subnets)

This approach was intentionally chosen over serverless to:

- Learn container orchestration
- Work with networking (VPC, subnets, security groups)
- Handle database-backed production workloads

---

### PostgreSQL on RDS (Persistent State)

Unlike serverless architectures, this app requires persistent data:

- Recipes
- Ingredients
- Plans
- Shopping lists

RDS provides:

- Managed backups
- Automated maintenance
- Encrypted storage
- Snapshot recovery capabilities

---

### Cost Optimization Strategy

This project is designed to remain under approximately $10/month.

To achieve this:

- ECS desired count can be set to 0
- RDS instance can be stopped
- GitHub Actions workflows automate:
  - `prod-stop`
  - `prod-start`

This allows production infrastructure to be turned on only when needed, while preserving database state.

---

## CI/CD Pipelines

### Backend CI Pipeline

Triggered on pull requests to `master`:

- Run unit tests
- Run integration tests (PostgreSQL)
- Validate application integrity

---

### Backend CD Pipeline (ECS + Terraform)

Triggered on push to `master`:

1. Build Docker image (ARM64)
2. Push image to Amazon ECR
3. Run Terraform
4. Update ECS task definition with new image tag
5. Deploy new revision automatically

Terraform state is stored remotely in S3 with DynamoDB locking.

---

### Frontend Pipeline

Triggered on changes to `/frontend`:

1. Install dependencies
2. Build production assets
3. Upload to S3 (`/shopping-app`)
4. Invalidate CloudFront cache

---

### Infrastructure Control Pipelines

Manual workflows:

- `prod-stop` → Stops ECS service and RDS
- `prod-start` → Starts RDS and ECS service

These workflows allow full infrastructure control without manual console access.

---

## Secrets Management

- AWS credentials stored in GitHub Environments (prod)
- No secrets committed to the repository
- Google OAuth Client ID injected at build time
- Database credentials stored securely in AWS Secrets Manager

IAM permissions follow least-privilege principles.

---

## Validation and Debugging

- ECS service health checks via ALB
- Spring Boot `/actuator/health`
- CloudWatch Logs for runtime debugging
- CORS validation and OAuth integration testing
- Terraform state validation and lock verification

Real-world issues solved:

- CORS misconfiguration
- IAM permission errors in CI
- Terraform backend migration
- ECS image architecture mismatch (ARM64 vs x86)
- Remote state duplication cleanup

---

## Live Deployment

- Frontend: https://www.acortesdev.xyz/shopping-app/
- Backend API: https://api.acortesdev.xyz
- HTTPS via ACM + ALB
- Containerized production deployment

---

## Current Status

- Frontend deployed via S3 + CloudFront
- Backend deployed via ECS Fargate
- RDS PostgreSQL integrated
- Remote Terraform state configured (S3 + DynamoDB)
- CI/CD fully operational
- Infrastructure start/stop automation implemented
- IAM least-privilege configured
- Branch protection enabled

---

## Learning Outcomes

- Designing production-grade VPC networking
- Managing container-based deployments
- Implementing Infrastructure as Code with Terraform
- Building safe CI/CD pipelines
- Rotating IAM credentials securely
- Remote state management and locking
- Cost-aware architecture design
- Production debugging and incident resolution

---

## Possible Next Enhancements

- Blue/Green deployments for ECS
- Observability improvements (metrics and alarms)
- Automated RDS snapshot workflow
- OIDC authentication for GitHub Actions (remove static keys)
- Horizontal scaling policies
- Load testing and performance profiling

---

## Environment Strategy

This project intentionally uses:

- Local environment (Docker + Postgres)
- Production environment (AWS)

A separate dev cloud environment was avoided to:

- Minimize AWS costs
- Keep architecture focused
- Maintain simplicity for a portfolio use case

This reflects real-world cost trade-offs in small-scale production systems.

---

## Versioning and Releases

This project follows Semantic Versioning (SemVer):

- MAJOR – Breaking changes
- MINOR – Backward-compatible features
- PATCH – Bug fixes

All production deployments are triggered from the protected `master` branch.