#!/usr/bin/env bash
# Idempotent repository bootstrap for the Re:View full-stack dev environment.
# Installs Docker (for Oracle XE + MinIO), warms the Maven cache and installs
# frontend dependencies. Safe to run multiple times.
set -euo pipefail

cd "$(dirname "$0")/.."

# Docker engine + compose plugin (used to run Oracle XE and MinIO locally).
if ! command -v docker >/dev/null 2>&1; then
  sudo apt-get update -qq
  sudo apt-get install -y -qq docker.io docker-compose-v2
fi

# The Maven wrapper ships without the executable bit on some checkouts.
chmod +x mvnw

# Warm the backend dependency cache (does not fail the build if a transient
# download hiccup occurs; the terminal run will retry as needed).
./mvnw -q -DskipTests dependency:go-offline || true

# Frontend dependencies.
if [ -f View/package-lock.json ]; then
  npm --prefix View ci
else
  npm --prefix View install
fi

# Local infra credentials for docker-compose (dummy dev values).
if [ ! -f infra/.env ]; then
  cp infra/.env.dev infra/.env
fi

echo "dev-install complete"
