# Ticket 09 - parity gate: report

Code revision `615975a9a1163aa185973fe96c558aef4daff856` on branch `demo/05-09-parity-gate` (base `demo/05b-integration`). Files next to this one:
`test-output.txt` (the full Maven suite), `gate-output.txt` (legacy parity, the gate, the negative control, a failed
run, the database assertions), `parity-report.md` (the comparator per case).

## 1. Result

| Acceptance line of the ticket | Met | Shown by |
|---|---|---|
| `make parity-java`: 7 of 7 cases PASS in work/parity-java/report.md | met | gate-output.txt "make parity-java (exit code 0)" and the gate table; parity-report.md "Result per case" |
| the legacy `make parity` is still green | met | gate-output.txt "make parity (legacy, exit code 0)": 24 cases PASS, 0 FAIL |
| negative control: with `interest.policy.rounding=HALF_EVEN` the gate fails on i02-tie-half-cent only, as `make parity-naive` does | met | gate-output.txt "Negative control": six PASS, i02 FAIL in the comparator only, with the same two differences as `make parity-naive` (`TRAN-AMT` 12.35 / 12.36, `ACCT-CURR-BAL` 1247.85 / 1247.86) |
| CI runs the gate and the negative control on every pull request and uploads the report | met when the CI run of the last commit is green - `.github/workflows/parity.yml`, steps "Java interest gate", "Java negative control", "Upload Java parity reports"; the run is linked in the pull request, it cannot be part of the commit it runs on |
| a case whose run fails is reported as failed, not skipped | met | gate-output.txt "A case whose run fails": exit code 12, row `FAIL (exit code 12) / FAIL / FAIL (no finished run to check) / FAIL`, make ends non-zero |
| after each case RC1, RC2 and RC5 hold in interest_run | met | gate-output.txt "Database assertions": every RC1, RC2 and RC5 line EQUAL for seven cases (the report also checks RC3 and RC4: RECONCILED); `LoadCalcExportJobIT` asserts RC1 and RC5 against the fixture files |
| README lists every legacy behaviour reproduced on purpose, each with its decision id | met | `java/interest/README.md`, "Legacy behaviour reproduced on purpose" |

Tests (test-output.txt): `Tests run: 385, Failures: 0, Errors: 0, Skipped: 0` (unit), `Tests run: 119, Failures: 0,
Errors: 0, Skipped: 0` (integration), `BUILD SUCCESS`.

| Case | Job | Comparator | RC1-RC5 | Result |
|---|---|---|---|---|
| i01-baseline | PASS | PASS | PASS | PASS |
| i02-tie-half-cent | PASS | PASS | PASS | PASS |
| i03-zero-rate-skipped | PASS | PASS | PASS | PASS |
| i04-default-group-fallback | PASS | PASS | PASS | PASS |
| i05-multi-category | PASS | PASS | PASS | PASS |
| i06-negative-balance | PASS | PASS | PASS | PASS |
| i07-nontie-rounding | PASS | PASS | PASS | PASS |

What was built: `interestCalcJob` joins loadExtractsStep, calcInterestStep (jdbc adapters) and exportDatasetsStep under
profile `tables` (`interest.job.steps=load-calc-export`); without the profile the job is unchanged. `LoadedRunHandOver`
passes the loader's interest_run id to the calculation and the export, so the three steps work on one row.

## 2. What the ticket does not cover

Not proven by this gate - no fixture case exists:

| Id | What | Where it is tested instead | Owner |
|---|---|---|---|
| INT-Q06 | interest of 0.00 is still posted | unit tests only (ticket 04) | a new fixture case; the estate owner records it |
| INT-R08 | missing record or file error stops the run | `ErrorPathsTablesIT`, `LoadCalcExportJobIT` - no recorded abend to compare with | ticket 08 / a recorded abend case |
| INT-H02 | a second run charges again | not tested; `rerunGuard` is not built | decision register |
| INT-H03 | account with several cards | unit and integration tests of the proposed STOP | decision register |
| INT-H04 | closed or expired accounts | unit tests | decision register |
| INT-H05 | amounts and counters larger than their fields | unit tests of the proposed STOP | decision register |
| INT-H06 | order against posting and the reloads | outside the job | operations |

Also left: the gate proves the as-coded candidate only (every switch at its default, except rounding in the negative
control); making the check required in branch protection is a repository setting, not a file; `make recon-java
STAGE=yes` still uses the stand-in staging of ticket 10, which the joined job makes unnecessary (ticket 10).

## 3. For the decision register

| Finding | Evidence | Suggested owner |
|---|---|---|
| The loader (ticket 05) and the calculation/export (tickets 06, 07) name the run id in the job execution context differently: `interest.runId` and `interestRunId`. Ticket 09 may not change either, so `LoadedRunHandOver` copies one to the other. One key would remove the class. | `LoadExtractsStepConfig.RUN_ID_KEY`, `CalcInterestTableRun.INTEREST_RUN_ID`, `ExportDatasetsTasklet.INTEREST_RUN_ID` | tickets 05 / 06 |
| A parity run leaves its rows in `interest_run` and `interest_transaction` of the `db` service (one run per case); the replica tables hold the last case only. Nothing cleans them; whether a shared database may keep test runs is a release question. | gate-output.txt, "interest_run after the last case" | REL owner (decisions-release.md) |
| A failed job prints the Spring stack trace on the console of the gate (no account data in it). Whether operations want a one-line message is open with REL-10. | gate-output.txt, failed-run section | REL-10 |

## 4. Standards review checklist

- [x] S1.1 Sources used in order; recorded output decides - PASS - the recorded output decides every case: `LoadCalcExportJobIT.loadCalcExport_everyCaseEqualsTheRecordedLegacyRun`, `make parity-java` (gate-output.txt)
- [x] S1.2 Rule/code disagreements reproduced as coded and reported - PASS - nothing new found; the reproduced behaviours are listed with their ids in `java/interest/README.md`
- [x] S2.1 Designed from rules; no method per paragraph - N/A - no domain, codec, loader, calculation or export code is changed (excluded work of the ticket)
- [x] S2.2 Immutable business records; copybook names only at the mapping - N/A - no domain, codec, loader, calculation or export code is changed (excluded work of the ticket)
- [x] S2.3 Spring Batch shape, transaction and restart boundaries of the architecture - PASS - `InterestJobConfig.interestCalcJob`: loadExtractsStep -> calcInterestStep -> exportDatasetsStep as architecture.md section 4; the export is one tasklet in one transaction; `LoadCalcExportJobIT` asserts the step order and that a failed step ends the job
- [x] S2.4 Types and return values, no flags or result codes - PASS - `LoadedRunHandOver` and the job wiring pass the run id as a value in the execution context; no flag or result code
- [x] S2.5 Constructor injection; no mutable static state - PASS - `InterestJobConfig` bean methods take their collaborators as parameters; `LoadedRunHandOver` has no state
- [x] S3.1 `BigDecimal` with data-map precision and scale; no `double`/`float` - N/A - no domain, codec, loader, calculation or export code is changed (excluded work of the ticket)
- [x] S3.2 Formula: exact product, one division, mode from the policy - N/A - no domain, codec, loader, calculation or export code is changed (excluded work of the ticket)
- [x] S3.3 Rounding mode named on every other arithmetic step - N/A - no domain, codec, loader, calculation or export code is changed (excluded work of the ticket)
- [x] S3.4 Range check before store; overflow goes to the policy - N/A - no domain, codec, loader, calculation or export code is changed (excluded work of the ticket)
- [x] S3.5 Encodings only in `record-codec` - N/A - no domain, codec, loader, calculation or export code is changed (excluded work of the ticket)
- [x] S3.6 Width, padding and FILLER only in codec and schema - N/A - no domain, codec, loader, calculation or export code is changed (excluded work of the ticket)
- [x] S3.7 Code page and key order each in one place - N/A - no domain, codec, loader, calculation or export code is changed (excluded work of the ticket)
- [x] S3.8 Clock and run parameters injected; `parmDate` is text - PASS - the gate passes the clock (`--interest.clock.frozen`) and `parmDate` as text job parameter (Makefile `parity-java`)
- [x] S4.1 Every PRESENT behaviour touched has its test; new ones reported - N/A - no behaviour of section 4 is touched by the join
- [x] S5.1 Each quirk/open decision in one named place behind its switch, rule id in names - PASS - no decision is coded here; the negative control only sets the existing switch `interest.policy.rounding` (INT-Q01) from the command line; README lists each quirk with id and switch
- [x] S5.2 Nothing fixed, tidied or added without a rule id - PASS - the join adds no behaviour; `application-tables.yml` sets adapters and steps only, policy defaults untouched
- [x] S6.1 Status and message mapping as in section 6 - N/A - no domain, codec, loader, calculation or export code is changed (excluded work of the ticket)
- [x] S6.2 One stop exception, one exit-code constant, no skip or retry - PASS - the joined job has no skip or retry; a stopped step ends the job FAILED with exit code 12: `LoadCalcExportJobIT.intR08_*`, failed-run section of gate-output.txt
- [x] S6.3 Restart as in the architecture - PASS - restart behaviour of the steps is unchanged (`CalcInterestTablesRestartIT` still passes); `LoadedRunHandOver` keeps the run of a restarted calculation (`LoadedRunHandOverTest.runOfARestartedCalculationStays`); exportDatasetsStep repeats on restart (`allowStartIfComplete`)
- [x] S6.4 Outputs in the recorded format; failed run leaves no SYSTRAN - PASS - outputs compared in the recorded format for seven cases; a failed run leaves no output directory: `LoadCalcExportJobIT.intR08_*`
- [x] S6.5 Run log byte for byte; no account data in the application log - PASS - run log compared byte for byte by the comparator (SYSOUT INTCALC.STEP15) in all seven cases; the new classes log nothing
- [x] S7.1 A test per rule id, named for it - PASS - `LoadCalcExportJobIT.intR08_missingExtractStopsTheJobBeforeCalculationAndExport`, `intR08_stoppedCalculationIsNotExported`; the join itself has no rule id
- [x] S7.2 Fixture and edge cases of each rule covered - PASS - all seven fixture cases run through the joined job in `LoadCalcExportJobIT` and in the gate
- [x] S7.3 Integration tests on PostgreSQL 15 in a container - PASS - `LoadCalcExportJobIT` uses `InterestTables.postgres()` (postgres:15, schema of FI-4)
- [x] S7.4 No assertion against the code's own output - PASS - `LoadCalcExportJobIT` compares with `fixtures/interest/<case>/expected` and counts RC1/RC5 from the fixture files, not from the code
- [x] S8.1 No edit under `app/`, `fixtures/`, `tools/parity` - PASS - `git diff --name-only origin/demo/05b-integration` names no file under app/, fixtures/, tools/parity, modernization/ or .agents/
- [x] S8.2 No golden file re-recorded - PASS - no `make record` run; fixtures/ unchanged
- [x] S8.3 Every new dependency explained in the pull request - PASS - no new dependency (pom.xml unchanged)
- [x] S8.4 Frozen shared interfaces unchanged - PASS - FI-1 to FI-5 untouched; FI-6 keeps `make parity-java [CASE=<case>]` and `work/parity-java/<case>/actual`
- [x] S8.5 Synthetic data only - PASS - only the synthetic fixtures are used
- [x] a Truncation without ROUNDED - N/A - no domain, codec, loader, calculation or export code is changed (excluded work of the ticket)
- [x] b High-order digit loss (range check, STOP) - N/A - no domain, codec, loader, calculation or export code is changed (excluded work of the ticket)
- [x] c Intermediate precision of the COMPUTE - N/A - no domain, codec, loader, calculation or export code is changed (excluded work of the ticket)
- [x] d Zoned overpunch signs - N/A - no domain, codec, loader, calculation or export code is changed (excluded work of the ticket)
- [x] e Binary items stay out of data and domain - N/A - no domain, codec, loader, calculation or export code is changed (excluded work of the ticket)
- [x] f Non-numeric data rejected at the boundary - N/A - no domain, codec, loader, calculation or export code is changed (excluded work of the ticket)
- [x] g Fixed width, padding, STRING - N/A - no domain, codec, loader, calculation or export code is changed (excluded work of the ticket)
- [x] h Code page and collating order - N/A - no domain, codec, loader, calculation or export code is changed (excluded work of the ticket)
- [x] i REDEFINES replaced by formatter and function - N/A - no domain, codec, loader, calculation or export code is changed (excluded work of the ticket)
- [x] j FILLER and record length round trip - N/A - no domain, codec, loader, calculation or export code is changed (excluded work of the ticket)
- [x] l No state carried between accounts except the counter - N/A - no domain, codec, loader, calculation or export code is changed (excluded work of the ticket)
- [x] m Control break and end of file (INT-Q04, INT-T09) - PASS (unchanged code, proven end to end) - INT-Q04 in all seven cases of the gate
- [x] o Level-88 result codes replaced by types - N/A - no domain, codec, loader, calculation or export code is changed (excluded work of the ticket)
- [x] p Indexed access through the ports; several cards to the policy - N/A - no domain, codec, loader, calculation or export code is changed (excluded work of the ticket)
- [x] q File statuses and abend - PASS (unchanged code) - a stopped step ends the joined job: `LoadCalcExportJobIT.intR08_*`
- [x] r PARM, generation, disposition, return code - PASS (unchanged code) - rc.json and the SYSTRAN generation compared in all seven cases of the gate
- [x] s DISPLAY output in the run log - PASS (unchanged code) - SYSOUT INTCALC.STEP15 compared in all seven cases of the gate
- [x] t PARM text against the clock - N/A - no domain, codec, loader, calculation or export code is changed (excluded work of the ticket)
- [x] u Timestamp format and hundredths - N/A - no domain, codec, loader, calculation or export code is changed (excluded work of the ticket)
- [x] w PARM length checked - N/A - no domain, codec, loader, calculation or export code is changed (excluded work of the ticket)
- [x] x No stale rate after a failed lookup - N/A - no domain, codec, loader, calculation or export code is changed (excluded work of the ticket)
- [x] y Typed fields where COBOL mixed text and numbers - N/A - no domain, codec, loader, calculation or export code is changed (excluded work of the ticket)
- [x] z Abend ends the step; stub output not reproduced - PASS (unchanged code) - a failed step ends the job, no output directory: `LoadCalcExportJobIT.intR08_*`
- [x] aa Dead and empty code not ported - N/A - no domain, codec, loader, calculation or export code is changed (excluded work of the ticket)

## 5. What is wrong or missing in the standards skill

- No standard names the job execution context key that carries the interest_run id between the steps. Tickets 05 and
  06/07 each chose one (`interest.runId`, `interestRunId`). Proposal: add to S2.3 "the run id is passed between the
  steps under one key, defined once in `batch`".

Skills interest-ticket-build / interest-ticket-verify / interest-pitfalls:

- interest-ticket-verify steps 1 and 8 describe `make parity-java` as the file-adapter pilot (sample output with
  i03 and i04 ending 12). After ticket 09 it runs the three steps under profile `tables`, all seven cases end 0, and
  each case also has `work/parity-java/<case>/recon.md`; `PARITY_JAVA_OUT` and `ROUNDING` are new variables.
- interest-ticket-verify step 7: the database assertions RC1-RC5 are now part of `make parity-java`
  (reconciliation report per case); the hand-run psql is only needed to look further.
- interest-pitfalls: `make parity` also rewrites the tracked `tools/runjcl/__pycache__/runjcl.cpython-310.pyc`;
  restore it with `git checkout -- tools/parity/__pycache__ tools/runjcl/__pycache__` before committing.
- interest-pitfalls: `make parity-naive` needs the `estate` service (`make up`); after only `docker compose up -d
  --wait db` it ends with `service "estate" is not running`.

## 6. Method

```sh
git checkout -b demo/05-09-parity-gate origin/demo/05b-integration
export JAVA_HOME=<JDK 21>
mvn -B -f java/interest/pom.xml clean verify                                   # test-output.txt
make up && make build && make parity                                           # legacy, 24 PASS
make parity-naive CASE=i02-tie-half-cent                                       # fails, as expected
.agents/skills/interest-ticket-verify/parity-report.sh 09-parity-gate <all seven cases>   # parity-report.md
make parity-java                                                               # 7 of 7 PASS
make parity-java ROUNDING=HALF_EVEN PARITY_JAVA_OUT=work/parity-java-half-even # fails on i02 only
make parity-java CASE=i01-baseline INTEREST_PARM=2022 PARITY_JAVA_OUT=work/parity-java-failed-run   # a run that fails
docker compose exec -T db psql -U carddemo -d carddemo -c "SELECT ... FROM carddemo.interest_run ..."
```

Failures hit:

| Symptom | Cause | Fix |
|---|---|---|
| `make parity-naive CASE=i02-tie-half-cent` ended with `service "estate" is not running` | only the `db` service had been started (by `make parity-java`) | `make up` first |
| `git status` showed `tools/runjcl/__pycache__/runjcl.cpython-310.pyc` modified after `make parity` | tracked bytecode rewritten by the run | `git checkout -- tools/parity/__pycache__ tools/runjcl/__pycache__` |

No test or parity failure occurred on the way.

## 7. Dependencies

None added. `pom.xml` is unchanged. CI uses `actions/setup-java@v4` (Temurin 21) to build the jar on the runner;
considered instead: building in a Maven container, which would need the Docker socket for nothing the runner does
not already have.
