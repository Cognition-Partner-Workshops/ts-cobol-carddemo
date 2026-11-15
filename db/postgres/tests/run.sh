#!/usr/bin/env bash
# Applies db/postgres/01-transaction-type.sql and 02-interest.sql to a new, empty database in
# the db service of docker-compose (postgres:15), applies 02 a second time to show it fails
# cleanly and changes nothing, runs the assertions, and drops the database again.
#
#   db/postgres/tests/run.sh
#
# Exit code 0 when every step behaves as expected, 1 otherwise. PSQL="psql -h ... -U ..."
# runs against another server; the role must be allowed to create a database.
set -u
cd "$(dirname "$0")/../../.." || exit 1

psqlCommand="${PSQL:-docker compose exec -T db psql -U carddemo}"
scratchDb="interest_schema_test_$$"
failed=0

run() { $psqlCommand -X -v ON_ERROR_STOP=1 -d "$1" -f - < "$2"; }
catalog() {
  $psqlCommand -X -At -d "$scratchDb" -c "select c.relkind, c.relname, (select count(*) from pg_attribute a where a.attrelid = c.oid and a.attnum > 0) from pg_class c where c.relnamespace = 'carddemo'::regnamespace order by 1, 2"
}

$psqlCommand -X -At -d postgres -c "show server_version" -c "create database $scratchDb" || exit 1
trap '$psqlCommand -X -q -d postgres -c "drop database if exists $scratchDb"' EXIT

echo "== apply db/postgres/01-transaction-type.sql"
run "$scratchDb" db/postgres/01-transaction-type.sql; echo "exit $?"

echo "== apply db/postgres/02-interest.sql"
run "$scratchDb" db/postgres/02-interest.sql; applyExit=$?; echo "exit $applyExit"
[ $applyExit -eq 0 ] || failed=1
before=$(catalog)

echo "== apply db/postgres/02-interest.sql a second time (must fail and change nothing)"
run "$scratchDb" db/postgres/02-interest.sql; secondExit=$?; echo "exit $secondExit"
[ $secondExit -ne 0 ] || { echo "FAIL  the second apply did not fail"; failed=1; }
if [ "$before" = "$(catalog)" ]; then
  echo "PASS  catalog of schema carddemo unchanged by the second apply"
else
  echo "FAIL  the second apply changed schema carddemo"; failed=1
fi

echo "== db/postgres/tests/02-interest-assertions.sql"
run "$scratchDb" db/postgres/tests/02-interest-assertions.sql; assertExit=$?; echo "exit $assertExit"
[ $assertExit -eq 0 ] || failed=1

rows=$($psqlCommand -X -At -d "$scratchDb" -c "select (select count(*) from carddemo.account) + (select count(*) from carddemo.card_xref) + (select count(*) from carddemo.tran_cat_balance) + (select count(*) from carddemo.disclosure_group) + (select count(*) from carddemo.interest_transaction) + (select count(*) from carddemo.interest_run)")
if [ "$rows" = 0 ]; then echo "PASS  the assertions left no row behind"; else echo "FAIL  the assertions left $rows rows"; failed=1; fi

echo "== result: $([ $failed -eq 0 ] && echo PASS || echo FAIL)"
exit $failed
