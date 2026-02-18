output "alb_dns_name" {
  value = aws_lb.this.dns_name
}

output "alb_arn" {
  value = aws_lb.this.arn
}

output "api_base_url" {
  value = "https://${var.api_domain_name}"
}

output "ecr_repository_url" {
  value = aws_ecr_repository.backend.repository_url
}

output "ecs_cluster_name" {
  value = aws_ecs_cluster.this.name
}

output "ecs_service_name" {
  value = aws_ecs_service.backend.name
}

output "task_definition_arn" {
  value = aws_ecs_task_definition.backend.arn
}

output "rds_identifier" {
  value = aws_db_instance.this.identifier
}

output "rds_endpoint" {
  value = aws_db_instance.this.address
}
