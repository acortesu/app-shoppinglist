variable "project_id" {
  type        = string
  description = "GCP project ID"
}

variable "region" {
  type        = string
  description = "GCP region for Cloud Run and Artifact Registry"
  default     = "us-east1"
}

variable "google_client_id" {
  type        = string
  description = "Google OAuth2 Web Client ID for authentication"
  sensitive   = true
}

variable "cors_allowed_origins" {
  type        = string
  description = "Comma-separated list of allowed CORS origins"
}

variable "image_tag" {
  type        = string
  description = "Backend Docker image tag (pushed to Artifact Registry)"
  default     = "latest"
}

variable "min_instances" {
  type        = number
  description = "Minimum number of Cloud Run instances (0 = scale to zero)"
  default     = 0
}

variable "max_instances" {
  type        = number
  description = "Maximum number of Cloud Run instances"
  default     = 3
}
