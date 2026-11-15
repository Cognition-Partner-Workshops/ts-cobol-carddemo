# Ticket 10 data move kit - report

Ticket: `modernization/design/tickets/10-data-move-kit.md` (INT-T10). Branch `demo/05-g3-data-flow`, third ticket of group
g3-data-flow. Code revision the evidence was produced from: `fe9541f`.

Evidence in this folder:

| File | What it is |
|---|---|
| [test-output.txt](test-output.txt) | `mvn clean verify`; search for `recon i0` |
| [reports/](reports/) | the seven reconciliation reports of a loaded, calculated and exported case, written by `ReconReporterIT` during that Maven run from PostgreSQL 15 (final-revision evidence of the ticket) |
| [make-targets.txt](make-targets.txt) | the two make targets run on this branch for every case, with their exit codes |
| [make-recon-java/](make-recon-java/) | the seven reports `make recon-java CASE=<case> LOAD=1` wrote |
| [make-sidebyside-java/](make-sidebyside-java/) | the reports `make sidebyside-java` wrote, both timestamp rules, and a comparison with differences made on purpose (`i01-demo-*.md`) |
| [parity-report.md](parity-report.md) | regression of the job: unchanged |

## 1. Result

| Acceptance line of the ticket | Result | Where it is shown |
|---|---|---|
| Input: a loaded and calculated case. Run: make recon-java CASE=<case>. Result: a report with RC1-RC5, each EQUAL or DIFFERENT with both figures; for i01-baseline 2 transactions summing 47.00, a balance change of 10.00 and 37.00 not added to the last account | met for the report: for a loaded, calculated and exported case every line of RC1-RC5 is EQUAL with both figures (36 lines), RC6 NOT AVAILABLE; i01-baseline reads "2 transaction(s) summing 47.00; ... a balance change of 10.00; interest of the last account 37.00, not added to the last account (INT-Q04)". **Limit on this branch:** the job does not load or calculate on the tables yet (section 2), so `make recon-java CASE=i01-baseline LOAD=1` shows RC1-RC4 EQUAL and the RC5 equation EQUAL from the files (same sentence, 47.00 = 10.00 + 37.00), and the five RC5 lines that need `interest_transaction` or the calculation's figures as NOT AVAILABLE | reports/i01-baseline.md; `ReconReporterIT.everyCase_reportFromTheDatabase_figuresOfTheStrategyTable`; make-recon-java/i01-baseline.md |
| Input: two output directories. Run: make sidebyside-java. Result: field-level differences via tools/parity/compare.py, TRAN-ORIG-TS and TRAN-PROC-TS compared for format only or exactly, as chosen; every difference line carries a class | met: `TS=format` (default) or `TS=exact`; the rule used is written into the report; classes `INT-Q01`, `INT-Q02`, `INT-Q03`, `G05`, `defect` | make-sidebyside-java/i01-demo-exact.md (4 differences: INT-Q01, INT-Q03 twice, defect) and i01-demo-format.md (2: the timestamps are accepted); `SideBySideTest` |
| The profile of TL4 is part of the report | met: section "Profile of the unloaded files (TL4)" with the seven counts; nothing found in the seven cases | every report; `ReconReportTest.profile_countsWhatTheGapsAskFor` |
| Errors: a figure that cannot be computed is shown as NOT AVAILABLE, never as equal | met: no run, unknown run, FAILED load, run not calculated, missing export, unreadable unloaded file, unreadable database - each NOT AVAILABLE with the reason | `ReconReportTest.noRun_...`, `failedRun_...`, `databaseNotReadable_...`, `noExportedFiles_...`, `unreadableUnloadedFile_...`, `figure_missingEqualsNothing_...`; `ReconReporterIT.unknownRun_...`, `failedLoad_...` |
| Database assertions: the report reads interest_run and the tables; it writes nothing | met: every row of the six tables compared before and after the report; the queries run in one read-only REPEATABLE READ transaction | `ReconReporterIT.everyCase_...` (assertion "the report writes nothing"); `ReconDatabase` |
| Build: mvn verify with the db service | met: 254 unit and 64 integration tests, 0 failures, BUILD SUCCESS (after ticket 07: 204 and 53) | test-output.txt |
| Tests: report tests with one deliberately broken figure per check | met: RC1 count, RC2 sum, RC3 row, RC4 key ends and repeated key, RC5 count, sum, recorded figure and the equation | `ReconReportTest.rc1_...` to `rc5_...`; `ReconReporterIT.brokenFigures_oneForEachCheck_areDifferentWithBothFigures` (by SQL) |
| Fixture cases: all seven; the figures equal the reconciliation table of strategy-interest.md | met: expected values typed from the strategy table | `ReconReporterIT.everyCase_...`, `ReconReportTest.calculatedCase_...` |
| Final-revision evidence: the seven reports from the last commit | met | reports/ |
| Deliverable: gaps - RC6 needs a mainframe unload and cannot be shown off-host; say so | met: RC6 is a line of every report, NOT AVAILABLE with that reason; README says so | every report; `java/interest/README.md` |

Figures of the seven reports (reports/), equal to the strategy table:

| Case | EQUAL / DIFFERENT / NOT AVAILABLE | Transactions | Sum TRAN-AMT | Balance before | after | Change | Interest of last account (not added) |
|---|---|---|---|---|---|---|---|
| i01-baseline | 36 / 0 / 1 | 2 | 47.00 | 3400.00 | 3410.00 | 10.00 | 37.00 |
| i02-tie-half-cent | 36 / 0 / 1 | 2 | 24.69 | 2470.00 | 2482.35 | 12.35 | 12.34 |
| i03-zero-rate-skipped | 36 / 0 / 1 | 1 | 10.00 | 700.00 | 700.00 | 0.00 | 10.00 |
| i04-default-group-fallback | 36 / 0 / 1 | 1 | 17.50 | 1000.00 | 1000.00 | 0.00 | 17.50 |
| i05-multi-category | 36 / 0 / 1 | 2 | 17.50 | 1500.00 | 1500.00 | 0.00 | 17.50 |
| i06-negative-balance | 36 / 0 / 1 | 1 | -3.00 | -300.00 | -300.00 | 0.00 | -3.00 |
| i07-nontie-rounding | 36 / 0 / 1 | 2 | 29.03 | 1778.10 | 1793.52 | 15.42 | 13.61 |

The make targets on this branch (make-targets.txt): `make recon-java CASE=<case> LOAD=1` ends with exit 0 for all seven: 31
EQUAL and 6 NOT AVAILABLE (the five RC5 lines above and RC6); for i03 and i04 30 and 7, because the pilot job stops on these
cases and leaves no output directory, so the RC5 equation is NOT AVAILABLE too. `make sidebyside-java CASE=<case>` shows 0
differences under both timestamp rules for the five cases the job completes, and stops with "not a directory" for i03 and i04
for the same reason.

## 2. What the ticket does not cover

| Left out | Why | Owner |
|---|---|---|
| A case loaded and calculated on the tables by the job | `interestCalcJob` still runs `calcInterestStep` on files; the job flow and the JDBC calculation are in `batch/`. The kit therefore has `LOAD=1` (trial load through ticket 05's loader) and reports the calculation's part as NOT AVAILABLE; the full report is proven in `ReconReporterIT` on rows put in by SQL | tickets 06, 08 |
| RC6 | needs ACCTDATA unloaded again after the write-back on the mainframe | mainframe operations |
| Mainframe JCL for unload, backup, write-back; production data; deciding the timestamp rule | excluded by the ticket | operations; test lead (SS3) |
| Side by side of host-format (EBCDIC) directories | the kit calls `compare.py` with its default encoding and prepares text files; `compare.py --encoding ebcdic` exists but the fixture layout is line-based | REL-08 owner, when a host unload exists |
| Class INT-Q04 in the side-by-side report | the kit sees two output directories, not the policy switches of the target; a difference on the last account's balance is listed as defect for a person to reclassify | test lead |
| SYSOUT and rc.json differences by class | every such difference is a defect | none |

## 3. For the decision register

| Id | Finding | Evidence | Suggested owner |
|---|---|---|---|
| F1 | `tools/parity/compare.py` always reads the legacy side from `fixtures/<chain>/<case>/expected`; it has no option for another directory. To compare two arbitrary directories without changing the comparator, `make sidebyside-java` copies the legacy directory to `work/sidebyside-java/<case>/legacy/interest/<case>/expected` (adding a manifest.json when it has none) and runs the comparator's own `main()` with its module variable `FIXTURES` pointed there. The comparison code is untouched; if this is not acceptable, the comparator needs an `--expected` option (FI-2 owner). | `Makefile` target `sidebyside-java`; `SideBySide.prepare` | FI-2 owner |
| F2 | Format rule of SS3: the kit hands the comparator a copy of the target in which each well-formed timestamp is replaced by the legacy record's, so only malformed ones are reported. The target directory itself is not changed. "Well formed" is `YYYY-MM-DD-HH.MM.SS.hh0000` (G10): a timestamp with non-zero last four digits is a defect. | `TimestampRule`, `SideBySideTest.formatRule_...` | test lead (SS3) |
| F3 | Classification rules (SS4) are the kit's proposal: one cent on TRAN-AMT = INT-Q01; same counter and other prefix on TRAN-ID = INT-Q02; well-formed timestamps under `exact` = INT-Q03; same TRAN-DESC text with other trailing bytes = G05; everything else defect. They are heuristics on the two values only. | `SideBySide.classify`, `SideBySideTest.everyDifference_getsItsClass` | test lead (SS4), REL-05 owner |
| F4 | RC1/RC2 compare the files with what the load recorded in `interest_run`, because after the calculation the account table no longer holds the loaded balances; RC1 also compares with the table's row count. RC3 leaves out ACCT-CURR-BAL, ACCT-CURR-CYC-CREDIT and ACCT-CURR-CYC-DEBIT of the account table once the run is CALCULATED or EXPORTED, and says so on the line. | `ReconReport.rc3`, `ReconReportTest.rc3_calculatedRun_...` | strategy owner (RC3 after the calculation) |
| F5 | "Interest of the last account" is the sum of TRAN-AMT of the exported transactions whose card belongs (CARDXREF) to the account of the last record of the unloaded TCATBALF. With INT-Q04 switched to update the last account, the RC5 equation of the strategy is DIFFERENT by design; the report shows it and does not decide. | `ReconReport.lastAccountInterest`, `ReconReportTest.rc5_lastAccountUpdatedInTheExport_...` | INT-Q04 owner |
| F6 | `interest_run` has no column naming the case or the input directory, so the report takes the newest run unless `RUN=` is given; it cannot tell whether that run belongs to the case named. | `ReconDatabase.read` | schema owner |
| F7 | The profile of TL4 exists twice: `load/ExtractProfile` (ticket 05, its factory is package-private) and `recon/TrialLoadProfile`, because ticket 10 may not edit `load/`. Same counts; one should go when a ticket owns both. | `TrialLoadProfile` | engineering lead |
| F8 | The reports show keys by the last four characters of each key field, and no other value of a record except the distinct type codes. | `ReconReport.shown` | data owner |
| F9 | REL-04 and REL-05 are OPEN; the kit states both in the side-by-side report and decides neither. Exit code 1 means "a DIFFERENT line" or "a defect", not "not ready for cutover". | `SideBySide.markdown` | business owner |
| F10 | `make recon-java` starts the application context, whose connection pool runs `CREATE SCHEMA IF NOT EXISTS batch` (application.yml, not in this ticket's files). The kit turns the job and the job-repository initialisation off; the report's own statements are SELECTs. | `DataMoveKit.context` | engineering lead |

## 4. Standards review checklist

Standards:

- [x] S1.1 Sources used in order; recorded output decides - PASS: expected figures typed from strategy-interest.md; files read with the codec
- [x] S1.2 Rule/code disagreements reproduced as coded and reported - PASS: F4, F5
- [x] S2.1 Designed from rules; no method per paragraph - PASS: no COBOL counterpart
- [x] S2.2 Immutable business records; copybook names only at the mapping - PASS: records `Figure`, `ReconLine`, `RunRow`, `DatabaseSide`, `ReconReport`, `TrialLoadProfile`; copybook field names in `ReconReport` and `TrialLoadProfile` are the names RC2 and TL4 are written in
- [ ] S2.3 Spring Batch shape - N/A: the kit is a command, not a step
- [x] S2.4 Types and return values, no flags or result codes - PASS: `Verdict`, `TimestampRule`, `Figure`; the process exit code is the only code
- [x] S2.5 Constructor injection; no mutable static state - PASS: `ReconReporter`, `ReconDatabase`; `SideBySide` and `DataMoveKit` are stateless
- [x] S3.1 BigDecimal with data-map precision and scale - PASS: sums with the field's scale (`RecordFiles.total`); amounts compared by value
- [ ] S3.2 Formula - N/A
- [x] S3.3 Rounding mode named on every other arithmetic step - PASS: additions and subtractions only
- [ ] S3.4 Range check before store - N/A: nothing is stored
- [x] S3.5 Encodings only in record-codec - PASS: `RecordFiles` decodes through `RecordLayout`; RC3 compares bytes the codec produced
- [x] S3.6 Width, padding and FILLER only in codec and schema - PASS, with one exception to note: `SideBySide.prepare` cuts the two timestamp fields out of a line by the offsets of `CardDemoLayouts.TRANSACTION`
- [x] S3.7 Code page and key order each in one place - PASS: `ExtractFormat.codePage()`, `ReplicaTableReader`
- [ ] S3.8 Clock and run parameters injected - N/A: the kit uses no clock
- [x] S4.1 Every PRESENT behaviour touched has its test - PASS for d, g, j, m, u below
- [x] S5.1 Quirks/open decisions in one named place - PASS: SS3 in `TimestampRule`; SS4 classes in `SideBySide.classify`; INT-Q04 in the RC5 equation
- [x] S5.2 Nothing fixed, tidied or added without a rule id - PASS: the kit reports and changes no data
- [ ] S6.1 Status and message mapping - N/A: no step
- [ ] S6.2 One stop exception - N/A: an unreadable side is NOT AVAILABLE, not a stop
- [ ] S6.3 Restart - N/A: reports can be repeated
- [ ] S6.4 Outputs in the recorded format - N/A: the kit writes reports under `work/`
- [x] S6.5 No account data in the application log - PASS: the kit prints counts; reports show key ends only (F8)
- [x] S7.1 A test per rule id, named for it - PASS: `rc1_` to `rc5_`, `formatRule_`, `exactRule_` in test names
- [x] S7.2 Fixture and edge cases covered - PASS: seven cases; broken figure per check; every NOT AVAILABLE cause
- [x] S7.3 Integration tests on PostgreSQL 15 in a container - PASS: `ReconReporterIT`
- [x] S7.4 No assertion against the code's own output - PASS: expected values typed from the strategy table; broken figures set by SQL
- [x] S8.1 No edit under app/, fixtures/, tools/parity - PASS (F1: the comparator is called, not changed)
- [x] S8.2 No golden file re-recorded - PASS
- [x] S8.3 Every new dependency explained - PASS: none
- [x] S8.4 Frozen shared interfaces unchanged - PASS: FI-1 to FI-6; `parity-java` in the Makefile is untouched
- [x] S8.5 Synthetic data only - PASS

Behaviours PRESENT in section 4:

- [ ] a, b, c - N/A: no calculation
- [x] d Zoned overpunch signs - PASS: i06 (negative sums) in both report tests
- [ ] e, f - N/A
- [x] g Fixed width, padding - PASS: RC3
- [ ] h Code page and collating order - N/A beyond the codec; host format is accepted by `recon` (`FORMAT=host`) and not exercised off-host
- [ ] i - N/A
- [x] j FILLER and record length round trip - PASS: RC3; non-blank fillers counted in the profile
- [ ] l - N/A
- [x] m Control break and end of file (INT-Q04) - PASS: the RC5 equation and its broken variant
- [ ] o, p, q, r, s, t - N/A
- [x] u Timestamp format and hundredths - PASS: `timestampRule_namesAndForm`
- [ ] w, x, y, z, aa - N/A

## 5. What is wrong or missing in the skills

Procedure skills (corrections):

| Skill | Step | What is wrong | Corrected text |
|---|---|---|---|
| interest-ticket-verify | 8 | the evidence table has three files; a ticket whose final-revision evidence is a set of reports (10: "the seven reports from the last commit") has no place for them | add a row: "other evidence the ticket's Verification names: files or a sub-folder in the ticket's evidence folder, listed at the top of report.md" |
| interest-ticket-verify | 8 | `parity-report.sh` marks the case on its command line "yes - acceptance case of the ticket"; for a ticket that does not change the job (10) the case is a regression case | "For a ticket that does not change `interestCalcJob`, change that cell to `regression case of the job`." |
| interest-ticket-verify | 3 / 10 | says to run `git checkout -- tools/parity/__pycache__` but not that importing `tools.parity.compare` from Python also leaves an untracked `compare.cpython-*.pyc` there | "Run Python that imports the comparator with `PYTHONDONTWRITEBYTECODE=1`; delete an untracked `tools/parity/__pycache__/compare.*.pyc`." (reproduced: first run of `make sidebyside-java`) |
| interest-ticket-build | step 2 | does not say how to run a second main class of the application | "A command other than the job is started from the same jar with `java -cp interest.jar -Dloader.main=<class> org.springframework.boot.loader.launch.PropertiesLauncher`; start the context with `spring.batch.job.enabled=false`." |
| interest-pitfalls | missing | `compare.py` has no option for the legacy directory (F1) | "P: to compare two directories, lay the legacy one out as `<root>/interest/<name>/expected` with a manifest.json and point `compare.FIXTURES` at `<root>`; never edit the comparator." |
| interest-pitfalls | missing | a factory another ticket needs may be package-private in a package it may not edit (`ExtractProfile.of`, `RoundTrip.compare`, `ExtractDecoder`) | "P: make what a later ticket of the design reads public when you write it; a later ticket cannot widen it." (reproduced: F7) |

Standards skill (proposals only):

- S6.5: say what a report (not a log) may show of a record: proposal "key ends and aggregates only".
- S7: add "a report line that cannot be computed is NOT AVAILABLE; test each cause" as a rule for reconciliation code.
- S8.1: say whether calling the comparator's functions with another fixtures root counts as using it unchanged (F1).

## 6. Method

Commands: `mvn -B -f java/interest/pom.xml clean verify`; `parity-report.sh 10-data-move-kit i01-baseline`;
`make recon-java CASE=<case> LOAD=1` and `make sidebyside-java CASE=<case> TS=format|exact` for every case, and the
`i01-demo` comparison (make-targets.txt).

| Symptom | Cause | Fix |
|---|---|---|
| RC4 line DIFFERENT while both shown key ends read `...0001` | the key of tran_cat_balance and disclosure_group has three fields; the last four characters of the whole key are the category code | the report shows the last four characters of each key field (`...2001/01/0001`) |
| untracked `tools/parity/__pycache__/compare.cpython-310.pyc` after `make sidebyside-java` | Python caches the imported comparator module | `PYTHONDONTWRITEBYTECODE=1` in the target; file deleted |
| `make sidebyside-java CASE=i03-...`: "not a directory" | the pilot job stops on i03 and i04 and leaves no output | none here; ticket 04 |
| parity of the job: i03 and i04 FAIL, exit 12 | pilot calculation, unchanged | none here; ticket 04 |

## 7. Dependencies

None added. `PropertiesLauncher` is part of the Spring Boot jar already built; python3 is already needed by `make parity-java`.
