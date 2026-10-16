#!/usr/bin/env bash
# Load the synthetic demo dataset into an already migrated database.
# Usage: seed/load.sh [database]   (default: recruiting)
set -euo pipefail
export MSYS_NO_PATHCONV=1  # Git Bash on Windows must not rewrite /tmp paths
DB="${1:-recruiting}"
cd "$(dirname "$0")/.."
docker compose exec -T postgres rm -rf /tmp/seed
docker compose cp seed postgres:/tmp/seed
docker compose exec -T postgres sh -c "cd /tmp/seed && psql -U recruiting -d '$DB' -v ON_ERROR_STOP=1 -q -f run.sql"
echo "Demo data loaded into '$DB'. Log in as admin@demo.example / manager@demo.example / consultant@demo.example (Demo-Password-2026)."
