terraform {
  required_version = ">= 1.8"

  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 5.0"
    }
  }

  backend "gcs" {
    bucket = "appcompras-tf-state"
    prefix = "env/prod/gcp"
  }
}

provider "google" {
  project = var.project_id
  region  = var.region
}
