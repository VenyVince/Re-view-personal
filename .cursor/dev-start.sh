#!/usr/bin/env bash
# Per-boot startup for the Re:View dev environment.
# Starts the Docker daemon (cloud VMs have no systemd) and brings up the
# Oracle XE + MinIO infrastructure containers. Idempotent.
set -euo pipefail

cd "$(dirname "$0")/.."

# Ensure local infra credentials exist.
if [ ! -f infra/.env ]; then
  cp infra/.env.dev infra/.env
fi

# Start the Docker daemon if it is not already running.
if ! sudo docker info >/dev/null 2>&1; then
  sudo bash -c 'nohup dockerd >/tmp/dockerd.log 2>&1 &'
  for _ in $(seq 1 30); do
    sudo docker info >/dev/null 2>&1 && break
    sleep 2
  done
fi

# The docker-compose file bind-mounts ~/volumes/oracle-data into the Oracle
# container, which runs as uid 54321. When compose is invoked through sudo the
# home directory resolves to /root, so pre-create and own the data dir there to
# avoid "Cannot open output file" permission errors on first boot.
sudo mkdir -p /root/volumes/oracle-data /root/volumes/minio_data
sudo chown -R 54321:54321 /root/volumes/oracle-data

# Bring up the infrastructure services (Oracle XE + MinIO).
( cd infra && sudo docker compose up -d minio oracle-db )

echo "dev-start complete: Oracle XE (1521) and MinIO (9000/9001) are starting"
