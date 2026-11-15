# Report - ticket 08-error-paths (INT-T08)

Branch `demo/05-08-error-paths`, base `demo/05b-integration`. Code revision `6e3c6375953ea0b518fd8c3b132f90be0d9dffd8`.
Evidence in this directory: `test-output.txt` (`mvn -B -f java/interest/pom.xml clean verify`) and `parity-report.md`
(`make parity-java` and `compare.py` on all seven cases), both produced from that revision.
Code: `java/interest/src/main/java/com/carddemo/interest/domain/` (`InterestCalculator`, `TransactionCounter`, `RunLogLines`,
`StopReason`), `batch/` (`CalcInterestTableRun`, `RunLogFile`), `policy/InterestPolicy.java` and `application.yml` (the two
switches the prompt grants). Tests: `domain/InterestCalculatorTest`, `policy/InterestPolicyTest`, `batch/ErrorPathsTablesIT`,
`batch/MultiCardFirstByCardNumIT`, `batch/CalcInterestTablesRestartIT`.

## 1. Result

| # | Acceptance line of the ticket | Met | Shown by |
|---|---|---|---|
| 1 | A balance whose account is missing: step FAILED, non-zero exit code, the log holds ACCOUNT NOT FOUND and the account id (INT-R08) | **met on the tables** (see gap 1 for the file adapters) | `ErrorPathsTablesIT.intR08_accountMissingStops`: step and job FAILED, exit code 12 (`InterestExitCode.STOPPED`), run log ends `ACCOUNT NOT FOUND: 00000002009`, `ERROR READING ACCOUNT FILE`, `FILE STATUS IS: NNNN0023`, `ABENDING PROGRAM`. Unit: `InterestCalculatorTest.intR08_accountMissingStops`. The same for a missing card: `intR08_cardMissingStops` |
| 2 | No rate for the group and none for DEFAULT: step FAILED (INT-R08) | **met** | `ErrorPathsTablesIT.intR08_defaultRateMissingStops`; unit `InterestCalculatorTest.intR08_defaultRateMissingStops` |
| 3 | An account with two cards: step FAILED naming INT-H03; with multiCard=FIRST_BY_CARD_NUM the lower card number is used | **met** | `ErrorPathsTablesIT.intH03_twoCardsStops` (exit message `MULTI_CARD (INT-H03)`, run-log line `INT-H03 ACCOUNT HAS SEVERAL CARDS: 00000002009`); `MultiCardFirstByCardNumIT.intH03_firstByCardNumberUsesLowerCard` (job COMPLETED, lower card on both accounts); units `intH03_twoCardsStops`, `intH03_firstByCardNumberUsesLowerCard` |
| 4 | Interest above 999999999.99, or a 1,000,000th transaction in one run: step FAILED naming INT-H05; nothing is truncated | **met** | `ErrorPathsTablesIT.intH05_interestAboveFieldStops` (no row with the amount, with or without its leading digit) and `intH05_millionthTransactionStops` (restart state set to 999,999 issued; no row for the failing account); units `intH05_interestAboveFieldStops`, `intH05_millionthTransactionStops`, `intH05_transaction999999IsTheLastOneWritten`, `intH05_largestInterestThatFitsIsWrittenUncut` |
| 5 | After each failure, accounts committed before the failing chunk keep their update, the failing account has neither transactions nor an update; interest_run.status is FAILED | **met** | `ErrorPathsTablesIT.assertStoppedAt2009KeepingTheCommittedAccounts`, called by the six stop tests: `commits=2 rollbacks=1`, the rows of 2001 and 2002 by hand (10.00, 37.00; 1010.00, 2437.00), accounts 2009 and 2010 unchanged row for row, the write audit holds the two committed updates only, `interest_run.status = FAILED` |

Verification lines: `mvn verify` BUILD SUCCESS, `Tests run: 361` (unit) and `Tests run: 84` (integration), 0 failures, 0 errors,
0 skipped (`test-output.txt`). One test per condition: the list above. The seven fixture cases: all PASS (`parity-report.md`).

## 2. What the ticket does not cover (gaps)

1. **File adapters keep no run log of a stopped run.** `interest.calc.adapters=file` (the default, used by `make parity-java`)
   stops with the same exception, FAILED and exit 12, and the calculator writes the same lines to the port, but
   `FileRunWorkspace.discard()` drops them with the rest of the run (standards S6.4: "a stopped run keeps nothing").
   `file/FileRunWorkspace.java` is outside this ticket's Allowed files, so on the file adapters "the log holds ACCOUNT NOT FOUND"
   is true only for the port (`InterestCalculatorTest`), not for a file on disk. On the tables the stopped run's log is the work
   file `<outputDir>.calc-runlog-<runId>`.
2. **No parity is claimed for any stop path.** No recorded legacy run drives an error branch. The INT-R08 lines are the literals
   of `CBACT04C:375, 386, 397, 408, 455, 629, 642`; what CBACT04C really does on the host for INT-H03 (which card VSAM returns
   through the alternate index) and INT-H05 (digit loss in `TRAN-AMT`, `WS-TRANID-SUFFIX` wrapping to 000000) stays unknown.
   The two run-log lines `INT-H03 ACCOUNT HAS SEVERAL CARDS: <id>` and `INT-H05 VALUE DOES NOT FIT ITS FIELD: <what>` are
   this ticket's wording; no legacy text exists.
3. **Legacy messages of database and file failures are not written.** Section 6 of the standards asks for
   `ERROR READING TRANSACTION CATEGORY FILE`, `ERROR RE-WRITING ACCOUNT FILE`, `ERROR WRITING TRANSACTION RECORD`, the
   `ERROR OPENING ...` and `ERROR CLOSING ...` lines and reasons `READ_BALANCES`, `UPDATE_ACCOUNT`, `WRITE_TRANSACTION`,
   `CLOSE_<file>`. The adapters under `jdbc/`, `file/` and `export/` raise `WRITE_OUTPUT`/`OPEN_INPUT` without saying which
   operation failed, and they are outside the Allowed files. Such a stop still ends FAILED, exit 12, `interest_run` FAILED,
   and its run log ends with `ABENDING PROGRAM` (`CalcInterestTablesRestartIT`), without the operation's message.
4. **`overflow=WIDEN` is a switch without a wide target.** With WIDEN the calculator hands the value on uncut
   (`intH05_widenHandsTheValueOnWithEveryDigit`); the schema (FI-4, `NUMERIC(11,2)`, `CHAR(16)`) and the codec then refuse it
   and the run stops as `WRITE_OUTPUT`. Widening the columns and record is a schema decision, not this ticket.
5. **The 1,000,000th transaction is reached through the restart state**, not by writing 999,999 rows: the integration test
   stops a run, sets the saved counter to 999,999 and restarts. The unit test issues 999,999 numbers for real.
6. **Checks added beyond the acceptance line** (standards S3.4, same switch): the total interest of an account
   (`WS-TOTAL-INT`, S9(09)V99) and the new account balance (`ACCT-CURR-BAL`, S9(10)V99) also stop as INT-H05.
7. **REL-10** (the exit code value) stays open: one constant, `InterestExitCode.STOPPED = 12`, unchanged.

## 3. For the decision register

| # | Finding | Evidence | Suggested owner |
|---|---|---|---|
| 1 | A stopped run on the tables now leaves its run log beside the output directory, holding the lines of the chunk that was rolled back and `ABENDING PROGRAM`. A restart cuts them off again. Before this ticket the work file held committed lines only; two assertions of ticket 06 in `CalcInterestTablesRestartIT` were changed to say so. Whether operations want this file, and where, is not decided. | `batch/RunLogFile.stopped()`, `CalcInterestTablesRestartIT` | mainframe operations owner |
| 2 | With file adapters the message of the stop is nowhere on disk (gap 1). Either S6.4 is kept and the acceptance line is read as "tables only", or the file workspace keeps the run log of a stopped run. | `file/FileRunWorkspace.discard()` | target architect |
| 3 | INT-H05 is one switch for four different fields (interest of a balance, interest of an account, account balance, transaction number). A business owner may want STOP for the counter and something else for an amount. | `domain/InterestCalculator.intH05Fits`, `interestTransaction` | card product owner |
| 4 | `FIRST_BY_CARD_NUM` compares card numbers as text; `JdbcCardLookup` orders with `COLLATE "C"` and the field is 16 digits, so text order is number order. A card number with spaces or letters would sort differently from VSAM's EBCDIC key order. | `InterestCalculator.cardNumberOf`, data-map row CVACT03Y/XREF-CARD-NUM | data architect |
| 5 | The account id of the stop is in the exit description of the step in the job repository (ticket 06, unchanged) and now in the run log; not in the application log. | `ErrorPathsTablesIT.stepExit` | security owner |

No golden file looked wrong.

## 4. Standards review checklist (`.agents/skills/interest-conversion-standards/SKILL.md`, section 9)

Paths are under `java/interest/src/main/java/com/carddemo/interest/`; tests under `java/interest/src/test/java/com/carddemo/interest/`.

Standards:

- [x] S1.1 Sources used in order; recorded output decides - **PASS** - seven cases PASS (`parity-report.md`); stop lines from `CBACT04C` as cited in `domain/RunLogLines.java`
- [x] S1.2 Rule/code disagreements reproduced as coded and reported - **PASS** - a missing card prints `ACCOUNT NOT FOUND` as coded (`CBACT04C:397`): `InterestCalculatorTest.intR08_cardMissingStops`; none new found
- [x] S2.1 Designed from rules; no method per paragraph - **PASS** - `domain/InterestCalculator.java` (`intH05Fits`, `cardNumberOf`)
- [x] S2.2 Immutable business records; copybook names only at the mapping - **PASS** - no record added; no copybook field name in the diff of `domain/` or `batch/`
- [x] S2.3 Spring Batch shape, transaction and restart boundaries of the architecture - **PASS** - unchanged step; `ErrorPathsTablesIT` (`commits=2 rollbacks=1`), `CalcInterestTablesRestartIT.restart_*`
- [x] S2.4 Types and return values, no flags or result codes - **PASS** - `StopReason.OVERFLOW`, enums `MultiCard`, `Overflow` in `policy/InterestPolicy.java`
- [x] S2.5 Constructor injection; no mutable static state - **PASS** - no new bean or static field; `domain/TransactionCounter.java` is per step execution
- [x] S3.1 `BigDecimal` with data-map precision and scale; no `double`/`float` - **PASS** - `grep -rnE '\b(double|float)\b'` on `domain`, `batch`, `policy` finds nothing; limits are `BigDecimal` constants in `InterestCalculator`
- [x] S3.2 Formula: exact product, one division, mode from the policy - **PASS** - `domain/InterestFormula.java` unchanged; `InterestFormulaTest`
- [x] S3.3 Rounding mode named on every other arithmetic step - **PASS** - `setScale(2, RoundingMode.UNNECESSARY)` kept in `InterestCalculator`
- [x] S3.4 Range check before store; overflow goes to the policy - **PASS** - `InterestCalculator.intH05Fits` before the transaction is built and before the balance is returned; `intH05_*` tests
- [x] S3.5 Encodings only in `record-codec` - **N/A** - no encoding touched
- [x] S3.6 Width, padding and FILLER only in codec and schema - **PASS with a note** - `RunLogLines.elevenDigits` formats the account id of the legacy message (`CBACT04C:375` displays `ACCT-ID` PIC 9(11)); it is run-log text, not a record
- [x] S3.7 Code page and key order each in one place - **PASS** - unchanged; the stop lines go through the same `RunLogFile` charset
- [x] S3.8 Clock and run parameters injected; `parmDate` is text - **PASS** - unchanged
- [x] S4.1 Every PRESENT behaviour touched has its test; new ones reported - **PASS** - b, p, q, s, z below; none new found
- [x] S5.1 Each quirk/open decision in one named place behind its switch, rule id in names - **PASS** - `InterestPolicy.intH03TakesTheLowestCardNumber`, `intH05StopsWhenAValueDoesNotFit`; `InterestPolicyTest.intH03_*`, `intH05_*`
- [x] S5.2 Nothing fixed, tidied or added without a rule id - **PASS** - every addition carries INT-R08, INT-H03 or INT-H05
- [ ] S6.1 Status and message mapping as in section 6 - **FAIL in part** - the rows for a missing account, card and DEFAULT rate, several cards, overflow and `ABENDING PROGRAM` are implemented (`ErrorPathsTablesIT`); the rows for read, rewrite, write, open and close failures are not (gap 3: their adapters are outside the Allowed files)
- [x] S6.2 One stop exception, one exit-code constant, no skip or retry - **PASS** - `InterestRunStoppedException`, `InterestExitCode.STOPPED`; no `faultTolerant`, `skip` or `retry` in `batch/`
- [x] S6.3 Restart as in the architecture - **PASS** - `CalcInterestTablesRestartIT.restart_afterTheFirstCommittedChunkEndsAsTheRecordedUninterruptedRun` (the lines of the stop are cut off, the completed log equals the recorded one)
- [x] S6.4 Outputs in the recorded format; failed run leaves no SYSTRAN - **PASS** - `assertFalse(Files.exists(temp.resolve("actual")))` in `ErrorPathsTablesIT`; committed account updates kept
- [x] S6.5 Run log byte for byte; no account data in the application log - **PASS** - exception messages carry no id (`assertTrue(!stop.getMessage().contains("2001"))` in `InterestCalculatorTest`); the id is in the run log only; seven recorded run logs equal (`parity-report.md`)
- [x] S7.1 A test per rule id, named for it - **PASS** - `intR08_*`, `intH03_*`, `intH05_*` in `InterestCalculatorTest`, `ErrorPathsTablesIT`, `MultiCardFirstByCardNumIT`
- [x] S7.2 Fixture and edge cases of each rule covered - **PASS** - largest value that fits and first that does not, positive and negative; transaction 999,999 and 1,000,000; two and three cards in any order
- [x] S7.3 Integration tests on PostgreSQL 15 in a container - **PASS** - `ErrorPathsTablesIT`, `MultiCardFirstByCardNumIT` use `InterestTables.postgres()` (Testcontainers `postgres:15`)
- [x] S7.4 No assertion against the code's own output - **PASS** - expected rows worked by hand in the test comments; run-log lines are literals of the COBOL; balance echoes are taken from the recorded run logs
- [x] S8.1 No edit under `app/`, `fixtures/`, `tools/parity` - **PASS** - `git diff --stat origin/demo/05b-integration -- app fixtures tools modernization .agents db docker-compose.yml` prints nothing
- [x] S8.2 No golden file re-recorded - **PASS** - same command
- [x] S8.3 Every new dependency explained in the pull request - **N/A** - none added; `pom.xml` unchanged
- [x] S8.4 Frozen shared interfaces unchanged - **PASS** - no change under `port/`, `app/cpy`, `db/postgres`; switch names `multiCard`, `overflow` and their values as in `architecture.md`
- [x] S8.5 Synthetic data only - **PASS** - test cards `4000000000001009`, `4000000000009002` and the like

Behaviours PRESENT in section 4:

- [x] a Truncation without ROUNDED - **PASS** - unchanged; i02 and i07 PASS
- [x] b High-order digit loss (range check, STOP) - **PASS** - `intH05_interestAboveFieldStops` (unit and integration), `intH05_accountBalanceAboveFieldStops`
- [x] c Intermediate precision of the COMPUTE - **PASS** - unchanged; `InterestFormulaTest`
- [x] d Zoned overpunch signs - **N/A** - codec not touched
- [x] e Binary items stay out of data and domain - **N/A** - not touched
- [x] f Non-numeric data rejected at the boundary - **N/A** - not touched
- [x] g Fixed width, padding, STRING - **N/A** - not touched
- [x] h Code page and collating order - **PASS** - lowest card by text order, register finding 4
- [x] i REDEFINES replaced by formatter and function - **N/A** - not touched
- [x] j FILLER and record length round trip - **N/A** - not touched
- [x] l No state carried between accounts except the counter - **PASS** - `TransactionCounter` only; `intH05_millionthTransactionStops` restores it from the restart state
- [x] m Control break and end of file (INT-Q04, INT-T09) - **PASS** - unchanged; seven cases PASS
- [x] o Level-88 result codes replaced by types - **PASS** - `StopReason`
- [x] p Indexed access through the ports; several cards to the policy - **PASS** - `InterestCalculator.cardNumberOf`; `intH03_*`
- [ ] q File statuses and abend - **FAIL in part** - status 23 line where the condition determines it; other statuses as S6.1
- [x] r PARM, generation, disposition, return code - **PASS** - exit 12 asserted in `ErrorPathsTablesIT`; no output directory after a stop
- [x] s DISPLAY output in the run log - **PASS** - the DISPLAY lines of the stop in `RunLogLines`; asserted in order in `ErrorPathsTablesIT`
- [x] t PARM text against the clock - **N/A** - not touched
- [x] u Timestamp format and hundredths - **N/A** - not touched
- [x] w PARM length checked - **N/A** - not touched
- [x] x No stale rate after a failed lookup - **PASS** - a missing DEFAULT rate stops; no rate is carried over (`intR08_defaultRateMissingStops`)
- [x] y Typed fields where COBOL mixed text and numbers - **N/A** - not touched
- [x] z Abend ends the step; stub output not reproduced - **PASS** - nothing after `ABENDING PROGRAM` (`ErrorPathsTablesIT`)
- [x] aa Dead and empty code not ported - **PASS** - the 9x binary-status branch of `CBACT04C:635-648` is not ported

## 5. What is wrong or missing in the standards skill

1. Section 6 and S6.5 ask for the stop lines in the run log, S6.4 says a stopped file-adapter run keeps nothing. Say where the
   run log of a stopped run is kept, for both sets of adapters (proposal: a file beside the output directory, never inside it).
2. Section 6 lists stop reasons `READ_BALANCES`, `UPDATE_ACCOUNT`, `WRITE_TRANSACTION`, `CLOSE_<file>`; the code of tickets
   01, 06 and 07 uses `WRITE_OUTPUT` and `OPEN_INPUT`. Name the ticket that owns the adapters' messages, or change the table.
3. S3.4 says "overflow goes to the policy" without listing the fields. List them: `TRAN-AMT`, `WS-MONTHLY-INT`,
   `WS-TOTAL-INT`, `ACCT-CURR-BAL`, `WS-TRANID-SUFFIX`.

## 6. Method

- `git checkout -b demo/05-08-error-paths origin/demo/05b-integration`; `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64`.
- Code, then `mvn -B -f java/interest/pom.xml clean verify`; commit `6e3c637`; then `test-output.txt` as in
  `interest-ticket-verify` step 8 and `.agents/skills/interest-ticket-verify/parity-report.sh 08-error-paths` with all seven cases;
  `git checkout -- tools/parity/__pycache__`; scope check of `interest-ticket-build` step 9 (prints nothing).

Failures hit:

| Symptom | Cause | Fix |
|---|---|---|
| `mvn -o` : `maven-failsafe-plugin:jar:3.5.4 has not been downloaded` | offline mode on a fresh local repository | run without `-o` |
| `CalcInterestTablesRestartIT` (3 failures): work log expected to hold committed lines only | this ticket writes the lines of the stop and `ABENDING PROGRAM` to it | the two assertions now expect those lines; the restart half of the test, unchanged, proves they are cut off |
| `ErrorPathsTablesIT`: run-log echo expected 28 characters, was 50 | the test took the echo from the input file, the echo is the 50-byte record | the expected echo is the line of the recorded run log |
| `ErrorPathsTablesIT.intH05_interestAboveFieldStops`: `bad SQL grammar` | column is `tran_cat_bal`, not `trancat_bal` | column name corrected |
| `ErrorPathsTablesIT.intH05_millionthTransactionStops`: 0 rows updated | the step's restart state is not stored as JSON text | set through `JobRepository.updateExecutionContext` |

## 7. Dependencies

None added. `java/interest/pom.xml` is unchanged.
