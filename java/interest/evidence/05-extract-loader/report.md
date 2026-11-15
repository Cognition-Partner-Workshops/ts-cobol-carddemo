# Report - ticket 05-extract-loader (INT-T05)

Branch `demo/05-05-extract-loader`, base `demo/05b-integration`. Code revision `c66551017de55872346ca67d54060cfad0f82d80`.
Evidence in this directory: `test-output.txt` (`mvn -B clean verify`, with the figures per case), `parity-report.md`.
Code: `java/interest/src/main/java/com/carddemo/interest/load/` (step, loader, check, `README.md`) and
`.../jdbc/ReplicaTableWriter.java`. Tests: `java/interest/src/test/java/com/carddemo/interest/load/`.

## 1. Result

| # | Acceptance line of the ticket | Met | Shown by |
|---|---|---|---|
| 1 | i01-baseline: account 2 rows, card_xref 2, tran_cat_balance 2, disclosure_group 15; SUM(acct_curr_bal) = 3400.00, SUM(tran_cat_bal) = 3400.00, SUM(dis_int_rate) = 120.50; the same figures in interest_run | **met** | `LoadExtractsStepIT.i01Baseline_countsAndSumsInTheTablesAndInInterestRun` (the step launched through Spring Batch; tables and every `loaded_*` column asserted) |
| 2 | Any of the seven cases: the figures equal the reconciliation table of strategy-interest.md, and re-encoding every row gives the input record (RC3) | **met** | `LoadExtractsStepIT.sevenCases_figuresEqualTheStrategyTableAndRowsEncodeBackToTheInput` (expected values typed from the strategy table; rows read back from the tables and encoded); `ExtractCheckTest.rc3_everyRecordOfTheSevenCasesEncodesBackToItsInput` |
| 3 | Loading twice leaves the same rows (truncate and insert in one transaction per table) | **met** | `LoadExtractsStepIT.loadingTwice_leavesTheSameRows`, `loadReplacesWhatAnEarlierCaseLeft`; one `TransactionTemplate` call per table in `ExtractLoader.replaceTable` |
| 4 | Errors: one undecodable record rolls back that table's load, writes the reject list (file, record number, field) and ends the step FAILED | **met** | `LoadExtractsStepIT.undecodableRecord_stepFailsTablesAreUntouchedRejectListIsWritten`: step `FAILED`, reject line `AWS.M2.CARDDEMO.TCATBALF.PS.txt<TAB>2<TAB>TRAN-CAT-BAL<TAB>not numeric (E2)`. Rollback of one table's transaction: `databaseRefusesARow_thatTablesLoadIsRolledBackAndTheRunIsFailed` |
| 5 | Database assertions: after a failed load the four tables hold what they held before; interest_run.status is FAILED | **met** for every failure the loader can see in the files (undecodable record, wrong length, duplicate key, missing file): `undecodableRecord_...`, `duplicateKey_isRejectedBeforeAnyTableIsTouched`, `missingFile_stepFailsBeforeAnyTableIsTouched`. See the limit in section 2, first row |

Verification lines: `mvn -B -f java/interest/pom.xml clean verify` on the committed revision: `Tests run: 182` (unit, was 168),
`Tests run: 23` (integration on `postgres:15`, was 12), 0 failures, `BUILD SUCCESS`.

Figures per case, as `interest_run` holds them after the load (`test-output.txt`, lines `RECONCILIATION`):

| Case | Accounts | Cards | Balances | Rates | Sum ACCT-CURR-BAL | Sum TRAN-CAT-BAL | Sum DIS-INT-RATE | Distinct keys | Hash totals acct / xref / trancat | G02 | G03 | Equals strategy table |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| i01-baseline | 2 | 2 | 2 | 15 | 3400.00 | 3400.00 | 120.50 | 2/2/2/15 | 4003 / 4003 / 4003 | 0 | 0 | yes |
| i02-tie-half-cent | 2 | 2 | 2 | 15 | 2470.00 | 2470.00 | 120.50 | 2/2/2/15 | 4007 / 4007 / 4007 | 0 | 0 | yes |
| i03-zero-rate-skipped | 1 | 1 | 2 | 15 | 700.00 | 700.00 | 120.50 | 1/1/2/15 | 2005 / 2005 / 4010 | 0 | 0 | yes |
| i04-default-group-fallback | 1 | 1 | 1 | 15 | 1000.00 | 1000.00 | 120.50 | 1/1/1/15 | 2006 / 2006 / 2006 | 0 | 0 | yes |
| i05-multi-category | 1 | 1 | 2 | 15 | 1500.00 | 1500.00 | 120.50 | 1/1/2/15 | 2007 / 2007 / 4014 | 0 | 0 | yes |
| i06-negative-balance | 1 | 1 | 1 | 15 | -300.00 | -300.00 | 120.50 | 1/1/1/15 | 2008 / 2008 / 2008 | 0 | 0 | yes |
| i07-nontie-rounding | 2 | 2 | 2 | 15 | 1778.10 | 1778.10 | 120.50 | 2/2/2/15 | 4019 / 4019 / 4019 | 0 | 0 | yes |

**Gap list asked for by the Deliverables line - TL4 profile on the fixtures: nothing found.** For each of the seven
cases (`test-output.txt`, lines `TL4-PROFILE`): accounts with several cards 0 (G08), accounts with mixed type codes 0
(G06), minus-zero amounts 0 (G03), F-signed amounts 0 (G02), fillers that are not blank 0, accounts not active 0
(INT-H04), balances with neither a group rate nor a DEFAULT rate 0 (INT-R08). Each counter is shown to count by a
synthetic input: `LoadExtractsStepIT.tl4_profileFinds...`, `g02AndG03_differencesAreCounted...`,
`ExtractCheckTest.tl4_nonBlankFillerIsCountedNotRejected`.

Comparator (`parity-report.md`): the job's output is unchanged by this ticket - i01, i02, i05, i06, i07 PASS; i03 and
i04 FAIL as on the base branch (ticket 04 is not integrated).

## 2. What the ticket does not cover (gaps)

| Gap | Detail | Owner |
|---|---|---|
| A database failure in the third table leaves the first two replaced | The architecture's unit of work is one table (section 5), so "the four tables hold what they held before" cannot hold for a failure *inside* a later table's transaction. The loader therefore decodes and checks all four files before it touches a table; every failure visible in the files leaves all four tables untouched. A database error (shown with a test-only constraint) rolls back its own table only; `interest_run` then shows which tables were replaced (`loaded_*_count` filled or empty) and `FAILED`. One transaction for all four tables would close this but contradicts section 5 | target architect |
| `loadExtractsStep` is not a step of `interestCalcJob` yet | The step bean exists and is tested through Spring Batch in a job of its own. Wiring it in front of `calcInterestStep` is in `batch/InterestJobConfig.java`, outside the Allowed files; tickets 06 and 09 own it. `InterestJobParameters` still refuses `format=host`; the loader itself accepts it (`rel08_hostFormatLoadsTheSameRowsAsTheFixtureFormat`) | tickets 06, 09 |
| The README section is in the package, not in `java/interest/README.md` | The ticket asks the loader to name REL-01, REL-02, REL-08 "in its README section". `java/interest/README.md` is outside the Allowed files, so the section is `java/interest/src/main/java/com/carddemo/interest/load/README.md`. A pointer from the module README is left to the integration | integration |
| Who creates the `interest_run` row | The architecture gives that to `interest-run-control` (a listener, not built). The loader inserts the row (`LOADING`), fills its `loaded_*` columns, sets `LOADED` or `FAILED`, and publishes `run_id` in the job execution context as `interest.runId`. When run-control exists the insert moves there | ticket 06 |
| RC4, second half (lowest and highest key equal the first and last record of the file) | Not checked and no column of `interest_run` holds it. Distinct keys = rows is checked (file side: a repeated key is rejected; table side: `count(DISTINCT key)`). All fixture files are in key order (`LC_ALL=C sort -c` on the 28 files) | ticket 10 (reconciliation report) |
| No run against the `db` service of docker-compose | The jar has no job that contains the step, so `interest-ticket-verify` steps 4, 6 and 7 cannot launch it there. The service has the seven objects of FI-4 (`psql`: account, card_xref, disclosure_group, interest_run, interest_transaction, tran_cat_balance); the assertions ran on the same image through Testcontainers | ticket 09 |
| Host file names | `format=host` reads `AWS.M2.CARDDEMO.<FILE>.PS` without suffix. The names of the real unloads are not in the estate (TL1, TL2) | mainframe operations owner |

## 3. For the decision register

| Finding | Evidence | Suggested owner |
|---|---|---|
| REL-01, REL-02, REL-08 are built as proposed and named in the loader's README section; all stay OPEN | `load/README.md`; `ExtractFormat`, `ExtractLoader` | enterprise architect, data owner, mainframe data owner |
| "Rolls back that table's load" (ticket) and "the four tables hold what they held before" (ticket) agree only if nothing is loaded before every file is checked. Built that way; the architecture text (section 5) does not say it | section 2, first row | target architect |
| Statuses of `interest_run`: the design names `EXPORTED`, `FAILED`, `ABANDONED`. The loader adds `LOADING` and `LOADED`; `finished_at` is set on `FAILED` only | `LoadRunRecord` | target architect |
| The reject list has no place in the design. Built: `<outputDir>/../rejects/loadExtractsStep.rejects.txt` or job parameter `rejectDir`; tab-separated file, record number, field, reason; never a value | `LoadExtractsStepConfig.rejectFile`, `load/README.md` | data engineering lead |
| G08: an account with two cards loads (the alternate key is not unique) and is counted by the profile; the stop is the calculation's (`multiCard=STOP`) | `tl4_profileFinds...` | business owner (INT-H03) |
| INT-H04 profile uses "not active" = `acct_active_status <> 'Y'`; the estate does not list the values of the field | `ExtractLoader.profile` | data owner |
| `loaded_*_id_sum` columns are NUMERIC(18,2) for whole-number hash totals; the sum of 11-digit ids overflows 16 integer digits above about 100,000 accounts of the highest ids | `db/postgres/02-interest.sql:97-99` | ticket 02 owner / data architect |

## 4. Standards review checklist (`.agents/skills/interest-conversion-standards/SKILL.md`, section 9)

Standards:

- [x] S1.1 Sources used in order; recorded output decides - PASS: ticket, architecture sections 2/3/5/6, data map, fixtures; expected figures from `strategy-interest.md` and the input files (`LoadExtractsStepIT.STRATEGY_TABLE`)
- [x] S1.2 Rule/code disagreements reproduced as coded and reported - N/A: the ticket names no rule and no CBACT04C line; the disagreement between two ticket lines is in section 3
- [x] S2.1 Designed from rules; no method per paragraph - PASS: one class per responsibility (`ExtractReader`, `ExtractCheck`, `ExtractLoader`, `LoadRunRecord`, `ReplicaTableWriter`); no COBOL is ported
- [x] S2.2 Immutable business records; copybook names only at the mapping - PASS with a note: records `LoadReject`, `TableFigures`, `ExtractProfile`, `LoadReport`, `CheckedExtract`; the only copybook field names in `load` are the three hash-total fields in `ExtractFile` (the row mapping), because the codec keeps its `FieldSpec`s private (section 5)
- [x] S2.3 Spring Batch shape, transaction and restart boundaries of the architecture - PASS: tasklet step `loadExtractsStep`, one transaction per table, restart repeats (`allowStartIfComplete`), `LoadExtractsStepConfig`; `loadingTwice_leavesTheSameRows`
- [x] S2.4 Types and return values, no flags or result codes - PASS: `LoadReport` returned, rejects as a list, failure as the stop exception
- [x] S2.5 Constructor injection; no mutable static state - PASS: `ExtractLoader(JdbcTemplate, PlatformTransactionManager, Clock)`; statics are constants
- [x] S3.1 `BigDecimal` with data-map precision and scale; no `double`/`float` - PASS: amounts travel as the codec's `BigDecimal` into NUMERIC columns; `grep -rnE 'double|float' src/*/java/**/load src/main/java/**/jdbc` finds nothing
- [x] S3.2 Formula: exact product, one division, mode from the policy - N/A: no calculation in this ticket
- [x] S3.3 Rounding mode named on every other arithmetic step - PASS: sums are `BigDecimal.add`; the scale-2 store uses `RoundingMode.UNNECESSARY` (`LoadRunRecord.recordTable`)
- [x] S3.4 Range check before store; overflow goes to the policy - N/A for the policy (no computed value); every stored value was decoded from a field of the PIC its column maps, so it fits by construction
- [x] S3.5 Encodings only in `record-codec` - PASS: `ExtractCheck` decodes and encodes through `RecordCodec` only; the G02/G03 distinction asks the codec whether the sign byte is a PIC 9 digit; `tl3_nonNumericAmountIsRejected...`, `g07_textByteThatIsNoCharacterIsRejected`
- [x] S3.6 Width, padding and FILLER only in codec and schema - PASS: `RecordImage.fromLine` pads, CHAR(n) pads, FILLER goes to BYTEA untouched; `sevenCases_...` (rows encode back to the input), `tl3_recordOfTheWrongLengthIsRejected`
- [x] S3.7 Code page and key order each in one place - PASS: code page from `ExtractFormat.codePage()` -> `CodePage`; the loader does not depend on order; `rowsInKeyOrder` orders by the key columns, whose collation is the schema's
- [x] S3.8 Clock and run parameters injected; `parmDate` is text - PASS: `started_at`/`finished_at` from the injected `Clock runClock`; `parmDate` stored as given
- [x] S4.1 Every PRESENT behaviour touched has its test; new ones reported - PASS: rows d, f, h, j below; none new found
- [x] S5.1 Each quirk/open decision in one named place behind its switch, rule id in names - N/A for INT-Q/INT-H (none in this ticket); REL-01/02/08 are each in one named place (`load/README.md` table)
- [x] S5.2 Nothing fixed, tidied or added without a rule id - PASS: no record is repaired or skipped; rejects stop the load (TL3); the profile only counts
- [x] S6.1 Status and message mapping as in section 6 - PASS: missing file -> `OPEN_INPUT` carrying the file name (`missingFile_...`); the run log is not written by the loader
- [x] S6.2 One stop exception, one exit-code constant, no skip or retry - PASS: only `InterestRunStoppedException` leaves the loader; no skip, no retry; see proposal 2 in section 5 for the reason used
- [x] S6.3 Restart as in the architecture - PASS: restart repeats the step; `loadingTwice_leavesTheSameRows`
- [x] S6.4 Outputs in the recorded format; failed run leaves no SYSTRAN - PASS (as far as it applies): the loader writes no output directory (`i01Baseline_...` asserts it); the reject list is beside it
- [x] S6.5 Run log byte for byte; no account data in the application log - PASS: log lines hold run id, table, counts, sums, profile counts; rejects hold file, record number, field, reason
- [x] S7.1 A test per rule id, named for it - N/A for INT-R/Q/H (the ticket names none); tests are named for the strategy item, gap or decision they prove (`tl3_`, `tl4_`, `rc3_`, `rc4_`, `g02_`, `g03_`, `g07_`, `rel08_`)
- [x] S7.2 Fixture and edge cases of each rule covered - PASS: all seven inputs; synthetic inputs for rejects, G02, G03, G07, G08, G06, INT-H04, INT-R08, duplicate key, missing file, host format
- [x] S7.3 Integration tests on PostgreSQL 15 in a container - PASS: `LoadExtractsStepIT` on `postgres:15` with `01-transaction-type.sql` and `02-interest.sql`
- [x] S7.4 No assertion against the code's own output - PASS: expected figures typed from the strategy table and hand arithmetic; the round trip is compared with the fixture input lines
- [x] S8.1 No edit under `app/`, `fixtures/`, `tools/parity` - PASS: `git diff --stat origin/demo/05b-integration -- app fixtures tools modernization .agents db docker-compose.yml` prints nothing
- [x] S8.2 No golden file re-recorded - PASS
- [x] S8.3 Every new dependency explained in the pull request - PASS: none added; `pom.xml` unchanged
- [x] S8.4 Frozen shared interfaces unchanged - PASS: FI-1, FI-3, FI-4, FI-5 untouched (diff is `load/`, `jdbc/`, test `load/`, evidence)
- [x] S8.5 Synthetic data only - PASS: fixture accounts 2001-2010 and card numbers 400000000000xxxx

Behaviours PRESENT in section 4:

- [x] a Truncation without ROUNDED - N/A: calculation
- [x] b High-order digit loss (range check, STOP) - N/A: calculation
- [x] c Intermediate precision of the COMPUTE - N/A: calculation
- [x] d Zoned overpunch signs - PASS: through the codec; `g02_fSignedAmountIsCountedAndComesBackCSigned`, `g03_minusZeroIsCountedAndComesBackPlusZero`, i06 (negative balance) in `sevenCases_...`
- [x] e Binary items stay out of data and domain - N/A: no binary field; the writer refuses a NOT_DISPLAY field
- [x] f Non-numeric data rejected at the boundary - PASS: `tl3_nonNumericAmountIsRejectedWithFileRecordNumberAndField`, `undecodableRecord_...`
- [x] g Fixed width, padding, STRING - PASS for width and padding: rows encode back to the input (`sevenCases_...`)
- [x] h Code page and collating order - PASS: `rel08_hostFormatLoadsTheSameRowsAsTheFixtureFormat`; mixed type codes counted (G06)
- [x] i REDEFINES replaced by formatter and function - N/A
- [x] j FILLER and record length round trip - PASS: BYTEA fillers, `sevenCases_...`, `tl4_nonBlankFillerIsCountedNotRejected`
- [x] l No state carried between accounts except the counter - N/A: calculation
- [x] m Control break and end of file (INT-Q04, INT-T09) - N/A: calculation
- [x] o Level-88 result codes replaced by types - N/A
- [x] p Indexed access through the ports; several cards to the policy - N/A for the ports; several cards are loaded and counted (G08)
- [x] q File statuses and abend - N/A beyond `OPEN_INPUT` for a missing file
- [x] r PARM, generation, disposition, return code - N/A: `parmDate` is only stored
- [x] s DISPLAY output in the run log - N/A
- [x] t PARM text against the clock - N/A
- [x] u Timestamp format and hundredths - N/A
- [x] w PARM length checked - N/A: the job's validator (ticket 01) does it
- [x] x No stale rate after a failed lookup - N/A
- [x] y Typed fields where COBOL mixed text and numbers - PASS: ids NUMERIC, codes CHAR, as the data map
- [x] z Abend ends the step; stub output not reproduced - PASS: the stop exception ends the step `FAILED`
- [x] aa Dead and empty code not ported - N/A

## 5. What is wrong or missing in the standards skill (proposals only)

1. **S2.2 / S3.5 - the codec gives a loader nothing to hold on to.** The `*Record` classes keep their `FieldSpec`s
   private and a rejection carries file, record number and field only inside its message. The loader had to (a) name
   the three hash-total fields again in `ExtractFile`, (b) read the field of a rejection back out of the message text
   (`ExtractCheck.CODEC_REJECTION`), (c) tell G02 from G03 by decoding the sign byte as PIC 9(01). Proposal: the
   standards say that `record-codec` exposes its fields by name, a rejection with `source()` and `field()`, and
   whether a decoded signed field was F-signed or minus zero.
2. **Section 6 has no row for the load.** Nothing says which reason stops the run when the load does not reconcile
   or the database refuses a row. Used: `RECORD_REJECTED` for rejects, `OPEN_INPUT` for a missing file,
   `WRITE_OUTPUT` for a failed table transaction. Proposal: a row "load does not reconcile / table write fails"
   with its own reason (`LOAD_RECONCILIATION`), and where the reject list goes.
3. **S7.1 names tests after INT-R/Q/H rule ids only.** A ticket with "Rules: none" (02, 03, 05) has no id to use.
   Proposal: allow the strategy item, gap or decision id (`tl3_`, `rc3_`, `g02_`, `rel08_`) as the prefix.
4. **S6.4 "a stopped run leaves no output directory"** does not say whether a reject list may exist. Proposal: say
   it lives outside the output directory.

Corrections to the other skills (not edited):

* `interest-ticket-verify` steps 1-7 assume the ticket's proof is the job's output through the comparator. A ticket
  whose result is tables (05, later 06) has no command there: no way to launch one step against the `db` service, and
  step 8's `parity-report.sh` labels the named case "acceptance case of the ticket" although the comparator proves
  nothing about the ticket. Proposal: a step "ticket without comparator output: the integration test is the proof;
  run the comparator on all cases only to show nothing changed".
* `interest-ticket-verify` step 8: `parity-report.sh` pulls `eclipse-temurin:21-jre` on first use and the pull
  progress lands in `parity-report.md` (section "Acceptance run"). Proposal: `docker pull -q` before the run.
* `interest-ticket-build` step 5 says packages `jdbc`, `load` do not exist and "no table exists in schema carddemo
  beyond 01-transaction-type.sql"; after ticket 02 the seven objects exist and an integration test must copy
  `02-interest.sql` into the container itself (`LoadExtractsStepIT`).
* `interest-ticket-build` step 9: the `git diff --stat` list includes `db`, right for this ticket, but the sentence
  "Allowed files are `java/interest/**` and the `Makefile`" does not fit tickets with narrower scopes; the check
  that matters is `git diff --name-only origin/<base>` against the ticket's Allowed files.

## 6. Method

Commands (repository root, `JAVA_HOME` on JDK 21): `git checkout -b demo/05-05-extract-loader origin/demo/05b-integration`;
`mvn -q -f java/interest/pom.xml -DskipTests package`; `mvn -B -f java/interest/pom.xml verify`;
`docker compose up -d --wait db`; the `test-output.txt` command of `interest-ticket-verify` step 8;
`.agents/skills/interest-ticket-verify/parity-report.sh 05-extract-loader i01-baseline`; `git checkout -- tools/parity/__pycache__`.

Failures hit:

| Symptom | Cause | Fix |
|---|---|---|
| `ExtractCheckTest.tl4_nonBlankFillerIsCountedNotRejected`: `StringIndexOutOfBounds Range [37, 36) out of bounds for length 36` | test data: a recorded CARDXREF line ends at byte 35, the trailing spaces of the FILLER are not in the file | the test appends the byte instead of replacing it |
| `rel08_hostFormat...`: expected filler `4040...` but was `2020...` | test data: the test builds the host files by re-encoding fixture records, and a FILLER is raw bytes (E4), so it stays X'20' | the assertion compares the rows as they are; nothing changed in the loader |
| `parity-report.sh` exit 0 but i03 and i04 `12 / 1` | the base branch has the pilot's calculation (ticket 04 not integrated) | none - not this ticket; stated in `parity-report.md` |

No pitfall P1-P7 was hit except P6 (tracked `.pyc`), fixed as written.

## 7. Dependencies

None added. `pom.xml` is unchanged; the loader uses `spring-boot-starter-batch` (JdbcTemplate, transactions) and the
tests the Testcontainers PostgreSQL module that ticket 01 added.
