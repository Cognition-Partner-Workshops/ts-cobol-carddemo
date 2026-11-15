# Skills from the pilot - interest stream

> **PROPOSED - not approved.** Written by an agent in step 5a from the pilot ticket
> (`modernization/design/tickets/01-pilot-thin-slice.md`, branch `demo/05-pilot`, pull request #174).
> Owner who must confirm the skills and decide the corrections in section 4: **the target architect.**

Based on `demo/05-pilot` at `2c4b023` (code revision `0294cc0`). No application code and no ticket was
changed in this step; `carddemo-parity` was not edited. `interest-conversion-standards` was edited only for
the five corrections the reviewer approved (section 4: 1, 2, 10, 13, 15), one commit each; it stays PROPOSED.

## 1. The three skills

| Skill | One line |
|---|---|
| `.agents/skills/interest-ticket-build/SKILL.md` | Where things are in `java/interest`, which tool versions, and the commands to put Maven on JDK 21, start PostgreSQL 15, build, run the tests and check the scope before a commit. |
| `.agents/skills/interest-ticket-verify/SKILL.md` | The commands that run the job on a fixture case, compare each case with `tools/parity/compare.py`, show how a difference reads, check the error path and the database, and write the evidence in the pilot's format (with the helper `parity-report.sh` beside it). |
| `.agents/skills/interest-pitfalls/SKILL.md` | The seven failures the pilot hit, each as symptom, cause, fix, each reproduced in this step. |

They hold commands and failures only. What the code must be stays in
`.agents/skills/interest-conversion-standards/SKILL.md`; the three skills cite its sections by number.

Every command in the skills was run in this step and its output is quoted there. All seven pitfalls of
the pilot's report were reproduced; none is listed as "reported by the pilot, not reproduced".

## 2. Replay

A fresh clone of `demo/05a-skills` in an empty directory, no `java/interest/target`, no `work/`, the
`db` container and its volume removed first (`docker compose down -v`), a new shell without `JAVA_HOME`.
`interest-ticket-build` steps 1-9 and `interest-ticket-verify` steps 1-8 were followed as written, on
ticket 01. Tools: JDK 21.0.13, Maven 3.6.3, Docker Engine 29.9.2, Docker Compose v5.6.1, Python 3.10.12,
GNU Make 4.3, PostgreSQL 15.20 (`postgres:15`).

| Skill step | Command | Result |
|---|---|---|
| build 1 | `docker --version && docker compose version && python3 --version && make --version \| head -1` | versions above |
| build 2 | `export JAVA_HOME="$(dirname "$(dirname "$(update-alternatives --list java \| grep -m1 -- '-21-')")")"; mvn -v` | `Java version: 21.0.13.0.3` |
| build 3 | `git fetch origin; git ls-remote --heads origin 'demo/*'; gh pr list --state open --json number,title,headRefName,baseRefName` | branches `demo/01-intake` to `demo/05a-skills`; pull requests #165 to #174; none covers another ticket |
| build 6 | `docker compose up -d --wait db; docker compose exec -T db psql -U carddemo -d carddemo -c "show server_version"` | `Healthy`, `15.20` |
| build 7 | `mvn -q -f java/interest/pom.xml -DskipTests package && ls java/interest/target/interest.jar` | jar listed |
| build 8 | `mvn -B -f java/interest/pom.xml verify` | **60 unit tests, 12 integration tests, 0 failures, BUILD SUCCESS** |
| build 8 | `mvn -B -f java/interest/pom.xml test -Dtest=InterestFormulaTest` | 8 tests, BUILD SUCCESS |
| build 9 | `git checkout -- tools/parity/__pycache__; git diff --stat origin/demo/05-pilot -- app fixtures tools modernization .agents db docker-compose.yml; git status --short` | the diff lists only the four files of this step under `.agents/skills`; with `.agents` taken out of the list (the path this step may write) and `java Makefile` added, nothing is printed |
| verify 1 | `make parity-java CASE=i01-baseline` | `interest/i01-baseline [java]: PASS (4 records, 50 fields, 0 differences)`, exit 0 |
| verify 2 | `find work/parity-java/i01-baseline -type f \| sort && wc -l .../datasets/*` | two datasets of 2 records, `rc.json`, `sysout/INTCALC.STEP15.txt` |
| verify 3 | `python3 tools/parity/compare.py interest <case> --actual work/parity-java/<case>/actual --label java` for each case | table below |
| verify 4 | the job by hand with `--interest.policy.rounding=HALF_UP` on i02 | job exit 0; comparator `FAIL (... 3 differences)`, as the skill says |
| verify 6 | the job by hand without the CARDXREF input | `OPEN_INPUT (INT-R08)`, exit 12, no output directory |
| verify 7 | the two `psql` queries | one COMPLETED `interestCalcJob` with `calcInterestStep:COMPLETED read=2 write=2`; schema `carddemo` holds `transaction_type`, `transaction_type_categor` only |
| verify 8 | `mvn -B -f java/interest/pom.xml clean verify` into `test-output.txt` | `# exit code 0`, 60 and 12 tests, BUILD SUCCESS |
| verify 8 | `.agents/skills/interest-ticket-verify/parity-report.sh i01-baseline` | exit 0; report written |

After the replay, on the reviewer's instruction, verify step 8 was changed: every ticket writes its
evidence under `java/interest/evidence/<ticket file name>/` (`test-output.txt`, `parity-report.md`,
`report.md`), and `parity-report.sh` takes the ticket file name as its first argument. The changed
commands were run again with ticket 01 (60 and 12 tests, BUILD SUCCESS; the same seven case results;
report written to `java/interest/evidence/01-pilot-thin-slice/parity-report.md`), and that directory was
removed: the pilot's files stay where they are.

Parity per case in the replay, against the pilot's `java/interest/evidence/parity-report.md`:

| Case | Java job exit code | Comparator | Pilot | Same |
|---|---|---|---|---|
| i01-baseline | 0 | PASS (4 records, 50 fields, 0 differences) | PASS | yes |
| i02-tie-half-cent | 0 | PASS (4 records, 50 fields, 0 differences) | PASS | yes |
| i03-zero-rate-skipped | 12 | FAIL (2 records, 25 fields, 4 differences) | FAIL, job stops on INT-R04 | yes |
| i04-default-group-fallback | 12 | FAIL (2 records, 25 fields, 4 differences) | FAIL, job stops on INT-R03 | yes |
| i05-multi-category | 0 | PASS (3 records, 38 fields, 0 differences) | PASS | yes |
| i06-negative-balance | 0 | PASS (2 records, 25 fields, 0 differences) | PASS | yes |
| i07-nontie-rounding | 0 | PASS (4 records, 50 fields, 0 differences) | PASS | yes |

Checks that the replay matches the pilot:

* The `Tests run:` lines of the replay's `test-output.txt` equal the pilot's, class by class (elapsed times aside).
* The case, exit-code and result columns of the replay's parity report equal the pilot's.
* The four output files of i01-baseline are byte-identical to `fixtures/interest/i01-baseline/expected`.

The replay's evidence files were discarded afterwards: the pilot's evidence in `java/interest/evidence/`
is unchanged on this branch.

**Corrections the replay needed: none.** One correction was made while writing, before the replay:
`interest-ticket-verify` step 6 used a shell variable set in step 4; it now sets it itself. One thing the
runs of this step found beyond the pilot's report: the pilot said the comparator's default report is
appended to; so is a report named with `--report` (pitfall P7).

## 3. Not carried over from the pilot's report

The pilot fetched the `demo/*` branches with an explicit refspec because its checkout was a shallow
single-branch clone. A full clone needs only `git fetch origin`, which is what the skill says and what
the replay ran.

## 4. Proposed corrections to `interest-conversion-standards` - for the target architect to decide

**Applied after review:** corrections 1, 2, 10, 13 and 15 were approved by the reviewer and are now in the
skill, one commit each. For 2, 13 and 15 the reviewer chose: add what holds for a file adapter; the digits
of an id inside a text are a domain matter; one stop reason `OPEN_INPUT` with the file as data. The other
thirteen rows are still proposals and the skill is unchanged for them. "Pilot" in the last column means the pilot's report
(`java/interest/evidence/PILOT-REPORT.md`, section 5) raised it; "5a" means it was found in this step
by reading the standard against the pilot's code.

| # | Standard | What the pilot's code shows | Proposed change | Raised by |
|---|---|---|---|---|
| 1 (applied) | Section 9, checklist template | About ten lines are N/A for a ticket that does not own the behaviour; the template prints "PASS / FAIL" only, though its introduction allows N/A | Print "PASS / FAIL / N/A" on every line | Pilot |
| 2 (applied) | S2.3, S3.7, S6.3, S6.4, row h | Ticket 01 has file adapters and no table. Key order is fixed by sorting the 17-byte key in `file/FileRunWorkspace.java`, which S3.7's "Do not" forbids; there is no restart state; a stopped run keeps nothing | Add what holds for a file adapter, or say these standards start with ticket 06 | Pilot |
| 3 | S6.5 against S2.2 and S3.5 | The run log needs the 50-byte record image, the domain is meant to see typed values only. The image travels beside the typed balance in `domain/AccountBalances.BalanceRead` | Rule on it; say how ticket 06 rebuilds the image from table columns, the 22-byte FILLER of CVTRA01Y included | Pilot |
| 4 | S7.1, S3.8 | `interest.policy.timestampSource=CYCLE_DATE` is refused in `policy/InterestPolicy.java`: it needs a cycle-date job parameter that `architecture.md` section 4 does not list | Name the ticket that builds the non-default values and the parameter; say ticket 01 tests defaults | Pilot |
| 5 | Section 6, table | `StopReason.RECORD_REJECTED` (a byte that breaks E1/E2) and a launch refused before the job starts (parmDate length, `format=host`) have no row | Add both rows: stop reason, run-log line or "none", exit code | Pilot |
| 6 | Section 4, column "Test" | Rows name tests that cannot exist in the ticket that first touches the behaviour (row d `intQ05_negativeInterestIsDSigned`, row s "all seven") | Say per test name which ticket owns it | Pilot |
| 7 | S6.4 | `file/RecordedReturnCodes.java` writes five step entries that are not INTCALC's as 0, to match the recorded `rc.json` | Say what `rc.json` holds, and what the target writes outside the harness | Pilot |
| 8 | S3.8, row u | `batch/InterestJobConfig.runClock` uses the JVM's default zone; the JRE container runs in UTC | Name the time zone of the injected clock | Pilot |
| 9 | S7.3, FI-6 | The `db` service publishes no port and the `estate` image has no JDK, so the tests use Testcontainers `postgres:15` and `make parity-java` runs the jar on the `db` container's network | S7.3 names "the `db` service ... or the same image": say Testcontainers with `db/postgres/*.sql` as init scripts; point FI-6 to `interest-ticket-verify` | Pilot (as a method note) |
| 10 (applied) | The banner at the top and the sentence before the section 4 table | They say "Nothing here has been run", "No Java exists yet" and that no named test exists yet. The pilot applied the standards and its tests exist | Update the banner: applied once, by ticket 01; still PROPOSED | 5a |
| 11 | S7.1 | Pilot tests that carry no rule id: `e1_`...`e4_` (the standard itself names such tests), `cvtra01y_decodesTheBalanceOfI01` and three like it, `sysout_matchesRecorded`, `rcJson_matchesRecorded`, `rcJson_recordedFormat`, `i01Baseline_datasetsMatchRecorded`, `jobRepository_oneCompletedExecutionWithOneStep`, `schemaCarddemo_holdsOnlyTheTablesOfTheEstate`, plus `hostFormatRefusedUntilTicket03` and `outputDirectoryHoldsOnlyTheRecordedFiles` | List the prefixes allowed besides a rule id (encoding rule, layout, output file, ticket assertion) | 5a |
| 12 | Section 4, rows m and h | The pilot's test is `intT09_emptyInputGivesNoItem`, the standard names `intT09_emptyInputWritesNothing`; the pilot's `intR01_readsInKeyOrder` runs on synthetic files, the standard names fixture i05 on PostgreSQL | Either rename in the ticket that builds the JDBC step, or accept an adapter-level name; say which | 5a |
| 13 (applied) | S3.6 | Its "Do not" keeps width out of the domain, its "Do" writes `elevenDigits(accountId)` in the domain. The pilot formats eleven digits in `domain/InterestCalculator.java` and `file/FileRunWorkspace.java`, and six in `domain/TransactionCounter.java`; the pilot's checklist marked S3.6 PASS | Say whether the digits of an id inside a text (description, transaction id, lookup key) are a domain matter or a codec matter | 5a |
| 14 | S3.2 against S5.1 | S3.2's example calls `policy.roundingMode()`; S5.1 wants the rule id in the name. The pilot has `intQ01InterestRoundingMode()` | Make the example of S3.2 follow S5.1 | 5a |
| 15 (applied) | Section 6, row "Open fails (five files)" | The standard has one reason per file, `OPEN_<file>`; the pilot has one `StopReason.OPEN_INPUT` for the four inputs and names the file in the message | Decide before ticket 08: one reason per file (the legacy message differs per file) or one reason with the file as data | 5a |
| 16 | S2.1 / `architecture.md` section 2 ("the calculation depends on port and policy only", "domain: no framework") | `policy/InterestPolicy.java` carries Spring Boot's `@ConfigurationProperties`, so the calculation compiles against a Spring type through the policy | Say whether `policy` may carry the binding annotation or needs a plain type bound in `batch` | 5a |
| 17 | S5.1 ("exactly one named place") | INT-Q02 lives in two places: the prefix in `InterestPolicy.intQ02IdPrefix` and the PARM length check in `batch/InterestJobParameters.java`. INT-Q04 is decided in the reader (`endsInput`) and applied in the calculator, as row m asks | Say that a job-parameter check citing the rule id is allowed beside the policy method | 5a |
| 18 | S3.4 | The pilot has no `policy.checkFits`; `codec/Field.java` refuses a value that does not fit when it is encoded, which is after the calculation and without naming INT-H05 | Say whether the codec's refusal is enough until ticket 04, and that ticket 04 adds the check before the store | 5a |

Findings of the pilot for the decision register (not standards) are in section 3 of
`java/interest/evidence/PILOT-REPORT.md`; they are not repeated here.

## 5. What the skills do not cover

* **Anything after ticket 01.** No command for `db/postgres/02-interest.sql`, a JDBC adapter, the loader,
  the export, a restart, the host code page or CI was run, so none is in the skills. Tickets 02-10 add
  their own commands and pitfalls to their pull requests.
* **Other machines.** The `JAVA_HOME` line needs `update-alternatives` (Debian/Ubuntu); elsewhere only the
  check `mvn -v` says 21 is proven. Docker Engine older than 29 with Testcontainers 1.21.4 was not run.
* **`make up`, `make build`, `make parity` (the legacy chain).** Not needed for a ticket and not run here;
  they are in `carddemo-parity`.
* **The scope check for tickets whose Allowed files differ.** The proven form is the one for
  `java/interest/**` and the `Makefile`.
* **What the code must be.** By design: `interest-conversion-standards`.
* **Pull request review, merge, ticket systems.** Out of scope; nothing is merged by a session.
