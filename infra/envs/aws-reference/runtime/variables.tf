variable "aws_region" { type = string }

variable "project" { type = string }
variable "env" { type = string }

variable "tags" {
  type    = map(string)
  default = {}
}

# Networking (sin NAT para ahorrar)
variable "vpc_cidr" { type = string }

variable "public_subnet_cidrs" {
  type = list(string)
}

# Backend (ECS)
variable "container_port" { type = number }
variable "desired_count" { type = number }
variable "cpu" { type = number }    # 256, 512, 1024...
variable "memory" { type = number } # 512, 1024, 2048...
variable "healthcheck_path" { type = string }

# App config / auth
variable "app_security_require_auth" { type = bool }
variable "google_client_id" {
  type      = string
  default   = ""
  sensitive = true
}

variable "cors_allowed_origins" {
  type = list(string)
}

# Domain
variable "api_domain_name" { type = string } # api.acortesdev.xyz

# DB (populated via AWS Secrets Manager, not generated)
variable "db_name" {
  type = string
  default = "appcompras"
}

variable "backend_image_tag" {
  type    = string
  default = "latest"
}

variable "api_domain" {
  type        = string
  description = "Public API domain (GoDaddy DNS), e.g. api.acortesdev.xyz"
}

variable "enable_https" {
  type        = bool
  description = "When true, creates the HTTPS (443) listener and redirects HTTP (80) to HTTPS. Use false until ACM cert is ISSUED."
  default     = false
}