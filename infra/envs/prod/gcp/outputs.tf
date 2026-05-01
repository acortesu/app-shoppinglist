output "cloud_run_url" {
  value       = google_cloud_run_v2_service.backend.uri
  description = "Cloud Run service URL (*.run.app)"
}

output "artifact_registry_url" {
  value       = "${var.region}-docker.pkg.dev/${var.project_id}/appcompras-backend"
  description = "Artifact Registry Docker repository URL"
}

output "service_account_email" {
  value       = google_service_account.deploy.email
  description = "Service account email for GitHub Actions"
}

output "wif_provider_name" {
  value       = google_iam_workload_identity_pool_provider.github.name
  description = "WIF provider resource name (use in GitHub Actions auth)"
}

output "custom_domain" {
  value       = google_cloud_run_domain_mapping.backend.name
  description = "Custom domain mapping (api.acortesdev.xyz)"
}

output "project_id" {
  value = var.project_id
}

output "region" {
  value = var.region
}
