# Ticket 05-extract-loader - report

Ticket: `modernization/design/tickets/05-extract-loader.md` (INT-T05). Branch `demo/05-05-extract-loader`, started from
`demo/05b-integration`. Code revision the evidence was produced from: `e7b1af9ae06d11d542c03c439984c62e54bc7f19`.

Read this with [parity-report.md](parity-report.md) and [test-output.txt](test-output.txt).

## 1. Result

| Acceptance line of the ticket | Result | Where it is shown |
|---|---|---|
| i01-baseline: account 2 rows, card_xref 2, tran_cat_balance 2, disclosure_group 15; SUM(acct_curr_bal) 3400.00, SUM(tran_cat_bal) 3400.00, SUM(dis_int_rate) 120.50; the same figures in interest_run | met | `LoadExtractsStepIT.everyCase_tablesAndInterestRunHoldTheReconciliationTable[i01-baseline]`, `baseline_everyLoadedFigureOfInterestRun`; test-output.txt line `reconciliation i01-baseline` |
| Any of the seven cases: figures equal the reconciliation table of strategy-interest.md; re-encoding every row gives the input record (RC3) | met, seven of seven | the same parameterized test, rows read back from the tables and encoded again; test-output.txt lines `reconciliation <case>` |
| Loading twice leaves the same rows (truncate and insert in one transaction per table) | met | `LoadExtractsStepIT.loadingTwice_leavesTheSameRows` |
| One undecodable record rolls back that table's load, writes the reject list (file, record number, field) and ends the step FAILED | met; no table is touched at all, because the four files are decoded first | `LoadExtractsStepIT.undecodableRecord_changesNoTableWritesTheRejectListAndEndsFailed`, `ExtractDecoderTest.tl3_*` |
| After a failed load the four tables hold what they held before; interest_run.status is FAILED | met for an undecodable record and a missing file; for a database failure in a later table see finding 2 | the same test, `missingFile_stopsTheRunBeforeAnyTableAndEndsFailed`, `failedInsert_rollsBackThatTable` |
| Build: `mvn -q -f java/interest/pom.xml verify`; integration tests against PostgreSQL 15 | met: 195 unit tests and 40 integration tests, 0 failures (base: 176 and 28) | test-output.txt |
| Gaps: profile results of TL4 on the fixtures (expected: nothing found) | met: nothing found | finding 5; test-output.txt lines `TL4 profile` |

Parity per case: all seven PASS, as on `demo/05b-integration` before the change (parity-report.md). The parity run
does not execute the loader: `interestCalcJob` still has the one step `calcInterestStep` (section 2).

Not touched: `app/`, `fixtures/`, `tools/`, `modernization/`, `.agents/`, `db/`, `Makefile`, `docker-compose.yml`,
`pom.xml`, `port/`, `codec/`, `batch/`. No golden file was re-recorded. Nothing was merged.

## 2. What the ticket does not cover

- **`loadExtractsStep` is a bean, not yet a step of `interestCalcJob`.** `InterestJobConfig` is in `batch/`, which
  tickets 06 and 08 own. The job is wired with `.start(loadExtractsStep).next(calcInterestStep)`; the step reads the
  job parameters `inputDir`, `outputDir`, `format`, `parmDate` and puts `interestRunId` into the job execution context.
  Until then the step runs only in `LoadExtractsStepIT` (a test job). Owner: ticket 06 or 09.
- The calculation on the tables (ticket 06), the export (07), the error paths of the job (08), the parity gate through
  PostgreSQL (09), the reconciliation report (10).
- The ticket asks that the loader "names them [REL-01, REL-02, REL-08] in its README section". `java/interest/README.md`
  is not in the ticket's Allowed files (tickets 09 and 10 own it); the decisions are named in the Javadoc of
  `ExtractLoader` and `LoadExtractsStepConfig`. Owner: ticket 09 or 10.
- `format=host` (EBCDIC, fixed length) is mapped by the step but refused by `InterestJobParameters` and has no test here.
- RC3 from the tables is proven in the integration test, not at run time: `RecordValues` has no public way to set a
  FILLER, so main code cannot build a record from a row without going around the codec. At run time RC3 is proven on
  the decoded values before the insert (`ExtractDecoder`). Owner: ticket 07 (export needs the same) or 03.

## 3. For the decision register

1. **interest_run has no link to the job's `runId` parameter.** `run_id` is a generated BIGINT, the job parameter `runId` is text, and no column holds it. The loader inserts the row and hands `run_id` on in the job execution context (key `interestRunId`). Evidence: `db/postgres/02-interest.sql:21`, `LoadExtractsStepConfig.RUN_ID_KEY`. Suggested owner: architect (who creates the row: architecture.md section 2 names a listener `interest-run-control` that no ticket of this wave builds).
2. **"After a failed load the four tables hold what they held before" holds for decode errors only.** All four files are decoded before the first TRUNCATE, so a reject changes nothing. A database failure while table N is replaced rolls back table N, but tables 1..N-1 are already replaced, because architecture.md section 5 makes one table one transaction. Evidence: `LoadExtractsStepIT.failedInsert_rollsBackThatTable`. Suggested owner: architect / data engineering lead (one transaction for the four tables, or accept).
3. **The reject list has no defined place.** The schema has no reject table and architecture.md names no file. The step writes `<outputDir>/load/rejects.tsv` (file, record number, field, reason) and logs each line. Suggested owner: data engineering lead (TL3).
4. **RC4 low/high key assumes the file is in the order of the target's primary key.** On host files with mixed type codes (G06) the first/last record of the file and the lowest/highest key under collation C differ. The loader reports the check per table (`TableLoad.isKeyRangeOfFile`) and does not fail on it; on all seven fixtures it is true. Suggested owner: data owner (G06).
5. **TL4 profile on the seven fixtures: nothing found.** 0 accounts with several cards (G08), type codes digits only, 01 and 02 (G06), 0 minus zero (G03), 0 F-signed (G02), 0 non-blank fillers, 0 accounts not active (INT-H04), 0 balances without a rate and without a DEFAULT rate (INT-R08). Evidence: test-output.txt, lines `TL4 profile`. The fixtures therefore prove none of these gaps on production-like data. Suggested owner: data owner.
6. **REL-01, REL-02, REL-08 stay open.** The loader implements the proposed options: file bridge at the dataset boundary, PostgreSQL as a copy replaced on every load, `format=host` as IBM037 with C and D signs. `format=host` is not accepted by `InterestJobParameters` yet and has no host fixture, so it is untested in the loader.

## 4. Standards review checklist

- [x] S1.1 Sources used in order; recorded output decides - PASS: the fixture inputs decide the figures; `LoadFixtures.reconciliationTable` is the table of strategy-interest.md
- [x] S1.2 Rule/code disagreements reproduced as coded and reported - N/A: the ticket implements no rule (Rules: none) and found no disagreement
- [x] S2.1 Designed from rules; no method per paragraph - PASS: units are named for TL3/RC1-RC4/TL4 (`ExtractDecoder`, `ExtractLoader`, `ExtractProfile`); no COBOL paragraph is ported
- [x] S2.2 Immutable business records; copybook names only at the mapping - PASS: `ExtractFile` derives each column from the layout of the frozen record class; field names appear again only as keys, hash totals and in `ExtractProfile` (reported in section 5)
- [x] S2.3 Spring Batch shape, transaction and restart boundaries of the architecture - PASS for the step: one transaction per table, restart repeats the load (`LoadExtractsStepIT.loadingTwice_leavesTheSameRows`, `failedInsert_rollsBackThatTable`); the step is not yet in `interestCalcJob` (section 2)
- [x] S2.4 Types and return values, no flags or result codes - PASS: `Optional<DecodedExtract>`, `LoadOutcome`, the stop exception
- [x] S2.5 Constructor injection; no mutable static state - PASS: `ExtractLoader`, `ReplicaTableWriter`, `InterestRunLoadJournal`
- [x] S3.1 `BigDecimal` with data-map precision and scale; no `double`/`float` - PASS: amounts are the codec's `BigDecimal`, bound with `setBigDecimal`; no double or float in `load/` or `jdbc/load/`
- [x] S3.2 Formula: exact product, one division, mode from the policy - N/A: no interest formula in the loader
- [x] S3.3 Rounding mode named on every other arithmetic step - PASS: the only arithmetic is the addition of values of one scale (exact, commented in `ExtractDecoder`)
- [x] S3.4 Range check before store; overflow goes to the policy - N/A: values are stored as decoded, each within its picture; the sums go to NUMERIC(18,2)
- [x] S3.5 Encodings only in `record-codec` - PASS: decode and encode go through `RecordCodec`; `ExtractDecoder` only asks `CodePage.signedDigitOf` which sign a differing byte had (G02/G03 count)
- [x] S3.6 Width, padding and FILLER only in codec and schema - PASS: text is bound unpadded to CHAR columns, FILLER bytes go to BYTEA unchanged
- [x] S3.7 Code page and key order each in one place - PASS: code page from `RecordFormat`; key order is the primary key of the schema (`ReplicaTableWriter.edgeKey` orders by it)
- [x] S3.8 Clock and run parameters injected; `parmDate` is text - PASS: the `runClock` bean; `parm_date` is written as the text of the job parameter
- [x] S4.1 Every PRESENT behaviour touched has its test; new ones reported - PASS: d, f, j touched (`ExtractDecoderTest.g02_*`, `g03_*`, `tl3_*`, `LoadExtractsStepIT` RC3); none new
- [x] S5.1 Each quirk/open decision in one named place behind its switch, rule id in names - N/A: the loader implements no QUIRK or OPEN rule and reads no switch; REL-01, REL-02, REL-08 are named in the Javadoc of `ExtractLoader` and `LoadExtractsStepConfig.format`
- [x] S5.2 Nothing fixed, tidied or added without a rule id - PASS: nothing changes an output of the calculation; all seven cases still PASS
- [x] S6.1 Status and message mapping as in section 6 - PASS: unreadable file is `OPEN_INPUT`, undecodable record `RECORD_REJECTED`; the legacy texts belong to the run log of the calculation
- [x] S6.2 One stop exception, one exit-code constant, no skip or retry - PASS: `InterestRunStoppedException` only; the step has no skip and no retry
- [x] S6.3 Restart as in the architecture - PASS: `allowStartIfComplete(true)`, truncate and insert; `loadingTwice_leavesTheSameRows`
- [x] S6.4 Outputs in the recorded format; failed run leaves no SYSTRAN - N/A: the loader writes no dataset; a failed load writes only `load/rejects.tsv`
- [x] S6.5 Run log byte for byte; no account data in the application log - PASS: log lines hold file, record number, field, counts and sums; `ExtractDecoderTest.tl3_aBadDigitIsRejectedWithFileRecordNumberAndField` checks a reject line holds no value
- [x] S7.1 A test per rule id, named for it - N/A: Rules: none; tests are named for the strategy item or gap they prove (`tl3_`, `rc1Rc2_`, `rc4_`, `g02_`, `g03_`, `g06_`, `g08_intH03_`, `intH04_`, `intR08_`)
- [x] S7.2 Fixture and edge cases of each rule covered - PASS: all seven inputs (parameterized), synthetic bad digit, bad sign, long record, duplicate key, F sign, minus zero, missing file, failed insert
- [x] S7.3 Integration tests on PostgreSQL 15 in a container - PASS: `LoadExtractsStepIT`, `postgres:15` with 01 and 02 init scripts
- [x] S7.4 No assertion against the code's own output - PASS: expected figures typed by hand from strategy-interest.md and the fixture inputs, arithmetic shown; RC3 compares with the input files
- [x] S8.1 No edit under `app/`, `fixtures/`, `tools/parity` - PASS: `git diff --name-only origin/demo/05b-integration...HEAD`
- [x] S8.2 No golden file re-recorded - PASS: `make record` not run
- [x] S8.3 Every new dependency explained in the pull request - PASS: none added, pom.xml unchanged
- [x] S8.4 Frozen shared interfaces unchanged - PASS: no change to app/cpy, port/, 02-interest.sql, policy names, Makefile
- [x] S8.5 Synthetic data only - PASS: fixture data and changes to it
- [x] a Truncation without ROUNDED - N/A: calculation
- [x] b High-order digit loss (range check, STOP) - N/A: calculation
- [x] c Intermediate precision of the COMPUTE - N/A: calculation
- [x] d Zoned overpunch signs - PASS: `g02_anFSignedAmountIsLoadedAndCounted`, `g03_minusZeroIsLoadedAsZeroAndCounted`, i06 negative sums
- [x] e Binary items stay out of data and domain - N/A: no binary item in the four layouts
- [x] f Non-numeric data rejected at the boundary - PASS: `tl3_*`, `undecodableRecord_changesNoTableWritesTheRejectListAndEndsFailed`
- [x] g Fixed width, padding, STRING - PASS: RC3 in `everyCase_tablesAndInterestRunHoldTheReconciliationTable`
- [x] h Code page and collating order - PASS: RC4 key range by the primary key (collation C) in `baseline_everyLoadedFigureOfInterestRun`; G06 profiled
- [x] i REDEFINES replaced by formatter and function - N/A: none in the four copybooks
- [x] j FILLER and record length round trip - PASS: RC3 from the tables, FILLER as BYTEA; `tl3_aRecordLongerThanItsLayoutIsRejectedAsARecord`
- [x] l No state carried between accounts except the counter - N/A: calculation
- [x] m Control break and end of file (INT-Q04, INT-T09) - N/A: calculation
- [x] o Level-88 result codes replaced by types - PASS: no result code; see S2.4
- [x] p Indexed access through the ports; several cards to the policy - N/A for access (calculation); every card row is loaded and accounts with several cards are counted (`g08_intH03_countsAccountsWithSeveralCards`)
- [x] q File statuses and abend - PASS for the loader: `missingFile_stopsTheRunBeforeAnyTableAndEndsFailed`
- [x] r PARM, generation, disposition, return code - N/A: export and job
- [x] s DISPLAY output in the run log - N/A: calculation
- [x] t PARM text against the clock - N/A: calculation
- [x] u Timestamp format and hundredths - N/A: calculation
- [x] w PARM length checked - N/A: `InterestJobParameters`, not changed
- [x] x No stale rate after a failed lookup - N/A: calculation
- [x] y Typed fields where COBOL mixed text and numbers - PASS: account ids NUMERIC from long, type codes text
- [x] z Abend ends the step; stub output not reproduced - PASS: the step ends FAILED on the stop exception
- [x] aa Dead and empty code not ported - N/A: no legacy code ported

## 5. What is wrong or missing in the standards skill

- S2.2 says a copybook field name appears once, in the codec/row mapping. A loader also needs the key fields, the
  hash-total field and the fields a profile reads (`ExtractFile`, `ExtractProfile`). Proposal: add "and in the table
  mapping of the loader, which derives the column name from the field name".
- Section 6 has no row for the loader: which status and stop reason a reject list, a failed table replace and a missing
  extract file give. Proposal: add `RECORD_REJECTED` (reject list, interest_run FAILED), `WRITE_OUTPUT` (table replace)
  and `OPEN_INPUT` (extract file) for `loadExtractsStep`.
- S7.1 asks for tests named for a rule id; a ticket with "Rules: none" has only strategy items and gaps. Proposal: allow
  `<strategyItemOrGapId>_<whatItProves>`.

## 6. Method

Commands, on JDK 21 (`JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64`), Docker 29:

- `make parity-java` on the base before any change: seven PASS.
- `mvn -B -f java/interest/pom.xml clean verify` (test-output.txt): 195 + 40, BUILD SUCCESS.
- `.agents/skills/interest-ticket-verify/parity-report.sh 05-extract-loader <the seven cases>` (parity-report.md): exit 0.

Steps of interest-ticket-verify that do not apply:

- Step 6 (error path from the command line) and step 7 (database assertions in the `db` service): the jar's job does
  not contain `loadExtractsStep`, so a command-line run cannot reach the loader. Both are shown in `LoadExtractsStepIT`
  on a Testcontainers `postgres:15` with `01-transaction-type.sql` and `02-interest.sql`.

Failures hit: none. The first build and the first parity run passed.

## 7. Dependencies

None added. `pom.xml` is unchanged; `ResourcelessTransactionManager` and `TransactionTemplate` come with
`spring-boot-starter-batch`.
