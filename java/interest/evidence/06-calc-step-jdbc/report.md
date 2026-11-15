# Ticket 06-calc-step-jdbc - report

Branch `demo/05-06-calc-step-jdbc`, code revision `4c70b7fa8103cc146113e8a36b1764da37b1dbef`, base `demo/05b-integration` (`03f2b01bfba36f82b4599023b957864d8db6164e`).
Build and tests: `mvn -B -f java/interest/pom.xml clean verify` - BUILD SUCCESS, 182 unit tests (176 before) and 57 integration tests (28 before), no failure (`test-output.txt`).
Parity: 7 of 7 with `make parity-java` (file adapters, as before) and 7 of 7 through the tables (`parity-report.md`).

## 1. Result

| Acceptance line of the ticket | Met | Shown by |
|---|---|---|
| Tables holding the i05-multi-category input: interest_transaction in seq_no order equals the expected SYSTRAN field by field; account equals the expected ACCTDATA | yes | `CalcInterestTablesIT.fixtureCase_tablesAndRunLogMatchRecorded[i05-multi-category]` (byte for byte) and compare.py `java-tables` PASS, 3 records, 38 fields (`parity-report.md`, "Through the tables"). Same for the other six cases |
| Read order `ORDER BY trancat_acct_id, trancat_type_cd COLLATE "C", trancat_cd`; the last account is recognised without a second query per item | yes | `JdbcCalcAdaptersIT.intR01_readsInKeyOrder`, `intR01_everyPageSizeGivesTheSameAccounts`, `intQ04_onlyTheAccountThatEndsTheInputIsMarked`, `intQ04_lastAccountIsKnownWithoutAQueryPerAccount` (40 accounts, 1 scan) |
| The inserts of one account's transactions and the update of that account commit together; a forced failure between them leaves neither | yes | `CalcInterestRestartIT.intR07_forcedFailureBetweenInsertsAndUpdateLeavesNeither` (a trigger refuses the UPDATE of account 2009 after the INSERTs) |
| A run forced to fail after the first committed chunk and restarted ends with the rows of an uninterrupted run - same transaction ids, no account updated twice | yes | `CalcInterestRestartIT.rel11_restartEndsWithTheRowsOfAnUninterruptedRun`, `rel11_failureInTheFirstChunkRestartsFromTheFirstAccount`; the uninterrupted run is `intR07_transactionsAndUpdateOfAnAccountCommitTogether`, all three against the same hand-written rows |
| SYSOUT: the run log equals the recorded INTCALC.STEP15.txt; after a restart no line is missing or written twice | yes | seven cases in `CalcInterestTablesIT`; restart: `WHOLE_RUN_LOG` in `CalcInterestRestartIT`; `RestartableRunLogTest` |
| Errors: a failure ends the step FAILED with the account id in the message; committed chunks stay committed | yes, with the id in the exit description of the step, not in the log (finding 1) | `intR07_forcedFailure...`, `intR08_failureInTheCalculationNamesTheAccountAndKeepsCommittedChunks` |
| No UPDATE touches a column of account other than acct_curr_bal, acct_curr_cyc_credit, acct_curr_cyc_debit; tran_cat_balance, card_xref and disclosure_group are unchanged (row hash) | yes | trigger `test_other_columns` and the row hashes in `CalcInterestTablesIT` (seven cases) and `JdbcCalcAdaptersIT.intR07_postInterestWritesThreeColumnsOnly` |
| Fixture cases i01-baseline, i05-multi-category, i07-nontie-rounding through the tables | yes | `parity-report.md`, "Through the tables" |
| Final-revision evidence: test summary including the restart test | yes | `test-output.txt`: `CalcInterestRestartIT` 5 tests |

## 2. What the ticket does not cover

- **Chunk size is a guess (G11).** Default chosen: `interest.calc.commitInterval` = 100 accounts per commit (the existing setting, architecture.md section 5). The page of `tran_cat_balance` rows read per query is a constant, 1000 rows; no result depends on either.
- **The job still runs on the file adapters by default.** `interest.calc.store=file|jdbc` chooses the adapters of `calcInterestStep`; the default `file` is in `InterestJobConfig` because `application.yml` and the `Makefile` are frozen for this ticket. Line for `application.yml` (ticket 09, with the loader and the export): `interest.calc.store: jdbc` under `interest.calc`.
- **`make parity-java` does not go through the tables yet** (ticket 09). The seven cases go through the tables in `CalcInterestTablesIT`; the datasets compared there are re-encoded by test support, not by the export of ticket 07.
- **The jar was not run from the command line with `interest.calc.store=jdbc`** against the `db` service: nothing loads its tables until ticket 05 is merged. The step is proven in the integration tests only.
- **interest_run.** The schema has no column that links the job's `runId` (text) to `interest_run.run_id` (identity). The step takes the row id from the job execution context key `interestRunId` when an earlier step put it there, and otherwise inserts the row itself with status `LOADED`. Tickets 05 and 07 were told through the wave notes; whether they use that key is not checked here.
- **Run log location.** In jdbc mode the step writes `<outputDir>/sysout/INTCALC.STEP15.txt` in place while it runs, and a failed run keeps the file (it is cut back on restart). An export that replaces `<outputDir>` would delete it; S6.4 says a failed run with file adapters keeps nothing, and does not say where the run log of a restartable run lives.
- **Error paths are ticket 08.** A failure writes no legacy message to the run log; a driver exception (`DataAccessException`) is not wrapped into `InterestRunStoppedException`, and its text is logged by Spring Batch - a constraint message of PostgreSQL could carry key values of a row. `StopReason` has no UPDATE_ACCOUNT or OVERFLOW (domain is frozen here), so `WRITE_OUTPUT` and `RECORD_REJECTED` are used.
- **Restart and the clock.** Tests freeze the clock; with a running clock, records written after a restart carry the later time (architecture.md section 6, INT-Q03 as coded). Not exercised.
- **Restart needs the same job parameters** (same `runId`, same `outputDir`) and an untouched run log file; a run log shorter than the committed length stops the restart. A restart after the replica tables were reloaded is not detected.
- **Pool size.** The reader uses the chunk's transaction (keyset pages), not a cursor on a second connection, so the pool of 2 in `application.yml` is enough.
- Rules and work left to other tickets: the loader (05), the export and rc.json (07), the legacy failure messages and the switches multiCard and overflow (08), `make parity-java` through the database and CI (09), reconciliation (10). INT-H02 `rerunGuard=REFUSE_SAME_PARM` is not built: the decision is open and the ticket keeps it OFF.

## 3. For the decision register

1. **Ticket 06 Acceptance against standard S6.5.** The ticket says "a failure ends the step FAILED with the account id in the message"; S6.5 and `InterestRunStoppedException` say no account id in the application log or the exception message. Built: the id is in the step's exit description (`batch.batch_step_execution.exit_message`), not in the log. Evidence: `CalcInterestRestartIT.intR07_forcedFailureBetweenInsertsAndUpdateLeavesNeither`. Suggested owner: target architect.
2. **INT-H02 through the tables.** A second run with a new `runId` on tables that were not reloaded charges the accounts again (i01: 2001 goes 1000.00 -> 1010.00 -> 1020.00) and writes the same transaction ids under a new `run_id`. That is the legacy behaviour with rerunGuard OFF; with tables it depends on the load being repeated before every run (architecture.md section 7, rule 1). Evidence: `CalcInterestTablesIT.intH02_secondRunWithTheSameParmIsNotRefused`. Suggested owner: mainframe operations owner (REL-11, INT-H02).
3. **REL-11 as proposed leaves a window the legacy does not have.** After a failed execution the committed accounts are updated and their transactions are in `interest_transaction`, status `FAILED`; until the restart nothing marks the replica tables as half-calculated except that status. Suggested owner: mainframe operations owner.
4. **No link between the job's runId and interest_run.run_id in the schema (FI-4).** See gaps. Suggested owner: target architect.
5. **`TransactionCounter` cannot be set to a saved value.** The restart restores it by issuing the saved number of ids on a new counter (`CalcInterestTableRun.counterAt`). A constructor `TransactionCounter(int issued)` in the domain would say it directly. Suggested owner: owner of ticket 04 / target architect.
6. **`RecordValues` has no setter for a FILLER.** A row's FILLER is copied into the encoded image by offset in `JdbcBalanceSource` (and in the test support). Suggested owner: owner of ticket 03.

## 4. Standards review checklist

Standards:

- [x] S1.1 Sources used in order; recorded output decides - PASS: ticket, rules, architecture sections 4-6, data map, CBACT04C lines, then the recorded files; `CalcInterestTablesIT` asserts against `fixtures/interest/<case>/expected`
- [x] S1.2 Rule/code disagreements reproduced as coded and reported - PASS: INT-Q04 as coded (account that ends the input not updated, i01 and i07 through the tables); the ticket/standard disagreement on the account id in the failure message is in "For the decision register"
- [x] S2.1 Designed from rules; no method per paragraph - PASS: one adapter per port, `CalcInterestTableRun`, `CalcRestartState`, `RestartableRunLog`
- [x] S2.2 Immutable business records; copybook names only at the mapping - PASS: copybook field names appear only in the row mappings of `jdbc/calc`; `CalcRestartState` is a record; no WORKING-STORAGE class
- [x] S2.3 Spring Batch shape, transaction and restart boundaries of the architecture - PASS: reader/processor/writer on one item = one account, chunk of whole accounts, restart state saved in `update()` inside the chunk transaction (`CalcInterestRestartIT`)
- [x] S2.4 Types and return values, no flags or result codes - PASS: `Optional`/`OptionalLong`, end of input is an empty `Optional`, failure is an exception
- [x] S2.5 Constructor injection; no mutable static state - PASS: constructors only; counter, clock and policy are injected or built per step execution in `open()`
- [x] S3.1 `BigDecimal` with data-map precision and scale; no `double`/`float` - PASS: `getBigDecimal` on NUMERIC columns; tests use `new BigDecimal("...")`
- [ ] S3.2 Formula: exact product, one division, mode from the policy - N/A: the formula is ticket 04's domain code, not changed
- [ ] S3.3 Rounding mode named on every other arithmetic step - N/A: no arithmetic added (sums of `interest_run` are computed by SQL `sum` on NUMERIC(…,2))
- [x] S3.4 Range check before store; overflow goes to the policy - PASS for the check (`JdbcTransactionSink` encodes every row through the codec before the INSERT, `JdbcAccountStore` checks ACCT-CURR-BAL against S9(10)V99: `intH05_amountThatDoesNotFitThePictureIsRefusedBeforeTheInsert`, `intR07_balanceThatDoesNotFitThePictureIsRefusedBeforeTheUpdate`); the `overflow` switch itself is ticket 08, so the stop reason is RECORD_REJECTED, not OVERFLOW
- [x] S3.5 Encodings only in `record-codec` - PASS: the echo image and the FILLER bytes come from `RecordCodec`; no overpunch or code page logic in `jdbc` or `batch`
- [x] S3.6 Width, padding and FILLER only in codec and schema - PASS: CHAR(n) pads, the codec gives the FILLER; the UPDATE names three columns (`intR07_postInterestWritesThreeColumnsOnly`, trigger `test_other_columns`)
- [x] S3.7 Code page and key order each in one place - PASS: one `ORDER BY trancat_acct_id, trancat_type_cd COLLATE "C", trancat_cd` constant in `JdbcBalanceSource` (`intR01_readsInKeyOrder`); the code page is `RecordFormat.fixture()` (the job accepts `format=fixture` only)
- [x] S3.8 Clock and run parameters injected; `parmDate` is text - PASS: unchanged; `parm_date` of `interest_run` is the PARM text
- [x] S4.1 Every PRESENT behaviour touched has its test; new ones reported - PASS: rows l, m, p, s below; nothing new found
- [x] S5.1 Each quirk/open decision in one named place behind its switch, rule id in names - PASS: INT-Q04 stays in the policy and the calculator; INT-H02 `rerunGuard` is OFF and not built (no code reads it; `intH02_secondRunWithTheSameParmIsNotRefused`)
- [x] S5.2 Nothing fixed, tidied or added without a rule id - PASS with one thing to confirm: `interest.calc.store` (file|jdbc) is wiring, not behaviour; it has no rule id and is reported in the gaps
- [ ] S6.1 Status and message mapping as in section 6 - N/A: the legacy messages of the failure paths are ticket 08; this ticket writes no failure line to the run log
- [x] S6.2 One stop exception, one exit-code constant, no skip or retry - PASS: no `faultTolerant()`; a `DataAccessException` of the driver also fails the step (wrapping it into the stop exception with the legacy message is ticket 08)
- [x] S6.3 Restart as in the architecture - PASS: `rel11_restartEndsWithTheRowsOfAnUninterruptedRun`, `rel11_failureInTheFirstChunkRestartsFromTheFirstAccount`
- [ ] S6.4 Outputs in the recorded format; failed run leaves no SYSTRAN - N/A for the datasets (export is ticket 07). The run log is written in place and a failed run keeps it, because the restart needs it: reported in the gaps
- [x] S6.5 Run log byte for byte; no account data in the application log - PASS: run log equals the recorded SYSOUT for seven cases; the account id of a failure goes to the step's exit description in the job repository only (`intR07_forcedFailureBetweenInsertsAndUpdateLeavesNeither` asserts it is not in the captured log). A driver message could carry row data: gap
- [x] S7.1 A test per rule id, named for it - PASS: `intR01_*`, `intR07_*`, `intQ04_*`, `intH02_*`, `intT02_*`; restart tests are named `rel11_*` after the release decision (no rule id exists for restart)
- [x] S7.2 Fixture and edge cases of each rule covered - PASS: seven cases through the tables; synthetic: key order with mixed-case type codes, page boundaries, empty input, missing account, FILLER that is not spaces
- [x] S7.3 Integration tests on PostgreSQL 15 in a container - PASS: `postgres:15` with `db/postgres/01-*.sql` and `02-interest.sql`
- [x] S7.4 No assertion against the code's own output - PASS: recorded files, or hand-written values with the arithmetic in the test (`CalcInterestRestartIT` class comment)
- [x] S8.1 No edit under `app/`, `fixtures/`, `tools/parity` - PASS: scope check prints nothing
- [x] S8.2 No golden file re-recorded - PASS
- [x] S8.3 Every new dependency explained in the pull request - PASS: none added
- [x] S8.4 Frozen shared interfaces unchanged - PASS: ports, schema, pom, switch names, Makefile untouched
- [x] S8.5 Synthetic data only - PASS: fixture accounts 2001-2010 and made-up ids 3000-3039, 9999

Behaviours PRESENT in section 4:

- [ ] a Truncation without ROUNDED - N/A: domain, ticket 04 (i07 through the tables still gives 15.42 and 13.61)
- [ ] b High-order digit loss (range check, STOP) - N/A: switch is ticket 08; the check before store is S3.4
- [ ] c Intermediate precision of the COMPUTE - N/A: domain
- [ ] d Zoned overpunch signs - N/A: codec, ticket 03
- [ ] e Binary items stay out of data and domain - N/A: not touched
- [ ] f Non-numeric data rejected at the boundary - N/A: the boundary is the loader (ticket 05); the tables hold typed columns
- [x] g Fixed width, padding, STRING - PASS: `intR06_transactionRowHoldsTheRecordFields` (row re-encoded equals the recorded record)
- [x] h Code page and collating order - PASS: `intR01_readsInKeyOrder` (`COLLATE "C"`)
- [ ] i REDEFINES replaced by formatter and function - N/A: not touched
- [x] j FILLER and record length round trip - PASS: `intT02_echoIsTheRecordImageWithItsFiller`; account FILLER untouched (row hash)
- [x] l No state carried between accounts except the counter - PASS: the counter and `seq_no` are the only values carried, and they are the restart state
- [x] m Control break and end of file (INT-Q04, INT-T09) - PASS: `intQ04_onlyTheAccountThatEndsTheInputIsMarked`, `intQ04_lastAccountIsKnownWithoutAQueryPerAccount`, `emptyInput_writesStartAndEndOnly`
- [ ] o Level-88 result codes replaced by types - N/A: no result code introduced
- [x] p Indexed access through the ports; several cards to the policy - PASS for the ports (`intH03_cardNumbersOfAnAccountInCardNumberOrder` returns every card in order); the `multiCard` switch is ticket 08
- [ ] q File statuses and abend - N/A: ticket 08
- [ ] r PARM, generation, disposition, return code - N/A: not touched (`rc.json` is the export's)
- [x] s DISPLAY output in the run log - PASS: recorded SYSOUT equal for seven cases through the tables
- [ ] t PARM text against the clock - N/A: not touched
- [ ] u Timestamp format and hundredths - N/A: not touched
- [ ] w PARM length checked - N/A: not touched
- [ ] x No stale rate after a failed lookup - N/A: domain; `JdbcRateLookup` returns `Optional` (`intR02_rateByGroupTypeAndCategory`)
- [ ] y Typed fields where COBOL mixed text and numbers - N/A: not touched
- [ ] z Abend ends the step; stub output not reproduced - N/A: ticket 08
- [ ] aa Dead and empty code not ported - N/A: nothing ported

## 5. What is wrong or missing in the standards skill

- S6.4 / S6.3: say where the run log of a restartable run is written and whether a failed run keeps it. Proposed: "With table adapters the run log is written in place under the output directory while the step runs; a failed run keeps it for the restart, and the export does not remove it."
- S6.5 against ticket 06: say where the account id of a failure may appear. Proposed: "The account id of a failure may be stored in the step's exit description in the job repository; it is not written to the application log."
- S2.3: name the place of the `interest_run` id. Proposed: "The id of the run's `interest_run` row travels in the job execution context under `interestRunId`."
- S7.1: restart has no rule id (REL-11 is a release decision). Proposed: "Tests of a release decision are named `rel<nn>_<whatItProves>`."

## 6. Method

Commands, from the repository root, with `JAVA_HOME` on JDK 21:

```sh
make parity-java                                   # on the base, before any change: 7 of 7 PASS
mvn -B -f java/interest/pom.xml verify             # while working
git checkout -- tools/parity/__pycache__ ; scope check of interest-ticket-build step 9 (prints nothing)
mvn -B -f java/interest/pom.xml clean verify       # test-output.txt, from the committed revision
for c in $(ls fixtures/interest); do python3 tools/parity/compare.py interest $c --actual java/interest/target/parity-tables/$c/actual --label java-tables; done
.agents/skills/interest-ticket-verify/parity-report.sh 06-calc-step-jdbc i01-baseline i05-multi-category i07-nontie-rounding
```

Steps of interest-ticket-verify that do not fit this ticket:

- Steps 1, 3 and 8 (`make parity-java`, `parity-report.sh`) run the file adapters; they show no regression, not the tables. The section "Through the tables" of `parity-report.md` was added by hand.
- Step 6 (error path from the command line): skipped. The error line of the ticket needs loaded tables and a forced database failure; it is shown by the integration tests.
- Step 7 (database assertions in the `db` service): not run there, because nothing loads the tables of the `db` service; the assertions run in the integration tests on a `postgres:15` container with both init scripts. The job repository section of `parity-report.md` is the script's.

Failures hit:

| Symptom | Cause | Fix |
|---|---|---|
| `intH02_secondRunWithTheSameParmIsNotRefused` expected 1020.10 but was 1020.00 | the test's own arithmetic was wrong: interest is computed on the category balance (still 1000.00), not on the account balance | expectation corrected to 1000.00 x 12.00 / 1200 = 10.00 twice; no code change |

No pitfall of interest-pitfalls was hit (P1 and P6 were avoided by following them).

## 7. Dependencies

None added. `JdbcTemplate` comes with `spring-boot-starter-batch`; the tests use the Testcontainers and Spring Boot test modules already in the pom.
