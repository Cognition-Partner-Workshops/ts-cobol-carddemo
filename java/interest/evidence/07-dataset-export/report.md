# Report - ticket 07-dataset-export (INT-T07)

Branch `demo/05-07-dataset-export`, base `demo/05b-integration`. Code revision `e88aaf25122fef91a37e35de5365171d4169ddc6`.
Evidence in this directory: `test-output.txt` (`mvn -B -f java/interest/pom.xml clean verify`), `parity-report.md`
(`compare.py` on the exported directories of i01-baseline and i06-negative-balance, then `make parity-java` on all seven cases).
Code: `java/interest/src/main/java/com/carddemo/interest/export/` and `jdbc/FinishedRunReader.java`.
Tests: `java/interest/src/test/java/com/carddemo/interest/export/`, SQL of the finished runs in `src/test/resources/export/`.

## 1. Result

| # | Acceptance line of the ticket | Met | Shown by |
|---|---|---|---|
| 1 | Input: tables holding a finished run of i01-baseline. Result: SYSTRAN.G0001V00.txt, ACCTDATA.VSAM.KSDS.txt and rc.json equal the recorded files; sysout/INTCALC.STEP15.txt, written by the calculation step, is in the same directory | **met** | `DatasetExportIT.intR06_i01BaselineExportEqualsRecorded` (byte for byte, four files and no other); `compare.py` PASS on `target/export-parity/i01-baseline/actual` (parity-report.md, first section). The same for i06-negative-balance: `e2_i06NegativeAmountsAreDSignedAsRecorded`, `compare.py` PASS |
| 2 | Host format: 350-byte and 300-byte records and, decoded with the codec, the same field values | **met** | `DatasetExportIT.host_fixedLengthFilesDecodeToTheRecordedFieldValues` (i01, i06: file length = records x 350 / x 300, every non-FILLER field equal to the recorded file's); `intT04_descriptionTailAndFillerAreSpaces`; `e4_accountFillerIsWrittenBackByteForByte`; `ExportFormatTest` |
| 3 | Order: SYSTRAN in seq_no order; accounts in acct_id order | **met** | `DatasetExportIT.order_transactionsBySeqNoAndAccountsByAcctId` (rows inserted out of order, ids that sort the other way, a second run that must stay out) |
| 4 | Errors: a run whose status is not CALCULATED is refused; an existing output directory is replaced as a whole, never mixed | **met** | `errors_runThatIsNotCalculatedIsRefused` (LOADED, EXPORTED, FAILED, ABANDONED), `errors_unknownRunIsRefused`, `errors_existingOutputDirectoryIsReplacedAsAWhole`, `errors_failedExportLeavesTheOldDirectoryAndTheRunCalculated`, `errors_missingRunLogOfTheCalculationStepIsRefused`; `OutputDirectoryTest` (5 tests) |
| 5 | Database assertions: no row changes except interest_run (status EXPORTED, exported counts and sum equal to RC5) | **met** | `rc5_exportChangesOnlyTheRunRow` (i01: 2 and 47.00; i06: 1 and -3.00; fingerprint of every row of the five other tables unchanged; the changed columns of the run row listed by name), `rc5_otherRunsKeepTheirRow` |

Verification lines: `mvn verify` BUILD SUCCESS, `Tests run: 185` (unit; 168 on the base) and `Tests run: 33` (integration; 12 on the base), 0 failures, 0 skipped.
Export tests run on rows inserted by SQL. `compare.py` PASS on both exported directories.

## 2. What the ticket does not cover (gaps)

- **rc.json mirrors the recorded shape.** It lists six steps under INTCALC; only STEP15 is INTCALC's, the other five are steps of the harness's setup jobs (`tools/parity/record.py`). All six are written as 0. Said in `export/README.md`.
- **That README is `java/interest/src/main/java/com/carddemo/interest/export/README.md`, not `java/interest/README.md`.** The Deliverables line says "say so in the README"; `java/interest/README.md` is outside this ticket's Allowed files (ticket 09 lists it). The sentence should be copied there by the ticket that owns it.
- **The export is not a step of the job.** `ExportDatasetsTasklet` is the body of exportDatasetsStep, tested in `DatasetExportIT.step_*`; declaring the step in `interestCalcJob` is in package `batch` (ticket 06). So `make parity-java` does not run the export, and the exported directories of the parity report come from the integration test.
- **The finished run is inserted by SQL** (as the ticket's Verification says). No calculation step writes tables on this branch (ticket 06), so the export has not run behind a real `calcInterestStep` on tables. The run log in the two parity directories is written by the file-adapter `calcInterestStep` on the same case.
- **Only i01-baseline and i06-negative-balance** have a finished run in the tests. i03 and i04 fail `make parity-java` on this branch as on the base (ticket 04 is not in `demo/05b-integration`).
- **Transfer to the mainframe and the write-back JCL** are excluded by the ticket (strategy CO2).
- **Close failures have no stop reason of their own.** Section 6 of the standards names `CLOSE_<file>` "from the export step"; `StopReason` is in package `domain` (ticket 08). The export uses the existing `WRITE_OUTPUT` (INT-R08) for a refused or failed export, and no legacy `ERROR CLOSING ...` line is written to the run log.
- **Volumes** (G11): rows are streamed with a fetch size of 1000; nothing was measured.

## 3. For the decision register

| # | Finding | Evidence | Suggested owner |
|---|---|---|---|
| 1 | REL-03 as proposed (write back only the accounts the run updated) cannot be done from the tables: no column or table records which accounts a run updated. The export writes every account, which is also what the recorded ACCTDATA file holds. | `db/postgres/02-interest.sql`; `fixtures/interest/i01-baseline/expected` holds account 2002, which the run did not update | mainframe application owner, with the owner of ticket 06 |
| 2 | Nothing connects the job parameter `runId` (text, new per launch) with `interest_run.run_id` (BIGINT identity). The tasklet expects the key under `interestRunId` in the job execution context; that name is this ticket's proposal. | `architecture.md` section 4; `export/ExportDatasetsTasklet.java` | target architect |
| 3 | The export also fills the RC6 columns `written_acctdata_count` and the five `written_acct_*_sum`. The acceptance names RC5 only; the schema comment calls these columns "the exported ACCTDATA image", and only the export knows them. | `db/postgres/02-interest.sql` (comment RC6); `rc5_exportChangesOnlyTheRunRow` | target architect |
| 4 | `tran_filler` is not exported: rule E4 says the FILLER of a new transaction is spaces of the output code page (G05), and a BYTEA written by the calculation cannot know that code page. The column is then never read by anything. | data map, rule E4; `jdbc/FinishedRunReader.java` | data architect |
| 5 | The run log is written by the calculation step into the directory the export then replaces. The export carries `sysout/INTCALC.STEP15.txt` over and refuses to export when it is missing. It cannot tell a run log of an earlier run in the same directory from this run's. | `errors_missingRunLogOfTheCalculationStepIsRefused` | target architect |
| 6 | What rc.json should hold outside the harness (open since the pilot). | `export/RecordedReturnCodes.java` | mainframe operations owner |
| 7 | REL-08: the host format uses cp037 and C/D signs, unconfirmed. | `codec/CodePage.java`, `export/ExportFormat.java` | mainframe data owner |

No golden file looked wrong.

## 4. Standards review checklist (`.agents/skills/interest-conversion-standards/SKILL.md`, section 9)

Paths are under `java/interest/src/main/java/com/carddemo/interest/`; tests under `src/test/java/com/carddemo/interest/export/`.

Standards:

- [x] S1.1 Sources used in order; recorded output decides - **PASS** - `DatasetExportIT.assertExportEqualsRecorded` against `fixtures/interest/<case>/expected`
- [x] S1.2 Rule/code disagreements reproduced as coded and reported - **PASS** - none found in the export; account 2002 leaves not updated, as stored (INT-Q04); section 3
- [x] S2.1 Designed from rules; no method per paragraph - **PASS** - `export/DatasetExporter.java`, `export/OutputDirectory.java`, `export/ExportFormat.java`
- [x] S2.2 Immutable business records; copybook names only at the mapping - **PASS** - `export/ExportTotals.java` is a record; no copybook field name in `export` or `jdbc` (`jdbc/FinishedRunReader.java` maps column n to field n of the codec layout)
- [x] S2.3 Spring Batch shape, transaction and restart boundaries of the architecture - **PASS for the export's unit of work** (staging directory, move, then EXPORTED: `export/DatasetExporter.java`; a restart repeats it: a run that is still CALCULATED is exported again). The step declaration is ticket 06
- [x] S2.4 Types and return values, no flags or result codes - **PASS** - `Optional<String> statusOf`, `ExportFormat`, the stop exception
- [x] S2.5 Constructor injection; no mutable static state - **PASS** - `DatasetExporter`, `FinishedRunReader`, `ExportDatasetsTasklet`
- [x] S3.1 `BigDecimal` with data-map precision and scale; no `double`/`float` - **PASS** - `grep -rnE "double|float" java/interest/src/main/java/com/carddemo/interest/{export,jdbc} java/interest/src/test/java/com/carddemo/interest/export` finds nothing
- [ ] S3.2 Formula: exact product, one division, mode from the policy - **N/A** - the calculation is excluded work
- [x] S3.3 Rounding mode named on every other arithmetic step - **PASS** - the sums in `DatasetExporter.SumOfSignedFields` use `RoundingMode.UNNECESSARY`
- [x] S3.4 Range check before store; overflow goes to the policy - **PASS for the export** - a value that does not fit its field is refused by the codec, never cut (`codec/RecordCodec.java`); the policy switch is the calculation's
- [x] S3.5 Encodings only in `record-codec` - **PASS** - the export calls `RecordCodec.encode`; `e2_i06NegativeAmountsAreDSignedAsRecorded`
- [x] S3.6 Width, padding and FILLER only in codec and schema - **PASS** - `e4_accountFillerIsWrittenBackByteForByte`, `intT04_descriptionTailAndFillerAreSpaces`
- [x] S3.7 Code page and key order each in one place - **PASS** - code page from `codec/CodePage` through `ExportFormat.codePage()`; the two ORDER BY of the export only in `jdbc/FinishedRunReader.java`
- [ ] S3.8 Clock and run parameters injected; `parmDate` is text - **N/A** - the export reads no clock and no parmDate; timestamps leave as stored (`intT05_timestampsAreExportedAsStored`)
- [x] S4.1 Every PRESENT behaviour touched has its test; new ones reported - **PASS** - list below
- [ ] S5.1 Each quirk/open decision in one named place behind its switch, rule id in names - **N/A** - the export implements no policy switch; REL-03 and REL-08 as proposed are in `export/README.md` and section 3
- [x] S5.2 Nothing fixed, tidied or added without a rule id - **PASS, with two things to know** - the refusal for a missing run log and the RC6 columns are additions of this ticket; both are in section 3
- [ ] S6.1 Status and message mapping as in section 6 - **N/A** - ticket 08; no status line or legacy message is written by the export (gap in section 2)
- [x] S6.2 One stop exception, one exit-code constant, no skip or retry - **PASS** - only `InterestRunStoppedException` is thrown; nothing is caught to continue
- [x] S6.3 Restart as in the architecture - **PASS for the export** - the step repeats and changes no business table (`rc5_exportChangesOnlyTheRunRow`); an EXPORTED run is not exported again
- [x] S6.4 Outputs in the recorded format; failed run leaves no SYSTRAN - **PASS** - `intR06_i01BaselineExportEqualsRecorded`, `errors_failedExportLeavesTheOldDirectoryAndTheRunCalculated`
- [x] S6.5 Run log byte for byte; no account data in the application log - **PASS** - the run log is copied unchanged; the one log statement of `DatasetExporter` holds run id, step, format, counts and status; stop messages hold run id and status only
- [x] S7.1 A test per rule id, named for it - **PASS** - `intR06_*`, `intT03_rcJsonMatchesRecorded`, `intT04_*`, `intT05_*`, `e2_*`, `e4_*`, `rc5_*`; tests of acceptance lines with no rule id are named `order_`, `errors_`, `host_`, `step_`, `format_`, `export_`
- [x] S7.2 Fixture and edge cases of each rule covered - **PASS** - i01-baseline and i06-negative-balance (the ticket's cases); synthetic rows for order and errors
- [x] S7.3 Integration tests on PostgreSQL 15 in a container - **PASS** - `DatasetExportIT` (`postgres:15`, init scripts `db/postgres/01-transaction-type.sql` and `02-interest.sql`)
- [x] S7.4 No assertion against the code's own output - **PASS** - recorded files, or values written by hand with the arithmetic in the SQL and test comments
- [x] S8.1 No edit under `app/`, `fixtures/`, `tools/parity` - **PASS** - `git diff --stat origin/demo/05b-integration -- app fixtures tools modernization .agents db docker-compose.yml` prints nothing
- [x] S8.2 No golden file re-recorded - **PASS**
- [x] S8.3 Every new dependency explained in the pull request - **PASS** - none; `pom.xml` is unchanged
- [x] S8.4 Frozen shared interfaces unchanged - **PASS** - FI-1 to FI-6 untouched
- [x] S8.5 Synthetic data only - **PASS** - fixture accounts 2001, 2002, 2008 and made-up 2010-2030

Behaviours PRESENT in section 4:

- [ ] a Truncation without ROUNDED - **N/A** - calculation
- [ ] b High-order digit loss (range check, STOP) - **N/A** - calculation
- [ ] c Intermediate precision of the COMPUTE - **N/A** - calculation
- [x] d Zoned overpunch signs - **PASS** - `e2_i06NegativeAmountsAreDSignedAsRecorded` (fixture), `host_fixedLengthFilesDecodeToTheRecordedFieldValues` (host)
- [ ] e Binary items stay out of data and domain - **N/A** - ticket 03; the export has no binary type
- [ ] f Non-numeric data rejected at the boundary - **N/A for reading**; on the way out a character outside the code page stops the export: `errors_failedExportLeavesTheOldDirectoryAndTheRunCalculated`
- [x] g Fixed width, padding, STRING - **PASS** - `intR06_i01BaselineExportEqualsRecorded`, `intT04_descriptionTailAndFillerAreSpaces`
- [x] h Code page and collating order - **PASS for the code page** (`ExportFormat`, host test); the TCATBALF key order is not the export's
- [ ] i REDEFINES replaced by formatter and function - **N/A** - calculation and ticket 08
- [x] j FILLER and record length round trip - **PASS** - `e4_accountFillerIsWrittenBackByteForByte`; 350 and 300 bytes in the host test
- [ ] l No state carried between accounts except the counter - **N/A** - calculation
- [x] m Control break and end of file (INT-Q04, INT-T09) - **PASS as stored** - the account that ends the input is exported not updated (i01 account 2002, i06 account 2008)
- [ ] o Level-88 result codes replaced by types - **N/A** - no result code in the export
- [ ] p Indexed access through the ports; several cards to the policy - **N/A** - calculation
- [ ] q File statuses and abend - **N/A** - ticket 08
- [x] r PARM, generation, disposition, return code - **PASS** - generation `SYSTRAN.G0001V00`, `RecordedReturnCodesTest.intT03_rcJsonMatchesRecorded` (all seven cases); a failed export leaves no new generation
- [x] s DISPLAY output in the run log - **PASS for carrying it** - the run log of the calculation step is in the exported directory unchanged (`assertExportEqualsRecorded`)
- [ ] t PARM text against the clock - **N/A** - calculation
- [x] u Timestamp format and hundredths - **PASS as stored** - `intT05_timestampsAreExportedAsStored`
- [ ] w PARM length checked - **N/A** - job launch
- [ ] x No stale rate after a failed lookup - **N/A** - calculation
- [x] y Typed fields where COBOL mixed text and numbers - **PASS** - `tran_cat_cd` 5 leaves as `0005` (`intR06_i01BaselineExportEqualsRecorded`)
- [ ] z Abend ends the step; stub output not reproduced - **N/A** - ticket 08; the export's stop is the same exception
- [ ] aa Dead and empty code not ported - **N/A** - nothing of CBACT04C is ported here

## 5. What is wrong or missing in the standards skill (not edited)

1. **Section 6 has no row for the export's own stops**: a run that is not CALCULATED, and a run log that is missing. It names `CLOSE_<file>` "from the export step", but the stop reasons are in `domain`, which the export ticket may not change. Say which reason the export uses, or give ticket 07 the enum.
2. **S6.4 says "export to a temporary directory, move on success" and S6.5 says the calculation writes the run log to `sysout/INTCALC.STEP15.txt`** - the same output directory. Say that the export carries the run log over, and what it does when the run log is missing.
3. **Rule E4 against the column `tran_filler`**: the standard should say whether the export reads the column or writes spaces of the output code page (finding 4).
4. **S7.1 has no naming rule for tests of acceptance lines that have no rule id** (order, errors, host format). Also the work item id of this ticket, INT-T07, is already a technical note in `rules.yaml` about the empty fee paragraph; a test named `intT07_...` would point at the wrong thing.
5. **S2.2 and the codec**: the `FieldSpec` constants of `codec/*Record` are private, so a row mapping outside `codec` can only map by position. Say whether row mappings belong in `codec` or how `jdbc` gets at the fields.

Skill corrections (`interest-ticket-build`, `interest-ticket-verify`):

1. `interest-ticket-build` step 5 is out of date after tickets 02 and 03: it says packages `jdbc`, `export` do not exist and that no table exists beyond `01-transaction-type.sql`; `codec/Field` is now `RecordCodec`/`FieldSpec`. An integration test that needs the tables must copy `db/postgres/02-interest.sql` into the container as a second init script (see `DatasetExportIT`).
2. `interest-ticket-verify` steps 1, 6, 7 and `parity-report.sh` assume the ticket's code runs inside `make parity-java`. For a ticket whose code is not yet a step of the job (07), the script should accept a directory per case to compare (`--actual`), and the skill should say where such a ticket leaves its directories. Here: `java/interest/target/export-parity/<case>/actual`, compared by hand and added to the parity report.
3. `interest-ticket-build` step 2: the `export JAVA_HOME` line lasts for one shell; an agent whose every command starts a new shell hits pitfall P1 once. Say "set it in the environment of every command".

## 6. Method

```sh
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
mvn -q -f java/interest/pom.xml -DskipTests package
mvn -B -f java/interest/pom.xml clean verify            # test-output.txt
for c in i01-baseline i06-negative-balance; do
  python3 tools/parity/compare.py interest $c --actual java/interest/target/export-parity/$c/actual --label java-export --report work/export-compare.md
done
.agents/skills/interest-ticket-verify/parity-report.sh 07-dataset-export i01-baseline i06-negative-balance
git checkout -- tools/parity/__pycache__
```

`interest-ticket-verify` step 6 (error path from the command line) and step 7 (assertions in the `db` service) were not run: the jar does not reach the export (gap 3). Both are proven in `DatasetExportIT` on a `postgres:15` container instead.

Failures hit:

| Symptom | Cause | Fix |
|---|---|---|
| `Fatal error compiling: error: release version 21 not supported` | pitfall P1: a new shell without JAVA_HOME | JAVA_HOME on every command |

No other failure; the tests passed on their first run.

## 7. Dependencies

None added. `pom.xml` is unchanged.
