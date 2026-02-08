#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKEND_DIR="$ROOT_DIR/backend"
API_BASE_URL="${API_BASE_URL:-http://127.0.0.1:8088}"
SERVER_PORT="${SERVER_PORT:-8088}"
GRADLE_USER_HOME="${GRADLE_USER_HOME:-$ROOT_DIR/.gradle-user-home}"
GRADLE_BIN="${GRADLE_BIN:-gradle}"

if [[ ! -d "$BACKEND_DIR" ]]; then
  echo "backend directory not found at $BACKEND_DIR"
  exit 1
fi

if ! command -v curl >/dev/null 2>&1; then
  echo "curl is required"
  exit 1
fi

if ! command -v docker >/dev/null 2>&1; then
  echo "docker is required"
  exit 1
fi

if ! command -v jq >/dev/null 2>&1; then
  echo "jq is required"
  exit 1
fi

if ! command -v "$GRADLE_BIN" >/dev/null 2>&1; then
  echo "gradle is required (set GRADLE_BIN to custom path if needed)"
  exit 1
fi

BACKEND_LOG="$(mktemp /tmp/appcompras-backend-smoke.XXXXXX)"
BACKEND_PID=""

cleanup() {
  if [[ -n "$BACKEND_PID" ]] && kill -0 "$BACKEND_PID" >/dev/null 2>&1; then
    kill "$BACKEND_PID" >/dev/null 2>&1 || true
    wait "$BACKEND_PID" >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT

request() {
  local method="$1"
  local url="$2"
  local data="${3:-}"
  if [[ -n "$data" ]]; then
    curl -sS -X "$method" "$url" -H "Content-Type: application/json" -d "$data" -w $'\n%{http_code}'
  else
    curl -sS -X "$method" "$url" -w $'\n%{http_code}'
  fi
}

assert_status() {
  local actual="$1"
  local expected="$2"
  local body="$3"
  if [[ "$actual" != "$expected" ]]; then
    echo "expected HTTP $expected but got $actual"
    echo "response: $body"
    exit 1
  fi
}

echo "[1/7] Starting postgres with docker compose"
cd "$ROOT_DIR"
docker compose up -d postgres >/dev/null

echo "[2/7] Starting backend on port $SERVER_PORT (auth disabled for smoke)"
cd "$BACKEND_DIR"
APP_SECURITY_REQUIRE_AUTH=false SERVER_PORT="$SERVER_PORT" GRADLE_USER_HOME="$GRADLE_USER_HOME" "$GRADLE_BIN" --no-daemon bootRun >"$BACKEND_LOG" 2>&1 &
BACKEND_PID=$!

echo "[3/7] Waiting for backend health endpoint"
for _ in {1..60}; do
  if curl -sS "$API_BASE_URL/actuator/health" >/dev/null 2>&1; then
    break
  fi
  sleep 1
done
if ! curl -sS "$API_BASE_URL/actuator/health" >/dev/null 2>&1; then
  echo "backend did not become healthy in time"
  echo "log: $BACKEND_LOG"
  exit 1
fi

echo "[4/7] Creating recipe"
recipe_call="$(request POST "$API_BASE_URL/api/recipes" '{
  "name": "Smoke arroz con huevo",
  "type": "DINNER",
  "ingredients": [
    { "ingredientId": "rice", "quantity": 200, "unit": "GRAM" },
    { "ingredientId": "egg", "quantity": 2, "unit": "PIECE" }
  ],
  "notes": "smoke"
}')"
recipe_status="${recipe_call##*$'\n'}"
recipe_body="${recipe_call%$'\n'*}"
assert_status "$recipe_status" "201" "$recipe_body"
recipe_id="$(printf '%s' "$recipe_body" | jq -r '.id')"
if [[ -z "$recipe_id" ]]; then
  echo "failed to parse recipe id"
  echo "response: $recipe_body"
  exit 1
fi

echo "[5/7] Creating meal plan and generating shopping list"
plan_call="$(request POST "$API_BASE_URL/api/plans" "{
  \"startDate\": \"2026-02-09\",
  \"period\": \"WEEK\",
  \"slots\": [
    { \"date\": \"2026-02-10\", \"mealType\": \"DINNER\", \"recipeId\": \"$recipe_id\" }
  ]
}")"
plan_status="${plan_call##*$'\n'}"
plan_body="${plan_call%$'\n'*}"
assert_status "$plan_status" "201" "$plan_body"
plan_id="$(printf '%s' "$plan_body" | jq -r '.id')"
if [[ -z "$plan_id" ]]; then
  echo "failed to parse plan id"
  echo "response: $plan_body"
  exit 1
fi

draft_call="$(request POST "$API_BASE_URL/api/shopping-lists/generate?planId=$plan_id")"
draft_status="${draft_call##*$'\n'}"
draft_body="${draft_call%$'\n'*}"
assert_status "$draft_status" "200" "$draft_body"
draft_id="$(printf '%s' "$draft_body" | jq -r '.id')"
item_id="$(printf '%s' "$draft_body" | jq -r '.items[0].id')"
if [[ -z "$draft_id" || -z "$item_id" ]]; then
  echo "failed to parse shopping draft/id"
  echo "response: $draft_body"
  exit 1
fi

echo "[6/7] Updating shopping list, listing and reading by id"
update_call="$(request PUT "$API_BASE_URL/api/shopping-lists/$draft_id" "{
  \"items\": [
    {
      \"id\": \"$item_id\",
      \"ingredientId\": \"rice\",
      \"name\": \"Rice\",
      \"quantity\": 500,
      \"unit\": \"GRAM\",
      \"suggestedPackages\": 1,
      \"packageAmount\": 1,
      \"packageUnit\": \"KILOGRAM\",
      \"manual\": false,
      \"bought\": false,
      \"note\": \"smoke item\",
      \"sortOrder\": 0
    },
    {
      \"name\": \"Papel higienico\",
      \"quantity\": 2,
      \"unit\": \"pack\",
      \"manual\": true,
      \"bought\": false,
      \"sortOrder\": 1
    }
  ]
}")"
update_status="${update_call##*$'\n'}"
update_body="${update_call%$'\n'*}"
assert_status "$update_status" "200" "$update_body"

list_call="$(request GET "$API_BASE_URL/api/shopping-lists")"
list_status="${list_call##*$'\n'}"
list_body="${list_call%$'\n'*}"
assert_status "$list_status" "200" "$list_body"

get_call="$(request GET "$API_BASE_URL/api/shopping-lists/$draft_id")"
get_status="${get_call##*$'\n'}"
get_body="${get_call%$'\n'*}"
assert_status "$get_status" "200" "$get_body"

echo "[7/7] Deleting shopping list and verifying not found"
delete_call="$(request DELETE "$API_BASE_URL/api/shopping-lists/$draft_id")"
delete_status="${delete_call##*$'\n'}"
delete_body="${delete_call%$'\n'*}"
assert_status "$delete_status" "204" "$delete_body"

missing_call="$(request GET "$API_BASE_URL/api/shopping-lists/$draft_id")"
missing_status="${missing_call##*$'\n'}"
missing_body="${missing_call%$'\n'*}"
assert_status "$missing_status" "404" "$missing_body"

echo "Smoke E2E successful."
echo "Recipe: $recipe_id"
echo "Plan: $plan_id"
echo "Draft: $draft_id"
echo "Backend log: $BACKEND_LOG"
