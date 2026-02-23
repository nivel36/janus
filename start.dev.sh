#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="$ROOT_DIR/deploy/docker/compose.dev.yml"

if [[ ! -f "$COMPOSE_FILE" ]]; then
  echo "Compose file not found at: $COMPOSE_FILE" >&2
  exit 1
fi

if docker compose version >/dev/null 2>&1; then
  COMPOSE_CMD=(docker compose)
elif command -v docker-compose >/dev/null 2>&1; then
  COMPOSE_CMD=(docker-compose)
else
  echo "Docker Compose not found (docker compose or docker-compose)." >&2
  exit 1
fi

echo "Starting services with ${COMPOSE_CMD[*]} using $COMPOSE_FILE"
"${COMPOSE_CMD[@]}" -f "$COMPOSE_FILE" up -d --build

echo "Services started successfully."
