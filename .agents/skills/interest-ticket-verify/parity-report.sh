#!/usr/bin/env bash
# Writes the parity report of an interest ticket in the format of the pilot
# (java/interest/evidence/parity-report.md of ticket 01).
#
#   .agents/skills/interest-ticket-verify/parity-report.sh <ticket> <case> [<case> ...]
#
# <ticket> is the ticket's file name under modernization/design/tickets without ".md"
# (for example 04-calculation-rules).  <case> is a fixture case the ticket names in its
# Acceptance (i01-baseline for ticket 01).
# Runs `make parity-java` for those cases, then for all cases, compares every case with
# tools/parity/compare.py and writes java/interest/evidence/<ticket>/parity-report.md, so
# tickets built in parallel never write the same file.  REPORT=<file> writes somewhere else.
# The columns "In the ticket's scope" and "Why" of cases the ticket does not name are left
# as "fill in": the session completes them by hand.  Needs JAVA_HOME on a JDK 21 and Docker.
# Exit code: 0 when every case named on the command line passes, 1 otherwise, 2 on a usage error.
set -u

[ $# -ge 2 ] || { echo "usage: $0 <ticket> <case> [<case> ...]" >&2; exit 2; }
cd "$(git rev-parse --show-toplevel)" || exit 2

ticket="$1"; shift
[ -f "modernization/design/tickets/$ticket.md" ] || { echo "no ticket file modernization/design/tickets/$ticket.md" >&2; exit 2; }
for case in "$@"; do
  [ -d "fixtures/interest/$case" ] || { echo "no fixture case fixtures/interest/$case" >&2; exit 2; }
done

ticketCases="$*"
report="${REPORT:-java/interest/evidence/$ticket/parity-report.md}"
logDir=work/parity-java-logs
rm -rf "$logDir"; mkdir -p "$logDir" "$(dirname "$report")"

acceptanceExit=0
for case in $ticketCases; do
  make parity-java CASE="$case" > "$logDir/acceptance-$case.log" 2>&1 || acceptanceExit=1
  find "work/parity-java/$case" -type f | sort > "$logDir/files-$case.txt"
  wc -l "work/parity-java/$case"/actual/datasets/* >> "$logDir/files-$case.txt" 2>/dev/null
  rm -rf "$logDir/actual-$case"; cp -r "work/parity-java/$case/actual" "$logDir/actual-$case" 2>/dev/null
done

make parity-java > "$logDir/all.log" 2>&1; allExit=$?

rm -f work/parity-java/compare.md; : > "$logDir/cases.tsv"
for case in $(ls fixtures/interest); do
  line=$(python3 tools/parity/compare.py interest "$case" --actual "work/parity-java/$case/actual" \
           --label java --report work/parity-java/compare.md 2>&1); compareExit=$?
  jobExit=$(sed -n "s|^interest/$case: java job ended with exit code \([0-9]*\)$|\1|p" "$logDir/all.log")
  printf '%s\t%s\t%s\t%s\n' "$case" "${jobExit:-0}" "$compareExit" "$line" >> "$logDir/cases.tsv"
done
git checkout -- tools/parity/__pycache__ 2>/dev/null

fence='```'
{
  echo "# Parity report - ticket $ticket (java/interest)"
  echo
  echo "Revision of the code: \`$(git rev-parse HEAD)\` (branch \`$(git rev-parse --abbrev-ref HEAD)\`$(git diff --quiet HEAD -- java Makefile || echo ', with uncommitted changes'))."
  echo "Produced $(date -u +%Y-%m-%dT%H:%M:%SZ), comparator \`tools/parity/compare.py\` unchanged."
  echo
  echo "## Result per case"
  echo
  echo "\`python3 tools/parity/compare.py interest <case> --actual work/parity-java/<case>/actual --label java\`"
  echo
  echo "| Case | In the ticket's scope | Java job exit code | Comparator exit code | Result | Why |"
  echo "|---|---|---|---|---|---|"
  while IFS=$'\t' read -r case jobExit compareExit line; do
    scope="_fill in_"; why="_fill in_"
    for ticketCase in $ticketCases; do
      [ "$case" = "$ticketCase" ] && { scope="**yes - acceptance case of the ticket**"; why=""; }
    done
    result=$([ "$compareExit" = 0 ] && echo PASS || echo FAIL)
    echo "| $case | $scope | $jobExit | $compareExit | **$result** - \`$line\` | $why |"
  done < "$logDir/cases.tsv"
  for case in $ticketCases; do
    echo
    echo "## Acceptance run: \`make parity-java CASE=$case\`"
    echo
    echo "$fence"; cat "$logDir/acceptance-$case.log"; echo "$fence"
    echo
    echo "Files under \`work/parity-java/$case/actual\` and record counts:"
    echo
    echo "$fence"; cat "$logDir/files-$case.txt"; echo "$fence"
    echo
    echo "Output of the Java job for $case:"
    for file in $(cd "$logDir/actual-$case" 2>/dev/null && find . -type f | sort | sed 's|^\./||'); do
      echo; echo "\`$file\`"; echo; echo "$fence"; cat "$logDir/actual-$case/$file"; echo; echo "$fence"
    done
  done
  echo
  echo "## All cases: \`make parity-java\` (exit code $allExit)"
  echo
  echo "$fence"; grep -vE '^\s+at |^\s*$|common frames omitted' "$logDir/all.log"; echo "$fence"
  echo
  echo "## Comparator report (\`work/parity-java/compare.md\`, written by tools/parity/compare.py)"
  echo
  sed 's/^## /### /' work/parity-java/compare.md
  echo "## Job repository and schema after these runs (\`db\` service)"
  echo
  echo "$fence"
  docker compose exec -T db psql -U carddemo -d carddemo \
    -c "show server_version" \
    -c "select i.job_name, e.status, (select string_agg(s.step_name || ':' || s.status || ' read=' || s.read_count || ' write=' || s.write_count, ', ' order by s.step_execution_id) from batch.batch_step_execution s where s.job_execution_id = e.job_execution_id) as steps, p.parameter_value as run_id from batch.batch_job_execution e join batch.batch_job_instance i using (job_instance_id) join batch.batch_job_execution_params p on p.job_execution_id = e.job_execution_id and p.parameter_name = 'runId' order by e.job_execution_id desc limit $(( $(ls fixtures/interest | wc -l) + $# ))" \
    -c "select table_schema, table_name from information_schema.tables where table_schema in ('carddemo', 'public') order by 1, 2"
  echo "$fence"
} > "$report"

cut -f1-3 "$logDir/cases.tsv"
echo "report: $report"
exit $acceptanceExit
