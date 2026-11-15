# Ticket 05 extract loader - report

Ticket: `modernization/design/tickets/05-extract-loader.md` (INT-T05). Branch `demo/05-g3-data-flow`, first ticket of group
g3-data-flow. Code revision the evidence was produced from: `93b388b`.

Read this with [test-output.txt](test-output.txt) (search for `reconciliation i0` and `TL4 profile`) and
[parity-report.md](parity-report.md).

## 1. Result

| Acceptance line of the ticket | Result | Where it is shown |
|---|---|---|
| i01-baseline: account 2 rows, card_xref 2, tran_cat_balance 2, disclosure_group 15; SUM(acct_curr_bal) = 3400.00, SUM(tran_cat_bal) = 3400.00, SUM(dis_int_rate) = 120.50; the same figures in interest_run | met; the test reads the tables and the `interest_run` row with SQL (all 14 `loaded_*` columns, status `LOADED`) | `ExtractLoaderIT.i01Baseline_rowsAndSums_inTheTablesAndInInterestRun` |
| Any of the seven cases: figures equal the reconciliation table of strategy-interest.md; re-encoding every row gives the input record (RC3) | met for all seven; table below. RC3: every row read back from PostgreSQL and encoded equals its input line, 0 differences, 0 explained by G02/G03 | `ExtractLoaderIT.everyCase_figuresOfTheStrategyTable_andRoundTrip` (7 rows; expected values typed from the strategy table) |
| Loading twice leaves the same rows (truncate and insert in one transaction per table) | met; another case is loaded first, so the test also shows replace-not-merge | `ExtractLoaderIT.loadingTwice_leavesTheSameRows`; `ExtractLoader.replace` runs in one `TransactionTemplate` call per table |
| Errors: one undecodable record rolls back that table's load, writes the reject list (file, record number, field) and ends the step FAILED | met, and stronger than "that table": all four files are decoded before any table is touched, so no table is loaded at all (see finding F1). Reject list: tab-separated `file record field reason`, one line per broken field | `ExtractLoaderIT.undecodableRecord_rejectList_noTableChanged_runFailed`, `step_endsFailed_onAnUndecodableRecord`; `ExtractDecoderTest` (not numeric, several fields, wrong length, repeated key, host file not on a record boundary) |
| Database assertions: after a failed load the four tables hold what they held before; interest_run.status is FAILED | met for an undecodable record and for a missing file | same two tests; `ExtractLoaderIT.missingFile_runFailed_noTableChanged` |
| Build: `mvn -q -f java/interest/pom.xml verify` with the db service | met: 195 unit tests and 28 integration tests, 0 failures, BUILD SUCCESS (starting branch: 162 and 12) | test-output.txt |
| Tests: integration tests against PostgreSQL 15 | met: `ExtractLoaderIT`, 16 tests on a Testcontainers `postgres:15` initialised with `db/postgres/01-transaction-type.sql` and `02-interest.sql` | test-output.txt |
| Fixture cases: all seven inputs | met | `ExtractLoaderIT.everyCase...`, `ExtractDecoderTest.everyRecordOfEveryCaseDecodes`, `ExtractProfileTest.fixtures_nothingFound` |
| Final-revision evidence: the reconciliation figures per case printed by the test run | met | test-output.txt, lines `reconciliation <case>:` |
| Deliverable: profile results of TL4 on the fixtures (expected: nothing found) | met: nothing found in any case | table below; test-output.txt, lines `TL4 profile <case>:` |

Reconciliation figures per case, as printed by the test run from the tables after the load (RC1, RC2), all equal to the
strategy table:

| Case | account | card_xref | tran_cat_balance | disclosure_group | SUM acct_curr_bal | SUM tran_cat_bal | SUM dis_int_rate | SUM acct_id (account / card_xref / tran_cat_balance) | RC3 rows equal / G02 / G03 / other | RC4 key ends |
|---|---|---|---|---|---|---|---|---|---|---|
| i01-baseline | 2 | 2 | 2 | 15 | 3400.00 | 3400.00 | 120.50 | 4003 / 4003 / 4003 | 21 / 0 / 0 / 0 | equal |
| i02-tie-half-cent | 2 | 2 | 2 | 15 | 2470.00 | 2470.00 | 120.50 | 4007 / 4007 / 4007 | 21 / 0 / 0 / 0 | equal |
| i03-zero-rate-skipped | 1 | 1 | 2 | 15 | 700.00 | 700.00 | 120.50 | 2005 / 2005 / 4010 | 19 / 0 / 0 / 0 | equal |
| i04-default-group-fallback | 1 | 1 | 1 | 15 | 1000.00 | 1000.00 | 120.50 | 2006 / 2006 / 2006 | 18 / 0 / 0 / 0 | equal |
| i05-multi-category | 1 | 1 | 2 | 15 | 1500.00 | 1500.00 | 120.50 | 2007 / 2007 / 4014 | 19 / 0 / 0 / 0 | equal |
| i06-negative-balance | 1 | 1 | 1 | 15 | -300.00 | -300.00 | 120.50 | 2008 / 2008 / 2008 | 18 / 0 / 0 / 0 | equal |
| i07-nontie-rounding | 2 | 2 | 2 | 15 | 1778.10 | 1778.10 | 120.50 | 4019 / 4019 / 4019 | 21 / 0 / 0 / 0 | equal |

The other RC2 sums (credit limit, cash credit limit, cycle credit, cycle debit) are in test-output.txt and in `interest_run`.

TL4 profile of the fixtures (list of gaps): **nothing found** in any of the seven cases - no account with several
cards (G08), no account with letter and digit type codes (G06), no minus zero (G03), no F sign (G02), no FILLER that is not
blank, no inactive account (INT-H04), no balance without a rate or without an account (INT-R08). Type codes seen: `01`
(i03 also `02`). Information only: one balance of i04 takes the DEFAULT rate (INT-R03), which is what that case is for.
`ExtractProfileTest.countsEveryKindOfFinding` shows that each counter does count on synthetic input.

## 2. What the ticket does not cover

| Left out | Why | Owner |
|---|---|---|
| `loadExtractsStep` in the flow of `interestCalcJob` | the job is built in `batch/InterestJobConfig.java`, outside this ticket's Allowed files. The step is a bean named `loadExtractsStep`; `ExtractLoaderIT` launches it in a job of its own. Adding `.start(loadExtractsStep).next(calcInterestStep)` is a change in `batch/**` | ticket 06 (calc-step-jdbc; Allowed files `batch/**`) |
| `format=host` accepted by the job | `InterestJobParameters` (batch/) rejects every format except `fixture`. The loader itself handles both (`ExtractFormat`, tested) | ticket 06 or 08 (`batch/**`) |
| README section naming REL-01, REL-02, REL-08 | `java/interest/README.md` is not in this ticket's Allowed files. The text is in `load/package-info.java`; ticket 10 (this group, README allowed) adds the README section | ticket 10 |
| The calculation, the export, RC5, RC6 | excluded by the ticket | tickets 04/06, 07, 10 |
| Loads of TRANCATG and TRANTYPE | not among the four replica tables | none |

## 3. For the decision register

| Id | Finding | Evidence | Suggested owner |
|---|---|---|---|
| F1 | The ticket says both "one undecodable record rolls back that table's load" and "after a failed load the four tables hold what they held before". With one transaction per table (architecture) a bad record in the third file would leave the first two tables replaced. The loader decodes all four files before it touches a table, which satisfies both lines for every decode error. What remains: a database error or a failed RC1/RC3 check while writing table N rolls back table N only; tables before N stay replaced (status FAILED, so the calculation must not start). | `ExtractLoader.load`; `ExtractLoaderIT.undecodableRecord...` uses a bad record in the third file | architecture owner: confirm, or ask for one transaction over the four tables |
| F2 | A failed load inserts a new `interest_run` row with status FAILED and no figures; nothing else is written. The schema has no column for the reject count or the reason; the reject list is a file (`<outputDir>.load-rejects.txt`, beside the output directory so that a failed run still leaves no output directory). | `LoadRunJournal.failed`, `LoadExtractsStepConfig.rejectList` | REL-01 owner |
| F3 | REL-01, REL-02, REL-08 are OPEN; implemented as proposed: files at the dataset boundary, tables replaced on every run, host files cp037 with C/D signs and no record separator. | `load/package-info.java`, `ExtractFormat` | owners of REL-01/02/08 |
| F4 | RC4 "first and last key equal the file ends" assumes the unload is in key order. The loader reports it (`LoadResult.keyRangeEqualsFileEnds`) and does not fail on it; repeated keys do fail (reject, reason `key already in record n (RC4)`). | `ExtractDecoderTest.repeatedKey_isRejected_rc4` | strategy owner |
| F5 | G02/G03: an F-signed or minus-zero amount does not round-trip byte for byte; the loader counts these per gap and loads; any other difference fails the load. | `RoundTripTest`, `ExtractLoaderIT.g02_g03_areCountedAndTheLoadSucceeds` | data-map owner |
| F6 | `interest_run.run_id` is a database sequence, the job parameter `runId` is text; there is no column to hold the job's `runId`. The step passes the numeric id on in the job execution context (`interest.run.id`). | `LoadExtractsStepConfig.RUN_ID_KEY` | ticket 02 / schema owner |

## 4. Standards review checklist

Standards:

- [x] S1.1 Sources used in order; recorded output decides - PASS: expected figures from strategy-interest.md and the recorded inputs (`ExtractLoaderIT`)
- [x] S1.2 Rule/code disagreements reproduced as coded and reported - PASS: F1 above
- [x] S2.1 Designed from rules; no method per paragraph - PASS: no COBOL paragraph is ported; `load/` is new off-host work (TL3, TL4, RC1-RC4)
- [x] S2.2 Immutable business records; copybook names only at the mapping - PASS: records `LoadFigures`, `RoundTrip`, `ExtractProfile`, `Reject`; copybook names only in `jdbc/ReplicaTable` and the profile/figure mapping
- [x] S2.3 Spring Batch shape, transaction and restart boundaries of the architecture - PASS: tasklet step `loadExtractsStep`, one transaction per table (`ExtractLoader`), rerun replaces; not yet in the job flow (section 2)
- [x] S2.4 Types and return values, no flags or result codes - PASS: `LoadResult`, `ExtractFormat`, `InterestRunStoppedException`
- [x] S2.5 Constructor injection; no mutable static state - PASS: `ExtractLoader`, `ReplicaTableWriter`, `ReplicaTableReader`, `LoadRunJournal`
- [x] S3.1 `BigDecimal` with data-map precision and scale; no `double`/`float` - PASS: `LoadFigures`, `ReplicaTableWriter`
- [ ] S3.2 Formula: exact product, one division, mode from the policy - N/A: no calculation in this ticket
- [x] S3.3 Rounding mode named on every other arithmetic step - PASS: only exact additions (`LoadFigures`); no division or rescale
- [ ] S3.4 Range check before store; overflow goes to the policy - N/A: values come from the codec with the column's precision; no arithmetic result is stored except sums in NUMERIC columns wider than the fields
- [x] S3.5 Encodings only in `record-codec` - PASS: `ExtractDecoder` and `ReplicaTableReader` call `RecordLayout`; `RoundTrip` compares bytes the codec produced
- [x] S3.6 Width, padding and FILLER only in codec and schema - PASS: `ReplicaTable` maps names; padding by `RecordLayout.replace`
- [x] S3.7 Code page and key order each in one place - PASS: `ExtractFormat.codePage()`; key order in `ReplicaTable.keyOrder()` (COLLATE "C")
- [x] S3.8 Clock and run parameters injected; `parmDate` is text - PASS: `Clock` injected into `ExtractLoader`; `parm_date` stored as text
- [ ] S4.1 Every PRESENT behaviour touched has its test; new ones reported - PASS for d, f, g, h, j below; none new
- [x] S5.1 Each quirk/open decision in one named place behind its switch, rule id in names - PASS: G02/G03 in `RoundTrip`; REL-08 in `ExtractFormat.HOST`
- [x] S5.2 Nothing fixed, tidied or added without a rule id - PASS: records are stored as decoded; nothing normalised beyond G02/G03 of the codec
- [x] S6.1 Status and message mapping as in section 6 - PASS: `StopReason.OPEN_INPUT`, `RECORD_REJECTED`, `WRITE_OUTPUT`
- [x] S6.2 One stop exception, one exit-code constant, no skip or retry - PASS: `InterestRunStoppedException` only; no skip, no retry
- [x] S6.3 Restart as in the architecture - PASS: a rerun replaces the tables (`loadingTwice_leavesTheSameRows`)
- [x] S6.4 Outputs in the recorded format; failed run leaves no SYSTRAN - PASS: the step writes no output directory (`step_endsFailed...` asserts it is absent)
- [x] S6.5 Run log byte for byte; no account data in the application log - PASS: the step logs counts and sums only; rejects go to the reject file
- [x] S7.1 A test per rule id, named for it - PASS for the ids this ticket has: `g02_`, `g03_`, `rc4` in test names; no INT-R rule belongs to the ticket
- [x] S7.2 Fixture and edge cases of each rule covered - PASS: seven cases plus synthetic F sign, minus zero, repeated key, wrong length, missing file, host format
- [x] S7.3 Integration tests on PostgreSQL 15 in a container - PASS: `ExtractLoaderIT`
- [x] S7.4 No assertion against the code's own output - PASS: expected values typed from the strategy table and the fixture lines; SQL read-back
- [x] S8.1 No edit under `app/`, `fixtures/`, `tools/parity` - PASS: `git diff --stat origin/demo/05b-integration`
- [x] S8.2 No golden file re-recorded - PASS
- [x] S8.3 Every new dependency explained in the pull request - PASS: none added
- [x] S8.4 Frozen shared interfaces unchanged - PASS: FI-1, FI-3, FI-4, FI-5 untouched
- [x] S8.5 Synthetic data only - PASS: fixture inputs and edits of them

Behaviours PRESENT in section 4:

- [ ] a Truncation without ROUNDED - N/A: no calculation
- [ ] b High-order digit loss (range check, STOP) - N/A: no calculation
- [ ] c Intermediate precision of the COMPUTE - N/A
- [x] d Zoned overpunch signs - PASS: via the codec; `RoundTripTest`, i06 (negative) in `ExtractLoaderIT.everyCase...`
- [ ] e Binary items stay out of data and domain - N/A: none in the four copybooks
- [x] f Non-numeric data rejected at the boundary - PASS: `ExtractDecoderTest.notNumericAmount_isRejectedWithFileRecordAndField`
- [x] g Fixed width, padding, STRING - PASS: RC3 round trip of every row
- [x] h Code page and collating order - PASS: `hostFormat_sameRowsAsTheFixtureFormat_andRoundTripOnHostBytes`; COLLATE "C" in `ReplicaTable.keyOrder`
- [ ] i REDEFINES replaced by formatter and function - N/A
- [x] j FILLER and record length round trip - PASS: FILLER kept as `bytea`; RC3
- [ ] l No state carried between accounts except the counter - N/A
- [ ] m Control break and end of file (INT-Q04, INT-T09) - N/A
- [ ] o Level-88 result codes replaced by types - N/A
- [ ] p Indexed access through the ports; several cards to the policy - N/A: ports untouched; the profile counts accounts with several cards (G08)
- [x] q File statuses and abend - PASS for the load: missing file is `OPEN_INPUT`, step FAILED
- [ ] r PARM, generation, disposition, return code - N/A
- [ ] s DISPLAY output in the run log - N/A
- [ ] t PARM text against the clock - N/A
- [ ] u Timestamp format and hundredths - N/A
- [ ] w PARM length checked - N/A
- [ ] x No stale rate after a failed lookup - N/A
- [ ] y Typed fields where COBOL mixed text and numbers - N/A
- [ ] z Abend ends the step; stub output not reproduced - N/A
- [ ] aa Dead and empty code not ported - N/A

## 5. What is wrong or missing in the skills

Procedure skills (corrections):

| Skill | Step | What is wrong | Corrected text |
|---|---|---|---|
| interest-ticket-verify | 8 (`parity-report.sh <ticket> <case the ticket names>`) | a ticket whose code is not in the job flow (05) names fixture inputs, not parity cases; the script needs at least one case and marks it "acceptance case of the ticket" | "If the ticket's step is not part of `interestCalcJob`, pass `i01-baseline` as a regression case and change its scope cell to `regression only`; the ticket's own figures come from its integration test in `test-output.txt`." |
| interest-ticket-verify | 7 | the expected table list shows only the two `transaction_type` tables; since ticket 02 the schema also has `account`, `card_xref`, `tran_cat_balance`, `disclosure_group`, `interest_transaction`, `interest_run` | list the eight tables of `db/postgres/01-*.sql` and `02-interest.sql` |
| interest-ticket-build | scope check (step 9) | says nothing about a step bean that cannot be added to the job because `batch/**` belongs to another ticket | "A new step is a `@Bean Step` in the ticket's own package; test it by launching a job built in the test. Adding it to `InterestJobConfig` belongs to the ticket that owns `batch/**`; report it in section 2." |
| interest-pitfalls | missing | the datasource sets `search_path` to `batch`: unqualified table names in application SQL resolve to the job repository schema | "P: qualify every application table as `carddemo.<table>`; an unqualified name is looked up in schema `batch`." (not reproduced: the loader was written with qualified names from the start) |
| interest-pitfalls | missing | an integration test needs both init scripts; `CREATE SCHEMA carddemo` is only in `01-transaction-type.sql`, so `02-interest.sql` alone cannot create its tables | "P: copy `db/postgres/01-transaction-type.sql` and `02-interest.sql` into `/docker-entrypoint-initdb.d/` of the Testcontainers `postgres:15`, as `ExtractLoaderIT` does." |

Standards skill (proposals only):

- S2.3: say which ticket adds a new step to the job flow when the step's ticket may not edit `batch/**`.
- S6: add the load to the status table - reject list location, and that a failed load leaves the four tables as they were (all files decoded first).
- S3.7: name `COLLATE "C"` as the key order of text keys in SQL, so a database locale cannot change RC4 or the export order.

## 6. Method

Commands: `mvn -B -f java/interest/pom.xml clean verify` (test-output.txt);
`.agents/skills/interest-ticket-verify/parity-report.sh 05-extract-loader i01-baseline` (parity-report.md).

| Symptom | Cause | Fix |
|---|---|---|
| `loadingTwice_leavesTheSameRows` expected 19 rows but was 21 | the test's own arithmetic (2+2+2+15) | corrected the expected number to 21 |
| `hostFormat_...` compared FILLER X'40' against X'20' | the test's host files are built from the fixture records by the codec, which copies FILLER bytes unchanged (E4), so both loads store X'20' | the test compares the rows directly |
| parity: i03 and i04 FAIL, job exit 12 | pilot calculation, unchanged by this ticket | none here; ticket 04 |

## 7. Dependencies

None added. `JdbcTemplate` and `TransactionTemplate` come with `spring-boot-starter-batch`; Testcontainers was already in `pom.xml`.
