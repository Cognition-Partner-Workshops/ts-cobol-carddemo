#!/usr/bin/env bash
# Proves db/postgres/02-interest.sql in the db service of docker-compose.yml (postgres:15).
#
#   db/postgres/tests/run.sh
#
# 1. creates an empty database interest_schema_test and applies 01-transaction-type.sql
# 2. applies 02-interest.sql                      - must exit 0
# 3. applies 02-interest.sql a second time        - must fail (psql exit 3) and change nothing
# 4. runs tests/02-interest-assert.sql            - must exit 0
# 5. drops the database
# Exit code: 0 when every step did what it must, 1 otherwise.  The database carddemo of the
# service is not touched.  Needs: docker compose up -d --wait db
set -u
cd "$(git rev-parse --show-toplevel)" || exit 1

testDb=interest_schema_test
psqlIn() { docker compose exec -T db psql -U carddemo -v ON_ERROR_STOP=1 "$@"; }
catalog="select c.relkind, c.relname from pg_class c join pg_namespace n on n.oid = c.relnamespace where n.nspname = 'carddemo' order by 1, 2"
failed=0

echo "## server"
psqlIn -d carddemo -At -c "select version()" || exit 1
psqlIn -d carddemo -q -c "drop database if exists $testDb" -c "create database $testDb" || exit 1

echo "## 1. psql -v ON_ERROR_STOP=1 -f db/postgres/01-transaction-type.sql (empty database $testDb)"
psqlIn -d $testDb -f - < db/postgres/01-transaction-type.sql; rc=$?
echo "exit code $rc"; [ $rc -eq 0 ] || failed=1

echo "## 2. psql -v ON_ERROR_STOP=1 -f db/postgres/02-interest.sql"
psqlIn -d $testDb -f - < db/postgres/02-interest.sql; rc=$?
echo "exit code $rc (expected 0)"; [ $rc -eq 0 ] || failed=1
before=$(psqlIn -d $testDb -At -c "$catalog")

echo "## 3. the same a second time: fails cleanly"
psqlIn -d $testDb -f - < db/postgres/02-interest.sql 2>&1; rc=$?
echo "exit code $rc (expected 3)"; [ $rc -eq 3 ] || failed=1
after=$(psqlIn -d $testDb -At -c "$catalog")
if [ "$before" = "$after" ]; then
  echo "objects of schema carddemo unchanged by the second run: $(echo "$after" | wc -l) (tables, indexes, sequence)"
else
  echo "FAIL: the second run changed schema carddemo"; failed=1
fi

echo "## 4. psql -v ON_ERROR_STOP=1 -q -At -f db/postgres/tests/02-interest-assert.sql"
psqlIn -d $testDb -q -At -f - < db/postgres/tests/02-interest-assert.sql 2>&1; rc=$?
echo "exit code $rc (expected 0)"; [ $rc -eq 0 ] || failed=1
rows=$(psqlIn -d $testDb -At -c "select (select count(*) from carddemo.account) + (select count(*) from carddemo.card_xref) + (select count(*) from carddemo.tran_cat_balance) + (select count(*) from carddemo.disclosure_group) + (select count(*) from carddemo.interest_transaction) + (select count(*) from carddemo.interest_run)")
echo "rows left in the six tables by the assertions: $rows (expected 0)"; [ "$rows" = 0 ] || failed=1

psqlIn -d carddemo -q -c "drop database $testDb"
echo "## result: $([ $failed -eq 0 ] && echo PASS || echo FAIL)"
exit $failed
