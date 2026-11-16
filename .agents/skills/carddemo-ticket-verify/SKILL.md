---
name: carddemo-ticket-verify
description: How to prove one ticket of any converted batch stream of the CardDemo estate - compare each fixture case of the stream with tools/parity/compare.py, read a difference, write the evidence in one folder per ticket, write the report, and run the combined check of the stream (tests, every case, the legacy chains). Paths and commands take the stream as <stream>; the stream's parity target, job parameters, error path and database assertions are in its annex (carddemo-stream-<stream>). Use after carddemo-ticket-build, when a parity case fails, or when a ticket's pull request needs its evidence.
---

# CardDemo ticket: verify (shared)

> **Proven on one stream only: interest.** Every command below was run in step 5c from a clean checkout with
> `<stream>` = `interest`, following this skill and `.agents/skills/carddemo-stream-interest/SKILL.md` only; the
> output quoted is that run's (`modernization/design/evidence/05c-shared-skills-check.txt`). For another stream
> the commands are unproven until its annex says otherwise.

Commands only. What the code must be is `.agents/skills/carddemo-conversion-standards/SKILL.md` with the annex
of the stream; this skill points to their sections and does not repeat them. Set up the shell as in
`carddemo-ticket-build` steps 1-2 first. Run every command from the repository root.

**Placeholders.** `<stream>`, `<case>` and `<ticket>` as in `carddemo-ticket-build`. `<parity-target>` is the
make target that runs the stream's Java job on its fixture cases and compares; `<out>` is the directory that
target writes to, one `<out>/<case>/actual` per case. Both are in section 1 of the annex.

## Workflow

1. **Run the job on a case the ticket names and compare.**

   ```sh
   make <parity-target> CASE=<case>
   ```
   Example output (interest: `make parity-java CASE=i01-baseline`):
   ```
@@v1@@
   ```
   The target is the stream's own: what it starts, which parameters it passes to the job and what makes a
   case fail are in the annex. Exit code 0 only if every case it ran passed. Without `CASE` it runs every
   case under `fixtures/<stream>`.

2. **Know the files the comparator reads.** The job writes them itself in the recorded format (standard
   S6.4); nothing has to be converted.

   ```sh
   find <out>/<case> -type f | sort && wc -l <out>/<case>/actual/datasets/*
   ```
   Example output (interest):
   ```
@@v2@@
   ```
   The layout is the one of `fixtures/<stream>/<case>/expected`: `datasets/<DSN>.txt`,
   `sysout/<JOB>.<STEP>.txt`, `rc.json`. `work/` is ignored by git.

3. **Compare each case with the comparator.** One case:

   ```sh
   python3 tools/parity/compare.py <stream> <case> --actual <out>/<case>/actual --label java
   ```
   Example output (interest):
   ```
@@v3a@@
   ```
   Exit code 0 is PASS, 1 is FAIL. All cases, into one fresh Markdown report (pitfall P7: the report
   file is appended to, so remove it first):

   ```sh
   make <parity-target>            # runs every case; non-zero while any case fails
   rm -f <out>/compare.md
   for c in $(ls fixtures/<stream>); do
     python3 tools/parity/compare.py <stream> $c --actual <out>/$c/actual --label java --report <out>/compare.md
     echo "  exit $?"
   done
   ```
   Example output of the loop (interest, step 5c):
   ```
@@v3b@@
   ```
   A case your ticket does not name may pass or fail; report it, do not claim it. Once every case of the
   stream passes on your base branch, any FAIL is a regression.

   Run Python of your own that imports the comparator with `PYTHONDONTWRITEBYTECODE=1`, and delete an
   untracked `tools/parity/__pycache__/compare.*.pyc`: importing `tools.parity.compare` leaves one
   there, which `git checkout -- tools/parity/__pycache__` does not remove.

4. **Run the job by hand when you need another setting** (a policy switch, another input directory).
   The command is the stream's: annex, "Run the job by hand". The job runs in a JRE container on the
   network of the `db` container (pitfall P3).

5. **Read a difference.** The Markdown report names the dataset, the record with its key, the field,
   the legacy value and the candidate value, already decoded. The fix is always in `java/<stream>`,
   never in `fixtures/` or the comparator (standards S8.1, S8.2). The annex lists the differences the
   stream has produced and the standard each one belongs to.

6. **Check the error path of the ticket** from the command line. The stop conditions, the command and what
   a stopped run leaves are the stream's: annex, "Error path".

7. **Check the database assertions of the ticket** in the `db` service. The tables of schema `carddemo`:

   ```sh
   docker compose exec -T db psql -U carddemo -d carddemo \
     -c "select table_schema, table_name from information_schema.tables where table_schema in ('carddemo', 'public') order by 1, 2"
   ```
   Example output (interest, step 5c):
   ```
@@v7@@
   ```
   The job-repository query and the queries on the stream's own tables are in the annex.

8. **Write the evidence, from the committed revision, in the ticket's own directory.** Commit the code
   first, then produce the files, then commit them alone, so the evidence names the revision it came
   from. Every ticket writes under `java/<stream>/evidence/<ticket>/`, where `<ticket>` is the ticket's
   file name without `.md`, so tickets built in parallel never write the same file.

   | File in `java/<stream>/evidence/<ticket>/` | How it is produced |
   |---|---|
   | `test-output.txt` | the command below |
   | `parity-report.md` | the report script of the stream (annex) |
   | `report.md` (the ticket's report) | by hand, step 9 |
   | other evidence the ticket's Verification names | files or a sub-folder in the ticket's evidence folder, listed at the top of `report.md` |

   ```sh
   ticket=<ticket>
   mkdir -p java/<stream>/evidence/$ticket
   { echo "# mvn -B -f java/<stream>/pom.xml clean verify"
     echo "# revision $(git rev-parse HEAD), $(date -u +%Y-%m-%dT%H:%M:%SZ), $("$JAVA_HOME"/bin/java -version 2>&1 | head -1), Docker $(docker version --format '{{.Server.Version}}')"
     mvn -B -f java/<stream>/pom.xml clean verify 2>&1 | sed 's/\x1b\[[0-9;]*m//g'
     echo "# exit code ${PIPESTATUS[0]}"; } > java/<stream>/evidence/$ticket/test-output.txt
   tail -1 java/<stream>/evidence/$ticket/test-output.txt; grep -E 'Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+$|BUILD' java/<stream>/evidence/$ticket/test-output.txt
   ```
   Example output (interest, step 5c):
   ```
@@v8@@
   ```
   Files the tests leave under `java/<stream>/target/` are those of the last `clean verify`; use them
   before running Maven again.

9. **Write the ticket's report** as `java/<stream>/evidence/<ticket>/report.md`, with seven sections, in
   this order:

   1. Result - one row per Acceptance line of the ticket: met or not, and the test or report section that shows it.
   2. What the ticket does not cover - rules and work left, with the owning ticket.
   3. For the decision register - findings with evidence and a suggested owner.
   4. Standards review checklist - section 9 of `carddemo-conversion-standards` (the checklist of the stream's annex where it has one), every line PASS, FAIL or N/A with the file or test that shows it.
   5. What is wrong or missing in the standards skill - proposals only; do not edit that skill.
   6. Method - the commands run and the failures hit (symptom, cause, fix).
   7. Dependencies - every new one, why, and what was considered instead (standard S8.3).

10. **Commit the evidence and open the pull request.** `git checkout -- tools/parity/__pycache__`, then
    the scope check of `carddemo-ticket-build` step 9, then one pull request against the base your prompt
    names. Put the per-case table, the gaps and the register findings in its description. If you change
    code after review, repeat steps 1, 3 and 8 and replace the evidence in your ticket's directory. Do not
    merge.

## Combined check of a stream

The check a coordinating session runs on a base branch after tickets were combined
(`modernization/design/build-run.md`, section 3). The legacy chains are part of it: they prove that nothing
under `app/`, `fixtures/` or `tools/` moved.

```sh
mvn -B -f java/<stream>/pom.xml clean verify 2>&1 | grep -E '^\[(INFO|ERROR|WARNING)\] (Tests run:|BUILD)' | tail -4
make up && make build
make <parity-target>
make parity
```
Result for interest in step 5c:
```
@@combined@@
```
`make up` builds the `estate` image the first time. `make parity` runs every case of every legacy chain and
ends with `report: work/parity/report.md`; exit code 0 only when all pass. Afterwards:
`git checkout -- tools/parity/__pycache__ tools/runjcl/__pycache__` (pitfall P6).
