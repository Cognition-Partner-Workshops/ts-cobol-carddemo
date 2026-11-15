# Ticket 04 calculation rules - report

Ticket: `modernization/design/tickets/04-calculation-rules.md` (INT-T04). Branch `demo/05-04-calculation-rules`, started from
`demo/05b-integration` at `7634342`. Code revision the evidence was produced from: `01b9e94`.

Read this with [parity-report.md](parity-report.md), [test-output.txt](test-output.txt) and
[switch-test-output.txt](switch-test-output.txt).

## 1. Result

| Acceptance line of the ticket | Result | Where it is shown |
|---|---|---|
| `make parity-java` on the seven cases: compare.py reports PASS for 7 of 7 | met | parity-report.md, "Result per case"; `InterestCalcJobIT.intT02_everyCaseMatchesTheRecordedRun` |
| i02 writes TRAN-AMT 12.35 for 00000002003/01/0001 (INT-Q01); i03 writes no transaction for the zero-rate balance (INT-R04); i04 logs the two fallback lines (INT-R03); i06 writes a negative amount (INT-Q05) | met | parity of i02, i03, i04, i06; `PolicySwitchFixtureTest.allSwitches_defaultGivesTheAsCodedValuesAndTheRecordedFiles`; `InterestCalcJobIT.intR03_fallbackLinesAreInTheRunLog` |
| `rounding=HALF_EVEN`: i02 fails on TRAN-AMT, i07 still passes; with the default both pass | met | switch-test-output.txt (first four runs); `PolicySwitchFixtureTest.intQ01_halfEvenChangesExactlyTheListedValues` |
| updateLastAccount, idPrefix, timestampSource, negativeBalance, zeroInterest change exactly the values `expected/<case>.json` lists | met | `PolicySwitchFixtureTest` (9 tests x 7 cases: every field of every transaction and account and the run log, per option); switch-test-output.txt for one case each through the job |
| inactiveAccounts changes exactly the listed values | **met in the calculation only, not through the job** | `PolicySwitchFixtureTest.intH04_skipInactiveAccountsChangesNothingInTheCases` passes with the active flag supplied by the test. Through the job `inactiveAccounts=SKIP` stops with exit 12 (switch-test-output.txt, last run): the frozen `codec/AccountRecord` does not supply the flag. See section 2 |
| An empty balance input writes no transaction, touches no account and ends COMPLETED (INT-T09) | met | `InterestCalcJobIT.intT09_emptyInputWritesNothing` |
| Database assertions: none | n/a | - |
| `mvn -q -f java/interest/pom.xml verify` | met: 142 unit tests and 20 integration tests, 0 failures (base: 60 and 12) | test-output.txt |
| One unit test per rule id and per switch, named by the id | met | section 4, S7.1 |

Parity per case: all seven PASS (i03 and i04 failed on the base branch and pass now; the five others still pass).

Not touched: `app/`, `fixtures/`, `tools/`, `modernization/`, `.agents/`, `db/`, `docker-compose.yml`, `Makefile`,
`.github/`, `java/interest/pom.xml`, the ports, `batch/`, `file/`, `codec/`, `src/main/resources`
(`git diff --stat origin/demo/05b-integration -- app fixtures tools modernization .agents db docker-compose.yml Makefile .github java/interest/pom.xml` is empty).
No golden file was re-recorded. No ticket system was written to. Nothing was merged.

## 2. What the ticket does not cover (gaps)

| Gap | State | Owner |
|---|---|---|
| `inactiveAccounts=SKIP` (INT-H04 option B) through the job | The calculation is built and tested. The job stops with `ACTIVE_FLAG_NOT_SUPPLIED (INT-H04)` because `codec/AccountRecord.decode` (frozen for this ticket) builds `Account` without the active flag. **Change requested:** `AccountRecord.decode` passes `Optional.of("Y".equals(ACCT-ACTIVE-STATUS))` as the fourth component of `Account` (data-map row CVACT01Y/ACCT-ACTIVE-STATUS, offset 11, length 1) | ticket 03 or the coordinator |
| `application.yml` does not list the three new switches | `src/main/resources` is outside the Allowed files. The defaults come from `InterestPolicy` itself and are tested (`InterestPolicyBindingTest`). Lines to add under `interest.policy`: `negativeBalance: CHARGE`, `zeroInterest: WRITE`, `inactiveAccounts: CHARGE` | coordinator |
| INT-Q06 has no fixture | Tested with synthetic data: `intQ06_zeroInterestStillWritten` (0.50 x 12.00 / 1200 = 0.005, written as 0.00), `intQ06_zeroInterestSkippedWhenSwitched`, and `PolicySwitchFixtureTest.intQ06_skipZeroInterestChangesNothingInTheCases` | mainframe application owner for a recorded run |
| INT-H04 has no fixture | Synthetic: `intH04_inactiveAccountChargedLikeAnyOther`, `intH04_inactiveAccountSkippedWhenSwitched`, `intH04_skipWithoutTheFlagStops` | same |
| INT-T09 has no fixture | Synthetic: the i01 input with an empty balance file, through the job | same |
| Legacy messages of the stops (INT-R08), INT-H03, INT-H05 | excluded work | ticket 08 |
| JDBC | excluded work | ticket 06 |
| rerunGuard, multiCard, overflow switches | not in the ticket's switch list | tickets 06 and 08 |

Where a skill step did not apply: interest-ticket-verify step 7 (database assertions) - the ticket has none; it was
not run beyond the integration tests the pilot already has. Step 6 (error path) - the ticket's error line is the
empty input, shown by the integration test instead of a command-line run.

## 3. For the decision register

1. **INT-Q03 option B and standard S3.8 disagree on where the cycle date comes from.** `rules.yaml` INT-Q03 option B is "the cycle date of the PARM at 00.00.00" and every `expected/<case>.json` gives `2022-07-18-00.00.00.000000` for PARM `2022071800`. S3.8 says the PARM is never read as a date and the business date is its own job parameter, which `architecture.md` section 4 does not list. Built as the rule says (`InterestPolicy.intQ03CycleDate`), non-default only. Suggested owner: target architect.
2. **INT-H04 option B cannot run through the job.** `inactiveAccounts=SKIP` needs `ACCT-ACTIVE-STATUS`, which `codec/AccountRecord.decode` does not map (data map: "carried, never read"). The domain is built and tested; through the job the option stops with `ACTIVE_FLAG_NOT_SUPPLIED (INT-H04)`, exit 12 (`switch-test-output.txt`). Also open for the business: the rule says "active flag is not Y"; the expiry date is named in the rule's statement but not in option B, so it is not looked at. Suggested owner: target architect (codec), business owner (expiry).
3. **Behaviour not in section 4 of the standards: an account whose balances all have a zero rate is still rewritten at the break.** `CBACT04C:194-201, 350-356` add a total of 0.00 and set cycle credit and cycle debit to zero. Reproduced (`intR04_accountWithOnlyZeroRatesIsStillUpdatedAtTheBreak`); no fixture shows it (in i03 the account is the last one). Suggested owner: mainframe application owner.
4. **What SKIP means for the lookups is not stated by the rules.** Built as: INT-Q05 SKIP still looks up the rate (so the two fallback lines still appear) and then writes nothing; INT-Q06 SKIP adds nothing to the account; INT-H04 SKIP echoes the balance records, does no card or rate lookup, writes no transaction and does not rewrite the account. A skipped balance or account uses no transaction number. Suggested owner: business owner with the decision on each rule.
5. **INT-Q06 and INT-H04 have no fixture; INT-T09 has none either.** Tested with synthetic data only (section 2). A recorded legacy run of a sub-cent balance, an inactive account and an empty TCATBALF would settle them. Suggested owner: mainframe application owner.
6. **A missing DEFAULT rate stops without the legacy lines.** `DEFAULT_RATE_MISSING (INT-R08)` is raised after the two fallback lines; `ERROR READING DEFAULT DISCLOSURE GROUP`, the status line and `ABENDING PROGRAM` are ticket 08.

## 4. Standards review checklist (`.agents/skills/interest-conversion-standards/SKILL.md`, section 9)

Standards:

- [x] S1.1 Sources used in order; recorded output decides - **PASS**. Ticket and rules first, then architecture section 8, then `CBACT04C:188-222, 415-468`; the recorded output decides (`InterestCalcJobIT.intT02_everyCaseMatchesTheRecordedRun`, parity 7 of 7).
- [x] S1.2 Rule/code disagreements reproduced as coded and reported - **PASS**. Reproduced as coded; findings in section 3 (INT-Q03 option B against S3.8, INT-H04 flag, zero-rate account still updated at the break).
- [x] S2.1 Designed from rules; no method per paragraph - **PASS**. `InterestCalculator.rateFor` / `intR03DefaultRate` / `interestOf` / `intH01Fees` / `intH04Charged`; no paragraph names.
- [x] S2.2 Immutable business records; copybook names only at the mapping - **PASS**. `Account` stays a record (one optional component added); no copybook name in `domain` or `policy`. Copybook names appear in `PolicySwitchFixtureTest` only as keys of the expectation files.
- [x] S2.3 Spring Batch shape, transaction and restart boundaries of the architecture - **N/A**. `batch/**` is outside this ticket's Allowed files and unchanged; the calculation still takes one account per item and uses only the ports.
- [x] S2.4 Types and return values, no flags or result codes - **PASS**. A missing rate is `Optional.empty()`, "no interest for this balance" is `Optional.empty()` from `interestOf`, a stop is `InterestRunStoppedException`.
- [x] S2.5 Constructor injection; no mutable static state - **PASS**. `InterestCalculator` constructor unchanged; `InterestPolicy` is a record.
- [x] S3.1 `BigDecimal` with data-map precision and scale; no `double`/`float` - **PASS**. `grep -rnE '\b(double|float)\b' java/interest/src` returns nothing.
- [x] S3.2 Formula: exact product, one division, mode from the policy - **PASS**. `InterestFormula` unchanged; `intQ01_*` tests, `PolicySwitchFixtureTest.intQ01_halfUp...`, `intQ01_halfEven...`.
- [x] S3.3 Rounding mode named on every other arithmetic step - **PASS**. Sums use `setScale(2, RoundingMode.UNNECESSARY)` (`InterestCalculator.calculate`, `newBalanceAtBreak`).
- [ ] S3.4 Range check before store; overflow goes to the policy - **N/A**. INT-H05 is excluded work of this ticket (ticket 08). No range check was added or removed.
- [x] S3.5 Encodings only in `record-codec` - **PASS** for main code (nothing added outside `codec`). The test `PolicySwitchFixtureTest.amount` decodes an overpunch itself, on purpose, so the expectation is not read through the code under test.
- [x] S3.6 Width, padding and FILLER only in codec and schema - **PASS**. The DEFAULT group is the trimmed constant in the domain (`intR03_defaultGroupKeyPadded`); padding stays in the codec.
- [x] S3.7 Code page and key order each in one place - **N/A**. `file/**` and `codec/**` are not touched.
- [ ] S3.8 Clock and run parameters injected; `parmDate` is text - **FAIL for the non-default option CYCLE_DATE only; PASS for the default**. Default: clock injected, PARM used as text. `timestampSource=CYCLE_DATE` reads the first eight characters of the PARM as a date (`InterestPolicy.intQ03CycleDate`), because option B of INT-Q03 and the expectation files define it as "the cycle date of the PARM at 00.00.00" and no cycle-date job parameter exists (`batch/**` is frozen for this ticket). Proposal in section 5.
- [x] S4.1 Every PRESENT behaviour touched has its test; new ones reported - **PASS**. Rows a, c, g, l, m, o, s, t, x, aa have the tests named in the list below; one behaviour not in the table is reported in section 3 (finding 3).
- [x] S5.1 Each quirk/open decision in one named place behind its switch, rule id in names - **PASS**. `InterestPolicy.intQ01...` to `intQ06WritesInterest`, `intH04ChargesAccount`, each called once in `InterestCalculator`; INT-H01 is `InterestCalculator.intH01Fees`.
- [x] S5.2 Nothing fixed, tidied or added without a rule id - **PASS**. 0.00 is written (INT-Q06), inactive accounts are charged (INT-H04), no fee (INT-H01); every default is the as-coded value (`InterestPolicyBindingTest.fi5_noPropertyGivesTheLegacyBehaviourOfEverySwitch`).
- [ ] S6.1 Status and message mapping as in section 6 - **N/A** except the DISCGRP status 23 row, which is **PASS** (`intR03_fallsBackToDefault`, i04 parity). The messages of the stops are ticket 08; `DEFAULT_RATE_MISSING` stops without the legacy lines.
- [x] S6.2 One stop exception, one exit-code constant, no skip or retry - **PASS** with one remark: an unreadable PARM under `CYCLE_DATE` raises `IllegalArgumentException` from the policy (the step still fails, exit 12), because `policy` does not depend on `domain`.
- [ ] S6.3 Restart as in the architecture - **N/A**. File adapters, no restart state; not in this ticket.
- [x] S6.4 Outputs in the recorded format; failed run leaves no SYSTRAN - **PASS**, unchanged from the pilot: seven cases byte for byte; `switch-test-output.txt` shows a stopped run leaves no output.
- [x] S6.5 Run log byte for byte; no account data in the application log - **PASS**. SYSOUT of all seven cases equals the recorded one; no log statement was added.
- [x] S7.1 A test per rule id, named for it - **PASS**. INT-R01 to R07, Q01 to Q06, H01, H04, T02, T09 each have tests named by the id; each switch has a default test and a test of every other value. INT-H06 is outside the job and has no test. INT-R08, H02 (pilot's test kept), H03, H05 belong to other tickets.
- [x] S7.2 Fixture and edge cases of each rule covered - **PASS**. All seven cases; synthetic data for INT-Q06, INT-H04 and INT-T09, which have no fixture.
- [x] S7.3 Integration tests on PostgreSQL 15 in a container - **PASS**. `InterestCalcJobIT` on Testcontainers `postgres:15`, 20 tests.
- [x] S7.4 No assertion against the code's own output - **PASS**. Expected values are the recorded files, the expectation files under `modernization/rules/interest/expected` (derived from rules and inputs) or hand arithmetic in the test comment.
- [x] S8.1 No edit under `app/`, `fixtures/`, `tools/parity` - **PASS**. The scope `git diff --stat` is empty.
- [x] S8.2 No golden file re-recorded - **PASS**.
- [x] S8.3 Every new dependency explained in the pull request - **PASS**. None added; `pom.xml` unchanged. The test uses `json-path`, already on the test classpath through `spring-boot-starter-test`.
- [ ] S8.4 Frozen shared interfaces unchanged - **PASS**, with one change requested and not made: `codec/AccountRecord.decode` must supply the active flag (section 2).
- [x] S8.5 Synthetic data only - **PASS**. Accounts 2001-2010 and the card numbers of the fixtures.

Behaviours PRESENT in section 4:

- [x] a Truncation without ROUNDED - **PASS**. `intQ01_truncatesHalfCent`, `intQ01_nonTieUnchanged` (pilot), i02 and i07 parity.
- [ ] b High-order digit loss (range check, STOP) - **N/A**. INT-H05, ticket 08.
- [x] c Intermediate precision of the COMPUTE - **PASS**. `intR05_formulaExactProductThenDivide` (pilot), unchanged.
- [ ] d Zoned overpunch signs - **N/A**. Codec, ticket 03. i06 passes parity with a D-signed amount.
- [ ] e Binary items stay out of data and domain - **N/A**. Codec, ticket 03.
- [ ] f Non-numeric data rejected at the boundary - **N/A**. Codec, ticket 03.
- [x] g Fixed width, padding, STRING - **PASS** for the part of this ticket: `intR03_defaultGroupKeyPadded`. The rest is the codec's.
- [ ] h Code page and collating order - **N/A**. File adapter and codec.
- [ ] i REDEFINES replaced by formatter and function - **N/A**. Not touched (`intT05_*` of the pilot still pass).
- [ ] j FILLER and record length round trip - **N/A**. Codec; ACCTDATA passes parity in all seven cases.
- [x] l No state carried between accounts except the counter - **PASS**. `intR07_totalIsPerAccount`, `intQ02_counterRunsAcrossAccounts`, `intH04_inactiveAccountSkippedWhenSwitched` (a skipped account uses no number).
- [x] m Control break and end of file (INT-Q04, INT-T09) - **PASS**. `intQ04_lastAccountNotUpdated`, `PolicySwitchFixtureTest.intQ04_updateLastAccountChangesExactlyTheListedValues`, `InterestCalcJobIT.intT09_emptyInputWritesNothing`.
- [x] o Level-88 result codes replaced by types - **PASS**. `intR03_*` tests: `Optional` from `RateLookup`, no result code.
- [ ] p Indexed access through the ports; several cards to the policy - **N/A**. INT-H03 is ticket 08; ports unchanged.
- [ ] q File statuses and abend - **N/A**. Ticket 08.
- [ ] r PARM, generation, disposition, return code - **N/A**. Not touched; `rcJson_matchesRecorded` still passes.
- [x] s DISPLAY output in the run log - **PASS**. `sysout_matchesRecorded`, `intR03_fallbackLinesAreInTheRunLog`, `intT02_everyCaseMatchesTheRecordedRun` (i04 for the fallback lines).
- [x] t PARM text against the clock - **PASS** for the default (`intQ02_idIsParmPlusCounter`, `intQ03_timestampsFromRunClock`); see S3.8 for CYCLE_DATE.
- [ ] u Timestamp format and hundredths - **N/A**. `RunTimestamp` not touched.
- [ ] w PARM length checked - **N/A**. `batch/**`, not touched.
- [x] x No stale rate after a failed lookup - **PASS**. `intR03_fallsBackToDefault`, `intR03_noStaleRateAfterAFailedLookup`.
- [ ] y Typed fields where COBOL mixed text and numbers - **N/A**. Not touched (`intR06_categoryCodeIs0005` of the pilot still passes).
- [ ] z Abend ends the step; stub output not reproduced - **N/A**. Ticket 08.
- [x] aa Dead and empty code not ported - **PASS**. `intH01_noFeeTransaction`; the fee hook returns nothing and line 219-220 has no counterpart.

## 5. What is wrong or missing in the standards skill (not edited)

1. **S3.8 against INT-Q03 option B.** S3.8 forbids reading the PARM as a date and asks for a separate job parameter;
   the rule and the expectation files define option B from the PARM. Proposed: either S3.8 allows the policy to read
   the first eight characters of the PARM as the cycle date under `CYCLE_DATE` only, or `architecture.md` section 4
   gains a `cycleDate` job parameter and the expectation files say where 2022-07-18 comes from.
2. **Section 4 has no row for the account break of an account with no interest.** Proposed row: "account whose
   balances all have rate zero is rewritten with total 0.00 and cycle amounts reset (`CBACT04C:194-201, 350-356`)",
   test `intR04_accountWithOnlyZeroRatesIsStillUpdatedAtTheBreak`.
3. **S5.1 does not say what a SKIP option does to lookups, run-log lines and the transaction counter.** Proposed: one
   sentence per switch (finding 4 of section 3).
4. **Section 4 row aa names `intH01_noFeeTransaction` but section 8 of the architecture calls INT-H01 "an empty fee
   hook".** S5.2 says empty code is not ported. Built as one private method returning no transaction; proposed: say
   whether a hook is wanted at all.
5. **Section 6 names `DEFAULT_RATE_MISSING` as a proposal for ticket 08, but ticket 04 needs a stop for it** once the
   fallback exists. Added here without the legacy lines; proposed: say so in the row.

## 6. Method: commands run and failures hit

```sh
export JAVA_HOME="$(dirname "$(dirname "$(update-alternatives --list java | grep -m1 -- '-21-')")")"
git fetch origin && git checkout -b demo/05-04-calculation-rules origin/demo/05b-integration
git ls-remote --heads origin 'demo/*'; gh pr list --state open ...      # no branch or PR for ticket 04
mvn -B -f java/interest/pom.xml verify                                   # 142 + 20, BUILD SUCCESS
make parity-java                                                         # 7 of 7 PASS
git checkout -- tools/parity/__pycache__; git diff --stat origin/demo/05b-integration -- app fixtures tools ...   # empty
# evidence, from the committed revision 01b9e94
mvn -B -f java/interest/pom.xml clean verify  > test-output.txt
.agents/skills/interest-ticket-verify/parity-report.sh 04-calculation-rules i01-baseline ... i07-nontie-rounding
# switch runs through the job, interest-ticket-verify step 4, one option each -> switch-test-output.txt
```

Failures hit:

| Symptom | Cause | Fix |
|---|---|---|
| `NullPointerException ... Map.get(Object) is null` in `PolicySwitchFixtureTest` | Spring Boot's `BasicJsonParser` does not read the nested, indented expectation files | read them with `json-path` (already on the test classpath); no new dependency |
| `i01-baseline run log ==> expected ... "000000020010100010000010000{" but was "...{   (spaces)"` | the expectation files give the record echo without its trailing spaces; the recorded SYSOUT has the full 50 bytes | the switch test compares the run log with trailing spaces removed; the byte-for-byte comparison is against the recorded SYSOUT |
| `mvn -o` fails: `maven-failsafe-plugin:3.5.4 ... has not been downloaded` | offline mode on a machine that had not built the module | run without `-o` |
| pitfall P6 (`tools/parity/__pycache__` changed) | as described | `git checkout -- tools/parity/__pycache__` |

## 7. Dependencies (S8.3)

None added. `java/interest/pom.xml` is unchanged.
