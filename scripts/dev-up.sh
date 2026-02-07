#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

MODE="${1:-db}"

if [[ "$MODE" == "app" ]]; then
  docker compose --profile app up -d postgres backend
else
  docker compose up -d postgres
fi

docker compose ps
