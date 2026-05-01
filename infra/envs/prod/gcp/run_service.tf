# Cloud Run Service for appCompras backend
resource "google_cloud_run_v2_service" "backend" {
  name     = local.service_name
  location = var.region

  template {
    service_account = google_service_account.deploy.email

    containers {
      image = "${var.region}-docker.pkg.dev/${var.project_id}/appcompras-backend/backend:${var.image_tag}"

      ports {
        container_port = 8080
      }

      # Environment variables (not sensitive)
      env {
        name  = "APP_SECURITY_REQUIRE_AUTH"
        value = "true"
      }

      env {
        name  = "APP_CORS_ALLOWED_ORIGINS"
        value = var.cors_allowed_origins
      }

      env {
        name  = "APP_ENV"
        value = "prod"
      }

      env {
        name  = "APP_VERSION"
        value = "0.1.0"
      }

      # Secrets (sensitive values from Secret Manager)
      env {
        name = "SPRING_DATASOURCE_URL"
        value_source {
          secret_key_ref {
            secret = google_secret_manager_secret.supabase_url.secret_id
            version = "latest"
          }
        }
      }

      env {
        name = "GOOGLE_CLIENT_ID"
        value_source {
          secret_key_ref {
            secret = google_secret_manager_secret.google_client_id.secret_id
            version = "latest"
          }
        }
      }

      # Startup probes (for slow-starting apps)
      startup_probe {
        http_get {
          path = "/actuator/health"
          port = 8080
        }
        failure_threshold = 10
        period_seconds    = 10
        timeout_seconds   = 5
      }

      # Liveness probe
      liveness_probe {
        http_get {
          path = "/actuator/health"
          port = 8080
        }
        failure_threshold = 3
        period_seconds    = 30
        timeout_seconds   = 5
      }

      resources {
        limits = {
          cpu    = "1"
          memory = "512Mi"
        }
      }
    }

    # Scaling
    scaling {
      min_instance_count = var.min_instances
      max_instance_count = var.max_instances
    }

    # Performance optimizations
    timeout          = "3600s"
    startup_cpu_boost = true
    service_account = google_service_account.deploy.email
  }

  # Allow unauthenticated invocations (frontend will call this)
  depends_on = [
    google_project_service.required["run.googleapis.com"],
    google_secret_manager_secret_iam_member.supabase_url,
    google_secret_manager_secret_iam_member.google_client_id,
  ]
}

# IAM binding: allow public (unauthenticated) access
resource "google_cloud_run_service_iam_member" "public_invocation" {
  service  = google_cloud_run_v2_service.backend.name
  location = google_cloud_run_v2_service.backend.location
  role     = "roles/run.invoker"
  member   = "allUsers"
}

# Domain mapping for api.acortesdev.xyz
resource "google_cloud_run_domain_mapping" "backend" {
  name     = "api.acortesdev.xyz"
  location = var.region
  service_name = google_cloud_run_v2_service.backend.name
}
