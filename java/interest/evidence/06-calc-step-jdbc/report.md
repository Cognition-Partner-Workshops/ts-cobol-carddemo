# Ticket 06 - calculation step on PostgreSQL with commit and restart boundaries - report

Ticket: `modernization/design/tickets/06-calc-step-jdbc.md` (INT-T06). Branch `demo/05-06-calc-step-jdbc`, started from
`demo/05b-integration` at `f09b623` (tickets 02, 03 and 04 integrated). The evidence in this directory was produced
from the committed revision `eac5c63` with a clean working tree.

Read this with [parity-report.md](parity-report.md) (the three fixture cases of the ticket through the tables, and
`make parity-java` unchanged) and [test-output.txt](test-output.txt) (`mvn clean verify`, restart tests included).

## 1. Result

| Acceptance line of the ticket | Result | Where it is shown |
|---|---|---|
| Input: tables holding the i05-multi-category input. `interest_transaction` rows of the run in `seq_no` order equal the expected SYSTRAN records field by field; `account` equals the expected ACCTDATA | met | `CalcInterestTablesIT.intR01_i05TransactionsInSeqNoOrderAndAccountEqualTheRecordedRun`; parity-report.md, "Through the tables": i05 PASS (3 records, 38 fields, 0 differences) |
| Read order: `ORDER BY trancat_acct_id, trancat_type_cd COLLATE "C", trancat_cd`; the last account of the input is recognised without a second query per item | met | `jdbc/JdbcBalanceSource.java` (the one ORDER BY of the step); `CalcInterestTablesIT.intR01_readsInKeyOrder` (digits, upper case, lower case), `intQ04_lastAccountIsRecognisedWithoutAQueryPerAccount` (five accounts: 1 query when the rows fit a page, 3 with pages of three rows) |
| Transaction: the inserts of one account's transactions and the update of that account commit together; a forced failure between them leaves neither | met | `CalcInterestTablesRestartIT.transaction_failureBetweenInsertAndUpdateOfAnAccountLeavesNeither` (the UPDATE of account 2001 fails after its INSERT: 0 transaction rows, account row hash unchanged) |
| Restart: a run forced to fail after the first committed chunk and then restarted ends with the same rows as an uninterrupted run - same transaction ids, no account updated twice | met | `CalcInterestTablesRestartIT.restart_afterTheFirstCommittedChunkEndsAsTheRecordedUninterruptedRun` (i01-baseline and i07-nontie-rounding against the recorded run; every UPDATE of `account` is recorded by a trigger: one, of the first account), `restart_fiveAccountsStoppedAtTheFourthEndAsAnUninterruptedRun` (five accounts, stopped after three commits, against an uninterrupted run of the same tables) |
| SYSOUT: the run log equals `fixtures/interest/<case>/expected/sysout/INTCALC.STEP15.txt`; after a restart no line is missing or written twice | met | `CalcInterestTablesIT.sysout_matchesRecorded`, `throughTheTables_everyCaseEqualsTheRecordedLegacyRun` (seven cases); after a restart: the two restart tests above; `RunLogFileTest` (6 tests) |
| Errors: a failure ends the step FAILED with the account id in the message; committed chunks stay committed | met, with a finding (section 3, F1) | the three failure tests of `CalcInterestTablesRestartIT`: step `FAILED`, `commits=1 rollbacks=1` (or 3 and 1), first transaction and first account still there, job exit message `calcInterestStep stopped at account 00000002002: WRITE_OUTPUT (INT-R08): ...`; exit code 12 |
| Database assertions: no UPDATE touches a column of account other than `acct_curr_bal`, `acct_curr_cyc_credit`, `acct_curr_cyc_debit`; `tran_cat_balance`, `card_xref` and `disclosure_group` are unchanged (row hash before and after) | met | `CalcInterestTablesIT.intR07_updateTouchesOnlyTheThreeAccountColumnsAndNoReplicaTable` (row hashes, and a trigger that records every write on the four tables: `account UPDATE 00000002001 false` and nothing else); `jdbc/JdbcAccountStore.java` (the one UPDATE) |
| Build: `mvn -q -f java/interest/pom.xml verify` with the db service; integration tests for commit, rollback and restart | met: 319 unit tests and 44 integration tests, 0 failures, BUILD SUCCESS (313 and 20 before the ticket) | test-output.txt |
| Fixture cases i01-baseline, i05-multi-category, i07-nontie-rounding through the tables | met: PASS, PASS, PASS (the other four are run too: PASS) | parity-report.md, "Through the tables" |

Parity per case, through the tables and on the file adapters (`make parity-java`): i01, i02, i03, i04, i05, i06, i07 PASS.

Not touched: `app/`, `fixtures/`, `tools/`, `modernization/`, `.agents/`, `db/`, `docker-compose.yml`, `Makefile`,
`pom.xml`, `application.yml`, the packages `domain`, `policy`, `port`, `codec`, `file`
(`git diff --stat origin/demo/05b-integration -- app fixtures tools modernization .agents db docker-compose.yml Makefile java/interest/pom.xml java/interest/src/main/resources`
is empty). Every changed file is under the ticket's Allowed files or this directory. No golden file was re-recorded.
No ticket system was written to. Nothing was merged.

What was built:

* `jdbc/`: `JdbcBalanceSource`, `JdbcAccountStore`, `JdbcCardLookup`, `JdbcRateLookup`, `JdbcTransactionSink` (the
  five table ports of FI-3) and `JdbcInterestRuns` (status and RC5 figures of the run in `interest_run`).
* `batch/CalcInterestTableRun`: reader, processor, writer, restart state and listeners of the step on the tables;
  `batch/RunLogFile`: the `RunLog` port as a file that can be cut back on restart; `batch/InterestJobConfig`: the step
  takes its adapters from `interest.calc.adapters` (`file`, the default, or `jdbc`).
* Restart state, saved in the step's execution context inside the transaction of each chunk: last committed account,
  transaction counter, last `seq_no`, length of the run log, interest not added so far, `interest_run.run_id`.

## 2. What the ticket does not cover (gaps)

* **The chunk size is a guess.** The default stays `interest.calc.commitInterval=100` accounts (architecture.md
  section 5) until G11 (volumes) is closed. The balance rows are read in pages of 1000 rows
  (`JdbcBalanceSource.DEFAULT_PAGE_ROWS`), also a guess from no volume figure.
* **The job still runs on the file adapters by default.** `interest.calc.adapters=jdbc` selects the tables. With the
  tables the job is this one step: nothing fills the replica tables and nothing writes the datasets until
  `loadExtractsStep` (ticket 05) and `exportDatasetsStep` (ticket 07) are steps of the job. Their classes are not on
  the base branch, so they could not be declared here. To do when 05 and 07 are integrated (package `batch`,
  `InterestJobConfig`): declare the three steps in order, make `jdbc` the default, and point `make parity-java` at it
  (ticket 09). Until then `make parity-java` (FI-6) proves the file adapters, and the tables are proven by the
  integration tests, which load and encode with test code (`src/test/.../jdbc/InterestTables.java`).
* **The key of the run id in the job execution context differs between tickets 05 and 07** (section 3, F2). This step
  reads and writes `interestRunId`.
* **Legacy messages of the stop conditions** (run log lines, status line, `ABENDING PROGRAM`), the reasons
  `READ_BALANCES`, `UPDATE_ACCOUNT`, `WRITE_TRANSACTION`, and INT-H03/INT-H05: ticket 08. A table that cannot be read
  stops the run with `OPEN_INPUT (INT-R08)`, a failed insert or update with `WRITE_OUTPUT (INT-R08)`; nothing is
  written to the run log for them.
* **`rerunGuard` (INT-H02) is OFF and has no switch here.** A new `runId` on tables that a stopped run already
  updated starts from the first account and charges the committed accounts again
  (`restart_aStoppedRunIsNotResumedByANewRunId`). Only a launch with the same parameters is a restart.
* **`format=host`** is still refused by the job parameters; the record echo of the run log uses the fixture code page.
* **`interest-run-control`** (architecture.md section 2) has no ticket. This step writes its own part of
  `interest_run`: it creates the row when no step before it did, sets `CALCULATING`, then `CALCULATED` with
  `written_transaction_count`, `written_tran_amt_sum`, `written_interest_not_added_sum` (RC5), or `FAILED`.
  `CALCULATING` is not among the statuses the data map lists (LOADED, CALCULATED, EXPORTED, FAILED, ABANDONED).
* **`java/interest/README.md`** still describes the pilot; it is outside the Allowed files.
* Records written after a restart carry the clock of the restart (architecture.md section 6, INT-Q03 as coded). The
  tests freeze the clock, so this is not exercised.

## 3. For the decision register

| Id | Finding | Evidence | Suggested owner |
|---|---|---|---|
| F1 | The ticket asks for "the account id in the message" of a failed step; standard S6.5 forbids an account id in the application log, and Spring Batch logs the exception of a failed step there. Built: the stop exception carries no account id; the account is named in the exit description of the step and in the exit message of the job (`batch.batch_job_execution.exit_message`). The step's own stored exit message is the stack trace, cut at 2500 characters by the job repository before the sentence appended to it. Decide where operations should read the account from. Also: the database's own error text, which Spring Batch logs as the cause, can hold key values of the failing row (for example a duplicate `tran_id`). | `CalcInterestTableRun.stopped`, `afterJob`; `transaction_failureBetweenInsertAndUpdateOfAnAccountLeavesNeither` | target architect, mainframe operations owner |
| F2 | Tickets 05 and 07 hand the `interest_run.run_id` over under different keys of the job execution context: `interest.runId` (`load/LoadExtractsStepConfig.RUN_ID_KEY` on `demo/05-05-extract-loader`) and `interestRunId` (`export/ExportDatasetsTasklet.INTEREST_RUN_ID` on `demo/05-07-dataset-export`). This step uses `interestRunId`. One key has to be chosen at integration. | the two branches; `CalcInterestTableRun.INTEREST_RUN_ID` | engineering lead |
| F3 | REL-11 is implemented as proposed. The restart state lives in the job repository (`batch.batch_step_execution_context`) and the run log in a work file beside the output directory (`<outputDir>.calc-runlog-<runId>`) until the step completes. A restart needs both: the same job parameters and that file. If the file is gone the restart stops (`WRITE_OUTPUT`). Where the work file lives in production is not decided. | `RunLogFile`, `RunLogFileTest.intR08_restartWithoutTheRunLogStopsTheRun` | mainframe operations owner (REL-11) |
| F4 | INT-H02: with `rerunGuard` OFF a second launch with a new `runId` after a stopped run charges the committed accounts twice. The restart of REL-11 only helps when the operator relaunches with the same parameters. | `restart_aStoppedRunIsNotResumedByANewRunId` | business owner of INT-H02 |
| F5 | `domain/TransactionCounter` cannot start at a value. On restart the step issues the saved number of ids again to reach it (`CalcInterestTableRun.issueAgain`). A constructor taking the last issued number belongs in `domain`, which this ticket may not change. | `CalcInterestTableRun.issueAgain` | owner of ticket 04 / target architect |
| F6 | `StopReason` has no reason for a failed read, insert or update of a table (section 6 of the standards names `READ_BALANCES`, `UPDATE_ACCOUNT`, `WRITE_TRANSACTION`). `OPEN_INPUT` and `WRITE_OUTPUT` are used until ticket 08 adds them. | `jdbc/*.java` | owner of ticket 08 |
| F7 | The tables hold a balance as columns, not as the record read. The run log echo is the record encoded again from the row (rules E1 to E4), which equals the unloaded record for every fixture. A record whose bytes do not survive the load (gaps G02, G03: F-signed positives, minus zero) would be echoed in its normalised form. | `JdbcBalanceSource.row`; RC3 of the loader counts these | mainframe data owner |

## 4. Standards review checklist

Standards:

- [x] S1.1 Sources used in order; recorded output decides - PASS: ticket, rules, architecture sections 4 to 6, data map, then the recorded files in `CalcInterestTablesIT`
- [x] S1.2 Rule/code disagreements reproduced as coded and reported - PASS: none new; INT-Q04 as coded (`intQ04_lastAccountNotUpdated`); ticket against S6.5 reported (F1)
- [x] S2.1 Designed from rules; no method per paragraph - PASS: `jdbc/*`, one adapter per port
- [x] S2.2 Immutable business records; copybook names only at the mapping - PASS: `jdbc` holds no copybook field name; rows map by column position to the layouts of `codec` (`JdbcBalanceSource.RowValues`)
- [x] S2.3 Spring Batch shape, transaction and restart boundaries of the architecture - PASS: `InterestJobConfig.calcInterestStep`, `CalcInterestTableRun.update`; `CalcInterestTablesRestartIT`
- [x] S2.4 Types and return values, no flags or result codes - PASS: reader returns `null` at the end, `Optional` from the lookups, one exception
- [x] S2.5 Constructor injection; no mutable static state - PASS: `CalcInterestTableRun`, `jdbc/*`; counter and state are per step execution
- [x] S3.1 `BigDecimal` with data-map precision and scale; no `double`/`float` - PASS: `JdbcAccountStore`, `JdbcRateLookup`, `JdbcTransactionSink`; tests use `BigDecimal` and text
- [ ] S3.2 Formula: exact product, one division, mode from the policy - N/A: domain, ticket 04, not changed
- [x] S3.3 Rounding mode named on every other arithmetic step - PASS: the sum of interest not added uses `RoundingMode.UNNECESSARY` (`CalcInterestTableRun.write`)
- [ ] S3.4 Range check before store; overflow goes to the policy - N/A: INT-H05 is ticket 08; no check was added or removed
- [x] S3.5 Encodings only in `record-codec` - PASS: the record echo is built by `CategoryBalanceRecord`'s codec; `jdbc` has no encoding code
- [x] S3.6 Width, padding and FILLER only in codec and schema - PASS: `JdbcAccountStore.POST_INTEREST` names three columns; `tran_filler` is spaces of the code page; CHAR(n) padding is the schema's
- [x] S3.7 Code page and key order each in one place - PASS: one ORDER BY (`JdbcBalanceSource.KEY_ORDER`), one code page choice (`CalcInterestTableRun.codePage`); `intR01_readsInKeyOrder`
- [x] S3.8 Clock and run parameters injected; `parmDate` is text - PASS: the injected `Clock` goes to the calculator; `started_at`/`finished_at` of `interest_run` are the database wall clock, as the data map says
- [x] S4.1 Every PRESENT behaviour touched has its test; new ones reported - PASS: rows h, j, l, m, p, r, s below; F7 reported
- [x] S5.1 Each quirk/open decision in one named place behind its switch, rule id in names - PASS: no decision is taken in `jdbc` or `batch`; INT-Q04 stays in the calculator
- [x] S5.2 Nothing fixed, tidied or added without a rule id - PASS: no retry, no skip, no rerun guard
- [ ] S6.1 Status and message mapping as in section 6 - N/A: legacy messages are ticket 08; table failures map to the existing reasons (F6)
- [x] S6.2 One stop exception, one exit-code constant, no skip or retry - PASS: every `DataAccessException` becomes `InterestRunStoppedException`; exit code 12 asserted in the transaction test
- [x] S6.3 Restart as in the architecture - PASS: `CalcInterestTablesRestartIT` (three restart tests)
- [x] S6.4 Outputs in the recorded format; failed run leaves no SYSTRAN - PASS for this step: a failed run leaves no output directory, keeps committed rows (`assertFalse(Files.exists(output))`); the datasets are ticket 07
- [x] S6.5 Run log byte for byte; no account data in the application log - PASS: `sysout_matchesRecorded`; `grep CalcInterestTableRun test-output.txt` shows run id, step, counts only (F1)
- [x] S7.1 A test per rule id, named for it - PASS: INT-R01 `intR01_*`, INT-R07 `intR07_*`, INT-Q04 `intQ04_*`, INT-H02 `intH02_*`, INT-T02 `sysout_matchesRecorded`, `intT02_*` in `RunLogFileTest`
- [x] S7.2 Fixture and edge cases of each rule covered - PASS: i01, i05, i07 (and the other four) through the tables; mixed type codes, page ends inside an account, empty table with synthetic data
- [x] S7.3 Integration tests on PostgreSQL 15 in a container - PASS: `postgres:15` with `db/postgres/01` and `02` (`InterestTables.postgres`)
- [x] S7.4 No assertion against the code's own output - PASS, with one exception the ticket asks for: `restart_fiveAccountsStoppedAtTheFourthEndAsAnUninterruptedRun` compares with an uninterrupted run (no recorded run has five accounts); the two-account restart tests compare with the recorded runs
- [x] S8.1 No edit under `app/`, `fixtures/`, `tools/parity` - PASS: `git diff --stat` in section 1
- [x] S8.2 No golden file re-recorded - PASS
- [x] S8.3 Every new dependency explained in the pull request - PASS: none (section 7)
- [x] S8.4 Frozen shared interfaces unchanged - PASS: FI-1, FI-3, FI-4, FI-5 untouched; FI-6 runs as before (parity-report.md)
- [x] S8.5 Synthetic data only - PASS: fixture accounts and made-up accounts 1 and 2

Behaviours PRESENT in section 4:

- [ ] a Truncation without ROUNDED - N/A: domain, not changed; i02 passes through the tables
- [ ] b High-order digit loss (range check, STOP) - N/A: ticket 08
- [ ] c Intermediate precision of the COMPUTE - N/A: domain, not changed
- [ ] d Zoned overpunch signs - N/A: codec, not changed; i06 passes through the tables
- [ ] e Binary items stay out of data and domain - N/A: not touched
- [ ] f Non-numeric data rejected at the boundary - N/A: the loader's boundary (ticket 05); the tables are typed
- [x] g Fixed width, padding, STRING - PASS: CHAR(n) columns padded by the schema, trailing spaces removed for the domain (`jdbc/Texts`); transaction lines equal the recorded ones
- [x] h Code page and collating order - PASS: `intR01_readsInKeyOrder`
- [ ] i REDEFINES replaced by formatter and function - N/A: not touched
- [x] j FILLER and record length round trip - PASS: `acct_filler` never written (`intR07_updateTouchesOnlyTheThreeAccountColumnsAndNoReplicaTable`); ACCTDATA equal through the tables
- [x] l No state carried between accounts except the counter - PASS: the counter is in the restart state; ids equal after restart
- [x] m Control break and end of file (INT-Q04, INT-T09) - PASS: `intQ04_lastAccountIsRecognisedWithoutAQueryPerAccount`, `intQ04_lastAccountNotUpdated`, `intT09_emptyBalanceTableCompletesAndWritesNothing`
- [ ] o Level-88 result codes replaced by types - N/A: not touched
- [x] p Indexed access through the ports; several cards to the policy - PASS: `JdbcCardLookup` reads through `card_xref_acct_ix` in card-number order; the several-cards stop stays in the calculator
- [ ] q File statuses and abend - N/A: ticket 08
- [x] r PARM, generation, disposition, return code - PASS for the step: a failed run leaves no output directory and keeps committed accounts; exit code 12
- [x] s DISPLAY output in the run log - PASS: `sysout_matchesRecorded`, seven cases through the tables, restart tests
- [ ] t PARM text against the clock - N/A: not touched
- [ ] u Timestamp format and hundredths - N/A: not touched
- [ ] w PARM length checked - N/A: not touched
- [x] x No stale rate after a failed lookup - PASS: `JdbcRateLookup` returns `Optional`; `intR03_i04LogsTheFallbackToTheDefaultGroupFromTheTables`
- [ ] y Typed fields where COBOL mixed text and numbers - N/A: not touched
- [x] z Abend ends the step; stub output not reproduced - PASS: the stop exception fails the step; nothing runs after it
- [ ] aa Dead and empty code not ported - N/A: not touched

## 5. What is wrong or missing in the standards skill

Proposals only; the skill was not edited.

1. **S6.5 against the ticket's "account id in the message".** The standard should say where the account of a failed
   step is recorded, since it may not be in the application log: proposed, the exit description of the step and job
   in the job repository (F1).
2. **Section 6 has no row for a table that cannot be read or written at run time** with JDBC adapters: it names
   `READ_BALANCES`, `UPDATE_ACCOUNT`, `WRITE_TRANSACTION`, which `StopReason` does not hold yet. Until ticket 08 the
   row should say which existing reason to use (F6).
3. **S2.3 lists the restart state** as last account, counter, `seq_no` and run-log length. Two more values are needed
   to restart: the `interest_run.run_id` of the run and the interest not added so far (RC5 spans the restart).
4. **S7.4** should allow the comparison with an uninterrupted run for restart tests: the ticket defines the result
   that way and no recorded run has more than two accounts.

Skills `interest-ticket-build`, `interest-ticket-verify`, `interest-pitfalls`:

1. `interest-ticket-verify` step 8: `parity-report.sh` only runs `make parity-java`, which is the file adapters. A
   ticket proven through the tables has to add its own section by hand (done in parity-report.md).
2. `interest-ticket-build` step 5 says packages `jdbc`, `load`, `export`, `recon` do not exist and that schema
   `carddemo` holds only the tables of `01-transaction-type.sql`: after tickets 02 to 04, `db/postgres/02-interest.sql`
   exists and the compose `db` service applies it on a new volume.
3. `interest-ticket-build` step 8 quotes 60 and 12 tests; the base after ticket 04 has 313 and 20.

## 6. Method

Commands, from the repository root, `JAVA_HOME` on JDK 21:

```sh
git checkout -b demo/05-06-calc-step-jdbc origin/demo/05b-integration
mvn -B -f java/interest/pom.xml verify -Dit.test='CalcInterestTables*IT' -Dtest=RunLogFileTest -Dsurefire.failIfNoSpecifiedTests=false
# evidence, from the committed revision eac5c63
mvn -B -f java/interest/pom.xml clean verify            # test-output.txt
.agents/skills/interest-ticket-verify/parity-report.sh 06-calc-step-jdbc i01-baseline i05-multi-category i07-nontie-rounding
for c in $(ls fixtures/interest); do python3 tools/parity/compare.py interest $c --actual java/interest/target/parity-tables/$c/actual --label java-tables --report work/parity-tables-compare.md; done
# the jar on the tables of the db service (empty replica tables: start line, end line, interest_run CALCULATED 0)
docker run --rm --network container:$(docker compose ps -q db) --user $(id -u):$(id -g) -v "$PWD":/estate -w /estate eclipse-temurin:21-jre \
  java -jar java/interest/target/interest.jar --interest.clock.frozen="2022-07-05 10:00:00.00" --logging.level.root=WARN \
    --interest.calc.adapters=jdbc parmDate=2022071800 runId=t06-$(date +%s) format=fixture inputDir=unused outputDir=work/manual/t06/actual
```

Failures hit:

| Symptom | Cause | Fix |
|---|---|---|
| Every table run failed with `PSQLException: ERROR: syntax error at or near "trancat_acct_id"` | The query was two Java text blocks joined; a text block drops the indentation of its lines, so the `ORDER BY` followed the table name with no space | The second block starts with an empty line (`JdbcBalanceSource.KEY_ORDER`) |
| The restart tests failed on the exit message: it began with the stack trace, not with the account | Spring Batch appends a listener's exit description after the stack trace of the failure, and the job repository cuts the stored message at 2500 characters | The sentence naming the account is also set as the exit message of the job (`CalcInterestTableRun.afterJob`); finding F1 |

No pitfall of `interest-pitfalls` other than P1 (JAVA_HOME) and P6 (`.pyc` files) came up.

## 7. Dependencies

None added. `pom.xml` is unchanged: `JdbcTemplate` comes with `spring-boot-starter-batch`, and the tests use the
Testcontainers modules that are already there.
