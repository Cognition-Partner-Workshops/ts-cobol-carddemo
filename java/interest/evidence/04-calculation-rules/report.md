# Ticket 04 calculation rules - report

Ticket: `modernization/design/tickets/04-calculation-rules.md` (INT-T04). Branch `demo/05-g2-logic`, first ticket of
group g2-logic, from `origin/demo/05b-integration` at `6e3dda8`. Code revision the evidence was produced from: `a4618c9`.

Read this with [test-output.txt](test-output.txt) and [parity-report.md](parity-report.md).

## 1. Result

| Acceptance line of the ticket | Result | Where it is shown |
|---|---|---|
| Each of the seven cases under fixtures/interest, `make parity-java`: compare.py reports PASS for 7 of 7 | met: 7 of 7 PASS, `make parity-java` exit 0 (starting branch: 5 PASS, i03 and i04 FAIL) | parity-report.md "Result per case"; `SevenCasesFileParityTest.datasets_matchRecorded`, `sysout_matchesRecorded`, `rcJson_matchesRecorded` (7 rows each); `CalculationRulesJobIT.sevenCases_completeWithTheRecordedOutput` (7 rows, through the job) |
| i02-tie-half-cent writes TRAN-AMT 12.35 for balance 00000002003/01/0001 (INT-Q01) | met | `SevenCasesFileParityTest.intQ01_truncatesHalfCent`; `InterestFormulaTest.intQ01_truncatesHalfCent` |
| i03-zero-rate-skipped writes no transaction for a balance whose rate is zero (INT-R04) | met: two balances, one transaction | `SevenCasesFileParityTest.intR04_zeroRateBalanceWritesNoTransaction`; `InterestCalculatorTest.intR04_zeroRateWritesNoTransaction` |
| i04 logs DISCLOSURE GROUP RECORD MISSING and TRY WITH DEFAULT GROUP CODE (INT-R03) | met | `SevenCasesFileParityTest.intR03_fallbackLinesInTheRunLog`, `sysout_matchesRecorded[i04]`; `InterestCalculatorTest.intR03_fallsBackToDefault`; `InterestCalcJobIT.intR03_fallbackCaseCompletesWithTheRecordedRunLog` |
| i06 writes a negative amount (INT-Q05) | met: -3.00, zoned `0000000030}` | `SevenCasesFileParityTest.intQ05_negativeInterestIsDSigned`; `CalculationSwitchesTest.intQ05_negativeBalanceGivesNegativeInterest` |
| Switch test: with rounding=HALF_EVEN i02-tie-half-cent fails on TRAN-AMT and i07-nontie-rounding still passes; with the default both pass | met. Comparator, job run by hand with `--interest.policy.rounding=HALF_EVEN`: i02 `FAIL (4 records, 50 fields, 2 differences)` - `TRAN-AMT` 12.35 against 12.36 and the `ACCT-CURR-BAL` that follows from it, 1247.85 against 1247.86; i07 `PASS (4 records, 50 fields, 0 differences)`. Default: both PASS | section 6 (the command and its output); `SevenCasesFileParityTest.intQ01_halfEvenFailsTheTieCaseOnTranAmtOnly`, `intQ01_halfEvenStillPassesTheNonTieCase` |
| Switch test: each of updateLastAccount, idPrefix, timestampSource, negativeBalance, zeroInterest, inactiveAccounts changes exactly the values that `modernization/rules/interest/expected/<case>.json` lists for that option | met: 49 rows = 7 cases x 7 options (INT-Q01 A, B, C; INT-Q02 B; INT-Q03 B; INT-Q04 B; INT-Q05 B). Every field the file names is compared with the file's value for the option; every account field it does not name must equal the input; the run log must equal the file's `run.sysout`. The files list no value for INT-Q06 or INT-H04 (no case reaches them): with those switches on SKIP all four output files of all seven cases equal the recorded ones | `PolicySwitchExpectationsTest.switch_changesExactlyTheListedValues` (49 rows); `SevenCasesFileParityTest.intQ06_skipChangesNothingOnTheRecordedCases`, `intH04_skipChangesNothingOnTheRecordedCases` (7 rows each); what SKIP does on synthetic data: `CalculationSwitchesTest.intQ06_*`, `intH04_*` |
| Errors: an empty balance input writes no transaction, touches no account and ends COMPLETED (INT-T09) | met | `CalculationRulesJobIT.intT09_emptyInputEndsCompleted` (job COMPLETED, read/write 0/0); `SevenCasesFileParityTest.intT09_emptyInputWritesNothing` |
| Database assertions: none | - | - |
| Build: `mvn -q -f java/interest/pom.xml verify` | met: 282 unit tests and 20 integration tests, 0 failures, BUILD SUCCESS (starting branch: 162 and 12) | test-output.txt |
| Tests: one unit test per rule id and per switch, named by the id | met, table below | test-output.txt |
| Final-revision evidence: the comparator report with seven PASS sections and the switch-test output | met | parity-report.md "The comparator's report"; section 6 |

One test or more per rule id of the ticket (new or changed by this ticket in **bold**):

| Rule | Tests |
|---|---|
| INT-R01 | `InterestCalculatorTest.intR01_everyBalanceOfTheAccountGetsOneDecision`; `FileRunWorkspaceTest.intR01_readsInKeyOrder`, `intR01_balancesOfOneAccountAreOneItem` |
| INT-R02 | `InterestCalculatorTest.intR02_rateByGroupTypeCategory`; `FileRunWorkspaceTest.intR02_rateLookupByGroupTypeCategory` |
| INT-R03 | **`InterestCalculatorTest.intR03_fallsBackToDefault`, `intR03_noStaleRateAfterAFailedLookup`; `SevenCasesFileParityTest.intR03_fallbackLinesInTheRunLog`, `intR03_defaultGroupKeyPadded`; `InterestCalcJobIT.intR03_fallbackCaseCompletesWithTheRecordedRunLog`** |
| INT-R04 | **`InterestCalculatorTest.intR04_zeroRateWritesNoTransaction`, `intR04_accountWithOnlyZeroRatesIsStillUpdatedAtTheBreak`; `SevenCasesFileParityTest.intR04_zeroRateBalanceWritesNoTransaction`** |
| INT-R05 | `InterestFormulaTest.intR05_formulaExactProductThenDivide`, `intR05_productIsNotRoundedBeforeTheDivision` |
| INT-R06 | `InterestCalculatorTest.intR06_postedAsSystemTransaction`, `intR06_categoryCodeIs0005`, `intR06_cardNumberOfAccount`; `RecordLayoutTest.intR06_transactionFieldsFixedWidth` |
| INT-R07 | `InterestCalculatorTest.intR07_updatesAccountAtBreak`, `intR07_totalIsPerAccount`; **`SevenCasesFileParityTest.intR07_updatesAccountAtBreak` (i01, i02, i07)** |
| INT-Q01 | `InterestFormulaTest.intQ01_*` (6); **`SevenCasesFileParityTest.intQ01_truncatesHalfCent`, `intQ01_halfEvenFailsTheTieCaseOnTranAmtOnly`, `intQ01_halfEvenStillPassesTheNonTieCase`; `PolicySwitchExpectationsTest` options A, B, C** |
| INT-Q02 | `InterestCalculatorTest.intQ02_idIsParmPlusCounter`, `intQ02_counterRunsAcrossAccounts`, `intQ02_runDatePrefixWhenSwitched`; **`PolicySwitchExpectationsTest` option B** |
| INT-Q03 | `InterestCalculatorTest.intQ03_timestampsFromRunClock`, `intQ03_clockIsReadOncePerTransaction`, `intQ03_defaultIsRunClock`; **`CalculationSwitchesTest.intQ03_cycleDateOfTheParmWhenSwitched`, `intQ03_cycleDateNeedsEightDigitsInTheParm`, `intQ03_runClockIgnoresWhatThePARMHolds`; `PolicySwitchExpectationsTest` option B** |
| INT-Q04 | `InterestCalculatorTest.intQ04_lastAccountNotUpdated`, `intQ04_lastAccountUpdatedWhenSwitched`; **`SevenCasesFileParityTest.intQ04_lastAccountNotUpdated` (all seven); `PolicySwitchExpectationsTest` option B** |
| INT-Q05 | **`CalculationSwitchesTest.intQ05_negativeBalanceGivesNegativeInterest`, `intQ05_truncatesTowardsZeroOnACreditBalance`, `intQ05_creditBalanceSkippedWhenSwitched`, `intQ05_skipLeavesDebitBalancesAlone`, `intQ05_defaultIsCharge`; `SevenCasesFileParityTest.intQ05_negativeInterestIsDSigned`; `PolicySwitchExpectationsTest` option B** |
| INT-Q06 | **`CalculationSwitchesTest.intQ06_zeroInterestIsStillPosted`, `intQ06_zeroInterestSkippedWhenSwitched`, `intQ06_defaultIsWrite`; `SevenCasesFileParityTest.intQ06_skipChangesNothingOnTheRecordedCases`** |
| INT-H01 | **`CalculationSwitchesTest.intH01_noFeeTransaction`** |
| INT-H04 | **`CalculationSwitchesTest.intH04_inactiveAccountIsChargedLikeAnyOther`, `intH04_inactiveAccountSkippedWhenSwitched`, `intH04_skipLeavesActiveAccountsAlone`, `intH04_defaultIsCharge`; `SevenCasesFileParityTest.intH04_skipChangesNothingOnTheRecordedCases`** |
| INT-T02 | **`SevenCasesFileParityTest.sysout_matchesRecorded` (all seven)**; `InterestCalcJobIT.sysout_matchesRecorded` |
| INT-T09 | **`SevenCasesFileParityTest.intT09_emptyInputWritesNothing`; `CalculationRulesJobIT.intT09_emptyInputEndsCompleted`**; `FileRunWorkspaceTest.intT09_emptyInputGivesNoItem` |

Changed: `java/interest/src/main/java/com/carddemo/interest/domain/**` (`InterestCalculator`, `Account`, `StopReason`,
`RunLogLines`, `RunTimestamp`), `.../policy/InterestPolicy.java`, `java/interest/src/test/**`, and under the grants of the
group prompt `.../codec/AccountRecord.java` (passes ACCT-ACTIVE-STATUS), `RecordLayoutTest` (one assertion) and
`src/main/resources/application.yml` (three switch defaults). The ports (FI-3), the switch names (FI-5), the Makefile
(FI-6), the batch and file packages and the pom are untouched.

## 2. What the ticket does not cover (gaps)

| Gap | State in this branch | Owner |
|---|---|---|
| INT-Q06 has no fixture | Tested with synthetic data only: a balance of 0.50 at 12.00 % gives 0.005, cut to 0.00, and the transaction is written with amount 0.00 and the next id (`CalculationSwitchesTest.intQ06_zeroInterestIsStillPosted`); with `zeroInterest=SKIP` nothing is written and no id is used. No recorded legacy run shows a 0.00 transaction; the behaviour is read from `CBACT04C:214-217, 464-468` | business owner (INT-Q06); a fixture would need `make record` |
| INT-H04 has no fixture | Every fixture account is active. Tested with a synthetic account whose flag is N | business owner (INT-H04) |
| What `inactiveAccounts=SKIP` and `negativeBalance=SKIP` mean in detail | The rulebook gives one sentence each. Built as: an inactive account gets no rate lookup, no transaction and no update at the break (its record is still echoed, and a missing account or card still stops the run); a credit balance gets the rate lookup (so the fallback lines of INT-R03 still appear) but no interest and no transaction, and the account is still updated at the break with the total of its other balances | business owner, when either option is chosen |
| Legacy messages of the stop conditions | `DEFAULT_RATE_MISSING` stops the run with the one exception; `ERROR READING DEFAULT DISCLOSURE GROUP`, the status line and `ABENDING PROGRAM` are not written to the run log | 08 |
| INT-R08, INT-H03, INT-H05 | Excluded work. `MULTI_CARD` still stops as in the pilot; no range check of amount or counter | 08 |
| JDBC | Excluded work; the calculation runs on the file adapters | 06 |
| `rerunGuard` (INT-H02) | not a switch of this ticket's list; `InterestCalcJobIT.intH02_secondRunWithTheSameParmIsNotRefused` (pilot) still passes | 06 |

## 3. For the decision register

| Finding | Evidence | Suggested owner |
|---|---|---|
| INT-Q03 option B has no cycle-date parameter. Standard S3.8 says a frozen business date is a job parameter and `parmDate` is never parsed as a date; `rules.yaml` option B says "the cycle date of the PARM at 00.00.00" and the expected files give `2022-07-18-00.00.00.000000` for PARM `2022071800`. `architecture.md` section 4 lists no such parameter and package `batch` is outside this ticket. Built from the PARM as text: characters 1-8 are copied into `YYYY-MM-DD` and followed by `-00.00.00.000000`; a PARM that does not start with eight digits stops the run (`CYCLE_DATE_UNREADABLE`, INT-Q03) only when the switch is CYCLE_DATE. The pilot refused the value at start-up | `RunTimestamp.ofCycleDate`; `CalculationSwitchesTest.intQ03_*`; `PolicySwitchExpectationsTest` INT-Q03 option B on all seven cases | target architect (S3.8) and business owner (INT-Q03) |
| An account whose balances all have a zero rate is still rewritten at the break: total 0.00 added, cycle credit and debit set to zero (`CBACT04C:194-201, 350-356`). No fixture shows it for an account that is not the last (i03's only account is the last one). Reproduced as coded | `InterestCalculatorTest.intR04_accountWithOnlyZeroRatesIsStillUpdatedAtTheBreak` | business owner (INT-R04, INT-R07) |
| A negative rate is charged like any other rate (the legacy tests `NOT = 0` only, `CBACT04C:214`); `rules.yaml` INT-Q05 mentions it in its "why". Reproduced: only a rate equal to zero skips. `negativeBalance=SKIP` looks at the balance's sign, not the rate's | `InterestCalculator.interestOn` | business owner (INT-Q05) |
| With `negativeBalance=SKIP`, `zeroInterest=SKIP` or `inactiveAccounts=SKIP` a skipped balance uses no transaction id, so the ids of later transactions move up | `CalculationSwitchesTest.intQ06_zeroInterestSkippedWhenSwitched` | business owner |
| The expected files (`modernization/rules/interest/expected`) agree with the recorded legacy output on every field they name, for the as-coded options, on all seven cases | `PolicySwitchExpectationsTest` INT-Q01 option A (7 rows) | - |

## 4. Standards review checklist

Standards:

- [x] S1.1 Sources used in order; recorded output decides - PASS: ticket, rules, architecture, COBOL lines cited in the Javadoc of `InterestCalculator`; every case asserted against `fixtures/interest/<case>/expected` (`SevenCasesFileParityTest`)
- [x] S1.2 Rule/code disagreements reproduced as coded and reported - PASS: section 3 (zero-rate account update, negative rate, S3.8 against INT-Q03 option B)
- [x] S2.1 Designed from rules; no method per paragraph - PASS: `InterestCalculator.rateFor` (INT-R02, R03), `interestOn` (INT-R04, Q05, Q06), `isCharged` (INT-H04), `newBalanceAtBreak` (INT-R07, Q04)
- [x] S2.2 Immutable business records; copybook names only at the mapping - PASS: `Account` gains `activeStatus`; `"ACCT-ACTIVE-STATUS"` appears only in `codec/AccountRecord.java`
- [x] S2.3 Spring Batch shape, transaction and restart boundaries of the architecture - N/A: the step is unchanged (file adapters, ticket 01); ticket 06
- [x] S2.4 Types and return values, no flags or result codes - PASS: a missing group rate is `Optional.empty()` from `RateLookup`; "no interest due" is `Optional.empty()` from `interestOn`; the pilot's two `*_NOT_BUILT` stop reasons are gone
- [x] S2.5 Constructor injection; no mutable static state - PASS: `InterestCalculator` constructor unchanged; no static state added
- [x] S3.1 `BigDecimal` with data-map precision and scale; no `double`/`float` - PASS: `grep -rnE "double|float" java/interest/src` finds nothing
- [x] S3.2 Formula: exact product, one division, mode from the policy - PASS: `InterestFormula` unchanged; `intQ01_*`, `intR05_*`
- [x] S3.3 Rounding mode named on every other arithmetic step - PASS: `InterestCalculator.calculate` and `newBalanceAtBreak` use `setScale(2, RoundingMode.UNNECESSARY)`
- [x] S3.4 Range check before store; overflow goes to the policy - N/A: INT-H05 is ticket 08 (Excluded work)
- [x] S3.5 Encodings only in `record-codec` - PASS: no encoding in `domain` or `policy`; tests compare zoned bytes only as recorded text
- [x] S3.6 Width, padding and FILLER only in codec and schema - PASS: the constant `DEFAULT` is unpadded in the domain, padded on the file (`intR03_defaultGroupKeyPadded`)
- [x] S3.7 Code page and key order each in one place - N/A: not touched
- [x] S3.8 Clock and run parameters injected; `parmDate` is text - PASS for the default; for `timestampSource=CYCLE_DATE` the date is taken from the PARM text, not from a job parameter of its own: reported in section 3 and section 5
- [x] S4.1 Every PRESENT behaviour touched has its test; new ones reported - PASS: rows a, g, l, m, o, s, x, aa below; section 3
- [x] S5.1 Each quirk/open decision in one named place behind its switch, rule id in names - PASS: `InterestPolicy.intQ05ChargesCreditBalance`, `intQ06WritesZeroInterest`, `intH04ChargesInactiveAccounts`, `intQ03StampsCycleDate`, each read once in `InterestCalculator`
- [x] S5.2 Nothing fixed, tidied or added without a rule id - PASS: the 0.00 transaction is written, inactive accounts are charged, no fee is written; `1400-COMPUTE-FEES` is not ported (`intH01_noFeeTransaction`)
- [x] S6.1 Status and message mapping as in section 6 - N/A for the stop rows (ticket 08); PASS for the row "DISCGRP status 23": `Optional.empty()`, then the DEFAULT lookup, the two lines in the run log
- [x] S6.2 One stop exception, one exit-code constant, no skip or retry - PASS: `DEFAULT_RATE_MISSING` and `CYCLE_DATE_UNREADABLE` are raised as `InterestRunStoppedException`; nothing catches it
- [x] S6.3 Restart as in the architecture - N/A: file adapters, no restart (ticket 06)
- [x] S6.4 Outputs in the recorded format; failed run leaves no SYSTRAN - PASS: all four output files of all seven cases equal the recorded ones; the failed-run path is unchanged
- [x] S6.5 Run log byte for byte; no account data in the application log - PASS: `sysout_matchesRecorded` on all seven; the new exception messages hold no id or amount
- [x] S7.1 A test per rule id, named for it - PASS: table in section 1. INT-H06 is outside the job and has no test
- [x] S7.2 Fixture and edge cases of each rule covered - PASS: all seven cases; synthetic data for INT-Q06, INT-H04, INT-T09
- [x] S7.3 Integration tests on PostgreSQL 15 in a container - PASS: `CalculationRulesJobIT` on Testcontainers `postgres:15` (job repository only; the ticket has no table)
- [x] S7.4 No assertion against the code's own output - PASS: expected values are the recorded files, `modernization/rules/interest/expected/*.json`, or worked by hand in a comment
- [x] S8.1 No edit under `app/`, `fixtures/`, `tools/parity` - PASS: `git diff --stat origin/demo/05b-integration -- app fixtures tools modernization .agents db docker-compose.yml` prints nothing
- [x] S8.2 No golden file re-recorded - PASS
- [x] S8.3 Every new dependency explained in the pull request - PASS: none (section 7)
- [x] S8.4 Frozen shared interfaces unchanged - PASS: FI-1, FI-2, FI-3, FI-6 untouched; FI-5: the three new switches carry the names of `architecture.md` section 8
- [x] S8.5 Synthetic data only - PASS: accounts 2001-2010 and card numbers built from them

Behaviours PRESENT in section 4:

- [x] a Truncation without ROUNDED - PASS: `intQ01_truncatesHalfCent` (i02), `intQ01_nonTieUnchanged` (i07), `intQ05_truncatesTowardsZeroOnACreditBalance`
- [x] b High-order digit loss (range check, STOP) - N/A: ticket 08
- [x] c Intermediate precision of the COMPUTE - PASS: `intR05_formulaExactProductThenDivide` (unchanged)
- [x] d Zoned overpunch signs - PASS: `intQ05_negativeInterestIsDSigned` (i06)
- [x] e Binary items stay out of data and domain - N/A: not touched
- [x] f Non-numeric data rejected at the boundary - N/A: not touched
- [x] g Fixed width, padding, STRING - PASS: `intR03_defaultGroupKeyPadded` (i04)
- [x] h Code page and collating order - N/A: not touched
- [x] i REDEFINES replaced by formatter and function - PASS: the cycle-date stamp is one function, `RunTimestamp.ofCycleDate`
- [x] j FILLER and record length round trip - PASS: `PolicySwitchExpectationsTest` requires every account field the expected file does not name, FILLER included, to equal the input
- [x] l No state carried between accounts except the counter - PASS: `intR07_totalIsPerAccount`, `intR03_noStaleRateAfterAFailedLookup`
- [x] m Control break and end of file (INT-Q04, INT-T09) - PASS: `intR07_updatesAccountAtBreak` (i01, i02, i07), `intQ04_lastAccountNotUpdated` (all seven), `intT09_emptyInputWritesNothing`
- [x] o Level-88 result codes replaced by types - PASS: `intR03_*`
- [x] p Indexed access through the ports; several cards to the policy - N/A: the switch `multiCard` is ticket 08; `intH03_twoCardsStops` (pilot) still passes
- [x] q File statuses and abend - N/A: ticket 08
- [x] r PARM, generation, disposition, return code - PASS: `rcJson_matchesRecorded` (all seven)
- [x] s DISPLAY output in the run log - PASS: `sysout_matchesRecorded` (all seven; i04 for the fallback lines)
- [x] t PARM text against the clock - PASS: `intQ03_runClockIgnoresWhatThePARMHolds`; CYCLE_DATE copies the PARM text and does not parse it
- [x] u Timestamp format and hundredths - PASS: unchanged; the cycle-date stamp has the same 26 characters (`intQ03_cycleDateOfTheParmWhenSwitched`)
- [x] w PARM length checked - N/A: not touched
- [x] x No stale rate after a failed lookup - PASS: `intR03_fallsBackToDefault`, `intR03_noStaleRateAfterAFailedLookup`
- [x] y Typed fields where COBOL mixed text and numbers - N/A: not touched
- [x] z Abend ends the step; stub output not reproduced - N/A: ticket 08
- [x] aa Dead and empty code not ported - PASS: `intH01_noFeeTransaction`

## 5. What is wrong or missing in the standards skill (proposals only)

1. **S3.8 against INT-Q03 option B.** S3.8 says the business date of `timestampSource=CYCLE_DATE` "is a job parameter
   too", but `architecture.md` section 4 lists no such parameter and `rules.yaml` defines option B as "the cycle date of
   the PARM". Proposed text: "With `timestampSource=CYCLE_DATE` the stamp is the first eight characters of `parmDate`,
   copied as text into `YYYY-MM-DD-00.00.00.000000`; a `parmDate` that does not start with eight digits stops the run."
2. **Section 8 of the architecture calls INT-H01 an "empty fee hook"; S5.2 and row aa say the empty paragraph is not
   ported.** The ticket repeats "empty fee hook". Built as row aa says (no hook, one Javadoc sentence, one test).
   Proposed: say in S5.2 that INT-H01 has no code and no extension point until a fee rule exists.
3. **Section 4 row g** names `intR03_defaultGroupKeyPadded` and says the fallback group "is the constant `DEFAULT`
   compared as CHAR(10)". The domain holds trimmed text (S3.5), so the comparison as CHAR(10) happens in the adapter.
   Proposed: "the domain passes `DEFAULT`; the adapter compares it with the 10-byte field".
4. **S7.1 "a switch has a test for the default and for each other value"** does not say what the expected side of a
   non-default value is. Proposed: "the expected values of a non-default option come from
   `modernization/rules/interest/expected/<case>.json` where it lists one, else are worked by hand."
5. **Section 4 has no row for the three SKIP options** (what else a skipped balance or account leaves out: rate
   lookup, fallback lines, transaction id, account update). Proposed: one row per option once the business answers.

## 6. Method

Skills used: `interest-conversion-standards` (all sections; checklist section 9), `interest-ticket-build` steps 1-9,
`interest-ticket-verify` steps 1-5 and 8-10, `interest-pitfalls` P1, P6, P7.

Commands, from the repository root, `JAVA_HOME` on JDK 21:

```sh
mvn -B -f java/interest/pom.xml verify                      # 282 + 20 tests, BUILD SUCCESS
make parity-java                                            # 7 of 7 PASS, exit 0
.agents/skills/interest-ticket-verify/parity-report.sh 04-calculation-rules $(ls fixtures/interest)
```

Switch test with the comparator (`interest-ticket-verify` step 4, for i02 and i07):

```sh
db=$(docker compose ps -q db)
for c in i02-tie-half-cent i07-nontie-rounding; do
  docker run --rm --network container:$db --user $(id -u):$(id -g) -v "$PWD":/estate -w /estate eclipse-temurin:21-jre \
    java -jar java/interest/target/interest.jar --interest.clock.frozen="2022-07-05 10:00:00.00" --logging.level.root=WARN \
      --interest.policy.rounding=HALF_EVEN parmDate=2022071800 runId=manual-$c-$(date +%s) format=fixture \
      inputDir=fixtures/interest/$c/input outputDir=work/manual/$c/actual
  python3 tools/parity/compare.py interest $c --actual work/manual/$c/actual --label java-halfeven --report work/manual/diff.md
done
```
```
job exit=0
interest/i02-tie-half-cent [java-halfeven]: FAIL (4 records, 50 fields, 2 differences)
job exit=0
interest/i07-nontie-rounding [java-halfeven]: PASS (4 records, 50 fields, 0 differences)

| Where | Field | Expected (legacy) | Actual (candidate) |
| AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS rec 1 (ACCT-ID=2003) | `ACCT-CURR-BAL` | 1247.85 | 1247.86 |
| AWS.M2.CARDDEMO.SYSTRAN.G0001V00 rec 1 (TRAN-ID=2022071800000001) | `TRAN-AMT` | 12.35 | 12.36 |
```

Failures hit:

| Symptom | Cause | Fix |
|---|---|---|
| `package com.fasterxml.jackson.databind does not exist` when compiling `PolicySwitchExpectationsTest` | Jackson is not on the test classpath of the module, and the pom is outside the ticket | the test reads the expected files with `org.json`, which `spring-boot-starter-test` already brings (through jsonassert). No dependency added |

Corrections to the three procedure skills:

| Skill | Step | What is wrong | Corrected text |
|---|---|---|---|
| interest-ticket-build | 8 | The quoted totals are the pilot's (60 and 12); on `demo/05b-integration` after tickets 02 and 03 they are 162 and 12 | "Compare with the totals in the newest `java/interest/evidence/*/test-output.txt` of your base branch, not with the figures quoted here." |
| interest-ticket-build | 5 | Says packages `jdbc`, `load`, `export`, `recon` do not exist and schema `carddemo` has no table beyond `01-transaction-type.sql`; `db/postgres/02-interest.sql` exists since ticket 02 | "`db/postgres/02-interest.sql` (ticket 02, FI-4) holds the six tables of the stream; the Java tests do not load it until ticket 06." |
| interest-ticket-verify | 3 | The quoted result (i03, i04 FAIL, `make parity-java` Error 1) is the pilot's | "From ticket 04 on, all seven cases PASS and `make parity-java` exits 0; any FAIL is a regression." |
| interest-ticket-verify | 8 | Does not say where the output of a switch test (step 4) goes; the ticket asks for it as evidence | "Paste the command and output of a step-4 run the ticket asks for into section 6 of `report.md`." |
| interest-pitfalls | new P8 | Not listed: a test that reads JSON cannot use Jackson | "Symptom: `package com.fasterxml.jackson.databind does not exist`. Cause: the module has no Jackson. Fix: `org.json.JSONObject` from `spring-boot-starter-test`; do not add a dependency." |

## 7. Dependencies

None added. The pom is unchanged.
