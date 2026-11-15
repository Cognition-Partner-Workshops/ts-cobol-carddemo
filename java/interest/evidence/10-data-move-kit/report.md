# Report - ticket 10-data-move-kit (INT-T10): reconciliation report and side-by-side comparison kit

Code revision `b24ada1545739d644052ef72dc9dc0b58323bf9a`, branch `demo/05-10-data-move-kit`, base `demo/05b-integration`.
Evidence in this directory: `test-output.txt` (`mvn -B clean verify`), `parity-report.md` (FI-6, seven cases),
`kit-output.txt` (the two commands of the kit from the command line, with their error paths and the database assertion).

Final build: `Tests run: 365, Failures: 0, Errors: 0, Skipped: 0` (unit), `Tests run: 78, Failures: 0, Errors: 0, Skipped: 0`
(integration, PostgreSQL 15 in a container), `BUILD SUCCESS`.

## 1. Result

| Acceptance line of the ticket | Met | Shown by |
|---|---|---|
| `make recon-java CASE=<case>` on a loaded and calculated case: RC1-RC5, each EQUAL or DIFFERENT with both figures; i01-baseline shows 2 transactions summing 47.00, a balance change of 10.00 and 37.00 not added to the last account | yes | `kit-output.txt`, "All seven cases" and "i01-baseline, whole report"; `ReconciliationReportIT.i01Baseline_theReportStatesTheAcceptanceFigures`, `everyCaseReconciles_withTheFiguresOfTheStrategyTable` (seven cases) |
| `make sidebyside-java` on two output directories: field-level differences via `tools/parity/compare.py`; `TRAN-ORIG-TS` and `TRAN-PROC-TS` for format only or exactly, as chosen; every difference line carries a class | yes | `kit-output.txt`, "Side by side" (`TS=format`: 4 differences accepted as decision INT-Q03, result EQUAL; `TS=exact`: the same 4 are defects); `DifferenceClassifierTest`, `SideBySideReportTest.everyDifferenceLineCarriesItsClass` |
| The profile of TL4 is part of the report | yes | section "Profile of the load (TL4)" of the report in `kit-output.txt`; `ReconciliationReportIT.theProfileOfTl4IsPartOfTheReport_allZeroOnTheRecordedCase`, `theProfileCountsWhatTheTablesHold`, `rc3_fSignedAndMinusZeroAmountsAreCountedPerGap_notAsOtherDifferences` |
| Errors: a figure that cannot be computed is NOT AVAILABLE, never equal | yes | `ReconLineTest`; `ReconciliationReportIT.aFigureTheRunDidNotRecordIsNotAvailable`, `aFileThatIsMissingOrUndecodableMakesItsLinesNotAvailable`, `aRunThatIsOnlyLoadedHasNoRc5_andARunThatDoesNotExistHasNoFigures`; `SideBySideReportTest.nothingComparedIsNotAvailable_neverEqual`; `kit-output.txt`, "Errors" (`RUN=999999`, legacy directory without `manifest.json`) |
| Database assertions: the report reads `interest_run` and the tables; it writes nothing | yes | `ReconciliationReportIT.theReportWritesNothing`, `theReportsTransactionIsReadOnly_aWriteInItIsRefusedByTheDatabase`; `kit-output.txt`, "Database assertion" (same md5 over all rows and same number of job executions before and after) |
| Verification: build with the db service | yes | `test-output.txt` |
| Verification: report tests with one deliberately broken figure per check | yes | `ReconciliationReportIT.rc1_*`, `rc2_*` (2), `rc3_*` (3), `rc4_*`, `rc5_*` (3); from the command line in `kit-output.txt`, "A broken figure" |
| Verification: all seven cases, figures of strategy-interest.md | yes | `ReconciliationReportIT.everyCaseReconciles_withTheFiguresOfTheStrategyTable`, expected values written by hand from the strategy's table; `kit-output.txt`; `parity-report.md` |

Per case (`kit-output.txt`; "FI-6" is `make parity-java` in `parity-report.md`, "export" is `compare.py` on the export of the tables after staging):

| Case | recon RC1-RC5 | Transactions | Sum | Balance change | Not added (INT-Q04) | FI-6 | Export |
|---|---|---|---|---|---|---|---|
| i01-baseline | RECONCILED | 2 | 47.00 | 10.00 | 37.00 | PASS | PASS |
| i02-tie-half-cent | RECONCILED | 2 | 24.69 | 12.35 | 12.34 | PASS | PASS |
| i03-zero-rate-skipped | RECONCILED | 1 | 10.00 | 0.00 | 10.00 | PASS | PASS |
| i04-default-group-fallback | RECONCILED | 1 | 17.50 | 0.00 | 17.50 | PASS | PASS |
| i05-multi-category | RECONCILED | 2 | 17.50 | 0.00 | 17.50 | PASS | PASS |
| i06-negative-balance | RECONCILED | 1 | -3.00 | 0.00 | -3.00 | PASS | PASS |
| i07-nontie-rounding | RECONCILED | 2 | 29.03 | 15.42 | 13.61 | PASS | PASS |

## 2. What the ticket does not cover

- **The calculation on the tables is ticket 06 (calc-step-jdbc), which is not in `demo/05b-integration`.** The acceptance
  input is "a loaded and calculated case". `make recon-java` makes one with a stand-in, `CalculatedRunStaging`: the
  loader (ticket 05), `interestCalcJob` on its file adapters, the result posted to `interest_transaction`, `account`
  and `interest_run`, then the exporter (ticket 07). It is in package `recon`, named as a stand-in in its Javadoc and
  in the README, and is to be removed when ticket 06 is merged; `STAGE=no` then reports on the job's own run.
  `written_interest_not_added_sum` is counted by the stand-in from the tables (transactions of the account that ends
  `tran_cat_balance`, through `card_xref`); ticket 06 owns that figure.
- RC6 needs ACCTDATA unloaded again on the mainframe after the write-back (operations, excluded work). The report
  shows it as NOT AVAILABLE and leaves it out of its result.
- The timestamp rule (SS3) and the exit of the side-by-side period (REL-04, REL-05) are not decided; both modes are offered.
- `format=host` is passed through to the loader and exporter but was run with `format=fixture` only (no host extract exists; G01).
- Mainframe JCL for unload, backup and write-back; production data.

## 3. For the decision register

| Finding | Evidence | Suggested owner |
|---|---|---|
| SS3: with a target on its own clock the four timestamp values of i01-baseline differ; `TS=format` accepts them as INT-Q03, `TS=exact` makes them defects. Exact comparison needs the legacy run's clock handed to the target | `kit-output.txt`, "Side by side" | owner of REL-05 |
| A one-cent `ACCT-CURR-BAL` difference that follows from INT-Q01 (rounding) is classed defect, because on an account with several categories the knock-on is not bounded by one cent. If INT-Q01 is ever switched, the class of the balance needs a rule | `kit-output.txt`, run with `--interest.policy.rounding=HALF_UP --interest.policy.updateLastAccount=true`: `ACCT-CURR-BAL` 1247.85 against 1247.86 | owner of INT-Q01 |
| The INT-Q04 class is given by position (last ACCTDATA record). The last account of TCATBALF is the last ACCTDATA record in all seven cases but need not be in production data; the report line says to confirm it | `DifferenceClassifier`, `kit-output.txt` | owner of INT-Q04 |
| Open decisions and gaps other than INT-Q03 under `TS=format` are never accepted by the kit: they count as open differences | `SideBySideReport.open` | owner of REL-04 |
| G06 profile: "mixed type codes" is counted as accounts whose `TRANCAT-TYPE-CD` values include both a digit and a non-digit; the data map does not define the count more precisely | `ReconciliationReporter.profile`, `theProfileCountsWhatTheTablesHold` | data-map owner |

## 4. Standards review checklist

Standards:

- [x] S1.1 Sources used in order; recorded output decides - PASS: expected figures from strategy-interest.md and `fixtures/interest/*/expected` (`ReconciliationReportIT`)
- [x] S1.2 Rule/code disagreements reproduced as coded and reported - N/A: none found; the kit adds no calculation
- [x] S2.1 Designed from rules; no method per paragraph - PASS: package `recon` follows RC1-RC6, TL4, SS2-SS4
- [x] S2.2 Immutable business records; copybook names only at the mapping - PASS: records `ReconLine`, `ReconReport`, `ComparedDifference`, `DifferenceClass`; copybook names appear only as report labels and in `DifferenceClassifier`, which works on the comparator's field names
- [x] S2.3 Spring Batch shape, transaction and restart boundaries of the architecture - N/A: the kit is not a job step; `ReconCommand` is a main of its own and `interestCalcJob` is unchanged
- [x] S2.4 Types and return values, no flags or result codes - PASS: `ReconStatus`, `Optional` figures, `DifferenceClass.Kind`
- [x] S2.5 Constructor injection; no mutable static state - PASS: `ReconciliationReporter`, `CalculatedRunStaging`, `DifferenceClassifier`
- [x] S3.1 `BigDecimal` with data-map precision and scale; no `double`/`float` - PASS: all sums in `ExtractSide`, `ReconciliationReporter`; no `double` or `float` in `recon`
- [x] S3.2 Formula: exact product, one division, mode from the policy - N/A: no interest formula here
- [x] S3.3 Rounding mode named on every other arithmetic step - PASS: only `add`, `subtract`, `abs`, `compareTo`; nothing rounds
- [x] S3.4 Range check before store; overflow goes to the policy - N/A: the stand-in stores decoded values of the job's own output
- [x] S3.5 Encodings only in `record-codec` - PASS: `ExtractSide` and `CalculatedRunStaging` decode with `RecordCodec`, `RecordImage`
- [x] S3.6 Width, padding and FILLER only in codec and schema - PASS: `ReconciliationReporter.image` uses `FieldSpec` offsets of the codec's layout
- [x] S3.7 Code page and key order each in one place - PASS: `ExtractFormat.codePage()`, `ReplicaTableWriter.rowsInKeyOrder`
- [x] S3.8 Clock and run parameters injected; `parmDate` is text - PASS: `CalculatedRunStaging(…, Clock)`, `parmDate` a `String`
- [x] S4.1 Every PRESENT behaviour touched has its test; new ones reported - N/A: no behaviour of section 4 is implemented here
- [x] S5.1 Each quirk/open decision in one named place behind its switch, rule id in names - PASS: the classes of INT-Q01 to INT-Q04 and G05 are in `DifferenceClassifier` only; the SS3 choice is `TimestampRule`
- [x] S5.2 Nothing fixed, tidied or added without a rule id - PASS: every check carries RC1-RC6, TL4 or SS3/SS4
- [x] S6.1 Status and message mapping as in section 6 - N/A: no file status handled; the stand-in stops with `InterestRunStoppedException`
- [x] S6.2 One stop exception, one exit-code constant, no skip or retry - PASS for the job (unchanged); the kit's own exit codes are `ReconCommand.EXIT_*`
- [x] S6.3 Restart as in the architecture - N/A
- [x] S6.4 Outputs in the recorded format; failed run leaves no SYSTRAN - PASS: export by `DatasetExporter`; `compare.py` PASS on all seven exports (`kit-output.txt`)
- [x] S6.5 Run log byte for byte; no account data in the application log - PASS: the run log is copied; the kit logs nothing
- [x] S7.1 A test per rule id, named for it - PASS: `rc1_` to `rc5_`, `rc6_`, TL4 and INT-Q01 to INT-Q04, G05 tests by name
- [x] S7.2 Fixture and edge cases of each rule covered - PASS: seven cases, broken figures, missing figures
- [x] S7.3 Integration tests on PostgreSQL 15 in a container - PASS: `ReconciliationReportIT`
- [x] S7.4 No assertion against the code's own output - PASS: expected figures typed from the strategy table and the fixtures' `expected`
- [x] S8.1 No edit under `app/`, `fixtures/`, `tools/parity` - PASS: `git diff --stat origin/demo/05b-integration`
- [x] S8.2 No golden file re-recorded - PASS
- [x] S8.3 Every new dependency explained in the pull request - PASS: none added (section 7)
- [x] S8.4 Frozen shared interfaces unchanged - PASS: FI-1 to FI-6 untouched; `parity-java` target unchanged
- [x] S8.5 Synthetic data only - PASS: fixture data and invented keys in the tests

Behaviours PRESENT in section 4: a, b, c, e, f, g, i, l, o, p, q, r, s, t, w, x, y, z, aa - N/A: the kit implements none of them and the
classes that do are unchanged. The others:

- [x] d Zoned overpunch signs - PASS: G02/G03 counted through the codec (`rc3_fSignedAndMinusZeroAmountsAreCountedPerGap_notAsOtherDifferences`)
- [x] h Code page and collating order - PASS: RC4 compares in the table's key order (`rc4_aTableWithoutItsHighestKeyShowsAsDifferent`)
- [x] j FILLER and record length round trip - PASS: RC3 and the filler line of the profile (`rc3_aRowThatNoLongerEncodesToItsRecordShowsAsDifferent`, `theProfileCountsWhatTheTablesHold`)
- [x] m Control break and end of file (INT-Q04, INT-T09) - PASS: RC5 states the interest not added (`rc5_theLastAccountUpdatedAfterAll_theBalanceChangeNoLongerAddsUp`)
- [x] u Timestamp format and hundredths - PASS: `DifferenceClassifierTest.formatOnly_*`, `exact_*`

## 5. What is wrong or missing in the skills

Standards (`interest-conversion-standards`), proposals only:

- Section 9 has no line for commands that are not job steps (a report, a comparison wrapper). S2.3, S6.1-S6.3 can only be N/A. Proposal: a line "tools outside the job read only, open no job repository and state their exit codes".

Other skills:

- `interest-ticket-verify` step 8: `parity-report.sh` marks every case given on the command line "acceptance case of the ticket". For a ticket whose acceptance is not `make parity-java` (this one) the label is misleading; the report here shows FI-6 is unchanged, not the acceptance.
- `interest-ticket-verify` steps 6 and 7 give the commands of ticket 01 only; a ticket with its own make targets has to design its own command-line evidence (here `kit-output.txt`). A sentence saying so would help.
- `interest-pitfalls`: running anything that imports `tools/parity` leaves `tools/parity/__pycache__/compare.cpython-310.pyc` as a new untracked file next to the tracked, modified `copybook…pyc`; `git checkout -- tools/parity/__pycache__` does not remove it.
- `interest-ticket-build`: `make` ends with 2 for any failing recipe; a recipe's own exit code 1 is visible only as `Error 1` in the output. Evidence that quotes exit codes of make targets should quote that line.

## 6. Method

Commands: `mvn -B -f java/interest/pom.xml clean verify` (JDK 21); `.agents/skills/interest-ticket-verify/parity-report.sh 10-data-move-kit <seven cases>`;
`make recon-java CASE=<case>` for the seven cases, each followed by `python3 tools/parity/compare.py interest <case> --actual work/recon-java/<case>/actual`;
`make recon-java ... STAGE=no [RUN=]`; `make sidebyside-java LEGACY= TARGET= [TS=]`; psql in the `db` service. All in `kit-output.txt`.

Failures hit:

| Symptom | Cause | Fix |
|---|---|---|
| `mvn -o` : `maven-failsafe-plugin:jar:3.5.4 has not been downloaded` | plugins not in the local repository | run without `-o` |
| `java` on the PATH is 17 | default JDK of the machine | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64` |
| First side-by-side run of i01 against i02 classed `ACCT-CURR-BAL` 2400.00 / 1234.50 and `ACCT-CURR-CYC-CREDIT` 2400.00 / 1234.50 on the last record as INT-Q04 | the class was given to any difference of the three amounts on the last record | cycle amounts are INT-Q04 only when the target has 0.00 (`DifferenceClassifier`, second commit); test added |
| `TS=sometimes` printed `No enum constant …TimestampRule.SOMETIMES` | `valueOf` | `TimestampRule.ofParameter` says `ts must be format or exact`; test added |
| README said exit code 1 for a difference; `make` ends with 2 | make's own exit code | README states `Error 1` / `Error 2` |
| `compare.py` has no option for the expected directory | it reads `fixtures/<chain>/<case>/expected` | `sidebyside-java` copies both directories under `work/sidebyside-java` and calls `compare.compare_case` with the module's `FIXTURES` set to the copy; nothing under `tools/parity` is edited |

## 7. Dependencies

None added. `ReconCommand` uses `DriverManagerDataSource` and `DataSourceTransactionManager` of spring-jdbc and logback, both already in the module; starting the Spring Boot application was not used because it would open the job repository and write to schema `batch`.
