#!/usr/bin/env bash
set -euo pipefail
: "${DB_URL:?DB_URL required}" "${DB_USERNAME:?DB_USERNAME required}" "${PGPASSWORD:?PGPASSWORD required}"
DEST="${BACKUP_DIR:-./backups}"; mkdir -p "$DEST"; FILE="$DEST/jnapanam-$(date -u +%Y%m%dT%H%M%SZ).dump"
pg_dump --format=custom --no-owner --no-acl --username="$DB_USERNAME" --dbname="$DB_URL" --file="$FILE"
find "$DEST" -type f -name 'jnapanam-*.dump' -mtime +"${BACKUP_RETENTION_DAYS:-14}" -delete
echo "$FILE"
