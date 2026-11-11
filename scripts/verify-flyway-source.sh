#!/usr/bin/env bash
# Source-only Flyway preflight; no schema writes. Compatible with Bash 4+.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SOURCE="$ROOT/apps/backend/src/main/resources/db/migration"
mode="${1:-plan}"
if [[ "$mode" != "plan" && "$mode" != "validate" ]]; then
  echo "Usage: $0 plan|validate" >&2
  exit 2
fi
[[ -d "$SOURCE" ]] || { echo "Missing Flyway SQL source directory" >&2; exit 2; }
mapfile -t files < <(find "$SOURCE" -maxdepth 1 -type f -name 'V*__*.sql' | sort -V)
((${#files[@]} > 0)) || { echo "No SQL migrations found" >&2; exit 2; }
declare -A seen=()
for path in "${files[@]}"; do
  name="${path##*/}"
  if [[ ! "$name" =~ ^V([0-9]+)__(.+)\.sql$ ]]; then
    echo "Invalid migration filename: $name" >&2
    exit 3
  fi
  version="${BASH_REMATCH[1]}"
  if [[ -n "${seen[$version]:-}" ]]; then
    echo "Duplicate Flyway version $version: ${seen[$version]} and $name" >&2
    exit 3
  fi
  seen[$version]="$name"
  [[ -s "$path" ]] || { echo "Empty SQL migration: $name" >&2; exit 3; }
done
echo "Validated source inventory: ${#files[@]} versioned SQL files."
for path in "${files[@]}"; do printf '%s\n' "${path##*/}"; done
echo "Source gaps are not automatically errors: check applied flyway_schema_history."
if [[ "$mode" == "plan" ]]; then
  echo "READ-ONLY. No database connection and no migrations applied."
  exit 0
fi
command -v flyway >/dev/null || { echo "Flyway CLI required for DB validation" >&2; exit 4; }
: "${FLYWAY_URL:?Provide the VERIFIED JDBC URL (via protected environment)}"
: "${FLYWAY_USER:?Provide migration service account}"
: "${FLYWAY_PASSWORD:?Provide Flyway password securely}"
echo "Validating against the explicitly configured Flyway target (no migration applied)."
FLYWAY_LOCATIONS="filesystem:$SOURCE" flyway validate
echo "Validation complete. See docs/architecture-kt/DB_SCHEMA_AND_MIGRATION.md."
