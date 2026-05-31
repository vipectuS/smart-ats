#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
BOOTSTRAP_SCRIPT="$ROOT_DIR/tools/maintenance/bootstrap_local_demo.sh"
STOP_SCRIPT="$ROOT_DIR/tools/maintenance/stop_local_demo.sh"
BACKEND_LOCAL_CONFIG="$ROOT_DIR/backend/local/backend-local.yml"
BACKEND_LOCAL_EXAMPLE="$ROOT_DIR/backend/local/backend-local.example.yml"
AI_ENV_FILE="$ROOT_DIR/ai-service/.env"
AI_ENV_EXAMPLE="$ROOT_DIR/ai-service/.env.example"
FRONTEND_ENV_LOCAL="$ROOT_DIR/frontend/.env.local"
FRONTEND_ENV_EXAMPLE="$ROOT_DIR/frontend/.env.example"

WITH_DEMO_DATA=false
NO_STOP=false
STRICT_CONFIG=false

usage() {
  cat <<'EOF'
Usage: ./start.sh [options]

Options:
  --with-demo-data   Start the full local stack and seed demo data.
  --no-stop          Do not stop existing services before starting.
  --strict-config    Treat placeholder config values as blocking errors.
  -h, --help         Show this help message.

This script is the preferred local one-click entrypoint. It validates
required local config files, warns about placeholder values, optionally
stops old processes, and then delegates to tools/maintenance/bootstrap_local_demo.sh.
EOF
}

require_command() {
  local command_name="$1"
  if ! command -v "$command_name" >/dev/null 2>&1; then
    echo "[error] missing required command: $command_name"
    exit 1
  fi
}

require_file() {
  local file_path="$1"
  local hint_path="$2"
  local label="$3"

  if [[ ! -f "$file_path" ]]; then
    echo "[error] missing $label: $file_path"
    echo "        hint: copy from $hint_path and fill in local values first"
    exit 1
  fi
}

warn_or_fail_placeholder() {
  local file_path="$1"
  local label="$2"

  if grep -Eiq 'change-me-|replace-with-|example\.com|replace-with-your-' "$file_path"; then
    if [[ "$STRICT_CONFIG" == true ]]; then
      echo "[error] $label still contains placeholder values: $file_path"
      exit 1
    fi
    echo "[warn] $label may still contain placeholder values: $file_path"
  fi
}

parse_args() {
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --with-demo-data)
        WITH_DEMO_DATA=true
        ;;
      --no-stop)
        NO_STOP=true
        ;;
      --strict-config)
        STRICT_CONFIG=true
        ;;
      -h|--help)
        usage
        exit 0
        ;;
      *)
        echo "[error] unknown option: $1"
        usage
        exit 1
        ;;
    esac
    shift
  done
}

validate_local_setup() {
  require_command curl
  require_command python3
  require_command npm
  require_command mvn

  require_file "$BACKEND_LOCAL_CONFIG" "$BACKEND_LOCAL_EXAMPLE" "backend local config"
  require_file "$AI_ENV_FILE" "$AI_ENV_EXAMPLE" "ai-service env file"

  if [[ ! -f "$FRONTEND_ENV_LOCAL" ]]; then
    echo "[info] frontend local env not found: $FRONTEND_ENV_LOCAL"
    echo "       optional: copy from $FRONTEND_ENV_EXAMPLE if you need local frontend overrides"
  fi

  warn_or_fail_placeholder "$BACKEND_LOCAL_CONFIG" "backend local config"
  warn_or_fail_placeholder "$AI_ENV_FILE" "ai-service env file"
}

stop_existing_services() {
  if [[ "$NO_STOP" == true ]]; then
    echo "[skip] leaving existing services untouched (--no-stop)"
    return
  fi

  echo "=================================================="
  echo "  stopping previously started local services"
  echo "=================================================="

  if [[ -f "$STOP_SCRIPT" ]]; then
    bash "$STOP_SCRIPT"
  else
    echo "[warn] stop script not found: $STOP_SCRIPT"
  fi

  sleep 2
}

start_stack() {
  echo ""
  echo "=================================================="
  echo "  starting Smart ATS local stack"
  echo "=================================================="

  if [[ ! -f "$BOOTSTRAP_SCRIPT" ]]; then
    echo "[error] bootstrap script not found: $BOOTSTRAP_SCRIPT"
    exit 1
  fi

  if [[ "$WITH_DEMO_DATA" == true ]]; then
    bash "$BOOTSTRAP_SCRIPT" --with-demo-data
  else
    bash "$BOOTSTRAP_SCRIPT"
  fi
}

main() {
  parse_args "$@"
  validate_local_setup
  stop_existing_services
  start_stack
}

main "$@"