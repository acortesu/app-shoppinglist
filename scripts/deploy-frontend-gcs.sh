#!/bin/bash
set -e

# Deploy frontend to Google Cloud Storage with Cloud CDN
# Usage: ./scripts/deploy-frontend-gcs.sh

GCS_BUCKET="appcompras-frontend-prod"
GCP_PROJECT="menu-shopping-list"
FRONTEND_DIR="frontend/dist"

echo "=== Deploying frontend to Google Cloud Storage ==="
echo "Bucket: gs://$GCS_BUCKET"
echo "Project: $GCP_PROJECT"
echo ""

# Check if dist directory exists
if [ ! -d "$FRONTEND_DIR" ]; then
  echo "❌ Error: $FRONTEND_DIR not found"
  echo "Run 'cd frontend && npm run build' first"
  exit 1
fi

# Upload to GCS with cache control under shopping-app/ path
echo "📤 Uploading frontend files to GCS..."
gsutil -m -h "Cache-Control:public, max-age=3600" \
  cp -r "$FRONTEND_DIR"/* "gs://$GCS_BUCKET/shopping-app/" 2>&1 | tail -5

echo ""
echo "✓ Frontend deployed to GCS"
echo "📍 URL: https://www.acortesdev.xyz/shopping-app/"
echo ""
echo "Note: Cloud CDN is enabled, files are cached globally"
