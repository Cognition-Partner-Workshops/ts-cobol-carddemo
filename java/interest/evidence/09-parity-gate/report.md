# Ticket 09-parity-gate - report

Branch `demo/05-09-parity-gate`, code revision `93d0eeec2b7487aacd46fc0d9e10ce417a71fc65`, base `demo/05b-integration` (`d0e361b`).
Evidence in this directory: `test-output.txt`, `parity-report.md`, `switch-test-output.txt`.

## 1. Result

| Acceptance line | Met | Shown by |
|---|---|---|
| Run: make parity-java. 7 of 7 cases PASS in work/parity-java/report.md | yes | `parity-report.md`: "All cases" (exit code 0) and "The gate's own report" (Gate: PASS, seven rows `0 / PASS / PASS`). Every launch is loadExtractsStep, calcInterestStep on the tables, exportDatasetsStep ("Job repository") |
| the legacy make parity is still green | yes | `parity-report.md`, last section: `make up && make build && make parity`, 27 cases PASS, exit code 0 |
| Negative control: with interest.policy.rounding=HALF_EVEN the gate fails on i02-tie-half-cent only | yes | `switch-test-output.txt`: exit code non-zero, i02 FAIL with 2 differences (TRAN-AMT 12.35/12.36, ACCT-CURR-BAL 1247.85/1247.86), six cases PASS, every job exit code 0 |
| CI: the workflow runs the gate and the negative control on every pull request and uploads the report | yes (the check is not marked required: a branch protection setting) | `.github/workflows/parity.yml`: five new steps; the run is named at the end of this file |
| Errors: a case whose run fails is reported as failed, not skipped | yes | `switch-test-output.txt`: a job ending with exit code 12 gives `java job ended with exit code 12`, comparator FAIL, interest_run FAIL, a row in the summary and a non-zero exit of the target |
| Database assertions: after each case RC1, RC2 and RC5 hold in interest_run | yes | the gate: 20 assertions per case against the input files and the exported files (`parity-report.md`, "The gate's own report"); `InterestJobTablesIT.fixtureCase_loadCalcExportGiveTheRecordedFiles` with the figures of the RC table of strategy-interest.md |
| java/interest/README.md lists every legacy behaviour reproduced on purpose, each with its decision id | yes | `java/interest/README.md`, "Legacy behaviours reproduced on purpose" |
| Deliverables: list of gaps - INT-Q06, INT-R08, INT-H02 to INT-H06 not proven by this gate | yes | README "Not proven by this gate"; section 2 |

Built under the coordinator's grant (no ticket owned it): `interestCalcJob` can run the three steps.
`interest.job.steps=load-calc-export` (profile `tables`) joins loadExtractsStep, calcInterestStep and
exportDatasetsStep; `batch/InterestRunHandover` gives the export the key of the run (`interestRunId` ->
`interest.run.id`). The default `calc` is the job as it was; all 228 unit and 90 integration tests of the base pass
unchanged.

## 2. What the ticket does not cover

- Not proven by this gate (no fixture): INT-Q06, INT-R08, INT-H02, INT-H03, INT-H04, INT-H05, INT-H06. They are tested on synthetic data by the Maven suite, except INT-H06 (scheduler order, outside the job).
- Restart of the three-step job does not give the result of an uninterrupted run (finding 1, standards S6.3 FAIL). Not part of the ticket's Acceptance; not fixable in its files.
- "Required CI check": the workflow runs the gate; the branch protection rule is not a file (finding 7).
- Host format (EBCDIC) is not run by the gate; RC3, RC4, RC6 are not checked by the gate.

## 3. For the decision register

1. **REL-11 / architecture.md section 6: a restart of the three steps does not work as designed.** Section 6 repeats `loadExtractsStep` and lets `calcInterestStep` continue after its last committed account. In one job these two contradict each other: the repeated load truncates and refills `account` (the committed interest of the first execution is gone) and opens a second `interest_run` row; the calculation continues the first row; the export is handed the second row, finds it `LOADED` and refuses. Observed with accounts 2001, 2002, 2007, 2009, 2010 and a forced failure at 2009 after the commit of 2001 and 2002: restarted job `FAILED` (`loadExtractsStep:COMPLETED, calcInterestStep:COMPLETED, exportDatasetsStep:FAILED`, `export refused: run 2 has status LOADED`), accounts 2001 and 2002 back at 1000.00 and 2400.00 while their transactions exist, run 1 `CALCULATED` with `written_account_count` 4. Test: `InterestJobRestartIT.rel11_restartOfTheThreeStepsExportsNothing`. The alternative of section 6, a new launch from the start, gives the rows of an uninterrupted run (`rel11_newLaunchAfterAStopGivesTheRowsOfAnUninterruptedRun`). To decide: the load is not repeated once the calculation has committed, or REL-11 becomes option B (restore and rerun). Suggested owner: target architect with the mainframe operations owner.
2. **`interest.job.steps` is a new setting and its default is not the target.** No ticket owned joining the steps. The tests of tickets 01 to 08 launch `interestCalcJob` as one step (file adapters, or tables filled by test support), so the default stays `calc` and the three steps need `interest.job.steps=load-calc-export` with `interest.calc.store=jdbc` (profile `tables`). A production launch without the profile runs the file adapters. Suggested owner: target architect.
3. **Two names for one key.** Tickets 05 and 06 use `interestRunId` in the job execution context, ticket 07 reads `interest.run.id`; `batch/InterestRunHandover` copies one to the other. One name would remove the class. Suggested owner: target architect.
4. **`interest_run.finished_at` is empty on a run that reached `EXPORTED`, and `started_at` is the run clock** (2022-07-05 10:00:00 in every parity run), although the schema comment says "wall clock of the target run". Seen in the `db` service after the gate. Suggested owner: tickets 05 and 07 / data engineering lead.
5. **RC5 as the gate checks it holds only for INT-Q04 as coded.** The assertion is the strategy's sentence: sum of TRAN-AMT = balance change + interest of the last account. With `interest.policy.updateLastAccount=true` the last term must go. Suggested owner: business owner (INT-Q04).
6. **The gate proves the as-coded candidate on seven cases only.** INT-Q06, INT-R08, INT-H02 to INT-H06 have no fixture; the host format (REL-08) cannot be run through the job because `InterestJobParameters` accepts `format=fixture` only; RC3, RC4 and RC6 are not checked by the gate.
7. **"Required CI check" is a branch protection setting.** The workflow now runs the Maven suite, the gate and the negative control on every pull request; making the job `parity` required for `main` and `demo/05b-integration` is a repository setting a person has to set. Until this ticket CI did not build `java/interest` at all.
8. **The `db` service is shared by every launch.** Each launch replaces the four replica tables; `interest_run` and `interest_transaction` keep every run (retention is REL-12). The gate finds the row of a launch as the one above the highest `run_id` before it, because the job execution context is stored as serialized Java, not as text SQL can read.
9. Open items of earlier tickets, unchanged: a stopped run writes no `rc.json`; `JdbcAccountStore` raises `WRITE_OUTPUT` for a failed account update and `batch/DatabaseFailureStops` turns it into `ERROR RE-WRITING ACCOUNT FILE`.

## 4. Standards review checklist

Standards:

- [x] S1.1 Sources used in order; recorded output decides - PASS: ticket, architecture.md sections 4 to 6, strategy RC table, then the recorded files; `InterestJobTablesIT` compares the four files byte for byte
- [x] S1.2 Rule/code disagreements reproduced as coded and reported - PASS: none found in the COBOL by this ticket; the design disagreement on restart (architecture.md section 6) is reported for the register
- [x] S2.1 Designed from rules; no method per paragraph - N/A: no calculation code; the assembly has one class per job decision (`JobSteps`, `InterestRunHandover`)
- [ ] S2.2 Immutable business records; copybook names only at the mapping - N/A: no record or mapping added; the Makefile names copybook fields only to decode the files with tools/parity/copybook.py
- [x] S2.3 Spring Batch shape, transaction and restart boundaries of the architecture - PASS for the shape: `interestCalcJob` = loadExtractsStep, calcInterestStep, exportDatasetsStep (`InterestJobStepsTest.jobSteps_loadCalcExportInThatOrderOnTheTables`, `InterestJobTablesIT`); the steps and their transaction boundaries are unchanged. Restart: see S6.3
- [x] S2.4 Types and return values, no flags or result codes - PASS: `JobSteps` enum; a refused combination is an exception at start
- [x] S2.5 Constructor injection; no mutable static state - PASS: `InterestRunHandover(Step)`, bean method parameters
- [ ] S3.1 `BigDecimal` with data-map precision and scale; no `double`/`float` - N/A: no arithmetic in main code; tests use `BigDecimal` from strings, the Makefile uses Python `Decimal` and SQL `numeric`
- [ ] S3.2 Formula: exact product, one division, mode from the policy - N/A: calculation not touched
- [ ] S3.3 Rounding mode named on every other arithmetic step - N/A: calculation not touched
- [ ] S3.4 Range check before store; overflow goes to the policy - N/A: calculation not touched
- [ ] S3.5 Encodings only in `record-codec` - N/A: codec not touched
- [ ] S3.6 Width, padding and FILLER only in codec and schema - N/A: codec and schema not touched
- [ ] S3.7 Code page and key order each in one place - N/A: no reader or query added to the job
- [x] S3.8 Clock and run parameters injected; `parmDate` is text - PASS: the gate passes `--interest.clock.frozen` and `parmDate` as before; a PARM of 8 characters is refused, exit code 12 (`switch-test-output.txt`)
- [ ] S4.1 Every PRESENT behaviour touched has its test; new ones reported - N/A: no behaviour of section 4 touched; none new found
- [x] S5.1 Each quirk/open decision in one named place behind its switch, rule id in names - PASS: no quirk code added; the negative control switches INT-Q01 through `interest.policy.rounding` only
- [x] S5.2 Nothing fixed, tidied or added without a rule id - PASS with a note: `interest.job.steps` is a new setting with no rule id; it changes no output (default `calc` is the job as it was) and is reported for the register
- [ ] S6.1 Status and message mapping as in section 6 - N/A: not touched; the run log lines of a missing account through the three steps are asserted in `InterestJobTablesIT.intR08_missingAccountStopsTheJobInTheCalculation`
- [x] S6.2 One stop exception, one exit-code constant, no skip or retry - PASS: the job adds no skip, retry or exit code; a failed step ends the job (`InterestJobTablesIT.intR08_*`: later steps do not run)
- [ ] S6.3 Restart as in the architecture - FAIL: the three steps are joined as section 6 says (load repeats, calculation continues), and that does not work: the repeated load replaces the account table and opens a new interest_run row, the committed updates are lost and the export refuses the run (`InterestJobRestartIT.rel11_restartOfTheThreeStepsExportsNothing`). Not fixable in the files of this ticket; reported for REL-11. A new launch after a stop works (`rel11_newLaunchAfterAStopGivesTheRowsOfAnUninterruptedRun`)
- [x] S6.4 Outputs in the recorded format; failed run leaves no SYSTRAN - PASS: seven cases byte for byte through the database; a stopped job leaves no `datasets` directory and no `rc.json` (`InterestJobTablesIT.intR08_*`, `InterestJobRestartIT`)
- [x] S6.5 Run log byte for byte; no account data in the application log - PASS: SYSOUT compared by the gate for all seven; the new classes log nothing; the gate report holds counts and sums only
- [ ] S7.1 A test per rule id, named for it - N/A for rules: the ticket implements no rule. Tests of the assembly are named for what they prove (`jobSteps_*`, `handover_*`, `fixtureCase_*`), those on a rule or decision carry its id (`intR08_*`, `rel11_*`)
- [x] S7.2 Fixture and edge cases of each rule covered - PASS: all seven cases in `InterestJobTablesIT` and in the gate; INT-Q06, INT-R08, INT-H02 to INT-H06 have no fixture and are listed as not proven by the gate
- [x] S7.3 Integration tests on PostgreSQL 15 in a container - PASS: `InterestJobTablesIT`, `InterestJobRestartIT` on `postgres:15` with both init scripts (`ReplicaTables.postgres()`)
- [x] S7.4 No assertion against the code's own output - PASS: recorded files, the RC table of strategy-interest.md written by hand, COBOL message lines; the gate decodes the input files with the estate's decoder
- [x] S8.1 No edit under `app/`, `fixtures/`, `tools/parity` - PASS: `git diff --stat origin/demo/05b-integration -- app fixtures tools` is empty
- [x] S8.2 No golden file re-recorded - PASS: `make record` not run
- [x] S8.3 Every new dependency explained in the pull request - PASS: none added (Mockito comes with `spring-boot-starter-test`); CI adds `actions/setup-java@v4`
- [x] S8.4 Frozen shared interfaces unchanged - PASS: FI-1 to FI-5 untouched; FI-6 (`make parity-java [CASE=<case>]`, `work/parity-java/<case>/actual`) keeps its name, argument and output place and is the target this ticket owns
- [x] S8.5 Synthetic data only - PASS: fixture data only

Behaviours PRESENT in section 4 (k, n and v are ABSENT and have no line):

- [ ] a Truncation without ROUNDED - N/A: not touched; proven again through the database by i02, i07 and the negative control
- [ ] b High-order digit loss (range check, STOP) - N/A: not touched, no fixture
- [ ] c Intermediate precision of the COMPUTE - N/A: not touched
- [ ] d Zoned overpunch signs - N/A: not touched; i06 passes through load and export
- [ ] e Binary items stay out of data and domain - N/A: not touched
- [ ] f Non-numeric data rejected at the boundary - N/A: not touched; `InterestJobTablesIT.intR08_rejectedRecordStopsTheJobInTheLoad` shows the job stops in the load
- [ ] g Fixed width, padding, STRING - N/A: not touched; compared field by field for all seven
- [ ] h Code page and collating order - N/A: not touched
- [ ] i REDEFINES replaced by formatter and function - N/A: not touched
- [ ] j FILLER and record length round trip - N/A: not touched; ACCTDATA passes through load, calculation and export for all seven
- [ ] l No state carried between accounts except the counter - N/A: not touched
- [ ] m Control break and end of file (INT-Q04, INT-T09) - N/A: not touched; RC5 of the gate holds with the interest of the last account not added
- [ ] o Level-88 result codes replaced by types - N/A: not touched
- [ ] p Indexed access through the ports; several cards to the policy - N/A: not touched
- [ ] q File statuses and abend - N/A: not touched
- [x] r PARM, generation, disposition, return code - PASS: `rc.json` and `SYSTRAN.G0001V00` compared for all seven; no generation after a stop
- [x] s DISPLAY output in the run log - PASS: SYSOUT compared for all seven through the three steps (the export keeps the run log of the calculation)
- [ ] t PARM text against the clock - N/A: not touched
- [ ] u Timestamp format and hundredths - N/A: not touched
- [x] w PARM length checked - PASS: PARM of 8 characters gives exit code 12 and a failed case in the gate
- [ ] x No stale rate after a failed lookup - N/A: not touched
- [ ] y Typed fields where COBOL mixed text and numbers - N/A: not touched
- [x] z Abend ends the step; stub output not reproduced - PASS: a stopped step ends the job, the steps after it do not run
- [ ] aa Dead and empty code not ported - N/A: not touched

## 5. What is wrong or missing in the standards skill

- S6.3: "`loadExtractsStep` and `exportDatasetsStep` repeat; `calcInterestStep` continues" cannot both hold in one job (finding 1). Proposed: state which of the two applies once the calculation has committed a chunk, after REL-11 is decided.
- S6.4: the sentence on a stopped run covers the file adapters only. Proposed addition: "**With the tables:** a stopped run leaves the run log (`sysout/INTCALC.STEP15.txt`) under the output directory and no `datasets` directory and no `rc.json`."
- S7.1: no naming rule for a test of code that implements no rule (job assembly, gate). Proposed: "A test of code with no rule id is named `<whatIsTested>_<whatItProves>`."
- Section 9 has no line for the reconciliation figures. Proposed line: "RC1, RC2, RC5 hold in interest_run after every fixture case - PASS / FAIL / N/A".

## 6. Method

Commands, from the repository root, JDK 21.0.13 (`JAVA_HOME` as interest-ticket-build step 2):

1. `make parity-java` on the base before any change: seven PASS on the file adapters.
2. `mvn -B -f java/interest/pom.xml clean verify`: 234 unit and 101 integration tests, 0 failures (`test-output.txt`). Base: 228 and 90. New: `InterestJobStepsTest` (6), `InterestJobTablesIT` (9), `InterestJobRestartIT` (2).
3. `.agents/skills/interest-ticket-verify/parity-report.sh 09-parity-gate <the seven cases>`: exit code 0 (`parity-report.md`).
4. `make parity-java INTEREST_OUT=work/parity-java-half-even INTEREST_OPTS=--interest.policy.rounding=HALF_EVEN`, a stopped job and the file adapters (`switch-test-output.txt`).
5. `make up && make build && make parity`: exit code 0.
6. Error path from the command line (interest-ticket-verify step 6): the ticket's error line is the gate's own ("a case whose run fails is reported as failed"), shown in step 4 with a PARM of 8 characters. A stop inside the load and inside the calculation of the three-step job is shown by `InterestJobTablesIT.intR08_*`, because a changed input cannot be a case under `fixtures/`.
7. Database assertions (step 7): made by the gate itself in the `db` service after each case, and by `InterestJobTablesIT` on Testcontainers.

Steps of the skills that did not fit: interest-ticket-verify step 1 and step 8 describe `make parity-java` as a run of the file adapters; step 4's command needs `--spring.profiles.active=tables` to run what the gate runs; the job repository query of step 7 and of `parity-report.sh` lists the steps in no fixed order.

Failures hit:

| Symptom | Cause | Fix |
|---|---|---|
| `InterestJobStepsTest`: `IllegalStateException: JobRepository must be set` (2 errors of 6) | `JobBuilder.build()` needs a repository even when the job is never launched | a Mockito mock of `JobRepository` in the test |
| first version of `InterestJobRestartIT`: `expected: <COMPLETED> but was: <FAILED>` on the restart | finding 1: the design of section 6, not a defect of one class | the test now states what happens and a second test proves the new launch; reported |
| `batch_job_execution_context.short_context` is not readable in SQL (`rO0ABXNy...`) | Spring Batch 5.2 stores the context as serialized Java | the gate takes the `interest_run` row above the highest `run_id` before the launch |
| ` M tools/runjcl/__pycache__/runjcl.cpython-310.pyc` after `make parity` | tracked .pyc rewritten, like pitfall P6 but another directory | `git checkout -- tools/runjcl/__pycache__` |

CI run of the last code commit: see the end of this file.

## 7. Dependencies

None added to `java/interest/pom.xml` (frozen). The tests use Mockito, which `spring-boot-starter-test` already brings.
CI: `actions/setup-java@v4` (Temurin 21) to build and test `java/interest` on the runner; the alternative, building
inside a Maven container, would not give Testcontainers the runner's Docker as simply.

## CI run (Final-revision evidence of the ticket)

Workflow `estate-parity`, job `parity`, event `pull_request`, commit `87fbae6cfe552cb52b3a02891dc7ce939685152e`
(code commit `93d0eee` plus the first evidence commit; the commit that adds this section changes this file only):
https://github.com/Cognition-Partner-Workshops/ts-cobol-carddemo/actions/runs/43714496306 - success. Steps: legacy
`make parity` and `make parity-naive` as before; `mvn -B -f java/interest/pom.xml verify` (234 unit, 101 integration
tests, BUILD SUCCESS); `make parity-java` (seven cases PASS, 20 of 20 assertions each); the negative control (i02 FAIL
with 2 differences, six PASS); artifact `parity-java-report` uploaded (both `report.md` files and the log of the
negative control). Section 1, line "CI": met.
