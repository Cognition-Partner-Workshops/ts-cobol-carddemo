# Ticket 06 calculation step on PostgreSQL - report

Ticket: `modernization/design/tickets/06-calc-step-jdbc.md` (INT-T06). Branch `demo/05-g2-logic`, second ticket of group
g2-logic, after ticket 04 on the same branch. Code revision the evidence was produced from: `b0dcde7`.

Read this with [test-output.txt](test-output.txt) and [parity-report.md](parity-report.md). The comparator
(`make parity-java`) runs the job on the file adapters, which stay the default (section 2); what this ticket builds is
proven by `JdbcCalcStepIT` on PostgreSQL 15, against the same recorded legacy output.

## 1. Result

| Acceptance line of the ticket | Result | Where it is shown |
|---|---|---|
| Tables holding the i05-multi-category input: interest_transaction rows of the run in seq_no order equal the expected SYSTRAN records field by field; account equals the expected ACCTDATA | met, for i05 and for the other six cases too (every field of CVTRA05Y and CVACT01Y, FILLER bytes included) | `JdbcCalcStepIT.fixtureCase_tablesAndRunLogEqualTheRecordedOutput` (7 rows) |
| Read order: `ORDER BY trancat_acct_id, trancat_type_cd COLLATE "C", trancat_cd`; the last account of the input is recognised without a second query per item | met: one query, in `JdbcBalanceSource`, one forward-only cursor for the step; the row after an account's last balance is read ahead, and no row ahead means the account ends the input | `JdbcCalcStepIT.intR01_readsInKeyOrder` (rows inserted out of order, type codes 01, 0A, A1), `intR01_restartReadsOnAfterTheLastCommittedAccount` |
| Transaction: the inserts of one account's transactions and the update of that account commit together; a forced failure between them leaves neither | met: a trigger makes the UPDATE of account 2001 fail after its transaction was inserted; no row of the run and no change of the account remain | `JdbcCalcStepIT.intR07_insertsAndUpdateOfAnAccountCommitTogether` |
| Restart: a run forced to fail after the first committed chunk and then restarted ends with the same rows as an uninterrupted run - same transaction ids, no account updated twice | met: the restarted run's tables equal the recorded legacy output of i01-baseline (ids ...000001, ...000002; account 2001 at 1010.00) | `JdbcCalcStepIT.intH02_failedRunRestartsWithoutChargingTwice` |
| SYSOUT: the run log equals the expected sysout; after a restart no line is missing or written twice | met, byte for byte, on all seven cases and after the restart | `fixtureCase_tablesAndRunLogEqualTheRecordedOutput`, `intH02_failedRunRestartsWithoutChargingTwice` |
| Errors: a failure ends the step FAILED with the account id in the message; committed chunks stay committed | met. The account id is in the exit description of the step in the job repository (`calcInterestStep failed at account 00000002002`), not in the application log (S6.5; section 3) | `intH02_failedRunRestartsWithoutChargingTwice`, `intR07_insertsAndUpdateOfAnAccountCommitTogether` |
| Database assertions: no UPDATE touches a column of account other than the three; tran_cat_balance, card_xref, disclosure_group unchanged (row hash before and after) | met on all seven cases: md5 over the ten other columns of account and over every row of the three tables, before and after | `fixtureCase_tablesAndRunLogEqualTheRecordedOutput`; the UPDATE statement in `JdbcAccountStore` names three columns |
| Build: `mvn -q -f java/interest/pom.xml verify` with the db service | met: 282 unit and 36 integration tests, 0 failures, BUILD SUCCESS (before this ticket: 282 and 20) | test-output.txt |
| Tests: integration tests for commit, rollback and restart | met | `JdbcCalcStepIT` (16 tests) |
| Fixture cases i01-baseline, i05-multi-category, i07-nontie-rounding through the tables | met (and the other four) | `fixtureCase_tablesAndRunLogEqualTheRecordedOutput` |
| Final-revision evidence: test summary including the restart test | met | test-output.txt, `Tests run: 16 ... JdbcCalcStepIT` |

Rules of the ticket and their tests in `JdbcCalcStepIT`: INT-R01 `intR01_readsInKeyOrder`,
`intR01_restartReadsOnAfterTheLastCommittedAccount`; INT-R07 `intR07_insertsAndUpdateOfAnAccountCommitTogether`,
`intR07_interestRunHoldsWhatTheCalculationWrote`; INT-Q04 the seven fixture rows (the last account equals the recorded,
not updated, record); INT-H02 `intH02_failedRunRestartsWithoutChargingTwice`, `intH02_secondRunWithTheSameParmIsNotRefused`;
INT-T02 the run log of the seven fixture rows; also `intR03_defaultGroupKeyPadded`, `intR06_cardNumbersOfAccountInCardNumberOrder`,
`intT09_emptyBalanceTableWritesNothing` for the adapters.

Added: package `jdbc` (`JdbcBalanceSource`, `JdbcAccountStore`, `JdbcCardLookup`, `JdbcRateLookup`, `JdbcTransactionSink`,
`InterestRunTable`, `JdbcAccessFailure`), `batch/JdbcCalcInterestRun`, `batch/RunLogFile`; changed `batch/InterestJobConfig`
(the step is built on the JDBC adapters when `interest.calc.store=jdbc`). Tests: `batch/JdbcCalcStepIT`,
`jdbc/InterestTables` (test support). Domain, policy, ports (FI-3), schema (FI-4), `application.yml` and the pom are untouched.

## 2. What the ticket does not cover (gaps)

| Gap | State in this branch | Owner |
|---|---|---|
| **Chunk size (G11)** | Default `interest.calc.commitInterval` = 100 accounts, unchanged from the pilot and equal to `architecture.md` section 5. It is a guess until the volumes are known; the tests run with 1 | mainframe operations owner (G11) |
| The job still runs on files by default | `interest.calc.store` defaults to `file`, read in `InterestJobConfig` with the default in the code (`application.yml` is outside this ticket). The job has no load step (05) and no export step (07) on this branch, so with `jdbc` it would read tables nobody filled and write no datasets; `make parity-java` therefore still proves the file path. Switching the default, and parity through the database, is the gate | 09 |
| Datasets and rc.json in jdbc mode | not written; only `sysout/INTCALC.STEP15.txt` is, directly in the output directory, because a restart needs the log of the failed execution. An export that replaces the output directory must keep it | 07, 09 |
| Row of the run in interest_run | The step takes `run_id` from the job execution context (key `interest.runId`) and inserts the row itself (status LOADED) when no earlier step did. It sets CALCULATED with the written_* figures, or FAILED. started_at / finished_at beyond that, and loaded_*, are the loader's and the export's | 05, 07 |
| Legacy messages on a database failure | A `DataAccessException` or `JdbcAccessFailure` fails the step as it is; it is not turned into `InterestRunStoppedException` and writes no legacy line to the run log | 08 |
| Run log after a failure | holds the lines displayed up to the failure, including those of the chunk that was rolled back; the restart cuts them off and writes them again | 08 (what a stopped run's log must hold) |
| Later clock after a restart | not shown by a test: the clock is frozen. As coded, records written after a restart carry the clock at that moment (INT-Q03) | - |
| `rerunGuard=REFUSE_SAME_PARM` | not built: the switch is not in `InterestPolicy` and policy is outside this ticket. OFF behaviour is tested | INT-H02 owner |
| Transaction counter after a restart | `TransactionCounter` (domain, not to be changed here) starts at zero with no other way in; the step advances a new counter to the saved value by calling `next()` | 08 may give it a starting value |

## 3. For the decision register

| Finding | Evidence | Suggested owner |
|---|---|---|
| The ticket wants "the account id in the message" of a failed step; standard S6.5 forbids an account id in the application log and in the stop exception. Built: the id is in the step's exit description (`batch.batch_step_execution.exit_message`), the application log line has counts only | `JdbcCalcInterestRun.afterStep` | target architect |
| A second run on the same tables charges every account again with the same amounts (interest is computed on the category balance, which the job never changes): account 2001 goes 1000.00 -> 1010.00 -> 1020.00. This is INT-H02 as coded | `intH02_secondRunWithTheSameParmIsNotRefused` | business owner (INT-H02) |
| `written_acct_curr_bal_sum` is the sum over the whole account table after the run, as the schema comment says, not only over updated accounts | `InterestRunTable.calculated` | data owner (RC5) |
| Status of a run row created by the calculation is `LOADED`: the data map has no status for "started" | `InterestRunTable.create` | target architect |
| The datasource pool has two connections (`application.yml`); the step needs both (the chunk's transaction and the read cursor) | `JdbcBalanceSource` | target architect |

## 4. Standards review checklist

Standards:

- [x] S1.1 Sources used in order; recorded output decides - PASS: every case asserted against `fixtures/interest/<case>/expected` (`JdbcCalcStepIT`)
- [x] S1.2 Rule/code disagreements reproduced as coded and reported - PASS: section 3
- [x] S2.1 Designed from rules; no method per paragraph - PASS: one adapter per port, named for what it reads or writes
- [x] S2.2 Immutable business records; copybook names only at the mapping - PASS: column names only in the SQL of package `jdbc`; copybook field names only where `JdbcBalanceSource` builds the echoed record through the codec
- [x] S2.3 Spring Batch shape, transaction and restart boundaries of the architecture - PASS: reader, processor, writer and `ItemStream` in `JdbcCalcInterestRun`; restart state saved in `update` inside the chunk transaction
- [x] S2.4 Types and return values, no flags or result codes - PASS: end of input is `Optional.empty()` / `null` from the reader; `endsInput` from the row read ahead
- [x] S2.5 Constructor injection; no mutable static state - PASS: `InterestJobConfig.jdbcCalcInterestRun`; counter, seq_no and log length are step state
- [x] S3.1 `BigDecimal` with data-map precision and scale; no `double`/`float` - PASS: `getBigDecimal` everywhere; rows compared with `BigDecimal.equals` (scale included) in `InterestTables`
- [x] S3.2 Formula: exact product, one division, mode from the policy - N/A: domain not changed
- [x] S3.3 Rounding mode named on every other arithmetic step - N/A: no arithmetic added
- [x] S3.4 Range check before store; overflow goes to the policy - N/A: ticket 08
- [x] S3.5 Encodings only in `record-codec` - PASS: the echoed record is built by `RecordLayout.encode`; no sign or code-page handling in `jdbc` or `batch`
- [x] S3.6 Width, padding and FILLER only in codec and schema - PASS: CHAR columns pad; FILLER of a new transaction is the codec's space; the UPDATE names three columns
- [x] S3.7 Code page and key order each in one place - PASS: the one `ORDER BY ... COLLATE "C"` in `JdbcBalanceSource`
- [x] S3.8 Clock and run parameters injected; `parmDate` is text - PASS: unchanged; `parm_date` stored as text
- [x] S4.1 Every PRESENT behaviour touched has its test; new ones reported - PASS: rows h, j, l, m, p, s below
- [x] S5.1 Each quirk/open decision in one named place behind its switch, rule id in names - PASS: no decision is taken in the adapters; INT-Q04 stays in the calculation
- [x] S5.2 Nothing fixed, tidied or added without a rule id - PASS: no rerun guard, no skip
- [x] S6.1 Status and message mapping as in section 6 - N/A: ticket 08 (section 2)
- [x] S6.2 One stop exception, one exit-code constant, no skip or retry - PASS: the step has no `faultTolerant()`; FAIL in part: a database failure is not yet the stop exception (ticket 08)
- [x] S6.3 Restart as in the architecture - PASS: `intH02_failedRunRestartsWithoutChargingTwice`; a new runId is a new run (`intH02_secondRunWithTheSameParmIsNotRefused`)
- [x] S6.4 Outputs in the recorded format; failed run leaves no SYSTRAN - N/A: the export is ticket 07; committed account updates are kept (`intH02_failedRun...`)
- [x] S6.5 Run log byte for byte; no account data in the application log - PASS: run log equal on seven cases; log lines of `JdbcCalcInterestRun` hold run id, step and counts
- [x] S7.1 A test per rule id, named for it - PASS: section 1
- [x] S7.2 Fixture and edge cases of each rule covered - PASS: seven cases through the tables; synthetic rows for key order and two cards
- [x] S7.3 Integration tests on PostgreSQL 15 in a container - PASS: Testcontainers `postgres:15` with `01-transaction-type.sql` and `02-interest.sql`
- [x] S7.4 No assertion against the code's own output - PASS: the restart test compares with the recorded legacy output, not with another Java run
- [x] S8.1 No edit under `app/`, `fixtures/`, `tools/parity` - PASS: the scope check prints nothing
- [x] S8.2 No golden file re-recorded - PASS
- [x] S8.3 Every new dependency explained in the pull request - PASS: none
- [x] S8.4 Frozen shared interfaces unchanged - PASS: FI-3 and FI-4 untouched
- [x] S8.5 Synthetic data only - PASS

Behaviours PRESENT in section 4:

- [x] a Truncation without ROUNDED - N/A: domain not changed (i02 through the tables passes)
- [x] b High-order digit loss (range check, STOP) - N/A: ticket 08
- [x] c Intermediate precision of the COMPUTE - N/A
- [x] d Zoned overpunch signs - N/A: codec not changed
- [x] e Binary items stay out of data and domain - N/A
- [x] f Non-numeric data rejected at the boundary - N/A: the tables are typed; the loader is ticket 05
- [x] g Fixed width, padding, STRING - PASS: `intR03_defaultGroupKeyPadded`; text columns keep their width (`InterestTables.assertRowsEqual`)
- [x] h Code page and collating order - PASS: `intR01_readsInKeyOrder`
- [x] i REDEFINES replaced by formatter and function - N/A
- [x] j FILLER and record length round trip - PASS: account FILLER and the other nine columns hashed before and after; transaction FILLER compared
- [x] l No state carried between accounts except the counter - PASS: restart state is the counter, seq_no, last account, counts and log length
- [x] m Control break and end of file (INT-Q04, INT-T09) - PASS: seven cases; `intT09_emptyBalanceTableWritesNothing`
- [x] o Level-88 result codes replaced by types - N/A
- [x] p Indexed access through the ports; several cards to the policy - PASS: `intR06_cardNumbersOfAccountInCardNumberOrder`
- [x] q File statuses and abend - N/A: ticket 08
- [x] r PARM, generation, disposition, return code - N/A: export, ticket 07
- [x] s DISPLAY output in the run log - PASS: seven cases and after a restart
- [x] t PARM text against the clock - N/A
- [x] u Timestamp format and hundredths - N/A
- [x] w PARM length checked - N/A
- [x] x No stale rate after a failed lookup - N/A
- [x] y Typed fields where COBOL mixed text and numbers - N/A
- [x] z Abend ends the step; stub output not reproduced - N/A: ticket 08
- [x] aa Dead and empty code not ported - N/A

## 5. What is wrong or missing in the standards skill (proposals only)

1. **S6.5 against ticket 06 "account id in the message".** Proposed addition to S6.5: "The account at which a step
   failed is recorded in the step's exit description in the job repository; it is not written to the application log."
2. **S2.3 / S6.3 name the restart state but not where the run log file lives.** Proposed: "The run log is written to
   `<outputDir>/sysout/INTCALC.STEP15.txt` as the step runs; the export does not replace it."
3. **S2.5 says the counter "comes from the step's restart state"** but the pilot's `TransactionCounter` cannot be
   started at a value. Proposed: name the constructor (`new TransactionCounter(issuedSoFar)`).
4. **No standard says how the calculation finds its row in `interest_run`.** Proposed: "the run's `run_id` is in the
   job execution context under `interest.runId`; the first step that needs it creates the row."
5. **S3.7** could say that the reader's cursor needs a connection of its own, so the pool must hold at least two.

## 6. Method

Skills used: `interest-conversion-standards` (sections 2, 3, 6, 7, 8, 9), `interest-ticket-build` steps 2, 5-9,
`interest-ticket-verify` steps 1, 3, 7-10, `interest-pitfalls` P1, P2 (the pin is kept), P6, P7.

```sh
mvn -B -f java/interest/pom.xml verify                      # 282 + 36 tests, BUILD SUCCESS
make parity-java                                            # 7 of 7 PASS (file adapters, no regression)
.agents/skills/interest-ticket-verify/parity-report.sh 06-calc-step-jdbc i01-baseline i05-multi-category i07-nontie-rounding
```

Failures hit:

| Symptom | Cause | Fix |
|---|---|---|
| `intH02_secondRunWithTheSameParmIsNotRefused` expected 1020.10, was 1020.00 | the expectation was worked wrongly by hand: interest is computed on the category balance (1000.00, never changed), not on the account balance, so the second run adds 10.00 again | the comment and the expected value in the test corrected to 1000.00 + 10.00 + 10.00 |

Corrections to the three procedure skills:

| Skill | Step | What is wrong | Corrected text |
|---|---|---|---|
| interest-ticket-build | 5 | The layout table has no row for package `jdbc`, and says the integration test is `InterestCalcJobIT` only | add: "`.../jdbc` - JDBC adapters of the ports (ticket 06); `batch/JdbcCalcStepIT` runs the step on the tables with `interest.calc.store=jdbc`; `src/test/.../jdbc/InterestTables` loads a case's input into the replica tables" |
| interest-ticket-verify | 7 | The query shows the job repository only; from ticket 06 the run's row and rows are in schema `carddemo` | add: `select run_id, status, written_transaction_count, written_tran_amt_sum, written_account_count from carddemo.interest_run order by run_id desc limit 1` |
| interest-ticket-verify | 8 | `parity-report.sh` proves the file path only; a ticket whose acceptance is "through the tables" has nothing to put in the report from it | add: "For a ticket proven through the tables, say in `report.md` section 1 which integration test compares the tables with `fixtures/interest/<case>/expected`; the parity report then shows no regression." |
| interest-pitfalls | - | no entry was wrong; none of P1-P7 occurred beyond P1, P6 | - |

## 7. Dependencies

None added. The pom is unchanged.
