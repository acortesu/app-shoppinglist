# Target architecture

## Frontend
- React + Vite
- SPA
- Static deploy on S3 + CloudFront

## Backend
- Java 17
- Spring Boot REST API
- Dockerized, stateless
- Business logic decoupled from infrastructure

## Database
- PostgreSQL on RDS
- One database per environment

## Auth
- AWS Cognito (User Pool + JWT)

## Infra (Terraform)
Resources:
- VPC, subnets, security groups
- ECS cluster/service (Fargate)
- ALB
- RDS
- IAM roles
- ECR
- S3
- CloudFront

Environments:
- dev
- prod

## CI/CD outline
1. PR: lint + tests
2. Merge: build Docker image
3. Push to ECR
4. Terraform plan/apply
5. ECS rolling deploy
