# Arquitectura objetivo

## Frontend
- React + Vite
- App SPA
- Deploy estático en S3 + CloudFront

## Backend
- Java 17
- Spring Boot REST API
- Dockerized, stateless
- Lógica de negocio desacoplada de infraestructura

## Database
- PostgreSQL en RDS
- Una base por entorno

## Auth
- AWS Cognito (User Pool + JWT)

## Infra (Terraform)
Recursos:
- VPC, subnets, security groups
- ECS cluster/service (Fargate)
- ALB
- RDS
- IAM roles
- ECR
- S3
- CloudFront

Entornos:
- dev
- prod

## CI/CD conceptual
1. PR: lint + tests
2. Merge: build Docker
3. Push a ECR
4. Terraform plan/apply
5. ECS rolling deploy
