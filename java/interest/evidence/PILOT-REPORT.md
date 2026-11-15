# Ticket 01 pilot - report

Ticket: `modernization/design/tickets/01-pilot-thin-slice.md` (INT-T01), the file
`modernization/design/tickets/README.md` names as the pilot. Branch `demo/05-pilot`, started from
`demo/04a-standards` at `f64e0fb`. Code revision the evidence was produced from: `0294cc0`.

Read this with [parity-report.md](parity-report.md) (open that first) and [test-output.txt](test-output.txt).

## 1. Result

| Acceptance line of the ticket | Result | Where it is shown |
|---|---|---|
| `make parity-java CASE=i01-baseline` writes the two datasets (2 records each), the SYSOUT file and rc.json under `work/parity-java/i01-baseline/actual` | met | parity-report.md, "Acceptance run" |
| `compare.py interest i01-baseline --actual ... --label java` exits 0 and says PASS | met | parity-report.md, "Result per case" |
| TRAN-AMT 10.00 and 37.00; ACCT-CURR-BAL 1010.00 for 00000002001; 2400.00 unchanged for 00000002002 (INT-Q04) | met | `InterestCalcJobIT.intR05_observableAmountsOfTheTicket`, `intR07_updatesAccountAtBreak`, `intQ04_lastAccountNotUpdated` |
| A missing input file ends the job FAILED, non-zero exit code, no output directory left half-written | met | `InterestCalcJobIT.intR08_missingInputFileFailsTheJobAndLeavesNoOutput`; command-line run in section 6 (exit 12) |
| Job repository: one COMPLETED execution of interestCalcJob with one step; no table in schema carddemo beyond `db/postgres/01-transaction-type.sql` | met | `InterestCalcJobIT.jobRepository_oneCompletedExecutionWithOneStep`, `schemaCarddemo_holdsOnlyTheTablesOfTheEstate`; parity-report.md, last section |
| `mvn -q -f java/interest/pom.xml verify` on JDK 21 | met: 60 unit tests and 12 integration tests, 0 failures | test-output.txt |

Parity per case: i01, i02, i05, i06, i07 PASS; i03 and i04 FAIL (the job stops, exit 12, no output). Only
i01-baseline is claimed by this ticket.

Not touched: `app/`, `fixtures/`, `tools/parity`, `modernization/`, `.agents/`, `db/`, `docker-compose.yml`
(`git diff --stat origin/demo/04a-standards -- app fixtures tools modernization .agents db docker-compose.yml` is empty).
No golden file was re-recorded. No ticket system was written to. Nothing was merged.

## 2. What the ticket does not cover (gaps)

Rules not covered, as the ticket lists them: **INT-R03, INT-R04, INT-R08 (legacy messages and status lines), INT-Q05, INT-Q06**.

| Gap | State in this branch | Owner ticket |
|---|---|---|
| INT-R03 DEFAULT fallback | not built; a missing group rate stops the run (`RATE_FALLBACK_NOT_BUILT`). i04 fails | 04 |
| INT-R04 zero rate | not built; a zero rate stops the run (`ZERO_RATE_NOT_BUILT`). i03 fails | 04 |
| INT-Q05 negative balance, INT-Q06 0.00 interest | no switch and no test. The formula is sign-neutral, so i06 passes, but that is not proof of the rule | 04 |
| Several categories per account | no fixture test in this ticket; i05 passes because one item is one account | 04 |
| INT-R08 run-log lines of a stop (`ACCOUNT NOT FOUND`, `FILE STATUS IS`, `ABENDING PROGRAM`) | not written; a stop raises `InterestRunStoppedException` and the run leaves no output at all | 08 |
| INT-H03 several cards | stops, fixed, no switch `interest.policy.multiCard` yet | 04 |
| INT-H05 overflow | no switch; the codec refuses a value that does not fit its field instead of cutting it | 04 |
| INT-Q03 other value `CYCLE_DATE` | refused at start-up: it needs a cycle-date job parameter the architecture does not list | 04 / architect |
| Business tables, JDBC adapters, restart state | none; ports have file adapters only | 02, 06 |
| `loadExtractsStep`, `exportDatasetsStep`, `interest_run` | not built; the pilot job has one step and the file adapter writes the outputs on normal end | 05, 07 |
| Copybook-driven codec, host code page, `format=host` | layouts are declared by hand for the fixture code page; `format=host` is refused | 03 |
| CI | none | 09 |

The ticket said the six other cases are expected to fail until ticket 04. Four pass already (i02, i05, i06, i07).
The ticket text should say: i03 and i04 fail; the others may pass but are accepted only by ticket 04.

## 3. For the decision register

| # | Finding | Evidence | Suggested owner |
|---|---|---|---|
| 1 | **rc.json of the recorded runs lists steps that are not INTCALC's.** INTCALC has one step, STEP15. The golden rc.json also holds STEP05, STEP10, STEP20, STEP25, STEP30, which are steps of the harness's setup jobs (XREFFILE has all six) carried into the first job's entry. To pass FI-2 the Java job writes them as constant 0. A decision is needed on what the target's return-code output is outside the harness. | `tools/parity/record.py:131`; `app/jcl/XREFFILE.jcl`; `RecordedReturnCodes.java` | mainframe operations owner (with REL-10) |
| 2 | **Exit code of a stopped run.** REL-10 is open; this branch uses 12 (the off-host stub's value), in one constant. A launch that is refused before the job starts (parmDate not 10 characters, `format=host`) also exits 12 and leaves no job execution in the repository. | `InterestExitCode.java`, `InterestExitCodes.java` | mainframe operations owner |
| 3 | **Time zone of the run clock.** CBACT04C takes local time from CURRENT-DATE. The Java job uses the JVM's default zone; in the JRE container that is UTC. The fixtures freeze the clock, so no case shows a difference. Which zone the target runs in changes TRAN-ORIG-TS and TRAN-PROC-TS (INT-Q03). | `InterestJobConfig.runClock`; standards row u | mainframe operations owner |
| 4 | **What survives a stop differs from the legacy inside one chunk.** The legacy rewrites each account at the break, so on an abend every account before the failing one is already updated (`CBACT04C:194-205`: 1050-UPDATE-ACCOUNT of the previous account runs before 1100-GET-ACCT-DATA of the next). With chunks of whole accounts, the accounts of the failing chunk that came before the failing one are rolled back. In the pilot (file adapters) a stopped run keeps nothing. REL-11 should state this difference. | `architecture.md` sections 5-6; `CalcInterestRun.afterStep` | release owner (REL-11) |
| 5 | **INT-H03 default.** The pilot stops on several cards without a switch, as the architecture proposes. Still OPEN. | `InterestCalculator.cardNumberOf` | business owner |
| 6 | **INT-Q02, short PARM.** The target refuses a parmDate that is not exactly 10 characters; the legacy reads 10 bytes regardless. This is a deliberate difference the standard prescribes (row w); it should be a register row, not only a standard. | `InterestJobParameters.java` | mainframe application owner |
| 7 | **Duplicate keys in an input file.** A KSDS load rejects a duplicate key; the file adapter keeps the last account record and both balance records. No fixture has one. Belongs to the loader's reconciliation (ticket 05). | `FileRunWorkspace` constructor | data owner |

No golden file looked wrong.

## 4. Standards review checklist (`.agents/skills/interest-conversion-standards/SKILL.md`, section 9)

Paths are under `java/interest/src/main/java/com/carddemo/interest/` (tests under `src/test/java/...`).

Standards:

- [x] S1.1 Sources used in order; recorded output decides - **PASS** - `RecordLayoutTest`, `InterestCalcJobIT` assert against `fixtures/interest/i01-baseline/expected`
- [x] S1.2 Rule/code disagreements reproduced as coded and reported - **PASS** - INT-Q04 as coded in `domain/InterestCalculator.java`; findings in section 3
- [x] S2.1 Designed from rules; no method per paragraph - **PASS** - `domain/InterestCalculator.java`, `domain/InterestFormula.java`
- [x] S2.2 Immutable business records; copybook names only at the mapping - **PASS** - `domain/*.java` records; copybook names only in `codec/*Record.java`
- [ ] S2.3 Spring Batch shape, transaction and restart boundaries of the architecture - **PASS for the shape** (reader/processor/writer, one item = one account, chunk of whole accounts: `batch/InterestJobConfig.java`, `batch/CalcInterestRun.java`); **N/A for the restart state** (file adapters; ticket 06)
- [x] S2.4 Types and return values, no flags or result codes - **PASS** - `port/*.java` return `Optional`/`List`; `domain/AccountBalances.java`
- [x] S2.5 Constructor injection; no mutable static state - **PASS** - `domain/InterestCalculator.java`, `batch/CalcInterestRun.java` (counter and adapters are created per step execution; no static state)
- [x] S3.1 `BigDecimal` with data-map precision and scale; no `double`/`float` - **PASS** - `codec/Field.java`, `domain/*.java`; `grep -rn "double\|float" java/interest/src` finds nothing
- [x] S3.2 Formula: exact product, one division, mode from the policy - **PASS** - `domain/InterestFormula.java`, `policy/InterestPolicy.java`
- [x] S3.3 Rounding mode named on every other arithmetic step - **PASS** - `domain/InterestCalculator.java` (`UNNECESSARY` on the total and the new balance), `codec/Field.java`
- [ ] S3.4 Range check before store; overflow goes to the policy - **N/A** - INT-H05 is not in ticket 01. `codec/Field.java` refuses a value that does not fit (never cuts); there is no `interest.policy.overflow` yet
- [x] S3.5 Encodings only in `record-codec` - **PASS** - `codec/Field.java`
- [x] S3.6 Width, padding and FILLER only in codec and schema - **PASS** - `codec/TransactionRecord.java`, `codec/AccountRecord.java` (three fields rewritten in the image)
- [ ] S3.7 Code page and key order each in one place - **PASS for the code page** (`codec/FixtureCodePage.java`); **key order: deviation** - there is no table, so the order is fixed once in `file/FileRunWorkspace.java` by sorting the 17-byte key in Java, which the standard's "Do not" forbids (see section 5, item 2)
- [x] S3.8 Clock and run parameters injected; `parmDate` is text - **PASS** - `batch/InterestJobConfig.java`, `batch/InterestJobParameters.java`, `domain/RunTimestamp.java`
- [x] S4.1 Every PRESENT behaviour touched has its test; new ones reported - **PASS** - list below
- [x] S5.1 Each quirk/open decision in one named place behind its switch, rule id in names - **PASS** - `policy/InterestPolicy.java` (`intQ01...`, `intQ02...`, `intQ03...`, `intQ04...`)
- [x] S5.2 Nothing fixed, tidied or added without a rule id - **PASS, with one thing to know** - the two stops for INT-R03 and INT-R04 are pilot behaviour, not legacy; each carries its rule id and goes away in ticket 04
- [ ] S6.1 Status and message mapping as in section 6 - **N/A** - ticket 08; no status line or legacy message is written
- [x] S6.2 One stop exception, one exit-code constant, no skip or retry - **PASS** - `domain/InterestRunStoppedException.java`, `batch/InterestExitCode.java`
- [ ] S6.3 Restart as in the architecture - **N/A** - ticket 06
- [x] S6.4 Outputs in the recorded format; failed run leaves no SYSTRAN - **PASS** - `file/FileRunWorkspace.java` (`commit`, `discard`). "Keeps committed account updates" is N/A: no table
- [x] S6.5 Run log byte for byte; no account data in the application log - **PASS** - `InterestCalcJobIT.sysout_matchesRecorded`; log statements in `batch/CalcInterestRun.java`; `InterestCalculatorTest.intR08_missingAccountStops`
- [x] S7.1 A test per rule id, named for it - **PASS** for INT-R01, R02, R05, R06, R07, Q01, Q02, Q03, Q04. `CYCLE_DATE` (INT-Q03) has only a "refused" test
- [x] S7.2 Fixture and edge cases of each rule covered - **PASS for the ticket's rules** (i01, plus the i02 and i07 amounts by hand)
- [x] S7.3 Integration tests on PostgreSQL 15 in a container - **PASS** - `batch/InterestCalcJobIT.java` (`postgres:15`, init script `db/postgres/01-transaction-type.sql`)
- [x] S7.4 No assertion against the code's own output - **PASS** - expected values are recorded files or hand arithmetic in comments
- [x] S8.1 No edit under `app/`, `fixtures/`, `tools/parity` - **PASS** - git diff against `demo/04a-standards`
- [x] S8.2 No golden file re-recorded - **PASS**
- [x] S8.3 Every new dependency explained in the pull request - **PASS** - section 7
- [x] S8.4 Frozen shared interfaces unchanged - **PASS** - FI-1, FI-2, FI-5 unchanged; FI-3 and FI-6 are defined by this ticket
- [x] S8.5 Synthetic data only - **PASS** - fixture accounts 2001-2010 and made-up values

Behaviours PRESENT in section 4:

- [x] a Truncation without ROUNDED - **PASS** - `InterestFormulaTest.intQ01_truncatesHalfCent`, `intQ01_nonTieUnchanged`
- [ ] b High-order digit loss (range check, STOP) - **N/A** - INT-H05, ticket 04; `FieldTest.e2_valueThatDoesNotFitIsRefusedNotCut`, `RecordLayoutTest.intQ02_idLongerThanTheFieldIsRefused` show nothing is cut
- [x] c Intermediate precision of the COMPUTE - **PASS** - `InterestFormulaTest.intR05_formulaExactProductThenDivide`, `intR05_productIsNotRoundedBeforeTheDivision`
- [x] d Zoned overpunch signs - **PASS** - `FieldTest.e2_overpunchRoundTrip` (`intQ05_negativeInterestIsDSigned` is ticket 04)
- [ ] e Binary items stay out of data and domain - **N/A** - the pilot has no copybook parser, so there is no USAGE to refuse (`e5_codecRefusesNonDisplayUsage` is ticket 03). No binary type exists in the code
- [x] f Non-numeric data rejected at the boundary - **PASS** - `FieldTest.e1_nonDigitRejectsRecord`, `FileRunWorkspaceTest.e1_badDigitInABalanceRejectsTheRecord`
- [x] g Fixed width, padding, STRING - **PASS** - `RecordLayoutTest.intR06_transactionFieldsFixedWidth` (`intR03_defaultGroupKeyPadded` is ticket 04)
- [ ] h Code page and collating order - **PASS for the fixture code page** (`codec/FixtureCodePage.java`); `FileRunWorkspaceTest.intR01_readsInKeyOrder` runs on the file adapter with synthetic data, not on PostgreSQL with `COLLATE "C"` (ticket 06); host code page is ticket 03
- [x] i REDEFINES replaced by formatter and function - **PASS for the timestamp** - `RunTimestampTest.intT05_timestampFormat`; the status line (`intT08_statusLineFormat`) is ticket 08
- [x] j FILLER and record length round trip - **PASS** - `RecordLayoutTest.e4_accountFillerRoundTrip`, `intR07_accountBreakRewritesThreeFieldsOnly`
- [x] l No state carried between accounts except the counter - **PASS** - `InterestCalculatorTest.intR07_totalIsPerAccount`, `intQ02_counterRunsAcrossAccounts`
- [x] m Control break and end of file (INT-Q04, INT-T09) - **PASS** - `InterestCalculatorTest.intR07_updatesAccountAtBreak`, `intQ04_lastAccountNotUpdated`, `FileRunWorkspaceTest.intQ04_onlyTheAccountEndingTheInputIsMarked`, `intT09_emptyInputGivesNoItem` (adapter level; no whole-job empty-input test)
- [x] o Level-88 result codes replaced by types - **PASS** - `port/*.java`
- [ ] p Indexed access through the ports; several cards to the policy - **PASS for the ports** (`InterestCalculatorTest.intR02_rateByGroupTypeCategory`, `intR06_cardNumberOfAccount`, `intH03_twoCardsStops`); the stop is not behind `interest.policy.multiCard` yet
- [ ] q File statuses and abend - **N/A** - ticket 08. The pilot only stops (`intR08_missingAccountStops`, `intR08_missingCardStops`, `intR08_missingInputFileStopsBeforeAnyOutput`)
- [x] r PARM, generation, disposition, return code - **PASS** - `InterestCalculatorTest.intQ02_idIsParmPlusCounter`, `InterestCalcJobIT.rcJson_matchesRecorded`, `outputDirectoryHoldsOnlyTheRecordedFiles`
- [x] s DISPLAY output in the run log - **PASS for i01** - `InterestCalcJobIT.sysout_matchesRecorded` (the fallback lines of i04 are ticket 04)
- [x] t PARM text against the clock - **PASS** - `intQ02_idIsParmPlusCounter`, `intQ03_timestampsFromRunClock`
- [x] u Timestamp format and hundredths - **PASS** - `RunTimestampTest.intT05_timestampFormat`, `intT05_hundredthsAreCutNotRounded`, `InterestCalculatorTest.intQ03_clockIsReadOncePerTransaction`
- [x] w PARM length checked - **PASS** - `InterestJobParametersTest.intQ02_shortParmRefused`
- [x] x No stale rate after a failed lookup - **PASS by construction** (`port/RateLookup.java` returns `Optional`; no rate outlives one balance); `intR03_fallsBackToDefault` is ticket 04
- [x] y Typed fields where COBOL mixed text and numbers - **PASS** - `InterestCalculatorTest.intR06_categoryCodeIs0005`
- [x] z Abend ends the step; stub output not reproduced - **PASS** - the exception fails the step; `InterestCalcJobIT.intR08_missingInputFileFailsTheJobAndLeavesNoOutput`
- [x] aa Dead and empty code not ported - **PASS** - no fee code, no record counter; `intH01_noFeeTransaction` is not written (INT-H01 is not in ticket 01)

## 5. What is wrong or missing in the standards skill (not edited)

1. **The checklist template offers only "PASS / FAIL"** on every line although its introduction allows N/A. A pilot ticket needs N/A on about ten lines. Print "PASS / FAIL / N/A".
2. **S2.3, S3.7, S6.3, S6.4 and row h are written for the JDBC step only.** Ticket 01 requires file adapters and no table. The skill should say what holds for a file adapter: key order is fixed once in the `BalanceSource` adapter (here by sorting the 17-byte key), there is no restart state, and a stopped run keeps nothing.
3. **S6.5 and S2.2/S3.5 pull in different directions.** `runLog.echo(recordImage)` needs the 50-byte record image in the calculation, while the domain is meant to see typed values only. Here the image travels beside the typed balance (`AccountBalances.BalanceRead`). The skill should rule on it, and say how ticket 06 rebuilds the image from table columns (the 22-byte FILLER of CVTRA01Y included).
4. **S7.1 asks for a test of every other switch value; INT-Q03 `CYCLE_DATE` cannot be built yet.** S3.8 says the frozen business date "is a job parameter too", but `architecture.md` section 4 lists no such parameter and FI-5 names no property for it. The skill should say which ticket builds the non-default values (ticket 04 by its title) and that ticket 01 tests defaults.
5. **Section 6 has no row for a rejected record** (S3.5: a byte that breaks E1/E2 rejects the record) and none for a launch refused before the job starts (S3.8: wrong parmDate length). Both need a stop reason, a run-log line or "none", and an exit code.
6. **Section 4 names tests that cannot exist in the ticket that first touches the behaviour** (for example row d names `intQ05_negativeInterestIsDSigned` beside `e2_overpunchRoundTrip`; row s names "all seven"). Say per row which ticket owns each test name.
7. **S6.4 names rc.json but not its content.** The recorded file holds five step entries that are not INTCALC's (section 3, finding 1). The skill should say what to write for them.
8. **Nothing on the time zone of the injected clock** (S3.8, row u).
9. **Nothing on how the job is launched for parity** (FI-6 is a name only): the `db` service publishes no port and the `estate` image has no JDK. See section 6.

## 6. Method: commands run and failures hit

No `interest-ticket-build`, `interest-ticket-verify` or `interest-pitfalls` skill exists on `demo/04a-standards`
(`.agents/skills` holds `carddemo-parity` and `interest-conversion-standards` only), so this session is the pilot.
Skills used: `interest-conversion-standards` (followed), `carddemo-parity` (read for the output format and the
comparator call).

Commands, in order (repository root unless stated):

```sh
# 0. start: no existing work for this ticket on this line
git ls-remote --heads origin 'demo/*'                    # no demo/05-pilot
gh pr list --state all --limit 40                        # no PR with base demo/04a-standards or demo/04-design other than steps 4/4a
git fetch origin 'refs/heads/demo/*:refs/remotes/origin/demo/*'
git checkout -b demo/05-pilot origin/demo/04a-standards

# 1. read, in the order of standard S1.1
modernization/design/tickets/README.md, 01-pilot-thin-slice.md
.agents/skills/interest-conversion-standards/SKILL.md, .agents/skills/carddemo-parity/SKILL.md
modernization/rules/interest/rules.yaml (INT-R01..R08, INT-Q01..Q04), modernization/design/architecture.md, data-map-interest.md (E1-E6, G01-G05)
app/cbl/CBACT04C.cbl lines 28-56, 122-232, 325-520, 613-648; the five copybooks; app/jcl/INTCALC.jcl
fixtures/interest/i01-baseline (input, expected, with cat -A to see trailing spaces and the missing final newline of rc.json)
tools/parity/compare.py, tools/parity/record.py, tools/runjcl/chains.json, Makefile, docker-compose.yml

# 2. environment
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64     # mvn -v otherwise reports Java 17
docker compose up -d --wait db                           # PostgreSQL 15 only; the estate image is not needed
docker pull eclipse-temurin:21-jre

# 3. build and test
mvn -q -f java/interest/pom.xml -DskipTests package
mvn -q -f java/interest/pom.xml verify                   # unit (surefire) + integration (failsafe, Testcontainers postgres:15)

# 4. end to end and compare
make parity-java CASE=i01-baseline
python3 tools/parity/compare.py interest i01-baseline --actual work/parity-java/i01-baseline/actual --label java
make parity-java                                         # all seven; exit 2 while i03 and i04 stop
for c in $(ls fixtures/interest); do python3 tools/parity/compare.py interest $c --actual work/parity-java/$c/actual --label java --report work/parity-java/compare.md; done

# 5. error path and database assertions from the command line
docker run --rm --network container:$(docker compose ps -q db) --user $(id -u):$(id -g) -v "$PWD":/estate -w /estate eclipse-temurin:21-jre \
  java -jar java/interest/target/interest.jar parmDate=2022071800 runId=miss-1 format=fixture inputDir=<dir without CARDXREF> outputDir=work/pilot-checks/out
# -> OPEN_INPUT (INT-R08), exit 12, no work/pilot-checks/out
# same with parmDate=20220718 -> "INT-Q02: parmDate must be exactly 10 characters", exit 12
docker compose exec -T db psql -U carddemo -d carddemo -c "select ... from batch.batch_job_execution ..." -c "select table_name from information_schema.tables where table_schema='carddemo'"

# 6. before every commit
git checkout -- tools/parity/__pycache__                 # compare.py rewrites tracked .pyc files
git diff --stat origin/demo/04a-standards -- app fixtures tools modernization .agents db docker-compose.yml   # must be empty

# 7. evidence from the committed revision
mvn -B -f java/interest/pom.xml clean verify > java/interest/evidence/test-output.txt
```

Failures hit, and what fixed them:

| # | Failure | Cause | Fix |
|---|---|---|---|
| 1 | `mvn verify`: `InterestCalcJobIT » ContainerFetch Can't get Docker image ... client version 1.32 is too old. Minimum supported API version is 1.40` | Testcontainers 1.21.3 (managed by Spring Boot 3.5.6) against Docker Engine 29 | `<testcontainers.version>1.21.4</testcontainers.version>` in `pom.xml` |
| 2 | The Java job cannot reach PostgreSQL from the host, and cannot run in the `estate` container | the `db` service publishes no port; the `estate` image has no JDK; ticket 01 allows a compose change only for the estate service | `make parity-java` runs the jar in `eclipse-temurin:21-jre` with `--network container:<db container>`, so `localhost:5432` is the `db` service. No compose change |
| 3 | First draft of rc.json would have held STEP15 only | the recorded rc.json lists six steps (finding 1) | `RecordedReturnCodes.java` writes the recorded shape; unit test against the golden file |
| 4 | `FieldTest.e3_textIsPaddedAndNeverCut`: `StringIndexOutOfBoundsException` instead of a rejection | the test passed a 9-character image to a 10-character field. `Field` assumes a full-length image; `RecordImage.fromLine` pads every line before decoding | test data corrected. A record line longer than the record length is cut by `fromLine`, as `tools/parity/record.py:193` does |
| 5 | `git status` shows `tools/parity/__pycache__/copybook.cpython-310.pyc` modified after every compare | the .pyc files are tracked | `git checkout -- tools/parity/__pycache__` before committing |
| 6 | `mvn` on the default JDK | `/usr/bin/java` is 8 and Maven picks 17 | `JAVA_HOME` set to a JDK 21 for every Maven call, `make parity-java` included |
| 7 | `compare.py` without `--report` appends to `work/parity/report.md` | default of the comparator | `make parity-java` removes and writes `work/parity-java/report.md` |

Things decided along the way that a later ticket inherits:

* Job repository in schema `batch`: `spring.datasource.hikari.connection-init-sql` creates the schema and sets the
  search path, so Spring Batch's own DDL lands there and schema `carddemo` stays as the estate made it.
* The outputs are staged in `<outputDir>.staging-<runId>` and moved into place after the last chunk; any failure
  deletes the staging directory.
* `InterestExitCodes` turns any job status other than COMPLETED, and any launch failure, into exit code 12.
* `CardLookup.cardNumbersOf` returns every card of the account so that INT-H03 can be decided in the calculation
  without changing the port.

## 7. Dependencies (S8.3)

| Dependency | Scope | Why | Considered instead |
|---|---|---|---|
| `spring-boot-starter-batch` (Spring Boot 3.5.6, Spring Batch 5.2) | compile | the target named by the ticket | - |
| `org.postgresql:postgresql` | runtime | JDBC driver for the job repository on PostgreSQL 15 | - |
| `spring-boot-starter-test`, `spring-batch-test` | test | JUnit 5 and the Spring test context | - |
| `spring-boot-testcontainers`, `org.testcontainers:junit-jupiter`, `org.testcontainers:postgresql` (1.21.4) | test | S7.3: integration tests on `postgres:15` in a container | reusing the compose `db` service from the tests: it publishes no port and would tie `mvn verify` to `make up` |

No COBOL-record or EBCDIC library was added; the codec is in `codec/`.
