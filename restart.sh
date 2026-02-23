#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if [[ "${1:-}" == "-dev" ]]; then
  "$ROOT_DIR/stop.sh" -dev
  "$ROOT_DIR/start.sh" -dev
elif [[ $# -eq 0 ]]; then
  "$ROOT_DIR/stop.sh"
  "$ROOT_DIR/start.sh"
else
  echo "Uso: $0 [-dev]" >&2
  exit 1
fi
