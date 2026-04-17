#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
PID_DIR="$ROOT_DIR/tools/maintenance/pids"

for name in frontend ai-service backend; do
  pid_file="$PID_DIR/$name.pid"
  if [[ -f "$pid_file" ]]; then
    pid="$(cat "$pid_file")"
    if kill -0 "$pid" 2>/dev/null; then
      echo "[stop] $name ($pid)"
      kill "$pid"
    else
      echo "[skip] $name pid file exists but process is already gone"
    fi
    rm -f "$pid_file"
  fi
done