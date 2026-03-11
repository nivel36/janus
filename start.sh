#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MODE="prod"

if [[ "${1:-}" == "-dev" ]]; then
  MODE="dev"
elif [[ $# -gt 0 ]]; then
  echo "Usage: $0 [-dev]" >&2
  exit 1
fi

if [[ "$MODE" == "dev" ]]; then
  COMPOSE_FILE="$ROOT_DIR/deploy/docker/compose.dev.yml"
else
  COMPOSE_FILE="$ROOT_DIR/deploy/docker/compose.yml"
fi

if [[ ! -f "$COMPOSE_FILE" ]]; then
  echo "Compose file not found at: $COMPOSE_FILE" >&2
  exit 1
fi

detect_compose_cmd() {
  if command -v podman >/dev/null 2>&1; then
    if podman compose version >/dev/null 2>&1; then
      echo "podman compose"
      return 0
    fi
  fi

  if command -v podman-compose >/dev/null 2>&1; then
    echo "podman-compose"
    return 0
  fi

  if command -v docker >/dev/null 2>&1; then
    if docker compose version >/dev/null 2>&1; then
      echo "docker compose"
      return 0
    fi
  fi

  if command -v docker-compose >/dev/null 2>&1; then
    echo "docker-compose"
    return 0
  fi

  return 1
}

COMPOSE_CMD_STR="$(detect_compose_cmd || true)"

if [[ -z "$COMPOSE_CMD_STR" ]]; then
  echo "No compatible Compose implementation found." >&2
  echo "Checked: podman compose, podman-compose, docker compose, docker-compose." >&2
  exit 1
fi

read -r -a COMPOSE_CMD <<< "$COMPOSE_CMD_STR"

echo "Starting services ($MODE) using: ${COMPOSE_CMD[*]}"
echo "Compose file: $COMPOSE_FILE"

"${COMPOSE_CMD[@]}" -f "$COMPOSE_FILE" up -d --build

echo "Services started successfully."
