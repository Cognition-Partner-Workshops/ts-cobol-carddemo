---
name: interest-ticket-verify
description: How to prove one ticket of the interest stream - run the Java job of java/interest on the fixture cases so that its output lands where the comparator reads it, compare each case with tools/parity/compare.py, read a difference, check the error path and the database, and write the evidence in the format the pilot used. Use after interest-ticket-build, when a parity case fails, or when a ticket's pull request needs its evidence.
---

# Interest ticket: verify

Commands only, each run on the pilot ticket (01) in step 5a and quoted with the output that shows
success. What the code must be is `.agents/skills/interest-conversion-standards/SKILL.md`; this skill
points to its sections and does not repeat them. Set up the shell as in `interest-ticket-build` steps 1-2
first. Run every command from the repository root.

## Workflow

1. **Run the job on a case the ticket names and compare (FI-6).**

   ```sh
   make parity-java CASE=i01-baseline
   ```
   ```
   docker compose up -d --wait db
   mvn -q -f java/interest/pom.xml -DskipTests package
   interest/i01-baseline [java]: PASS (4 records, 50 fields, 0 differences)
   report: work/parity-java/report.md
   ```
   The target starts the `db` service, builds `java/interest/target/interest.jar`, removes
   `work/parity-java/<case>` and `work/parity-java/report.md`, runs the jar once per case in an
   `eclipse-temurin:21-jre` container on the network of the `db` container with the PARM `2022071800`
   (`app/jcl/INTCALC.jcl:22`) and the clock frozen at `2022-07-05 10:00:00.00`, and compares. Exit code 0
   only if every case it ran passed. Without `CASE` it runs every case under `fixtures/interest`.

2. **Know the files the comparator reads.** The job writes them itself; nothing has to be converted.

   ```sh
   find work/parity-java/i01-baseline -type f | sort && wc -l work/parity-java/i01-baseline/actual/datasets/*
   ```
   ```
   work/parity-java/i01-baseline/actual/datasets/AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS.txt
   work/parity-java/i01-baseline/actual/datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt
   work/parity-java/i01-baseline/actual/rc.json
   work/parity-java/i01-baseline/actual/sysout/INTCALC.STEP15.txt
     2 work/parity-java/i01-baseline/actual/datasets/AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS.txt
     2 work/parity-java/i01-baseline/actual/datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt
   ```
   A run that stops leaves no `actual` directory at all. `work/` is ignored by git.

3. **Compare each case with the comparator.** One case:

   ```sh
   python3 tools/parity/compare.py interest i01-baseline --actual work/parity-java/i01-baseline/actual --label java
   ```
   ```
   interest/i01-baseline [java]: PASS (4 records, 50 fields, 0 differences)
   ```
   Exit code 0 is PASS, 1 is FAIL. All cases, into one fresh Markdown report (pitfall P7: the report
   file is appended to, so remove it first):

   ```sh
   make parity-java            # runs all seven; non-zero while any case fails
   rm -f work/parity-java/compare.md
   for c in $(ls fixtures/interest); do
     python3 tools/parity/compare.py interest $c --actual work/parity-java/$c/actual --label java --report work/parity-java/compare.md
     echo "  exit $?"
   done
   ```
   Result on the pilot's code (ticket 01 claims i01-baseline only):
   ```
   interest/i01-baseline [java]: PASS (4 records, 50 fields, 0 differences)
     exit 0
   interest/i02-tie-half-cent [java]: PASS (4 records, 50 fields, 0 differences)
     exit 0
   interest/i03-zero-rate-skipped [java]: FAIL (2 records, 25 fields, 4 differences)
     exit 1
   interest/i04-default-group-fallback [java]: FAIL (2 records, 25 fields, 4 differences)
     exit 1
   interest/i05-multi-category [java]: PASS (3 records, 38 fields, 0 differences)
     exit 0
   interest/i06-negative-balance [java]: PASS (2 records, 25 fields, 0 differences)
     exit 0
   interest/i07-nontie-rounding [java]: PASS (4 records, 50 fields, 0 differences)
     exit 0
   ```
   `make parity-java` printed for i03 and i04 `java job ended with exit code 12` and ended with
   `make: *** [Makefile:96: parity-java] Error 1`. Since ticket 04 (step 5b, wave 1) all seven pass:
   `interest/i03-zero-rate-skipped [java]: PASS (2 records, 25 fields, 0 differences)` and
   `interest/i04-default-group-fallback [java]: PASS (2 records, 25 fields, 0 differences)`, exit 0. A case your ticket does not name may pass or fail;
   report it, do not claim it.

4. **Run the job by hand when you need another setting** (a policy switch, another input directory).
   This is the command `make parity-java` runs, with one option added:

   ```sh
   db=$(docker compose ps -q db)
   docker run --rm --network container:$db --user $(id -u):$(id -g) -v "$PWD":/estate -w /estate eclipse-temurin:21-jre \
     java -jar java/interest/target/interest.jar --interest.clock.frozen="2022-07-05 10:00:00.00" --logging.level.root=WARN \
       --interest.policy.rounding=HALF_UP \
       parmDate=2022071800 runId=manual-$(date +%s) format=fixture \
       inputDir=fixtures/interest/i02-tie-half-cent/input outputDir=work/manual/i02-halfup/actual
   echo "job exit=$?"
   python3 tools/parity/compare.py interest i02-tie-half-cent --actual work/manual/i02-halfup/actual --label java-halfup --report work/manual/diff.md
   ```
   ```
   job exit=0
   interest/i02-tie-half-cent [java-halfup]: FAIL (4 records, 50 fields, 3 differences)
   ```
   `runId` must be new for every launch. Exit code 0 is a COMPLETED job, 12 a stopped one.

5. **Read a difference.** The Markdown report names the dataset, the record with its key, the field,
   the legacy value and the candidate value, already decoded. Each row below was produced in step 5a;
   the fix is always in `java/interest`, never in `fixtures/` or the comparator (standards S8.1, S8.2).

   | What the report shows | How it was produced | Where the rule is |
   |---|---|---|
   | `TRAN-AMT` 12.35 against 12.36 and `ACCT-CURR-BAL` 1247.85 against 1247.86 on i02-tie-half-cent | step 4 with `--interest.policy.rounding=HALF_UP` (3 differences). With `--interest.policy.rounding=HALF_EVEN`: 2 differences on i02-tie-half-cent (account 2003 only, the same two fields and values) and none on i07-nontie-rounding | standards S3.2, section 4 row a |
   | `ACCT-CURR-BAL` 2400.00 against 2437.00 and `ACCT-CURR-CYC-CREDIT` 2400.00 against 0.00 on the last account (i01-baseline, ACCT-ID=2002) | step 4 with `--interest.policy.updateLastAccount=true` | standards S5.1, section 4 row m |
   | `TRAN-ORIG-TS` and `TRAN-PROC-TS` 2022-07-05-10.00.00.000000 against today's time | step 4 without `--interest.clock.frozen` | standards S3.8, section 4 rows t and u |
   | `(file)` `1 records` against `missing` for both datasets, and the whole SYSOUT and rc.json as removed lines | the job stopped (exit 12) and left no output: i03, i04 on the pilot's code. Read the job's own `InterestRunStoppedException: <REASON> (<rule id>)` line in the `make parity-java` output | standards section 6 |
   | `rc.json` `(text)` with `STEP05` ... `STEP30` removed | an `rc.json` holding STEP15 only | pitfall P4 |

6. **Check the error path of the ticket** from the command line. Skip this step when the ticket's
   Acceptance has no error line that a command-line run can show, and say so in `report.md`.
   Missing input file (ticket 01):

   ```sh
   mkdir -p work/manual/miss/input && cp fixtures/interest/i01-baseline/input/* work/manual/miss/input/ \
     && rm work/manual/miss/input/AWS.M2.CARDDEMO.CARDXREF.PS.txt
   db=$(docker compose ps -q db)
   docker run --rm --network container:$db --user $(id -u):$(id -g) -v "$PWD":/estate -w /estate eclipse-temurin:21-jre \
     java -jar java/interest/target/interest.jar --logging.level.root=WARN parmDate=2022071800 runId=miss-$(date +%s) format=fixture \
       inputDir=work/manual/miss/input outputDir=work/manual/miss/actual 2>&1 | grep -E 'InterestRunStoppedException|CalcInterestRun  '
   echo "job exit=${PIPESTATUS[0]}"; ls work/manual/miss
   ```
   ```
   com.carddemo.interest.domain.InterestRunStoppedException: OPEN_INPUT (INT-R08): cannot read input file AWS.M2.CARDDEMO.CARDXREF.PS.txt
   ... c.c.interest.batch.CalcInterestRun       : run miss-1794733240 step calcInterestStep stopped before any work
   job exit=12
   input
   ```
   Only `input` is listed: no output directory and no staging directory were left.
   For a record error (ticket 03), copy the input the same way, change one byte
   (`sed -i '2s/^\(.\{12\}\)./\1X/' work/manual/miss/input/AWS.M2.CARDDEMO.DISCGRP.PS.txt`) and expect
   `InterestRunStoppedException: RECORD_REJECTED (INT-R08): file AWS.M2.CARDDEMO.DISCGRP.PS.txt record 2 field DIS-TRAN-CAT-CD: is not numeric`,
   `job exit=12` and again only `input`.

7. **Check the database assertions of the ticket** in the `db` service. Skip this step when the ticket's
   Acceptance says "Database assertions: none"; say so in section 6 of `report.md`.

   ```sh
   docker compose exec -T db psql -U carddemo -d carddemo \
     -c "select i.job_name, e.status, (select string_agg(s.step_name || ':' || s.status || ' read=' || s.read_count || ' write=' || s.write_count, ', ') from batch.batch_step_execution s where s.job_execution_id = e.job_execution_id) as steps, p.parameter_value as run_id from batch.batch_job_execution e join batch.batch_job_instance i using (job_instance_id) join batch.batch_job_execution_params p on p.job_execution_id = e.job_execution_id and p.parameter_name = 'runId' where p.parameter_value like '%i01-baseline' order by e.job_execution_id desc limit 1" \
     -c "select table_schema, table_name from information_schema.tables where table_schema in ('carddemo', 'public') order by 1, 2"
   ```
   Output, without the rule and count lines:
   ```
       job_name     |  status   |                   steps                   |            run_id
    interestCalcJob | COMPLETED | calcInterestStep:COMPLETED read=2 write=2 | 1794733207-33341-i01-baseline
    carddemo     | transaction_type
    carddemo     | transaction_type_categor
   ```
   The job repository is in schema `batch`. The same assertions run in `InterestCalcJobIT` on a
   Testcontainers `postgres:15`; a ticket that adds tables changes the expected list there.
   The output above is the pilot's. With `db/postgres/02-interest.sql` (ticket 02) on a new db volume the
   list in `carddemo` is `account, card_xref, disclosure_group, interest_run, interest_transaction,
   tran_cat_balance, transaction_type, transaction_type_categor`. A db volume created before that file
   existed does not get the tables (init scripts run only on an empty volume): `make reset`, or
   `docker compose exec -T db psql -U carddemo -d carddemo -v ON_ERROR_STOP=1 < db/postgres/02-interest.sql`.

8. **Write the evidence, from the committed revision, in the ticket's own directory.** Commit the code
   first, then produce the files, then commit them alone, so the evidence names the revision it came
   from. Every ticket writes under `java/interest/evidence/<ticket file name>/`, where the name is the
   ticket's file under `modernization/design/tickets` without `.md` (for example
   `java/interest/evidence/04-calculation-rules/`), so tickets built in parallel never write the same
   file. Write nothing directly in `java/interest/evidence/`: the three files there
   (`test-output.txt`, `parity-report.md`, `PILOT-REPORT.md`) are the pilot's, written before this
   rule, and stay as they are.

   | File in `java/interest/evidence/<ticket file name>/` | How it is produced |
   |---|---|
   | `test-output.txt` | the command below |
   | `parity-report.md` | `.agents/skills/interest-ticket-verify/parity-report.sh <ticket file name> <case the ticket names> [...]` |
   | `schema-test-output.txt` | tickets with SQL assertions under `db/postgres/tests`: `db/postgres/tests/run.sh > java/interest/evidence/<ticket file name>/schema-test-output.txt 2>&1` |
   | `switch-test-output.txt` | tickets with switch tests: the step 4 command once per switch value, each followed by the job exit code, the comparator line and the rows of its report |
   | `report.md` (the ticket's report) | by hand, step 9 |

   The commands were run in step 5a with ticket 01, the only ticket built, to prove them; that
   directory was removed again and is not in the repository.

   ```sh
   ticket=01-pilot-thin-slice          # your ticket's file name without .md
   mkdir -p java/interest/evidence/$ticket
   { echo "# mvn -B -f java/interest/pom.xml clean verify"
     echo "# revision $(git rev-parse HEAD), $(date -u +%Y-%m-%dT%H:%M:%SZ), $("$JAVA_HOME"/bin/java -version 2>&1 | head -1), Docker $(docker version --format '{{.Server.Version}}')"
     mvn -B -f java/interest/pom.xml clean verify 2>&1 | sed 's/\x1b\[[0-9;]*m//g'
     echo "# exit code ${PIPESTATUS[0]}"; } > java/interest/evidence/$ticket/test-output.txt
   tail -1 java/interest/evidence/$ticket/test-output.txt; grep -E 'Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+$|BUILD' java/interest/evidence/$ticket/test-output.txt
   ```
   If the unit phase fails, the integration tests do not run. Report the failure as it is; to show the rest,
   append a second, labelled run that leaves out the failing test:
   `mvn -B -f java/interest/pom.xml clean verify -Dtest='!<Class>#<method>' -Dsurefire.failIfNoSpecifiedTests=false`.
   With `-Dtest` surefire also runs the `*IT` classes, so its count is unit plus integration tests.
   ```
   # exit code 0
   [INFO] Tests run: 60, Failures: 0, Errors: 0, Skipped: 0
   [INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
   [INFO] BUILD SUCCESS
   ```
   ```sh
   .agents/skills/interest-ticket-verify/parity-report.sh $ticket i01-baseline
   ```
   ```
   i01-baseline	0	0
   i02-tie-half-cent	0	0
   i03-zero-rate-skipped	12	1
   i04-default-group-fallback	12	1
   i05-multi-category	0	0
   i06-negative-balance	0	0
   i07-nontie-rounding	0	0
   report: java/interest/evidence/01-pilot-thin-slice/parity-report.md
   ```
   The three columns are case, exit code of the Java job, exit code of the comparator. The script runs
   steps 1 and 3 again and writes the sections of the pilot's parity report: result per case, acceptance
   run with the job's output files, all cases, the comparator's report, job repository and schema. It
   exits 0 when every case named on the command line passes. In the result table it leaves
   `_fill in_` in the columns "In the ticket's scope" and "Why" of the cases the ticket does not name:
   complete them by hand. Before changing code, run `make parity-java` on the base and keep its output; use
   it for the "Why" of a case outside the ticket that already failed there. A ticket name with no file under `modernization/design/tickets`, or a case
   with no directory under `fixtures/interest`, ends the script with exit code 2
   (`no ticket file modernization/design/tickets/99-nothing.md`). `REPORT=<file>` writes somewhere else.
   A ticket whose Verification says "Fixture cases: none" passes one case that passed on the base branch
   (`i01-baseline`), because the script needs at least one case, and then, in the result table, replaces
   `**yes - acceptance case of the ticket**` with `no - N/A, the ticket names no fixture case` and renames
   the section "Acceptance run" to "Run of one case".

9. **Write the ticket's report** as `java/interest/evidence/<ticket file name>/report.md`, with the
   seven sections of the pilot's `java/interest/evidence/PILOT-REPORT.md`, in this order:

   1. Result - one row per Acceptance line of the ticket: met or not, and the test or report section that shows it.
   2. What the ticket does not cover - rules and work left, with the owning ticket.
   3. For the decision register - findings with evidence and a suggested owner.
   4. Standards review checklist - section 9 of `interest-conversion-standards`, every line PASS, FAIL or N/A with the file or test that shows it.
   5. What is wrong or missing in the standards skill - proposals only; do not edit that skill.
   6. Method - the commands run and the failures hit (symptom, cause, fix).
   7. Dependencies - every new one, why, and what was considered instead (standard S8.3).

10. **Commit the evidence and open the pull request.** `git checkout -- tools/parity/__pycache__`, then
    the scope check of `interest-ticket-build` step 9, then one pull request against the base your prompt
    names. Put the per-case table, the gaps and the register findings in its description. If you change
    code after review, repeat steps 1, 3 and 8 and replace the evidence in your ticket's directory. Do not
    merge.
