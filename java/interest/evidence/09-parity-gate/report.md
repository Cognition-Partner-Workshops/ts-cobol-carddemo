# Ticket 09 parity gate - report

Ticket: `modernization/design/tickets/09-parity-gate.md` (INT-T09). Branch `demo/05-g4-gate`, the only ticket of group
g4-gate, on `demo/05b-integration` with tickets 01 to 08 and 10. Code revision the evidence was produced from: `422fc1c`.

Read this with [test-output.txt](test-output.txt) and [parity-report.md](parity-report.md).

**What the gate proves:** the as-coded candidate (every switch at its legacy default) writes, through PostgreSQL, the
recorded legacy output of the seven fixture cases, field by field. It proves nothing about behaviour no fixture holds
(section 2) and nothing about Enterprise COBOL on z/OS: the golden files come from GnuCOBOL off-host.

## 1. Result

| Acceptance line of the ticket | Result | Where it is shown |
|---|---|---|
| Run: `make parity-java`. 7 of 7 cases PASS in `work/parity-java/report.md`; the legacy `make parity` is still green | met: `parity-java: 7 of 7 cases PASS`, exit code 0; each case ran `loadExtractsStep`, `calcInterestStep`, `exportDatasetsStep` (all COMPLETED); `make parity` exit code 0, every chain and case PASS | parity-report.md, sections "All cases", "Comparator report", "Job repository"; `InterestJobFlowIT.fixtureCase_threeStepsThroughTheTables_writeTheRecordedOutput` (seven cases) |
| Negative control: with `interest.policy.rounding=HALF_EVEN` the gate fails on i02-tie-half-cent only, as `make parity-naive` does | met: 6 of 7, exit code non-zero; i02 differs in `TRAN-AMT` (12.36 against 12.35) and `ACCT-CURR-BAL` (1247.86 against 1247.85), the same two fields and values as `make parity-naive CASE=i02-tie-half-cent` | parity-report.md, section "Negative control" |
| CI: the workflow runs the gate and the negative control on every pull request and uploads the report | met: `.github/workflows/parity.yml` (trigger `pull_request`, no branch filter) runs the Maven suite, `make parity-java`, the negative control, and uploads the artifact `parity-java-report`. Run of revision `422fc1c`: success, https://github.com/Cognition-Partner-Workshops/ts-cobol-carddemo/actions/runs/43756111281 . **Not met: "required check"** - making a check required is a branch-protection setting of the repository, not a file; this session cannot read or set it (section 2) | the run above; section 6 |
| Errors: a case whose run fails is reported as failed, not skipped | met: the case prints `java job ended with exit code 12`, the comparator's FAIL, counts as not passed and the target exits non-zero. Shown with a refused launch; a failed load ends the job FAILED after `loadExtractsStep` with no output directory | parity-report.md, section "Errors"; `InterestJobFlowIT.aFailedLoad_failsTheJob_noCalculationNoExportNoOutput` |
| Database assertions: after each case RC1, RC2 and RC5 hold in `interest_run` | met: per case 8 of 8 RC1 lines, 10 of 10 RC2 lines and 6 of 6 RC5 lines EQUAL; the gate fails a case when one is not EQUAL (NOT AVAILABLE included) | parity-report.md, section "Database assertions"; `InterestJobFlowIT.i01Baseline_rc1Rc2Rc5_holdInTheRowOfTheRun` (figures by hand) |
| `java/interest/README.md` lists every legacy behaviour reproduced on purpose, each with its decision id | met | README, "Legacy behaviour reproduced on purpose" and "Not proven by this gate" |
| Build: `make up`, `make build`, `make parity-java` | met | parity-report.md; CI run |
| Tests: the full Maven suite | met: 396 unit and 105 integration tests, 0 failures, BUILD SUCCESS (before this ticket: 391 and 95) | test-output.txt |
| Fixture cases: all seven | met, 7 of 7 PASS | parity-report.md |
| Final-revision evidence: the CI run of the last commit and its uploaded report | the run above is of the last code commit; the run of the branch head is linked in the pull request | pull request |

Changed: `batch/InterestJobConfig` (granted: `interest.job.steps`, the three-step flow), `Makefile` target `parity-java`,
`.github/workflows/parity.yml` (new steps), `java/interest/README.md`. New: `batch/LoadedRunHandOver` (granted: joins
the steps), `src/main/resources/application-database.yml` (profile), tests `batch/InterestJobFlowIT` (10) and
`batch/InterestJobStepsTest` (5). No domain, codec, loader, calculation or export code was changed.

How the steps are joined: `loadExtractsStep` leaves the id of its `interest_run` row in the job context as
`interest.run.id`; `calcInterestStep` reads `interest.runId` and inserts a row of its own when it finds none. Chained
as they were, a run would have two rows (one LOADED for ever, one CALCULATED) and the export, which takes the load's
row, would refuse it. `LoadedRunHandOver` copies the id before the calculation starts; one row goes
LOADED -> CALCULATED -> EXPORTED.

## 2. What the ticket does not cover (gaps)

Not proven by this gate - no fixture case exists (ticket, Deliverables):

| Rule | What is not proven | Covered elsewhere, on synthetic data |
|---|---|---|
| INT-Q06 | a 0.00 interest transaction is written | `CalculationSwitchesTest`, ticket 04 |
| INT-R08 | the failure path: legacy messages, `ABENDING PROGRAM`, exit code 12 | `StopConditionsTest`, `ErrorPathsJdbcIT`, ticket 08; texts read from the source, not from a recorded run |
| INT-H02 | second run in a cycle, restart after a failure | `JdbcCalcStepIT.intH02_*`, ticket 06 |
| INT-H03 | an account with several cards | `ErrorPathsJdbcIT`, `MultiCardSwitchJdbcIT`, ticket 08; the host behaviour is unknown |
| INT-H04 | inactive accounts are charged | `CalculationSwitchesTest`, ticket 04 |
| INT-H05 | values that do not fit their field | `StopConditionsTest`, ticket 08; the host behaviour is unknown |
| INT-H06 | order of posting, reloads and the interest run | none: outside the job (scheduler) |

Other gaps:

| Gap | State in this branch | Owner |
|---|---|---|
| **The check is not "required"** | the workflow runs on every pull request; whether GitHub blocks a merge on it is a branch-protection setting. The API answers 403 to this session | repository administrator |
| `format=host` through the job | `InterestJobParameters` refuses every format but `fixture` (ticket code, not to be changed here); loader and exporter handle both. The gate runs `fixture` only | ticket 05/07 gap, owner of `batch/InterestJobParameters` |
| The run log of a stopped three-step run | when `calcInterestStep` stops, `outputDir/sysout/INTCALC.STEP15.txt` stays (a restart needs it, ticket 06), so "a failed run leaves no output directory" (S6.4) holds for a failed load but not for a failed calculation. The gate still reports the case as failed (exit code 12, comparator FAIL) | target architect (S6.4 against architecture section 6) |
| Restart of the three-step job | not tested here: a restart repeats the load (new `interest_run` row, tables replaced) while the calculation continues with its saved state and its first run id - by the architecture's text ("restart repeats the step"), but after a part-run the reloaded account table no longer holds the committed updates. `LoadedRunHandOver` keeps the calculation's run on a restart (`handOver_keepsTheRunOfARestartedCalculation`); what a restart should do to the replica tables is REL-11 | REL-11 owner |
| Volume | the fixtures have one or two accounts; chunk size (G11) and the 1,000,000th transaction (ticket 08 gap) are not exercised by the gate | mainframe operations owner |
| RC6 | NOT AVAILABLE in every reconciliation report: needs a mainframe unload after the write-back | mainframe operations owner |
| Default of the job | without the profile `database` the job is still the pilot's one step on files (`interest.job.steps=calc`, `interest.calc.store=file`): the earlier tickets' integration tests launch it that way and are not to be changed here. Making the three steps the default means changing those tests | target architect |
| The file adapters | `file/FileRunWorkspace` and `SevenCasesFileParityTest` remain; no gate uses them any more | target architect: keep or remove |

## 3. For the decision register

| Finding | Evidence | Suggested owner |
|---|---|---|
| Two keys for one thing: the load publishes the run as `interest.run.id` (`LoadExtractsStepConfig.RUN_ID_KEY`), the calculation reads `interest.runId` (`JdbcCalcInterestRun.INTEREST_RUN_ID`). Joined here by a listener; one key in both steps would make it unnecessary | `LoadedRunHandOver`, `InterestJobFlowIT` (one row per run, EXPORTED) | owners of tickets 05 and 06 |
| The gate's database assertions use the whole reconciliation report: a DIFFERENT line of RC3 or RC4 also fails a case, although the ticket names RC1, RC2 and RC5 only | `Makefile`, target `parity-java` | data engineering lead |
| The schema reaches the `db` service only through the init scripts of a new volume. On a volume created before `db/postgres/02-interest.sql` existed the tables are missing; `make parity-java` applies the script when `carddemo.interest_run` does not exist. Nothing migrates a schema that exists in an older form | `Makefile`, target `parity-java` | release manager (how DDL is deployed) |
| The replica tables and `interest_run` of the `db` service are shared by every case and by `make recon-java`: the gate runs the cases one after the other and each load replaces the tables (REL-02 as proposed). Two gates at once on one database would mix | `Makefile` | REL-02 owner |
| `rc.json` is written by the export with five harness steps as 0 (ticket 07, F1); the gate compares it as recorded | parity-report.md | FI-2 owner |
| A second property family beside `interest.policy.*`: `interest.job.steps` and `interest.calc.store` are not decisions of the register and not part of FI-5; they exist only because the earlier tests launch the one-step job | `InterestJobConfig`, `application-database.yml` | target architect |

## 4. Standards review checklist

Standards:

- [x] S1.1 Sources used in order; recorded output decides - PASS: the gate compares with `fixtures/interest/<case>/expected`; `InterestJobFlowIT`
- [x] S1.2 Rule/code disagreements reproduced as coded and reported - N/A: no calculation code changed; README lists what is reproduced
- [x] S2.1 Designed from rules; no method per paragraph - PASS: `InterestJobConfig.interestCalcJob`, `LoadedRunHandOver` are named for what they do
- [x] S2.2 Immutable business records; copybook names only at the mapping - N/A: no record or mapping touched
- [x] S2.3 Spring Batch shape, transaction and restart boundaries of the architecture - PASS: the job is load -> calculation -> export (architecture section 4); the steps and their transactions are unchanged; `InterestJobFlowIT`
- [x] S2.4 Types and return values, no flags or result codes - PASS: no flag added; the hand-over is a job-context entry both steps already define
- [x] S2.5 Constructor injection; no mutable static state - PASS: `LoadedRunHandOver` has no state; `interestCalcJob` gets its steps as parameters
- [x] S3.1 `BigDecimal` with data-map precision and scale; no `double`/`float` - N/A: no amounts in the changed code; the test compares sums as text from the database
- [x] S3.2 Formula: exact product, one division, mode from the policy - N/A (the negative control exercises the switch: parity-report.md)
- [x] S3.3 Rounding mode named on every other arithmetic step - N/A
- [x] S3.4 Range check before store; overflow goes to the policy - N/A
- [x] S3.5 Encodings only in `record-codec` - N/A
- [x] S3.6 Width, padding and FILLER only in codec and schema - N/A
- [x] S3.7 Code page and key order each in one place - N/A
- [x] S3.8 Clock and run parameters injected; `parmDate` is text - PASS: the gate passes `--interest.clock.frozen` and `parmDate` (Makefile); a PARM of 8 characters is refused (parity-report.md, "Errors")
- [x] S4.1 Every PRESENT behaviour touched has its test; new ones reported - N/A: none touched; none found
- [x] S5.1 Each quirk/open decision in one named place behind its switch, rule id in names - N/A: no quirk code touched; README table names switch and id
- [x] S5.2 Nothing fixed, tidied or added without a rule id - PASS: the only additions join existing steps (INT-T09)
- [x] S6.1 Status and message mapping as in section 6 - N/A
- [x] S6.2 One stop exception, one exit-code constant, no skip or retry - PASS: the flow has no skip, retry or conditional transition; a step that fails ends the job (`aFailedLoad_failsTheJob_noCalculationNoExportNoOutput`); exit code 12 in parity-report.md
- [x] S6.3 Restart as in the architecture - N/A for the code (step restart state unchanged); the restart of the joined job is not tested - gap in section 2
- [ ] S6.4 Outputs in the recorded format; failed run leaves no SYSTRAN - PASS for the recorded format (7 of 7) and for no SYSTRAN after a failure (the export does not run); **FAIL in one point not caused here**: after a stopped calculation the run log stays in the output directory (ticket 06 behaviour; gap in section 2)
- [x] S6.5 Run log byte for byte; no account data in the application log - PASS: `sysout/INTCALC.STEP15.txt` equal in all seven cases; no log statement added
- [x] S7.1 A test per rule id, named for it - N/A: the ticket implements no rule id; its tests are named for what they prove
- [x] S7.2 Fixture and edge cases of each rule covered - PASS: all seven cases through the three steps; a failed load; settings refused
- [x] S7.3 Integration tests on PostgreSQL 15 in a container - PASS: `InterestJobFlowIT` on `postgres:15` with `db/postgres/*.sql`
- [x] S7.4 No assertion against the code's own output - PASS: expected side is the recorded files; the RC figures are written by hand with the arithmetic
- [x] S8.1 No edit under `app/`, `fixtures/`, `tools/parity` - PASS: `git diff --stat origin/demo/05b-integration -- app fixtures tools modernization .agents db docker-compose.yml` prints nothing
- [x] S8.2 No golden file re-recorded - PASS
- [x] S8.3 Every new dependency explained in the pull request - PASS: none (section 7); CI uses `actions/setup-java@v4`
- [x] S8.4 Frozen shared interfaces unchanged - PASS: FI-6 keeps `make parity-java [CASE=<case>]` and `work/parity-java/<case>/actual`; FI-3, FI-4, FI-5 untouched
- [x] S8.5 Synthetic data only - PASS: fixture data only

Behaviours PRESENT in section 4. The ticket changes none of them; each line is what the gate shows for it.

- [x] a Truncation without ROUNDED - PASS: i02-tie-half-cent through the database; negative control
- [x] b High-order digit loss (range check, STOP) - N/A: no fixture
- [x] c Intermediate precision of the COMPUTE - PASS: i07-nontie-rounding
- [x] d Zoned overpunch signs - PASS: i06-negative-balance, loaded and exported
- [x] e Binary items stay out of data and domain - N/A
- [x] f Non-numeric data rejected at the boundary - N/A: no fixture
- [x] g Fixed width, padding, STRING - PASS: datasets equal in seven cases
- [x] h Code page and collating order - PASS for the fixture code page only
- [x] i REDEFINES replaced by formatter and function - N/A
- [x] j FILLER and record length round trip - PASS: RC3 EQUAL in every `recon.md`; ACCTDATA image equal
- [x] l No state carried between accounts except the counter - PASS: seven cases
- [x] m Control break and end of file (INT-Q04, INT-T09) - PASS: seven cases; RC5 names the interest of the last account
- [x] o Level-88 result codes replaced by types - N/A
- [x] p Indexed access through the ports; several cards to the policy - N/A: no fixture with several cards
- [x] q File statuses and abend - N/A: no fixture
- [x] r PARM, generation, disposition, return code - PASS: `G0001V00`, `rc.json`, PARM 2022071800 in seven cases
- [x] s DISPLAY output in the run log - PASS: SYSOUT equal in seven cases (i04 for the fallback lines)
- [x] t PARM text against the clock - PASS: seven cases
- [x] u Timestamp format and hundredths - PASS: seven cases, frozen clock only
- [x] w PARM length checked - PASS: parity-report.md, "Errors"
- [x] x No stale rate after a failed lookup - PASS: i04-default-group-fallback
- [x] y Typed fields where COBOL mixed text and numbers - PASS: seven cases (`TRAN-CAT-CD` 0005)
- [x] z Abend ends the step; stub output not reproduced - N/A: no fixture
- [x] aa Dead and empty code not ported - PASS: no fee transaction in any case

## 5. What is wrong or missing in the standards skill (proposals only)

1. **S6.4 against the run log.** S6.4 says a failed run leaves no SYSTRAN and, for file adapters, no output directory;
   architecture section 6 needs the run log of a stopped calculation for the restart. Say where the run log of a stopped
   run lives when the steps are joined (in the output directory, or beside it like the load's reject list), so "no
   output" has one meaning.
2. **A standard for job-context keys.** Nothing says how steps hand data to each other; two tickets chose two keys for
   the run id. Proposed S2.6: values passed between steps are named once, in `batch`, and every step uses that constant.
3. **S2.3 and the restart of the whole job.** The standard describes the restart of `calcInterestStep` and says the load
   "repeats"; it does not say that a repeated load replaces account rows the failed calculation had committed. Add the
   expected state of the replica tables on a restart, or say that a restart must not repeat the load (REL-11).
4. **Section 9 for a gate ticket.** Most lines are N/A for a ticket that changes no product code. A line for gates would
   help: "the gate fails when the candidate is wrong (negative control), and a failed run is a failed case".
5. **S7.1** has no naming rule for a test that proves a work item (INT-T ids) rather than a rule; `intT09_...` is already
   used for the empty-input behaviour (row m), which collides with ticket 09's work item id INT-T09.

## 6. Method

Skills used: `interest-conversion-standards` (sections 1, 2, 6, 7, 8, 9), `interest-ticket-build` (steps 1-9),
`interest-ticket-verify` (steps 1-10), `interest-pitfalls` (P1, P3, P6, P7), `carddemo-parity`.

Commands, from the repository root with `JAVA_HOME` on JDK 21:

```sh
mvn -B -f java/interest/pom.xml clean verify      # baseline on origin/demo/05b-integration: 391 / 95, BUILD SUCCESS
make up && make build
make parity-java                                  # 7 of 7
make parity-java INTEREST_ARGS="--interest.policy.rounding=HALF_EVEN" PARITY_JAVA_OUT=work/parity-java-half-even
make parity-java CASE=i01-baseline INTEREST_PARM=20220718 PARITY_JAVA_OUT=work/parity-java-error
mvn -B -f java/interest/pom.xml clean verify      # 396 / 105, BUILD SUCCESS -> test-output.txt
.agents/skills/interest-ticket-verify/parity-report.sh 09-parity-gate $(ls fixtures/interest)
make parity                                       # exit code 0
```

Failures hit:

| Symptom | Cause | Fix |
|---|---|---|
| Reading the code: chained as they were, the calculation would not see the run of the load | two job-context keys (section 3) | `LoadedRunHandOver`, registered before the step's own listener |
| First version of `InterestJobFlowIT.theCalculationStep_isTheSecondOfThree` expected two items for i05-multi-category | the case has one account with two balances; one item is one account | the test expects 1 (my error, found before the first run) |
| The step list in parity-report.md, section "Job repository", is in no fixed order (`exportDatasetsStep` before `loadExtractsStep` in some rows) | `string_agg` without `ORDER BY` in `parity-report.sh` and in `interest-ticket-verify` step 7 | none here (the skill is not to be edited); correction reported in the pull request |
| `gh api .../branches/main/protection` answers 403 | the session's token may not read branch protection | none; reported as a gap |

None of P1 to P7 occurred beyond P6 (the `.pyc` files, restored before every commit).

## 7. Dependencies

No new Maven dependency. CI: the action `actions/setup-java@v4` (Temurin 21, Maven cache), needed because the gate
builds the jar on the runner; the alternative, building inside a Maven container, would have added an image and a
second build path next to the one `make parity-java` uses locally.
