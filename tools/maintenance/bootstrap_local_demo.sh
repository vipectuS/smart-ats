#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
PID_DIR="$ROOT_DIR/tools/maintenance/pids"
LOG_DIR="$ROOT_DIR/tools/maintenance/logs"
WITH_DEMO_DATA="${1:-}"

mkdir -p "$PID_DIR" "$LOG_DIR"

ensure_ai_venv() {
  if [[ ! -x "$ROOT_DIR/ai-service/.venv/bin/python" ]]; then
    echo "[setup] creating ai-service virtualenv"
    cd "$ROOT_DIR/ai-service"
    python3 -m venv .venv
    .venv/bin/pip install -r requirements.txt
  fi
}

ensure_frontend_deps() {
  if [[ ! -d "$ROOT_DIR/frontend/node_modules" ]]; then
    echo "[setup] installing frontend dependencies"
    cd "$ROOT_DIR/frontend"
    npm install
  fi
}

start_service() {
  local name="$1"
  local workdir="$2"
  local command="$3"
  local pid_file="$PID_DIR/$name.pid"
  local log_file="$LOG_DIR/$name.log"

  if [[ -f "$pid_file" ]] && kill -0 "$(cat "$pid_file")" 2>/dev/null; then
    echo "[skip] $name already running with pid $(cat "$pid_file")"
    return
  fi

  echo "[start] $name"
  (
    cd "$workdir"
    nohup env PATH="$PATH" bash -c "$command" >"$log_file" 2>&1 &
    echo $! >"$pid_file"
  )
}

wait_for_http() {
  local name="$1"
  local url="$2"
  local attempts="${3:-60}"

  for ((i=1; i<=attempts; i++)); do
    if curl -s -o /dev/null "$url"; then
      echo "[ready] $name -> $url"
      return 0
    fi
    sleep 1
  done

  echo "[error] $name did not become ready: $url"
  return 1
}

ensure_ai_venv
ensure_frontend_deps

start_service \
  backend \
  "$ROOT_DIR/backend" \
  "mvn spring-boot:run"

start_service \
  ai-service \
  "$ROOT_DIR/ai-service" \
  "export BACKEND_PARSE_FAILED_PATH=/internal/api/resumes/{resumeId}/parse-failed && .venv/bin/python -m uvicorn main:app --host 0.0.0.0 --port 8000"

start_service \
  frontend \
  "$ROOT_DIR/frontend" \
  "npm run dev -- --host 0.0.0.0 --port 5173"

wait_for_http backend http://127.0.0.1:18080/error
wait_for_http ai-service http://127.0.0.1:8000/health
wait_for_http frontend http://127.0.0.1:5173

if [[ "$WITH_DEMO_DATA" == "--with-demo-data" ]]; then
  echo "[seed] importing demo data"
  cd "$ROOT_DIR"
  python3 tools/maintenance/seed_demo_data.py
  python3 tools/maintenance/generate_phase6_report.py
fi

cat <<EOF

Smart ATS local demo is ready.
- Frontend: http://127.0.0.1:5173
- Backend:  http://127.0.0.1:18080
- AI:       http://127.0.0.1:8000/health
- Logs:     $LOG_DIR
- Stop:     $ROOT_DIR/tools/maintenance/stop_local_demo.sh

EOF