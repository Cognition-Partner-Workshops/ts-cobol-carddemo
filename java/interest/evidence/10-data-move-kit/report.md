# Ticket 10-data-move-kit - report

Branch `demo/05-10-data-move-kit`, code revision `8a58bb9c54b8c66838c3a560dd8dfd4f3aadd8dc`, base `demo/05b-integration` (`2e05141`).
Evidence in this directory: `test-output.txt`, `parity-report.md`, `recon-report.md` (the seven reconciliation
reports), `sidebyside-report.md`.

## 1. Result

| Acceptance line | Met | Shown by |
|---|---|---|
| Input: a loaded and calculated case. Run: make recon-java CASE=<case>. Result: a report with RC1-RC5, each EQUAL or DIFFERENT with both figures | yes | `recon-report.md`: seven reports, every line with two sides and two values; `ReconReportIT.rc1_` to `rc5_` (one broken figure per check, both figures asserted) |
| for i01-baseline it shows 2 transactions summing 47.00, a balance change of 10.00 and 37.00 not added to the last account | yes | `recon-report.md`, i01-baseline, figures row and the last RC5 line; `ReconReportIT.intQ04_baseline_showsTwoTransactionsOf47_aChangeOf10_and37NotAddedToTheLastAccount` |
| Input: two output directories. Run: make sidebyside-java. Result: field-level differences via tools/parity/compare.py | yes | `sidebyside-report.md`: `LEGACY=` and `TARGET=`; the comparator is used unchanged |
| TRAN-ORIG-TS and TRAN-PROC-TS compared for format only or exactly, as chosen | yes | `sidebyside-report.md`: i01 at another clock is EQUAL with `TS=format` (4 lines accepted) and DIFFERENT with `TS=exact`; the rule is written into the report; `DifferenceClassesTest.intQ03_*`, `SideBySideCommandTest.intQ03_*` |
| every difference line carries a class: defect, decision id or gap id | yes | `sidebyside-report.md`, i02 with HALF_EVEN: INT-Q01, INT-Q03, defect; `SideBySideCommandTest.everyDifferenceLine_carriesAClass`, `DifferenceClassesTest` |
| The profile of TL4 is part of the report | yes | section "Trial-load profile (TL4)" of every report: eight lines; asserted in `intQ04_baseline_...` |
| Errors: a figure that cannot be computed is shown as NOT AVAILABLE, never as equal | yes | `FigureTest`; `ReconReportIT.missingExportedFiles_`, `missingUnloadedFile_`, `unloadedFileWithARejectedRecord_`, `runThatDoesNotExist_`, `databaseThatCannotBeReached_`; `SideBySideCommandTest.legacyDirectoryThatIsNotARecordedRun_`, `comparisonThatDidNotRunOrCheckedNothing_`; `recon-report.md` and `sidebyside-report.md`, error paths |
| Database assertions: the report reads interest_run and the tables; it writes nothing | yes | `ReconReportIT.report_writesNothing_andItsConnectionRefusesAWrite`: the rows of the six tables are the same before and after, no job execution is added, and an UPDATE on the report's connection is refused with SQLSTATE 25006 |
| Verification: build with the db service; report tests with one deliberately broken figure per check | yes | `test-output.txt`: 257 unit, 123 integration, 0 failures; seven `rc<n>_broken...` tests |
| Fixture cases: all seven; the figures equal the reconciliation table of strategy-interest.md | yes | `ReconReportIT.intQ04_everyCase_isEqualWithTheFiguresOfTheReconciliationTable` (13 columns, seven rows typed by hand); `recon-report.md` |
| Final-revision evidence: the seven reports from the last commit | yes | `recon-report.md`, produced from the revision above |
| Deliverables: a list of gaps - RC6 needs a mainframe unload and cannot be shown off-host | yes | section 2; README "Gaps of the kit"; every report lists RC6 as NOT AVAILABLE |

Skill steps that do not fit this ticket: interest-ticket-verify step 4 (no policy switch belongs to the ticket: no
`switch-test-output.txt`); step 6 names no error-path command for ticket 10 - the commands used are in
`recon-report.md` and `sidebyside-report.md` under "Error path"; step 7's query shows the job repository only -
the database assertion of this ticket is the integration test named above. The parity run does not execute the
ticket's code (said under the table of `parity-report.md`).

## 2. What the ticket does not cover

- **RC6** needs a mainframe unload after the write-back and cannot be shown off-host; the report lists it as NOT AVAILABLE and it is not part of the result.
- **Mainframe JCL** for unload, backup and write-back, and any production data, are excluded by the ticket (TL1, TL2, TL5, CO2).
- **The timestamp rule (SS3) is not decided**: `TS=format` and `TS=exact` are both offered and the report states which was used.
- **Host format (REL-08)**: the kit reads the fixture format only, as the job does.
- **`interest_run.finished_at` is empty** on an EXPORTED run and `started_at` is the run clock (ticket 09): the report prints no time of the run.
- **Restart of the three steps (REL-11)** reloads and fails in the export, and **a stopped run leaves no `rc.json`** (tickets 08, 09): the report of such a run shows RC5 as NOT AVAILABLE or DIFFERENT, and the side-by-side comparison shows the missing files as defects; the kit does not repair the run.
- **The tables hold one case**: a report without a new load is about the last case that ran.
- **Classes of differences are proposals** (INT-Q03, INT-Q01, INT-Q02, G05, else `defect`); there is no file of agreed classes.
- **No test with `interest.policy.updateLastAccount=true`**, with a run in status LOADED, or with host data that holds G02 or G03 sign bytes; the counts of G02 and G03 come from the loader's decoder (tested by ticket 05).

## 3. For the decision register

1. **RC1 to RC4 are meant to be checked between the load and the calculation (CO2), but one launch runs the three steps.** `interest.job.steps` is `calc` or `load-calc-export`; there is no launch that stops after `loadExtractsStep`. After the calculation `carddemo.account` no longer holds the unloaded ACCT-CURR-BAL, ACCT-CURR-CYC-CREDIT and ACCT-CURR-CYC-DEBIT (INT-R07), so on a calculated run the kit compares these three with `interest_run.loaded_*` only and leaves them out of RC3, and says so in the report. A go or no-go after RC1-RC4 and before the calculation needs a load-only launch. Evidence: `recon-report.md`, the RC3 note of `account`. Suggested owner: target architect, mainframe operations owner (CO2, CO4).
2. **RC4 compares the first and last record of the file with the lowest and highest key in collation C.** That holds for the fixtures (digits only). An unload in EBCDIC key order with letters and digits in TRANCAT-TYPE-CD or in the group id has another first or last record (G06), and RC4 would then show DIFFERENT on correct data. Suggested owner: mainframe data owner (G06), data engineering lead (RC4).
3. **A run does not name its input.** `interest_run` holds no input directory and the replica tables hold the last load only, so a report without a new load (`RECON_LOAD=no`) relies on the operator giving the files of that run; the kit shows a wrong pairing as DIFFERENT (`recon-report.md`, error path), it cannot name the right files. Suggested owner: data engineering lead (REL-12).
4. **The RC5 identity is the strategy's sentence and holds for INT-Q04 as coded.** With `interest.policy.updateLastAccount=true` the report shows RC5 DIFFERENT; `interest_run` does not record the policy of the run, so the kit cannot tell. Reported by ticket 09 as well. Suggested owner: business owner (INT-Q04).
5. **SS4 classes are proposals from single lines.** The kit gives INT-Q03, INT-Q01, INT-Q02 or G05 only where the two values show it and `defect` to everything else; a difference that follows from another (the balance after a one-cent interest difference, `sidebyside-report.md`) stays a `defect`. Who confirms a class, and where agreed classes are kept between cycles, is not decided; no file of agreed classes is built. Suggested owner: test lead (SS4, REL-04, REL-05).
6. **The reports print keys.** RC4 prints the lowest and highest account id and card number, RC5 the id of the last account, as the reconciliation table of the strategy does. Standard S6.5 forbids account data in the application log; it says nothing about reports. On production data the report is then a document with account ids and card numbers. Suggested owner: data owner.
7. **The comparator can only compare with `fixtures/<chain>/<case>/expected`.** The kit uses `tools/parity/compare.py` unchanged by pointing its `FIXTURES` root at a link to the legacy directory and calling `compare_case`; a legacy directory without `manifest.json` would compare no dataset and is refused as NOT AVAILABLE. A file that exists only in the target directory is not reported by the comparator, and its Markdown report stops at 40 rows (the kit reads the full list). An `--expected <dir>` option would remove the link. Suggested owner: owner of FI-2.
8. **RC6 cannot be shown off-host.** It needs ACCTDATA unloaded again after the write-back; the report lists it as NOT AVAILABLE and leaves it out of the result. Suggested owner: mainframe operations owner.
9. **The kit reads the fixture format only**, like the job (`format=host`, REL-08). A trial load of host files needs the host format in the job parameters first. Suggested owner: mainframe data owner (REL-08).
10. **`interest_run.finished_at` is empty on an EXPORTED run and `started_at` is the run clock** (reported by ticket 09): the report prints status and PARM of the run and no time, so two runs with the same PARM can be told apart by `run_id` only. Suggested owner: data engineering lead.

## 4. Standards review checklist

Standards:

- [x] S1.1 Sources used in order; recorded output decides - PASS: ticket, strategy-interest.md (RC1-RC6, TL3, TL4, SS2-SS4), data map, then the recorded cases; `ReconReportIT.intQ04_everyCase_isEqualWithTheFiguresOfTheReconciliationTable` holds the RC table typed by hand
- [x] S1.2 Rule/code disagreements reproduced as coded and reported - PASS: none in the calculation; the disagreements between the strategy and what can be computed (RC2/RC3 on the three posted columns after the calculation, RC4 against host order) are in "For the decision register" 1 and 2
- [x] S2.1 Designed from rules; no method per paragraph - PASS: one unit per check (`Reconciliation.counts`, `sums`, `roundTrip`, `keys`, `afterTheCalculation`), one per class rule (`DifferenceClasses.intQ03Timestamp`, ...)
- [x] S2.2 Immutable business records; copybook names only at the mapping - FAIL (partly): the records are immutable (`Figure`, `CheckLine`, `ReconReport`, `Difference`), but the kit names copybook fields outside a codec mapping: `Reconciliation.POSTED_FIELDS`, `ExportedFiles` (`TRAN-AMT`, `ACCT-CURR-BAL`) and the field names of the comparator's report in `DifferenceClasses`. A report about fields has to name them; proposal S2.2 below
- [x] S2.3 Spring Batch shape, transaction and restart boundaries of the architecture - N/A: the kit is not a step of `interestCalcJob`; it reads in one read-only transaction (`JdbcReconTables.open`)
- [x] S2.4 Types and return values, no flags or result codes - PASS: `Figure` (value or reason), `Verdict`, `Optional`; the exit codes of the two commands are named constants
- [x] S2.5 Constructor injection; no mutable static state - PASS: no Spring bean, no static state; sources are passed to `Reconciliation.report`
- [x] S3.1 `BigDecimal` with data-map precision and scale; no `double`/`float` - PASS: sums are `BigDecimal` of the scale of the column; `grep -rn "double\|float" recon` finds nothing
- [x] S3.2 Formula: exact product, one division, mode from the policy - N/A: the kit calculates no interest
- [x] S3.3 Rounding mode named on every other arithmetic step - PASS: only `add` and `subtract` of values of one scale, which are exact (`Figure.plus`, `minus`, `ExportedFiles.countAndSum`); no `setScale`, no division
- [x] S3.4 Range check before store; overflow goes to the policy - N/A: the kit stores nothing
- [x] S3.5 Encodings only in `record-codec` - PASS: files are decoded by `load.ExtractDecoder` and `RecordCodec`; RC3 encodes rows with `RecordCodec.encode`; no sign or code-page logic in `recon`
- [x] S3.6 Width, padding and FILLER only in codec and schema - PASS with a note: RC3 places the FILLER bytes of the column at the offset of the layout after encoding, as `export.ExportRecords` does, because `RecordValues` has no way to take a FILLER (reported by ticket 07)
- [x] S3.7 Code page and key order each in one place - PASS: the kit's one ordered query is `JdbcReconTables.rows` (`COLLATE "C"` on text keys); code page from `RecordFormat.fixture()` in `ReconCommand`
- [x] S3.8 Clock and run parameters injected; `parmDate` is text - N/A: the kit reads no clock and prints no time; `parm_date` is printed as text
- [x] S4.1 Every PRESENT behaviour touched has its test; new ones reported - PASS: rows m (INT-Q04, `intQ04_*`) and u (timestamp format, `intQ03_*`) are the two the kit reports on; none reproduced, none new
- [x] S5.1 Each quirk/open decision in one named place behind its switch, rule id in names - PASS: INT-Q04 in `Reconciliation.afterTheCalculation` (the RC5 identity), INT-Q03 in `TimestampRule` and `DifferenceClasses.intQ03Timestamp`; both modes of SS3 are offered by `TS=format|exact`, which is an argument of the kit, not an `interest.policy.*` switch (FI-5 untouched)
- [x] S5.2 Nothing fixed, tidied or added without a rule id - PASS: the kit changes no output of the job; every check and class cites RC1-RC6, TL3, TL4, SS3, SS4, a decision id or a gap id
- [x] S6.1 Status and message mapping as in section 6 - N/A: no legacy event is handled by the kit
- [x] S6.2 One stop exception, one exit-code constant, no skip or retry - N/A for the job; the kit has its own exit codes (0, 1, 2) as constants in `ReconCommand` and `SideBySideCommand` and catches a failed read only to print NOT AVAILABLE with the reason
- [x] S6.3 Restart as in the architecture - N/A: nothing to restart; the kit can be run again at any time
- [x] S6.4 Outputs in the recorded format; failed run leaves no SYSTRAN - N/A: the kit writes Markdown reports under `work/`, no dataset
- [x] S6.5 Run log byte for byte; no account data in the application log - PASS with a note: the kit writes no log line. Its report prints keys (the lowest and highest account id and card number, the id of the last account), as the reconciliation table of the strategy does: "For the decision register" 6
- [x] S7.1 A test per rule id, named for it - PASS: INT-Q03 (`intQ03_*`, six tests), INT-Q04 (`intQ04_*`, two tests); the checks are named `rc1_` to `rc5_`; INT-Q01, INT-Q02 and G05 classes have `intQ01_*`, `intQ02_*`, `g05_*`
- [x] S7.2 Fixture and edge cases of each rule covered - PASS: all seven cases with the RC table; one broken figure per check (seven tests); missing file, rejected record, missing export, unknown run, unreachable database
- [x] S7.3 Integration tests on PostgreSQL 15 in a container - PASS: `ReconReportIT` on `postgres:15` with both init scripts (`ReplicaTables.postgres()`), tables filled by the job itself
- [x] S7.4 No assertion against the code's own output - PASS: figures typed by hand from strategy-interest.md with the arithmetic in comments; differences files of `SideBySideCommandTest` written by hand
- [x] S8.1 No edit under `app/`, `fixtures/`, `tools/parity` - PASS: scope check prints nothing; the `.pyc` files were restored before each commit
- [x] S8.2 No golden file re-recorded - PASS
- [x] S8.3 Every new dependency explained in the pull request - PASS: none; `pom.xml` unchanged
- [x] S8.4 Frozen shared interfaces unchanged - PASS: FI-1 to FI-6 untouched; `git diff` of the Makefile and of the README has no removed line
- [x] S8.5 Synthetic data only - PASS: fixture data only

Behaviours PRESENT in section 4:

- [x] a Truncation without ROUNDED - N/A: not calculated by the kit; a one-cent difference is classed INT-Q01 (`intQ01_amountsOneCentApart_areTheRoundingDecision`)
- [x] b High-order digit loss (range check, STOP) - N/A: outside this ticket
- [x] c Intermediate precision of the COMPUTE - N/A: outside this ticket
- [x] d Zoned overpunch signs - N/A: decoded by the codec; the kit counts G02 and G03 as the loader found them (RC3 note, TL4)
- [x] e Binary items stay out of data and domain - N/A: outside this ticket
- [x] f Non-numeric data rejected at the boundary - PASS: a rejected record makes the file side NOT AVAILABLE and is on the reject list (`unloadedFileWithARejectedRecord_isNotAvailable_andOnTheRejectList`)
- [x] g Fixed width, padding, STRING - N/A: outside this ticket; an unset TRAN-DESC tail is classed G05 (`g05_*`)
- [x] h Code page and collating order - PASS: RC4 orders the table in collation C; what that means on host data is "For the decision register" 2
- [x] i REDEFINES replaced by formatter and function - N/A: outside this ticket
- [x] j FILLER and record length round trip - PASS: RC3 (`rc3_brokenFillerByte_isDifferentAlthoughCountsAndSumsAreEqual`)
- [x] l No state carried between accounts except the counter - N/A: outside this ticket
- [x] m Control break and end of file (INT-Q04, INT-T09) - PASS: the RC5 identity shows the interest of the last account as not added (`intQ04_baseline_showsTwoTransactionsOf47_aChangeOf10_and37NotAddedToTheLastAccount`)
- [x] o Level-88 result codes replaced by types - N/A: outside this ticket
- [x] p Indexed access through the ports; several cards to the policy - N/A: the kit does not use the ports; accounts with several cards are a line of the profile (TL4)
- [x] q File statuses and abend - N/A: outside this ticket
- [x] r PARM, generation, disposition, return code - N/A: outside this ticket; a different PARM is classed INT-Q02
- [x] s DISPLAY output in the run log - N/A: outside this ticket; a SYSOUT difference is a defect
- [x] t PARM text against the clock - N/A: outside this ticket
- [x] u Timestamp format and hundredths - PASS: the format of INT-T05 is what `TS=format` checks (`intQ03_aTimestampOfAnotherFormatIsADefect_underBothRules`)
- [x] w PARM length checked - N/A: outside this ticket
- [x] x No stale rate after a failed lookup - N/A: outside this ticket
- [x] y Typed fields where COBOL mixed text and numbers - N/A: outside this ticket
- [x] z Abend ends the step; stub output not reproduced - N/A: outside this ticket
- [x] aa Dead and empty code not ported - N/A: outside this ticket

## 5. What is wrong or missing in the standards skill

- **S2.2** says a copybook field name appears once, in the codec or row mapping. A reconciliation report has to name the fields it sums and the fields the comparator reports (`ACCT-CURR-BAL`, `TRAN-AMT`, `TRAN-ORIG-TS`), and `load.ExtractProfile` (ticket 05) already does. Proposed addition: "Reporting code (`recon`, the profile of the load) may name a copybook field to say which figure it reports; it reads the value through the codec's `RecordValues` and never from the record image."
- **S6.5** covers the run log and the application log. Proposed addition: "A reconciliation or comparison report is a third output. It may hold keys (account id, card number) where a check is about keys (RC4, the last account of RC5) and holds no other field of an account; where reports are kept is a decision of the data owner."
- **S7.1** names tests `<ruleId>_<whatItProves>` for INT-R, INT-Q and INT-H rules. The checks of the data move have ids too (RC1-RC6, TL3, TL4, SS3, SS4). Proposed addition: "A test of a data-move item is named `<itemId>_<whatItProves>` (`rc3_brokenFillerByte...`)."
- **Section 6** has no row for a tool that is not a step of the job. Proposed sentence under S6.2: "A command beside the job (the reconciliation report, the side-by-side comparison) has its own exit codes as named constants: 0 equal, 1 different or not available, 2 nothing could be compared; it never uses the job's `STOPPED` code."

## 6. Method

Commands, from the repository root with `JAVA_HOME` on JDK 21:

- `mvn -B -f java/interest/pom.xml clean verify` - 257 unit and 123 integration tests, 0 failures (`test-output.txt`). Base: 234 and 101.
- `.agents/skills/interest-ticket-verify/parity-report.sh 10-data-move-kit i01-baseline ... i07-nontie-rounding` - seven of seven PASS, job exit code 0 (`parity-report.md`).
- `make recon-java` - seven reports EQUAL for RC1 to RC5, exit code 0; the error path `RECON_LOAD=no` with the wrong files ends non-zero (`recon-report.md`).
- `make sidebyside-java`, `TS=exact`, and a target run with rounding HALF_EVEN and another clock (`sidebyside-report.md`).

Failures hit:

| Symptom | Cause | Fix |
|---|---|---|
| `ReconReportIT.runThatDoesNotExist_isNotAvailable_neverEqual: expected: <NOT_AVAILABLE> but was: <DIFFERENT>` (22 tests, 1 failure) | with no `interest_run` row the report compared ACCT-CURR-BAL, ACCT-CURR-CYC-CREDIT and ACCT-CURR-CYC-DEBIT of the table with the unloaded file although the calculation had written them | in `Reconciliation`: without a run the three columns are NOT AVAILABLE in RC2 ("the run is not known") and left out of RC3, as after a calculation; the test asserts it |
| `make sidebyside-java` right after `make recon-java`: `NOT AVAILABLE (the target directory does not exist)` for all seven | `make recon-java` writes the run to `work/recon-java/run`, the default target of the comparison is `work/parity-java/<case>/actual` | not a defect: run `make parity-java` first, or give `TARGET=`; the README says which directory is the default |
| ` M tools/parity/__pycache__/copybook.cpython-310.pyc` after `make recon-java` | pitfall P6: the target calls `make parity-java` | `git checkout -- tools/parity/__pycache__ tools/runjcl/__pycache__` before each commit; `make sidebyside-java` itself sets `PYTHONDONTWRITEBYTECODE=1` |

How the kit is started: `InterestApplication` is frozen and is the `Start-Class` of the jar, so the two commands are
second main classes started from the same jar with Spring Boot's `PropertiesLauncher`
(`java -cp java/interest/target/interest.jar -Dloader.main=com.carddemo.interest.recon.ReconCommand org.springframework.boot.loader.launch.PropertiesLauncher ...`),
in the JRE container `make parity-java` uses (the host `java` is not a JDK 21, and the `db` service publishes no port).
No Spring context is started, so nothing creates or changes the schema `batch`.

## 7. Dependencies

None. `java/interest/pom.xml` is unchanged; the kit uses the PostgreSQL driver, `spring-jdbc` and the Spring Boot
loader that are already in the jar.
