#!/usr/bin/env bash
# Combined check of a wave on demo/05b-integration (step 5b).
#   modernization/design/waves/combined-check.sh <wave>
# Writes modernization/design/waves/combined-check-wave-<wave>.txt and prints a summary.
# Exit 0 when build and tests pass, make parity passes, and the cases in EXPECT_PASS (default: all seven) pass.
set -u
cd "$(git rev-parse --show-toplevel)" || exit 2
wave="${1:?usage: $0 <wave>}"
out="modernization/design/waves/combined-check-wave-$wave.txt"
expectPass="${EXPECT_PASS:-$(ls fixtures/interest | tr '\n' ' ')}"
export JAVA_HOME="$(dirname "$(dirname "$(update-alternatives --list java | grep -m1 -- '-21-')")")"
strip() { sed 's/\x1b\[[0-9;]*m//g'; }
fail=0
{
  echo "# Combined check, wave $wave, branch $(git rev-parse --abbrev-ref HEAD) at $(git rev-parse HEAD)"
  echo "# $(date -u +%Y-%m-%dT%H:%M:%SZ), $("$JAVA_HOME"/bin/java -version 2>&1 | head -1), Docker $(docker version --format '{{.Server.Version}}')"
  echo "# cases expected to pass: $expectPass"
  echo; echo "## make reset && make up && make build"
  make reset 2>&1 | tail -3; make up 2>&1 | tail -3; make build 2>&1 | tail -3; echo "# exit code ${PIPESTATUS[0]}"
  echo; echo "## mvn -B -f java/interest/pom.xml clean verify"
  mvn -B -f java/interest/pom.xml clean verify 2>&1 | strip | grep -E '^\[(INFO|ERROR|WARNING)\] (Tests run:|BUILD|.*FAIL)|ERROR'
  mvnExit=${PIPESTATUS[0]}; echo "# exit code $mvnExit"; [ "$mvnExit" = 0 ] || fail=1
  if ls db/postgres/tests/* >/dev/null 2>&1; then
    echo; echo "## db/postgres/tests (schema assertions, db service)"
    for sql in db/postgres/tests/*.sql; do
      docker compose exec -T db psql -U carddemo -d carddemo -v ON_ERROR_STOP=1 -f - < "$sql" 2>&1 | tail -5
      sqlExit=${PIPESTATUS[0]}; echo "# $sql exit code $sqlExit"; [ "$sqlExit" = 0 ] || fail=1
    done
  fi
  echo; echo "## make parity-java (every case under fixtures/interest, tools/parity/compare.py)"
  make parity-java 2>&1 | strip | grep -vE '^\s+at |^\s*$|common frames omitted'; echo "# exit code ${PIPESTATUS[0]}"
  echo; echo "## per case: python3 tools/parity/compare.py interest <case> --actual work/parity-java/<case>/actual --label java"
  for c in $(ls fixtures/interest); do
    python3 tools/parity/compare.py interest "$c" --actual "work/parity-java/$c/actual" --label java --report work/parity-java/combined.md
    rc=$?; echo "  exit $rc"
    case " $expectPass " in *" $c "*) [ "$rc" = 0 ] || { echo "  UNEXPECTED: $c must pass"; fail=1; };; esac
  done
  echo; echo "## make parity (legacy lane)"
  make parity 2>&1 | strip; parityExit=${PIPESTATUS[0]}; echo "# exit code $parityExit"; [ "$parityExit" = 0 ] || fail=1
  git checkout -- tools/parity/__pycache__ tools/runjcl/__pycache__ 2>/dev/null
  echo; echo "## git status --short (must be empty apart from this file)"; git status --short | grep -v "$out"
  echo; echo "# COMBINED CHECK: $([ $fail = 0 ] && echo PASS || echo FAIL)"
} > "$out" 2>&1
grep -E '^# (exit|COMBINED|db/)|Tests run:.*[0-9]$|BUILD|\[java\]|UNEXPECTED' "$out"; grep -c 'legacy rerun\]: PASS' "$out"; grep 'legacy rerun\]: FAIL' "$out"
exit $fail
