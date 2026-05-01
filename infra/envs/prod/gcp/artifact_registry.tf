resource "google_artifact_registry_repository" "backend" {
  location      = var.region
  repository_id = "appcompras-backend"
  format        = "DOCKER"
  description   = "Backend container images for appCompras"

  depends_on = [google_project_service.required["artifactregistry.googleapis.com"]]
}
