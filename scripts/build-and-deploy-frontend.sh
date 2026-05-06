#!/bin/bash
set -e

echo "🔨 Building frontend..."
cd frontend
npm run build
cd ..

echo ""
echo "📤 Deploying to Google Cloud Storage..."
./scripts/deploy-frontend-gcs.sh

echo ""
echo "✅ Build and deploy complete!"
