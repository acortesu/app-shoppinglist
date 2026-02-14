variable "aws_region" { type = string }

variable "project" { type = string }
variable "env"     { type = string }

variable "tags" {
  type    = map(string)
  default = {}
}

# Networking (sin NAT para ahorrar)
variable "vpc_cidr" { type = string }

variable "public_subnet_cidrs" {
  type = list(string)
}

variable "private_subnet_cidrs" {
  type = list(string)
}

# Backend (ECS)
variable "container_port" { type = number }
variable "desired_count"  { type = number }
variable "cpu"            { type = number } # 256, 512, 1024...
variable "memory"         { type = number } # 512, 1024, 2048...
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

# DB
variable "db_name" { type = string }

variable "db_username" {
  type      = string
  sensitive = true
}

variable "db_instance_class" { type = string }
variable "db_allocated_storage_gb" { type = number }

# Snapshot strategy (Opción C)
variable "snapshot_id_parameter_name" { type = string } # /appcompras/prod/db/latestSnapshotId
variable "restore_from_snapshot" {
  type    = bool
  default = false
}

# Si restore_from_snapshot=true, este valor se puede setear (temporalmente) o lo leeremos de SSM luego
variable "snapshot_identifier" {
  type    = string
  default = ""
}

variable "backend_image_tag" {
  type    = string
  default = "latest"
}