locals {
  name_prefix = "${var.project}-${var.env}"
  common_tags = merge(var.tags, {
    Project = var.project
    Env     = var.env
  })
}

# Fase 1 (runtime) va a incluir aquí:
# - VPC + subnets + IGW + route tables (SIN NAT)
# - Security Groups (ALB, ECS, RDS)
# - ECR repo
# - ECS cluster + task definition + service (Fargate)
# - ALB + target group + listener
# - RDS Postgres (o restore desde snapshot)
# - (Más adelante) ACM cert + ALB listener 443 para api.acortesdev.xyz
#
# En Fase 0 solo dejamos el skeleton para que el repo ya tenga el “layout final”.