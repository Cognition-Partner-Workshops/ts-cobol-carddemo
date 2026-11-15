# Ticket 07-dataset-export - report

Code revision `f4c9161d82e32f343f798f111fffdb983f88eb67`, branch `demo/05-07-dataset-export`, base `demo/05b-integration`.
Evidence in this directory: `test-output.txt` (188 unit, 39 integration tests, BUILD SUCCESS, and the comparator
on the exported directories), `parity-report.md`.

## 1. Result

| Acceptance line | Met | Shown by |
|---|---|---|
| Input: finished run of i01-baseline. SYSTRAN, ACCTDATA image and rc.json equal the recorded files; `sysout/INTCALC.STEP15.txt` of the calculation step is in the same directory | met | `DatasetExportIT.i01Baseline_fixtureFormat_equalsRecordedFiles_andReplacesTheDirectory` (byte comparison with `fixtures/interest/i01-baseline/expected`); `compare.py` PASS on `target/exported/i01-baseline/actual` (end of `test-output.txt`) |
| Fixture case i06-negative-balance | met | `DatasetExportIT.i06NegativeBalance_fixtureFormat_equalsRecordedFiles`, `DatasetExportTest.i06NegativeBalance_rows_giveTheRecordedFiles`; `compare.py` PASS on `target/exported/i06-negative-balance/actual` |
| Host format: 350-byte and 300-byte records; decoded with the codec, the same field values | met | `DatasetExportIT.i01Baseline_hostFormat_fixedLengthRecords_decodeToTheSameFieldValues` (700 and 600 bytes, every field of both layouts decoded with `RecordFormat.host()`) |
| Order: SYSTRAN in seq_no order, accounts in acct_id order | met | `DatasetExportIT.order_isSeqNoAndAcctId_whateverTheInsertOrder` (rows inserted in reverse order); `transactionsOfAnotherRun_areNotExported` |
| Errors: a run that is not CALCULATED is refused | met | `DatasetExportIT.runThatIsNotCalculated_isRefused_andNothingIsWrittenOrChanged` (LOADED, EXPORTED, FAILED, ABANDONED), `runThatDoesNotExist_isRefused`, `DatasetExportTest.statusOtherThanCalculated_isRefused` |
| Errors: an existing output directory is replaced as a whole, never mixed | met | `DatasetExportTest.existingDirectory_isReplacedAsAWhole_andTheRunLogOfTheCalculationIsKept`; every IT case starts with a stale file under `datasets/` and asserts the exact file list |
| Database assertions: no row changes except interest_run (status EXPORTED, exported counts and sum equal to RC5) | met, with the reading below | `DatasetExportIT.export_changesNoRowExceptTheStatusOfTheRun_andTheExportedFiguresEqualRc5`, `runWhoseRowsDoNotAgreeWithRc5_isRefused` |
| Verification: export step under Spring Batch | met for the step alone | `DatasetExportIT.exportDatasetsStep_exportsTheRunOfTheJobExecution_andARepeatIsRefused` (a job built in the test) |

Reading of the database line: FI-4 has no `exported_*` column. `interest_run` holds `written_transaction_count`
and `written_tran_amt_sum` (RC5), which `interest-run-control` records after the calculation (architecture.md
section 2). The export writes only `status = EXPORTED`. It counts the records and sums TRAN-AMT of what it
exports and refuses the export when they differ from the recorded figures or when the figures are null.

## 2. What the ticket does not cover

| Work | Owner |
|---|---|
| `exportDatasetsStep` is a bean; it is not added to `interestCalcJob`. The job is assembled in `batch/InterestJobConfig`, which is not an Allowed file here | ticket 06 (calc step on JDBC) or 09 (parity gate) |
| The step reads the `interest_run.run_id` of the execution from the job execution context, key `interest.run.id` (`ExportDatasetsTasklet.RUN_ID_KEY`, a Long). The step that creates the row has to put it there | ticket 05 or 06 (`interest-run-control`) |
| `InterestJobParameters` still rejects `format=host` (package batch). The export accepts `fixture` and `host` | ticket 06 or 09 |
| Until the calculation runs on JDBC, `FileRunWorkspace` writes the datasets and rc.json itself. When the export step joins the job, the file adapter must stop writing them and leave only the run log in the output directory | ticket 06 |
| `written_*` figures of `interest_run` | ticket 06 (`interest-run-control`) |
| Transfer to the mainframe, write-back JCL, cataloguing the generation (CO2) | operations; excluded by the ticket |
| RC6 and the reconciliation report | ticket 10 |
| Gap named by the ticket: rc.json mirrors the recorded shape, which includes STEP05, STEP10, STEP20, STEP25, STEP30 of the harness's setup jobs. Only STEP15 is a step of INTCALC; the others are written as 0. Stated in the Javadoc of `export/RecordedReturnCodes`; `java/interest/README.md` is not an Allowed file of this ticket, so it is said here instead | coordinator: add the sentence to the README |
| The export is only reached for a run that ended normally, so rc.json always holds 0. An rc.json for a stopped run (12) is not written by the export | ticket 08 (error paths) |

## 3. For the decision register

| Finding | Evidence | Suggested owner |
|---|---|---|
| REL-08 (open, implemented as proposed): host format is the codec's default cp037 with C/D signs. Nothing in the export names a code page | `ExportFormat.recordFormat()`; host IT asserts 0xF2 and sign byte 0xC0 | mainframe data owner |
| REL-03 (open, implemented as proposed): the ACCTDATA image holds every account as a whole record, also those the run did not change | `JdbcExportSource.accounts()` has no filter; i01 image has both accounts | mainframe application owner |
| New: E4 keeps a FILLER as raw bytes with no translation. A FILLER loaded from a fixture-format extract (0x20 spaces) and exported in host format stays 0x20 inside an EBCDIC record; the reverse for a host load exported as fixture. Load and export of one run must use the same format, or E4 must say how a FILLER crosses formats | data-map-interest.md E4; `DatasetExportTest.fillerBytes_areWrittenBackByteForByte`; the host IT inserts 0x40 fillers for that reason | mainframe data owner (with REL-08) |
| New: the ticket says "exported counts and sum equal to RC5" and FI-4 has no column for exported figures. Implemented as a check against `written_*` that refuses the export. If exported figures are to be stored, the schema needs columns | db/postgres/02-interest.sql:42-45 | coordinator / business owner of RC5 |
| New: the job parameter `runId` is text and `interest_run.run_id` is a generated BIGINT; no design file says how one maps to the other | 02-interest.sql:21; `InterestJobParameters` | architecture owner |
| rc.json shape (see section 2): what the target writes outside the harness is open | tools/parity/record.py:273 | mainframe operations owner |

## 4. Standards review checklist

Standards:

- S1.1 Sources used in order; recorded output decides - PASS: ticket, architecture sections 2-6, data map E1-E4, then the recorded files
- S1.2 Rule/code disagreements reproduced as coded and reported - N/A: the export touches no rule that disagrees with the code
- S2.1 Designed from rules; no method per paragraph - PASS: `DatasetExport` follows the component, not a paragraph
- S2.2 Immutable business records; copybook names only at the mapping - PASS: records `RunToExport`, `TransactionRow`, `AccountRow`; copybook names only in `ExportRecords`, column names only in `JdbcExportSource`
- S2.3 Spring Batch shape, transaction and restart boundaries of the architecture - PASS for the step (one tasklet, one transaction, files moved then status set, `allowStartIfComplete`); the job wiring is not in this ticket (section 2)
- S2.4 Types and return values, no flags or result codes - PASS: `Exported` record, `ExportFormat` enum, exception on refusal
- S2.5 Constructor injection; no mutable static state - PASS
- S3.1 `BigDecimal` with data-map precision and scale; no `double`/`float` - PASS: `ResultSet.getBigDecimal`, no floating point
- S3.2 Formula - N/A: no calculation
- S3.3 Rounding mode named on every other arithmetic step - PASS: the sum uses `setScale(2, RoundingMode.UNNECESSARY)`
- S3.4 Range check before store - N/A: nothing numeric is stored; the codec rejects a value that does not fit
- S3.5 Encodings only in `record-codec` - PASS: every byte of a field comes from `RecordCodec`
- S3.6 Width, padding and FILLER only in codec and schema - FAIL (minor): `ExportRecords.withFiller` copies the raw FILLER bytes into the encoded record, because `RecordValues` has no way to set a FILLER and `codec/` is not an Allowed file. Offset and length come from the layout; no width is written in the export. See section 5
- S3.7 Code page and key order each in one place - PASS: code page in the codec; the two orders in the two `ORDER BY` of `JdbcExportSource`
- S3.8 Clock and run parameters injected - N/A: the export uses neither clock nor parmDate
- S4.1 Every PRESENT behaviour touched has its test - PASS: d, g, j, r below
- S5.1 Each quirk/open decision in one named place - PASS: REL-08 in the codec default, REL-03 in `JdbcExportSource.accounts()`; no new switch
- S5.2 Nothing fixed, tidied or added without a rule id - PASS, with one addition to report: the RC5 check refuses an export (strategy RC5)
- S6.1 Status and message mapping as in section 6 - N/A: no file status is mapped; the run log is not written here
- S6.2 One stop exception, one exit-code constant, no skip or retry - PASS: `ExportRefusedException` extends `InterestRunStoppedException` (WRITE_OUTPUT); the step has no skip or retry
- S6.3 Restart as in the architecture - PASS for a failed export (the run stays CALCULATED and the step repeats). A repeat after EXPORTED is refused, as the ticket's error line says
- S6.4 Outputs in the recorded format; failed run leaves no SYSTRAN - PASS: staging directory, one atomic move; refusal tests assert no file
- S6.5 Run log byte for byte; no account data in the application log - PASS: the run log is copied unchanged; the log line holds run id, format, counts and the sum
- S7.1 A test per rule id, named for it - N/A: the ticket implements no INT-R rule; tests are named for the acceptance lines
- S7.2 Fixture and edge cases - PASS: i01, i06, empty generation, other run, four statuses, RC5 mismatch, missing figures, wrong filler length
- S7.3 Integration tests on PostgreSQL 15 in a container - PASS: `DatasetExportIT`, `postgres:15`, schema from `db/postgres/02-interest.sql`
- S7.4 No assertion against the code's own output - PASS: rows written by hand, expected files are the recorded ones. The first hand-written amounts were wrong by a factor of ten and the recorded files caught it (section 6)
- S8.1 No edit under `app/`, `fixtures/`, `tools/parity` - PASS: scope check prints nothing
- S8.2 No golden file re-recorded - PASS
- S8.3 Every new dependency explained - PASS: none
- S8.4 Frozen shared interfaces unchanged - PASS
- S8.5 Synthetic data only - PASS: fixture accounts 2001, 2002, 2008

Behaviours PRESENT in section 4:

- a Truncation without ROUNDED - N/A: calculation
- b High-order digit loss - N/A: calculation
- c Intermediate precision of the COMPUTE - N/A: calculation
- d Zoned overpunch signs - PASS: by the codec; i06 exports `0000000030}` and the host IT checks the sign byte
- e Binary items stay out of data and domain - N/A: none in the two layouts
- f Non-numeric data rejected at the boundary - N/A: the export reads typed columns
- g Fixed width, padding, STRING - PASS: by the codec; recorded files equal
- h Code page and collating order - PASS: code page from the codec; acct_id is numeric, seq_no an integer, so no collation is involved
- i REDEFINES replaced by formatter and function - N/A
- j FILLER and record length round trip - PASS: `fillerBytes_areWrittenBackByteForByte`, 350 and 300 bytes in the host IT (see S3.6)
- l No state carried between accounts - N/A: calculation
- m Control break and end of file - N/A: calculation; the export writes the last account as the table holds it
- o Level-88 result codes replaced by types - N/A
- p Indexed access through the ports - N/A: the export uses no port of the calculation
- q File statuses and abend - N/A: ticket 08
- r PARM, generation, disposition, return code - PASS for generation name G0001V00, disposition (nothing left after a refusal) and rc.json; PARM is N/A
- s DISPLAY output in the run log - N/A: written by the calculation step, kept unchanged
- t PARM text against the clock - N/A
- u Timestamp format and hundredths - N/A: the timestamps are exported as stored
- w PARM length checked - N/A
- x No stale rate after a failed lookup - N/A
- y Typed fields where COBOL mixed text and numbers - N/A
- z Abend ends the step; stub output not reproduced - N/A: ticket 08
- aa Dead and empty code not ported - N/A

## 5. What is wrong or missing in the standards skill

- S3.6 and E4: the codec can decode a FILLER (`RecordValues.filler(int)`) but a caller cannot set one, so no component outside `codec/` can write a FILLER back without placing bytes itself. Proposal: add `RecordValues.withFiller(int index, byte[] raw)` to the codec and let S3.6 name it as the only way.
- S6.3 says restart as in the architecture ("restart repeats the step"), and ticket 07 says an export of a run that is not CALCULATED is refused. After a completed export the two disagree. Proposal: S6.3 should say that a repeat is possible until the run is EXPORTED.
- No standard says where the key of `interest_run` travels between steps. Proposal: name the job execution context key in section 2.
- The checklist has no line for "evidence proves the ticket's own code": for a ticket whose step is not in the job, `make parity-java` passes without running it. Proposal: a line "the parity run goes through the code of the ticket, or the report says what does".

## 6. Method

Commands: `mvn -B -f java/interest/pom.xml clean verify` (JDK 21, Testcontainers `postgres:15`); `make parity-java`
on the base before the change (seven PASS) and through `.agents/skills/interest-ticket-verify/parity-report.sh
07-dataset-export i01-baseline i06-negative-balance` after it (seven PASS);
`python3 tools/parity/compare.py interest <case> --actual java/interest/target/exported/<case>/actual --label java-export`
for i01 and i06; the scope check of interest-ticket-build step 9.

Steps of interest-ticket-verify that do not apply:

- Steps 2-4 and 6 (run the jar on a case, error path from the command line): the jar's job does not contain the export step, so a command-line run cannot show the export or its refusal. Both are shown by `DatasetExportIT` on PostgreSQL 15.
- Step 7 (database assertions in the `db` service): the assertions are on rows the test inserts, in the Testcontainers database; the `db` service holds no finished run until tickets 05 and 06 are merged.

Failures hit:

| Symptom | Cause | Fix |
|---|---|---|
| `mvn -o verify`: surefire-junit-platform not available offline | the plugin's provider was never downloaded on this machine | ran without `-o` |
| `DatasetExportTest.i06NegativeBalance_rows_giveTheRecordedFiles`: expected `0000000030}`, was `0000000003}` | the test's hand-written amounts were a factor of ten too small (S9(09)V99 read as if it had one decimal less); the code was right | corrected the hand-written values in both test classes against strategy-interest.md RC table (47.00, 3410.00, -3.00, -300.00) |

## 7. Dependencies

None added. `pom.xml` is unchanged. `JdbcTemplate`, Spring Batch and Testcontainers were already there.
`export/RecordedReturnCodes` repeats the text of `file/RecordedReturnCodes`, which is package-private in a package
this ticket may not edit; one of the two should go when the file adapter stops writing rc.json (ticket 06).
