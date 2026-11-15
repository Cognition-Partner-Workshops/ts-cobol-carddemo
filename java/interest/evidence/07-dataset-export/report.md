# Ticket 07 dataset export - report

Ticket: `modernization/design/tickets/07-dataset-export.md` (INT-T07). Branch `demo/05-g3-data-flow`, second ticket of group
g3-data-flow. Code revision the evidence was produced from: `c2fe264`.

Read this with [test-output.txt](test-output.txt) (search for `RC5 i0`) and [parity-report.md](parity-report.md) (last section:
`compare.py` on the exported directories).

## 1. Result

| Acceptance line of the ticket | Result | Where it is shown |
|---|---|---|
| Input: tables holding a finished run of i01-baseline. Result: SYSTRAN, ACCTDATA and rc.json equal the files under expected; sysout/INTCALC.STEP15.txt, written by the calculation step, is in the same directory | met, text-equal, for i01-baseline and for the other six cases. The run log the calculation step left in the output directory is carried over unchanged; a directory without one gets none | `DatasetExporterIT.finishedRun_exportsTheRecordedDatasetsAndReturnCodes` (7 cases), `runLogOfTheCalculationStep_staysInTheSameDirectory`, `ExportedReturnCodesTest` |
| Host format: fixed-length files with 350-byte and 300-byte records and, decoded with the codec, the same field values | met for i01-baseline and i06-negative-balance (negative amount, zone D); file sizes are records x 350 and records x 300, no separator | `DatasetExporterIT.hostFormat_fixedLengthRecordsWithTheSameFieldValues` |
| Order: SYSTRAN records in seq_no order; account records in acct_id order | met; the test reverses seq_no against insert order and TRAN-ID order | `DatasetExporterIT.order_transactionsBySeqNo_accountsByAcctId`; `InterestTransactionReader.records` (ORDER BY seq_no, WHERE run_id) and `transactionsOfAnotherRun_areNotExported` |
| Errors: a run whose status is not CALCULATED is refused; an existing output directory is replaced as a whole, never mixed | met; refused for LOADED, EXPORTED, FAILED, ABANDONED and an unknown run, with the directory and every table as before | `runNotCalculated_isRefused_nothingWritten` (4), `unknownRun_isRefused`, `exportingTwice_isRefusedTheSecondTime`, `existingOutputDirectory_isReplacedAsAWhole` |
| Database assertions: the export changes no row except interest_run (status EXPORTED, exported counts and sum equal to RC5) | met; every row of the six tables is compared before and after; of the run's row only `status` and `finished_at` change | `export_changesNoRowExceptInterestRun`, `rc5FiguresTheCalculationLeftEmpty_areTakenFromTheExport`, `rc5Mismatch_isRefused_outputDirectoryUntouched` |
| Build: mvn verify with the db service | met: 204 unit and 53 integration tests, 0 failures, BUILD SUCCESS (after ticket 05: 195 and 28) | test-output.txt |
| Tests: export tests on rows inserted by SQL | met: `DatasetExporterIT.finishedRun` loads the case's input, then INSERTs the recorded SYSTRAN records into `interest_transaction`, UPDATEs the three account columns the calculation writes and sets the run CALCULATED; the calculation step is not involved | `DatasetExporterIT` (25 tests, PostgreSQL 15) |
| Fixture cases: i01-baseline and i06-negative-balance expected files | met, and the other five | as the first line |
| Final-revision evidence: compare.py PASS on the exported directories | met: PASS, 0 differences, for all seven | parity-report.md, last section |
| Deliverable: list of gaps - rc.json mirrors the recorded shape with the setup jobs' step names; say so in the README | gap stated in `ExportedReturnCodes` and `export/package-info.java`; the README sentence is added with ticket 10 (README is not in ticket 07's Allowed files) | section 2 |

RC5 figures per case as exported (printed by the test run), equal to the strategy table:

| Case | transactions | SUM(tran_amt) | account records | SUM(acct_curr_bal) |
|---|---|---|---|---|
| i01-baseline | 2 | 47.00 | 2 | 3410.00 |
| i02-tie-half-cent | 2 | 24.69 | 2 | 2482.35 |
| i03-zero-rate-skipped | 1 | 10.00 | 1 | 700.00 |
| i04-default-group-fallback | 1 | 17.50 | 1 | 1000.00 |
| i05-multi-category | 2 | 17.50 | 1 | 1500.00 |
| i06-negative-balance | 1 | -3.00 | 1 | -300.00 |
| i07-nontie-rounding | 2 | 29.03 | 2 | 1793.52 |

## 2. What the ticket does not cover

| Left out | Why | Owner |
|---|---|---|
| `exportDatasetsStep` in the flow of `interestCalcJob` | the job is built in `batch/InterestJobConfig.java`, outside the Allowed files. The step is a bean; `DatasetExporterIT` launches it in a job of its own. Until it is wired, the job's output files are still written by the pilot's `FileRunWorkspace` | ticket 06 / 08 (`batch/**`) |
| A calculation that leaves a run CALCULATED with rows in `interest_transaction` | the pilot calculates on files; the JDBC calculation is ticket 06. The export is therefore proven on rows inserted by SQL, as the ticket asks | ticket 06 |
| `format=host` accepted by the job | `InterestJobParameters` (batch/) rejects it; the exporter handles both | ticket 06 or 08 |
| README sentence on rc.json and on REL-03 / REL-08 | README is not in the Allowed files | ticket 10 (this group) |
| rc.json for a run that did not end with return code 0 | only a CALCULATED run is exported, and its STEP15 code is 0; a failed run leaves no output (S6.4). `ExportedReturnCodes.json` takes the code as a parameter | none |
| Transfer to the mainframe, write-back JCL, cataloguing SYSTRAN(+1) | excluded by the ticket (operations, CO2) | operations |

## 3. For the decision register

| Id | Finding | Evidence | Suggested owner |
|---|---|---|---|
| F1 | rc.json mirrors the recorded shape: six steps under INTCALC, of which only STEP15 belongs to INTCALC; STEP05/10/20/25/30 are steps of the harness's setup jobs and are written as 0. A consumer must not read them as steps the Java job ran. | `ExportedReturnCodes`, `ExportedReturnCodesTest` | FI-2 owner |
| F2 | The run log is not in the database, so the export cannot write it. The export carries over `sysout/INTCALC.STEP15.txt` when the calculation step left it in the output directory; everything else in an existing directory is dropped. This is the one file that survives "replaced as a whole"; confirm, or have the calculation step write the log somewhere the export reads. | `DatasetExporter.stage`, `runLogOfTheCalculationStep_staysInTheSameDirectory` | architecture owner / ticket 06 |
| F3 | `interest_run` has no "exported" columns; "exported counts and sum equal to RC5" is implemented as: the export refuses when `written_transaction_count`, `written_tran_amt_sum` or `written_acct_curr_bal_sum` differ from the rows it reads, and fills one of these only when the calculation left it NULL. `written_account_count` (accounts updated) cannot be derived by the export and is never written by it. | `ExportRunJournal.exported`, `DatasetExporter.requireRc5` | schema owner (ticket 02), ticket 06 |
| F4 | The export sets `finished_at`; the architecture does not say which step does. | `ExportRunJournal.exported` | ticket 06 / 09 |
| F5 | FILLER is stored and written back as raw bytes (E4, G05). Rows loaded from fixture files (X'20') and exported in host format keep X'20' in FILLER inside an otherwise cp037 record; rows loaded from host files keep their host bytes. Mixing formats between load and export is a test situation only, but nothing refuses it. | `DatasetExporterIT.hostFormat...` (FILLER compared as bytes) | REL-08 owner |
| F6 | The export reads in one read-only REPEATABLE READ transaction, moves the directory into place, then updates `interest_run` in a second transaction. If that update fails, the files are in place and the run stays CALCULATED; a restart repeats the export (architecture: restart repeats the step). | `DatasetExporter.export` | architecture owner |
| F7 | Without `loadExtractsStep` in the same job execution the step exports the newest CALCULATED run. With several CALCULATED runs the older ones stay CALCULATED. | `ExportDatasetsStepConfig`, `exportDatasetsStep_exportsTheNewestCalculatedRun` | ticket 06 / 09 |
| F8 | REL-03 and REL-08 are OPEN; implemented as proposed. | `export/package-info.java` | owners of REL-03/08 |

## 4. Standards review checklist

Standards:

- [x] S1.1 Sources used in order; recorded output decides - PASS: every exported file is compared with the recorded one (`DatasetExporterIT`)
- [x] S1.2 Rule/code disagreements reproduced as coded and reported - PASS: F1 (rc.json shape reproduced and reported)
- [x] S2.1 Designed from rules; no method per paragraph - PASS: `export/` has no counterpart paragraph; it reads tables and encodes
- [x] S2.2 Immutable business records; copybook names only at the mapping - PASS: `ExportResult`, `ExportRunJournal.Run`; copybook field names only in `jdbc/InterestTransactionReader` and the one `ACCT-CURR-BAL` sum
- [x] S2.3 Spring Batch shape, transaction and restart boundaries of the architecture - PASS: tasklet step, whole export one unit, staging directory moved into place, then status EXPORTED; not yet in the job flow (section 2)
- [x] S2.4 Types and return values, no flags or result codes - PASS: `ExportResult`, `InterestRunStoppedException`
- [x] S2.5 Constructor injection; no mutable static state - PASS: `DatasetExporter`, `ExportRunJournal`, `InterestTransactionReader`
- [x] S3.1 BigDecimal with data-map precision and scale; no double/float - PASS: sums and amounts are `BigDecimal` with the field's scale
- [ ] S3.2 Formula - N/A: no calculation
- [x] S3.3 Rounding mode named on every other arithmetic step - PASS: no division; `setScale` to the column's own scale only
- [ ] S3.4 Range check before store - N/A: nothing computed is stored; RC5 figures are compared
- [x] S3.5 Encodings only in record-codec - PASS: `RecordLayout.replace`, `RecordImage`, `CodePage`
- [x] S3.6 Width, padding and FILLER only in codec and schema - PASS: the exporter checks the record length and concatenates
- [x] S3.7 Code page and key order each in one place - PASS: `ExtractFormat.codePage()`; `ReplicaTable.keyOrder()`; `ORDER BY seq_no`
- [x] S3.8 Clock and run parameters injected - PASS: `Clock` injected into `DatasetExporter`
- [x] S4.1 Every PRESENT behaviour touched has its test - PASS for d, g, h, j, r below
- [x] S5.1 Quirks/open decisions in one named place - PASS: REL-08 in `ExtractFormat.HOST`; rc.json shape in `ExportedReturnCodes`
- [x] S5.2 Nothing fixed, tidied or added without a rule id - PASS: rows are written as stored, FILLER included (G05)
- [x] S6.1 Status and message mapping - PASS: `StopReason.WRITE_OUTPUT` for every refusal and write error
- [x] S6.2 One stop exception, no skip or retry - PASS
- [x] S6.3 Restart as in the architecture - PASS: a repeated export of a CALCULATED run replaces the directory; no business table changes
- [x] S6.4 Outputs in the recorded format; failed run leaves no SYSTRAN - PASS: refused and failed exports leave the directory as it was and no staging directory (`runNotCalculated...`, `rc5Mismatch...`, `exportDatasetsStep_withoutACalculatedRun_fails`)
- [x] S6.5 Run log byte for byte; no account data in the application log - PASS: the run log is copied, not rewritten; the step logs the run id and two counts
- [x] S7.1 A test per rule id - PASS for RC5 (`i01Baseline_rc5Figures`, `rc5Mismatch_...`); INT-R06/T03/T04/T05 values are in the recorded SYSTRAN records the export must equal
- [x] S7.2 Fixture and edge cases covered - PASS: seven cases, both formats, reversed order, two runs, four refused statuses
- [x] S7.3 Integration tests on PostgreSQL 15 in a container - PASS: `DatasetExporterIT`
- [x] S7.4 No assertion against the code's own output - PASS: expected values are the recorded files and typed figures; table rows read by SQL
- [x] S8.1 No edit under app/, fixtures/, tools/parity - PASS
- [x] S8.2 No golden file re-recorded - PASS
- [x] S8.3 Every new dependency explained - PASS: none
- [x] S8.4 Frozen shared interfaces unchanged - PASS: FI-1 to FI-4 untouched
- [x] S8.5 Synthetic data only - PASS

Behaviours PRESENT in section 4:

- [ ] a Truncation without ROUNDED - N/A
- [ ] b High-order digit loss - N/A
- [ ] c Intermediate precision - N/A
- [x] d Zoned overpunch signs - PASS: i06 exported in both formats (`finishedRun_...`, `hostFormat_...`)
- [ ] e Binary items - N/A
- [ ] f Non-numeric data rejected at the boundary - N/A: the export reads typed columns
- [x] g Fixed width, padding, STRING - PASS: exported lines equal the recorded ones; 350/300-byte host records
- [x] h Code page and collating order - PASS: host files decode with cp037 to the same values; account order by numeric key
- [ ] i REDEFINES - N/A
- [x] j FILLER and record length round trip - PASS: FILLER written from `bytea` (F5)
- [ ] l State between accounts - N/A
- [ ] m Control break and end of file - N/A: the export writes what the calculation stored (the last account's balance is unchanged in the recorded file and in the export)
- [ ] o Level-88 result codes - N/A
- [ ] p Indexed access through the ports - N/A
- [x] q File statuses and abend - PASS for the export: a write error is `WRITE_OUTPUT`, step FAILED
- [x] r PARM, generation, disposition, return code - PASS: generation name `SYSTRAN.G0001V00` and rc.json as recorded
- [ ] s DISPLAY output in the run log - N/A: not written by the export (F2)
- [ ] t, u, w PARM and timestamps - N/A: TRAN-ORIG-TS/PROC-TS are exported as stored
- [ ] x, y, z, aa - N/A

## 5. What is wrong or missing in the skills

Procedure skills (corrections):

| Skill | Step | What is wrong | Corrected text |
|---|---|---|---|
| interest-ticket-verify | 8 | `parity-report.sh` only compares `work/parity-java/<case>/actual`, the output of the job. A ticket whose acceptance is "compare.py PASS on the exported directories" (07) has no place for that in the generated report | "If the ticket's evidence is a directory the job does not write, run `compare.py interest <case> --actual <dir> --label <name>` on it yourself and append the command and its output as a last section of parity-report.md." |
| interest-ticket-verify | 8 | says the evidence comes "from the committed revision" but `test-output.txt` is from `clean verify`, which deletes `target/`; a later step that needs files the tests wrote must run after it and before the next `clean` | add: "Files the tests leave under `java/interest/target/` are those of the last `clean verify`; use them before running Maven again." |
| interest-ticket-build | scope check (step 9) | a ticket deliverable can name a file outside its Allowed files (07: "say so in the README", README not allowed) | "When a Deliverable names a file outside the Allowed files, put the text in the ticket's package-info and report.md section 2 and name the ticket that may edit the file." |
| interest-pitfalls | missing | `file/RecordedReturnCodes` is package-private: another package cannot reuse the rc.json writer | "P: rc.json outside `file/` needs its own writer (`export/ExportedReturnCodes`); test it against every case's recorded rc.json." (not reproduced: seen while reading) |

Standards skill (proposals only):

- S6.4: say who owns `sysout/INTCALC.STEP15.txt` once `exportDatasetsStep` replaces the output directory (F2).
- S6.3: name the order "files moved into place, then status EXPORTED" and what a restart does when the status update failed (F6).
- S3.6: state that FILLER bytes are written back as stored, also when the export format differs from the load format (F5).

## 6. Method

Commands: `mvn -B -f java/interest/pom.xml clean verify` (test-output.txt);
`.agents/skills/interest-ticket-verify/parity-report.sh 07-dataset-export i01-baseline i06-negative-balance`; the `compare.py`
loop on `java/interest/target/exported` (parity-report.md, last section).

| Symptom | Cause | Fix |
|---|---|---|
| `order_...`: duplicate key on `interest_transaction_pkey` | the test's own renumbering produced the same seq_no twice | renumber in two steps (+10, then 13 - seq_no) |
| `hostFormat_...`: "not readable as IBM037" | the test asserted that every byte is a printable cp037 character; FILLER holds the X'20' bytes of the fixture load (F5) | the test checks the zone of the numeric key and the absence of a separator instead; field values were already compared |
| parity of the job: i03 and i04 FAIL, exit 12 | pilot calculation, unchanged | none here; ticket 04 |

## 7. Dependencies

None added.
