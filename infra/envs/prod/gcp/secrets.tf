# Supabase DSN (user must populate post-apply)
resource "google_secret_manager_secret" "supabase_url" {
  secret_id = "appcompras-supabase-url"

  labels = {
    app    = "appcompras"
    secret = "supabase-dsn"
  }

  depends_on = [google_project_service.required["secretmanager.googleapis.com"]]
}

# Google Client ID for OAuth2
resource "google_secret_manager_secret" "google_client_id" {
  secret_id = "appcompras-google-client-id"

  labels = {
    app    = "appcompras"
    secret = "oauth-client-id"
  }

  depends_on = [google_project_service.required["secretmanager.googleapis.com"]]
}

# Grant Cloud Run service account access to secrets
resource "google_secret_manager_secret_iam_member" "supabase_url" {
  secret_id  = google_secret_manager_secret.supabase_url.id
  role       = "roles/secretmanager.secretAccessor"
  member     = "serviceAccount:${google_service_account.deploy.email}"
  depends_on = [google_service_account.deploy]
}

resource "google_secret_manager_secret_iam_member" "google_client_id" {
  secret_id  = google_secret_manager_secret.google_client_id.id
  role       = "roles/secretmanager.secretAccessor"
  member     = "serviceAccount:${google_service_account.deploy.email}"
  depends_on = [google_service_account.deploy]
}
