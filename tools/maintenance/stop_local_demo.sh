#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
PID_DIR="$ROOT_DIR/tools/maintenance/pids"

is_pid_running() {
  local pid="$1"
  [[ -n "$pid" ]] && kill -0 "$pid" 2>/dev/null
}

kill_pid_gracefully() {
  local pid="$1"
  local label="$2"

  if ! is_pid_running "$pid"; then
    return 0
  fi

  echo "[stop] $label ($pid)"
  kill "$pid" 2>/dev/null || true

  for _ in {1..10}; do
    if ! is_pid_running "$pid"; then
      return 0
    fi
    sleep 1
  done

  echo "[force-stop] $label ($pid)"
  kill -9 "$pid" 2>/dev/null || true
}

collect_port_pids() {
  local port="$1"

  if command -v lsof >/dev/null 2>&1; then
    lsof -tiTCP:"$port" -sTCP:LISTEN 2>/dev/null || true
    return
  fi

  if command -v fuser >/dev/null 2>&1; then
    fuser -n tcp "$port" 2>/dev/null | tr ' ' '\n' | sed '/^$/d' || true
    return
  fi
}

stop_project_port_listener() {
  local port="$1"
  local label="$2"
  local pids

  pids="$(collect_port_pids "$port")"
  if [[ -z "$pids" ]]; then
    return 0
  fi

  while IFS= read -r pid; do
    [[ -z "$pid" ]] && continue
    local command_line
    command_line="$(ps -p "$pid" -o command= 2>/dev/null || true)"
    if [[ "$command_line" == *"$ROOT_DIR"* ]]; then
      kill_pid_gracefully "$pid" "$label port:$port"
    fi
  done <<< "$pids"
}

for name in frontend ai-service backend; do
  pid_file="$PID_DIR/$name.pid"
  if [[ -f "$pid_file" ]]; then
    pid="$(cat "$pid_file")"
    if is_pid_running "$pid"; then
      kill_pid_gracefully "$pid" "$name"
    else
      echo "[skip] $name pid file exists but process is already gone"
    fi
    rm -f "$pid_file"
  fi
done

stop_project_port_listener 5173 frontend
stop_project_port_listener 8000 ai-service
stop_project_port_listener 18080 backend