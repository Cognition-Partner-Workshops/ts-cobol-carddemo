#!/usr/bin/env bash
# Tests of db/postgres/02-interest.sql (ticket 02 / INT-T02) against the `db` service of
# docker-compose.yml (postgres:15).  Run from the repository root:
#
#     docker compose up -d --wait db && db/postgres/tests/run.sh
#
# Creates an empty scratch database in the service, applies 01-transaction-type.sql and
# 02-interest.sql, runs the assertion script, applies 02-interest.sql a second time (which must
# fail cleanly and change nothing) and drops the scratch database.  The database `carddemo`
# itself is not touched.  Exits non-zero on the first failure.
set -euo pipefail

scratchDb="interest_schema_test_$$"
scripts=/docker-entrypoint-initdb.d      # db/postgres as the service mounts it
psqlIn() { docker compose exec -T db psql -X -q -v ON_ERROR_STOP=1 -U carddemo "$@"; }

psqlIn -d postgres -c "CREATE DATABASE $scratchDb"
trap 'psqlIn -d postgres -c "DROP DATABASE IF EXISTS $scratchDb" || true' EXIT

echo "# server: $(psqlIn -d "$scratchDb" -Atc 'SHOW server_version')"
echo "# psql -v ON_ERROR_STOP=1 -f db/postgres/01-transaction-type.sql"
psqlIn -d "$scratchDb" -f "$scripts/01-transaction-type.sql"
echo "# psql -v ON_ERROR_STOP=1 -f db/postgres/02-interest.sql"
psqlIn -d "$scratchDb" -f "$scripts/02-interest.sql"
echo "# exit code $?"

echo "# psql -v ON_ERROR_STOP=1 -f db/postgres/tests/02-interest-assertions.sql"
psqlIn -d "$scratchDb" -f "$scripts/tests/02-interest-assertions.sql" 2>&1 | sed 's/^psql:[^ ]*: //'
echo "# exit code 0"

objects='SELECT count(*) FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace WHERE n.nspname = $$carddemo$$'
before=$(psqlIn -d "$scratchDb" -Atc "$objects")
echo "# second run: psql -v ON_ERROR_STOP=1 -f db/postgres/02-interest.sql (must fail cleanly)"
set +e
psqlIn -d "$scratchDb" -f "$scripts/02-interest.sql" 2>&1 | sed 's/^psql:[^ ]*: //'
again=${PIPESTATUS[0]}
set -e
after=$(psqlIn -d "$scratchDb" -Atc "$objects")
echo "# exit code $again; objects in schema carddemo before $before, after $after"
if [ "$again" -eq 0 ] || [ "$before" != "$after" ]; then
    echo "FAILED: a second run must exit non-zero and change nothing"
    exit 1
fi
echo "02-interest tests: PASSED"
