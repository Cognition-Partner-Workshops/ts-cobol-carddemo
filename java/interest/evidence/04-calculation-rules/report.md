# Ticket 04 - complete calculation rules behind the policy switches - report

Ticket: `modernization/design/tickets/04-calculation-rules.md` (INT-T04). Branch `demo/05-04-calculation-rules`,
started from `demo/05b-integration` at `a5cce81`. Code revision the evidence was produced from: `a50a098`.

Read this with [parity-report.md](parity-report.md) (seven PASS sections), [switch-test-output.txt](switch-test-output.txt)
(the switch test and the empty-input run) and [test-output.txt](test-output.txt) (`mvn clean verify`).

## 1. Result

| Acceptance line of the ticket | Result | Where it is shown |
|---|---|---|
| Each of the seven cases under fixtures/interest, `make parity-java`: compare.py reports PASS for 7 of 7 | met | parity-report.md, "Result per case" and "All cases" (exit code 0); `FixtureCasesTest.allSevenCases_outputEqualsTheRecordedLegacyRun`, `InterestCalcJobIT.allSevenCases_jobOutputEqualsTheRecordedLegacyRun` |
| i02-tie-half-cent writes TRAN-AMT 12.35 for balance 00000002003/01/0001 (INT-Q01) | met | `FixtureCasesTest.intQ01_i02WritesTheTieAs1235`; parity-report.md, i02 |
| i03-zero-rate-skipped writes no transaction for a balance whose rate is zero (INT-R04) | met | `FixtureCasesTest.intR04_i03WritesNoTransactionForTheZeroRateBalance`, `InterestCalculatorTest.intR04_zeroRateWritesNoTransaction`; parity-report.md, i03 (1 record) |
| i04 logs DISCLOSURE GROUP RECORD MISSING and TRY WITH DEFAULT GROUP CODE (INT-R03) | met | `FixtureCasesTest.intR03_i04LogsTheFallbackAndUsesTheDefaultRate`, `InterestCalcJobIT.intR03_i04LogsTheFallbackToTheDefaultGroup`; parity-report.md, i04 SYSOUT |
| i06 writes a negative amount (INT-Q05) | met | `FixtureCasesTest.intQ05_negativeInterestIsDSigned` (-3.00 as `0000000030}`); parity-report.md, i06 |
| Switch test: with `interest.policy.rounding=HALF_EVEN` i02-tie-half-cent fails on TRAN-AMT and i07-nontie-rounding still passes; with the default both pass | met | switch-test-output.txt, first section (job jar and compare.py: i02 FAIL with `TRAN-AMT` 12.35 against 12.36 and the `ACCT-CURR-BAL` that follows from it, i07 PASS; default PASS and PASS); `FixtureCasesTest.intQ01_halfEvenFailsI02OnTranAmtAndLeavesI07AsRecorded` |
| Switch test: each of updateLastAccount, idPrefix, timestampSource, negativeBalance, zeroInterest, inactiveAccounts changes exactly the values that `modernization/rules/interest/expected/<case>.json` lists for that option | met | `PolicySwitchExpectationsTest` (105 runs = 15 switch values x 7 cases, rounding included); its 105 `switch-test` lines are the last section of switch-test-output.txt. zeroInterest and inactiveAccounts change no value of any case, as the expectations list none (see section 2) |
| Errors: an empty balance input writes no transaction, touches no account and ends COMPLETED (INT-T09) | met | `InterestCalcJobIT.intT09_emptyBalanceInputCompletesAndWritesNoTransaction` (job COMPLETED, exit code 0, read 0 / write 0); `FixtureCasesTest.intT09_emptyInputWritesNothing`; switch-test-output.txt, "Errors (INT-T09)" (job exit 0, SYSTRAN 0 bytes, account file identical to the input) |
| Database assertions: none (file adapters only) | n/a | the job repository checks of ticket 01 still pass in `InterestCalcJobIT`; parity-report.md, last section |
| Build: `mvn -q -f java/interest/pom.xml verify`; one unit test per rule id and per switch, named by the id | met: 205 unit tests and 20 integration tests, 0 failures, BUILD SUCCESS | test-output.txt; test names per rule in section 4, line S7.1 |

Parity per case: i01, i02, i03, i04, i05, i06, i07 PASS.

Not touched: `app/`, `fixtures/`, `tools/`, `modernization/`, `.agents/`, `db/`, `docker-compose.yml`, `Makefile`, `pom.xml`
(`git diff --stat origin/demo/05b-integration -- app fixtures tools modernization .agents db docker-compose.yml` is empty).
No golden file was re-recorded. No ticket system was written to. Nothing was merged.

Files changed outside the ticket's Allowed files, each inside the grant of the build prompt and nothing more:
`codec/AccountRecord.java` (passes ACCT-ACTIVE-STATUS to the domain), `RecordLayoutTest` (under `src/test`, the one
assertion that broke) and `application.yml` (the three new switches with their legacy defaults).

## 2. What the ticket does not cover (gaps)

* **INT-Q06 has no fixture.** No case has a balance small enough for interest of 0.00 at a non-zero rate. It is tested
  with synthetic data only: `InterestCalculatorTest.intQ06_zeroInterestIsWritten` (0.50 x 12.00 / 1200 = 0.005, cut to
  0.00, transaction written with id ...000001), `intQ06_zeroBalanceAtNonZeroRateIsWritten`,
  `intQ06_zeroInterestSkippedWhenSwitched` and `InterestPolicyTest.intQ06_zeroInterestWrittenUnlessSwitched`. On the
  seven cases `zeroInterest=SKIP` changes nothing, which is what the expectations list.
* **INT-H04 has no fixture** either (every fixture account has ACCT-ACTIVE-STATUS = Y). Tested with synthetic data:
  `InterestCalculatorTest.intH04_inactiveAccountIsCharged`, `intH04_inactiveAccountSkippedWhenSwitched`,
  `intH04_activeAccountChargedWhenSwitched`, `InterestPolicyTest.intH04_everyAccountChargedUnlessSwitched`.
* **INT-R04 at an account break** (an account with only zero rates that does not end the input is still updated) and
  **INT-R03 without a DEFAULT rate** are tested with synthetic data only; no fixture has them.
* **Error paths INT-R08, INT-H03, INT-H05** (legacy messages, status lines, overflow checks): ticket 08. The run still
  stops with `DEFAULT_RATE_MISSING (INT-R08)` when neither group has a rate, without the legacy message lines.
* **JDBC adapters, schema, load, export, restart**: tickets 02, 03, 05, 06, 07. `rerunGuard` (INT-H02), `multiCard`
  (INT-H03) and `overflow` (INT-H05) are not switches of this ticket.
* **`rateGroup` and `ratePercent`** of the expectation files are not outputs of the job and are not compared by the
  switch test; the amount that follows from them is.
* **`java/interest/README.md`** still describes the pilot (switches INT-Q01 to Q04, i03 and i04 stopping). It is
  outside the Allowed files and was left as it is; it should be brought up to date by a ticket that may touch it.
* No option other than the as-coded one is enabled by default; none was chosen.

## 3. For the decision register

| Rule | Finding | Evidence | Suggested owner |
|---|---|---|---|
| INT-Q03 | Option B says "the cycle date of the PARM at 00.00.00", but the PARM is free text (INT-Q02) and nothing checks that it starts with a date. As built, CYCLE_DATE takes the first eight characters as YYYYMMDD without parsing them as a date and stops the run (`CYCLE_DATE_UNREADABLE`) when they are not eight digits. Whether the cycle date should instead be its own job parameter is open (standards S3.8 says so; architecture.md section 4 lists none). | `RunTimestamp.ofCycleDate`; `InterestCalculatorTest.intQ03_cycleDateWhenSwitched`, `intQ03_cycleDateOfAParmThatIsNotDigitsStops`; expectations: `2022-07-18-00.00.00.000000` | business owner, target architect |
| INT-Q05 | Option B is built as "balance below zero". A zero balance is charged (and under INT-Q06 writes 0.00). A negative rate on a debit balance would also give negative interest and is not covered by the option. | `InterestPolicy.intQ05ChargesBalance`; `InterestPolicyTest.intQ05_creditBalanceChargedUnlessSwitched` | business owner |
| INT-H04 | Option B is built as the rulebook evaluator has it: an account whose flag is not Y gets no transaction and no update at its break (cycle credit and debit are not reset), but its balances are still read, displayed and their rate looked up, so the fallback lines of INT-R03 still appear and a missing DEFAULT rate still stops the run. Whether a skipped account should be read at all is not stated. The expiry date named in the rule statement is not part of either option. | `InterestCalculator.calculate`, `newBalanceAtBreak`; `intH04_inactiveAccountSkippedWhenSwitched` | business owner |
| INT-R04 / INT-R07 | As coded, an account whose balances all have a zero rate is still rewritten at its break: balance unchanged, cycle credit and cycle debit set to zero. No fixture shows it on an account that is not the last one. | `CBACT04C:194-197, 350-356`; `InterestCalculatorTest.intR04_accountWithOnlyZeroRatesIsStillUpdatedAtItsBreak` | business owner |
| INT-Q06 | As coded, a balance of exactly 0.00 at a non-zero rate also writes a 0.00 transaction; the rule statement speaks only of "interest less than one cent". | `CBACT04C:214-217`; `intQ06_zeroBalanceAtNonZeroRateIsWritten` | business owner |
| INT-Q01 | Under HALF_EVEN the comparator reports two differences on i02, not one: `TRAN-AMT` (12.35 -> 12.36) and the `ACCT-CURR-BAL` of account 00000002003 that follows from it (1247.85 -> 1247.86). Both are listed by the expectation; the ticket's acceptance line names only TRAN-AMT. | switch-test-output.txt, first section | target architect |

## 4. Standards review checklist (`.agents/skills/interest-conversion-standards/SKILL.md`, section 9)

Standards:

- [x] S1.1 Sources used in order; recorded output decides - PASS: ticket, rules, architecture section 8, `CBACT04C:188-222, 415-467`, then the recorded output (`FixtureCasesTest.allSevenCases_outputEqualsTheRecordedLegacyRun`)
- [x] S1.2 Rule/code disagreements reproduced as coded and reported - PASS: section 3 (INT-R04/R07 break, INT-Q06 zero balance); reproduced in `InterestCalculator`
- [x] S2.1 Designed from rules; no method per paragraph - PASS: `InterestCalculator.rateFor` / `defaultRateFor` (INT-R02, R03), `FeeHook` (INT-H01), one policy method per decision
- [x] S2.2 Immutable business records; copybook names only at the mapping - PASS: `domain/Account` (record, `activeStatus`); `ACCT-ACTIVE-STATUS` appears only in `codec/AccountRecord`
- [x] S2.3 Spring Batch shape, transaction and restart boundaries of the architecture - PASS (file adapters): reader, processor and writer of ticket 01 unchanged; `batch/` not touched
- [x] S2.4 Types and return values, no flags or result codes - PASS: fallback is `Optional.orElseGet`, no rate variable outlives a balance (`intR03_noStaleRateAfterAFailedLookup`); skipping is a boolean answer of the policy, no flag field
- [x] S2.5 Constructor injection; no mutable static state - PASS: `InterestCalculator` constructor unchanged; `FeeHook`, `RunTimestamp` hold constants only
- [x] S3.1 `BigDecimal` with data-map precision and scale; no `double`/`float` - PASS: `grep -rnE 'double|float' java/interest/src` finds nothing
- [x] S3.2 Formula: exact product, one division, mode from the policy - PASS: `InterestFormula` unchanged; `InterestFormulaTest`
- [x] S3.3 Rounding mode named on every other arithmetic step - PASS: `InterestCalculator` total and new balance `setScale(2, RoundingMode.UNNECESSARY)`
- [ ] S3.4 Range check before store; overflow goes to the policy - N/A: INT-H05 is excluded from this ticket (ticket 08)
- [x] S3.5 Encodings only in `record-codec` - PASS: the domain compares trimmed text and `BigDecimal`; the test decodes with `codec/Field`
- [x] S3.6 Width, padding and FILLER only in codec and schema - PASS: `DEFAULT` is the trimmed constant in the domain (`intR03_defaultGroupKeyPadded`); `AccountRecord.withInterestPosted` unchanged
- [x] S3.7 Code page and key order each in one place - PASS: not touched (`FileRunWorkspace`)
- [x] S3.8 Clock and run parameters injected; `parmDate` is text - PASS with a proposal: `RunTimestamp.ofCycleDate` slices the PARM text and never parses a date; the cycle date is not a job parameter of its own (section 5, proposal 1)
- [x] S4.1 Every PRESENT behaviour touched has its test; new ones reported - PASS: rows a, d, g, l, m, o, s, x, y, aa below; nothing new found
- [x] S5.1 Each quirk/open decision in one named place behind its switch, rule id in names - PASS: `InterestPolicy.intQ01...` to `intQ06...`, `intH04ChargesAccount`; `FeeHook.intH01FeesAfterInterestOf`; switch names as in architecture.md section 8 (`InterestPolicyTest.fi5_*`)
- [x] S5.2 Nothing fixed, tidied or added without a rule id - PASS: every new branch names INT-R03, R04, Q03, Q05, Q06, H01 or H04; defaults are the legacy behaviour (see section 5, proposal 2 on the wording of S5.2)
- [x] S6.1 Status and message mapping as in section 6 - PASS for the rows this ticket touches: DISCGRP 23 -> the two fallback lines; DEFAULT missing -> `DEFAULT_RATE_MISSING`. Its legacy message lines are ticket 08
- [x] S6.2 One stop exception, one exit-code constant, no skip or retry - PASS: `InterestRunStoppedException` only; `intR08_missingDefaultRateStops`
- [ ] S6.3 Restart as in the architecture - N/A: file adapters, no restart (ticket 07)
- [x] S6.4 Outputs in the recorded format; failed run leaves no SYSTRAN - PASS: seven cases byte for byte; `InterestCalcJobIT.intR08_missingInputFileFailsTheJobAndLeavesNoOutput` still passes
- [x] S6.5 Run log byte for byte; no account data in the application log - PASS: `sysout/INTCALC.STEP15.txt` equal on seven cases; the new exception messages hold no account id or amount
- [x] S7.1 A test per rule id, named for it - PASS: `intR01_*`, `intR02_*`, `intR03_*`, `intR04_*`, `intR05_*`, `intR06_*`, `intR07_*`, `intQ01_*` to `intQ06_*`, `intH01_noFeeTransaction`, `intH04_*`, `intT02_*`, `intT09_*`; each switch has a test for the default and each other value (`InterestPolicyTest`, `PolicySwitchExpectationsTest`). INT-H06 is outside the job and has no test
- [x] S7.2 Fixture and edge cases of each rule covered - PASS: all seven cases; synthetic data for INT-Q06, INT-H04, empty input, missing DEFAULT rate
- [x] S7.3 Integration tests on PostgreSQL 15 in a container - PASS: `InterestCalcJobIT` (20 tests, Testcontainers `postgres:15`)
- [x] S7.4 No assertion against the code's own output - PASS: expected values are the recorded files, `modernization/rules/interest/expected/*.json` (read only) or hand arithmetic in the test comment
- [x] S8.1 No edit under `app/`, `fixtures/`, `tools/parity` - PASS: scope check in section 6
- [x] S8.2 No golden file re-recorded - PASS
- [x] S8.3 Every new dependency explained in the pull request - PASS: none added (section 7)
- [x] S8.4 Frozen shared interfaces unchanged - PASS: `port/` untouched; switch names are those of architecture.md section 8; Makefile untouched
- [x] S8.5 Synthetic data only - PASS: fixture accounts 2001-2010 and their card numbers

Behaviours PRESENT in section 4 (k, n and v are ABSENT and have no line):

- [x] a Truncation without ROUNDED - PASS: `intQ01_truncatesHalfCent`, `intQ01_nonTieUnchanged`, `FixtureCasesTest.intQ01_i02WritesTheTieAs1235`
- [ ] b High-order digit loss (range check, STOP) - N/A: INT-H05, ticket 08
- [x] c Intermediate precision of the COMPUTE - PASS: `intR05_formulaExactProductThenDivide` (unchanged)
- [x] d Zoned overpunch signs - PASS: `FixtureCasesTest.intQ05_negativeInterestIsDSigned` (i06)
- [ ] e Binary items stay out of data and domain - N/A: not touched
- [ ] f Non-numeric data rejected at the boundary - N/A: not touched (`e1_badDigitInABalanceRejectsTheRecord` still passes)
- [x] g Fixed width, padding, STRING - PASS: `FixtureCasesTest.intR03_defaultGroupKeyPadded` (i04); `intR06_transactionFieldsFixedWidth` unchanged
- [ ] h Code page and collating order - N/A: not touched (`intR01_readsInKeyOrder` still passes)
- [ ] i REDEFINES replaced by formatter and function - N/A: not touched; `RunTimestamp.ofCycleDate` is a second formatter, no overlay
- [x] j FILLER and record length round trip - PASS: ACCTDATA byte for byte on seven cases; `e4_accountFillerRoundTrip` unchanged
- [x] l No state carried between accounts except the counter - PASS: `intR07_totalIsPerAccount`, `intQ02_counterRunsAcrossAccounts`; a skipped balance takes no counter value (`intQ05_creditBalanceSkippedWhenSwitched`)
- [x] m Control break and end of file (INT-Q04, INT-T09) - PASS: `intR07_updatesAccountAtBreak`, `intQ04_lastAccountNotUpdated`, `FixtureCasesTest.intT09_emptyInputWritesNothing`, `InterestCalcJobIT.intT09_emptyBalanceInputCompletesAndWritesNoTransaction`
- [x] o Level-88 result codes replaced by types - PASS: `intR03_*`, `intR08_missingDefaultRateStops`
- [ ] p Indexed access through the ports; several cards to the policy - N/A: not touched (`intH03_twoCardsStops` still passes)
- [ ] q File statuses and abend - N/A: ticket 08
- [x] r PARM, generation, disposition, return code - PASS: `rc.json` equal on seven cases
- [x] s DISPLAY output in the run log - PASS: SYSOUT equal on seven cases; i04 for the fallback lines; `FixtureCasesTest.intT02_runLogIsStartRecordsEnd`
- [x] t PARM text against the clock - PASS: `intQ02_idIsParmPlusCounter`, `intQ03_timestampsFromRunClock`; CYCLE_DATE uses the PARM as text
- [x] u Timestamp format and hundredths - PASS: `intT05_*` unchanged; `intQ03_cycleDateWhenSwitched` for the other value
- [ ] w PARM length checked - N/A: not touched
- [x] x No stale rate after a failed lookup - PASS: `intR03_fallsBackToDefault`, `intR03_noStaleRateAfterAFailedLookup`
- [x] y Typed fields where COBOL mixed text and numbers - PASS: `intR06_categoryCodeIs0005` unchanged
- [ ] z Abend ends the step; stub output not reproduced - N/A: ticket 08
- [x] aa Dead and empty code not ported - PASS: `intH01_noFeeTransaction`; `FeeHook` is the hook the ticket asks for and charges nothing (section 5, proposal 3)

## 5. What is wrong or missing in the standards skill (not edited)

1. **S3.8 against INT-Q03 option B.** S3.8 says a frozen business date for `timestampSource=CYCLE_DATE` "is a job
   parameter too", but the register's option is "the cycle date of the PARM", the expectations are computed from the
   PARM (`2022-07-18-00.00.00.000000`), and architecture.md section 4 lists no such parameter. Proposed wording: "With
   `timestampSource=CYCLE_DATE` the timestamp is built from the first eight characters of `parmDate`, as text; a
   PARM that does not start with eight digits stops the run."
2. **S5.2 "Do not" column.** It lists "add an active-status check" as forbidden because it "has no rule id", yet
   INT-H04 option B and the switch `interest.policy.inactiveAccounts` (architecture.md section 8) are exactly that
   check. Proposed: "... add an active-status check outside the INT-H04 switch, or with a default other than CHARGE".
3. **Section 4 row aa against INT-H01.** The row says `1400-COMPUTE-FEES` is not ported; architecture.md section 8
   and ticket 04 ask for "an empty fee hook". Proposed: say the hook is one named domain class that returns no
   transaction, and that this is the only trace of the paragraph.
4. **Section 6 table.** Add a row for `CYCLE_DATE_UNREADABLE` (INT-Q03, only when the switch is not the default) and
   rename nothing else; `DEFAULT_RATE_MISSING` is now raised by the code as the table has it.
5. **Section 4 row m.** `intT09_emptyInputWritesNothing` is marked "no fixture"; say that the test builds its input
   from a fixture's other three files and an empty balance file, so later tickets do it the same way.

Skill corrections (interest-ticket-build, interest-ticket-verify, interest-pitfalls):

1. **interest-ticket-verify step 8** names three evidence files. A ticket whose Verification asks for more ("the
   switch-test output" here) has no place named for it; this ticket wrote `switch-test-output.txt` next to the three.
   Proposed: "further output a ticket's Verification names goes into the same directory under a name that says what it is".
2. **interest-ticket-verify step 8, `parity-report.sh`.** When every case is named on the command line the "Why"
   column is left empty, not `_fill in_`; the text of the step should say so.
3. **interest-ticket-build step 5** (layout table) does not say that the module has no JSON library at compile
   scope. Tests that read `modernization/rules/interest/expected/*.json` can use `org.json`, which
   `spring-boot-starter-test` brings, without a new dependency. Worth one line in the table.
4. **interest-pitfalls**: add the failure of section 6, item 1 (the record echo is 50 bytes with trailing spaces).

## 6. Method: commands run and failures hit

```sh
export JAVA_HOME="$(dirname "$(dirname "$(update-alternatives --list java | grep -m1 -- '-21-')")")"
git fetch origin && git ls-remote --heads origin 'demo/*' && gh pr list --state open      # no branch or PR for ticket 04
git checkout -b demo/05-04-calculation-rules origin/demo/05b-integration
mvn -B -f java/interest/pom.xml verify                                                    # 205 + 20, BUILD SUCCESS
make parity-java                                                                          # 7 PASS, exit 0
git checkout -- tools/parity/__pycache__
git diff --stat origin/demo/05b-integration -- app fixtures tools modernization .agents db docker-compose.yml   # empty
git commit   # a50a098, then the evidence from that revision:
mvn -B -f java/interest/pom.xml clean verify      > java/interest/evidence/04-calculation-rules/test-output.txt
.agents/skills/interest-ticket-verify/parity-report.sh 04-calculation-rules $(ls fixtures/interest)   # exit 0
# switch-test-output.txt: interest-ticket-verify step 4 once per switch value, compare.py on each, the empty input, psql
```

Failures hit:

1. **Symptom.** `FixtureCasesTest.intT02_runLogIsStartRecordsEnd` failed: expected `000000020010100010000010000{`, was
   the same followed by 22 spaces. **Cause.** My test compared the echo with the lines of the input file, which the
   fixture stores without trailing spaces; the legacy DISPLAY writes the 50-byte record (`CBACT04C:193`). **Fix.** The
   test now states the 50-byte image by hand. The code was right.
2. **Symptom.** `mvn -o ... test` ended with `Plugin org.apache.maven.plugins:maven-failsafe-plugin:3.5.4 ... could
   not be resolved ... in offline mode`. **Cause.** I had added `-o`; the local repository did not hold the plugin.
   **Fix.** Run without `-o`, as the skill says.
3. No JSON library at compile or test scope besides `org.json` and json-path (from `spring-boot-starter-test`); the
   switch test uses `org.json`, so no dependency was added.

Nothing in `interest-pitfalls` P1-P7 was hit beyond P6 (the `.pyc` file, undone before each commit).

## 7. Dependencies (S8.3)

None added. `pom.xml` is unchanged.
