# Ticket 08 error paths and stop conditions - report

Ticket: `modernization/design/tickets/08-error-paths.md` (INT-T08). Branch `demo/05-g2-logic`, third ticket of group
g2-logic, after tickets 04 and 06 on the same branch. Code revision the evidence was produced from: `d342b28`.

Read this with [test-output.txt](test-output.txt) and [parity-report.md](parity-report.md).

**No parity is claimed for any path of this ticket.** No fixture has an abend; every test here uses synthetic data.
The run-log lines for INT-R08 are the DISPLAY texts of `app/cbl/CBACT04C.cbl`, read from the source, not from a
recorded run. What CBACT04C does on the host with several cards (INT-H03) or a value larger than its field (INT-H05)
is not determined by the source and stays unknown.

## 1. Result

| Acceptance line of the ticket | Result | Where it is shown |
|---|---|---|
| A balance whose account is missing: step FAILED, non-zero exit code, the log holds ACCOUNT NOT FOUND and the account id (INT-R08) | met: exit code 12; run log `ACCOUNT NOT FOUND: 00000002002`, `ERROR READING ACCOUNT FILE`, `FILE STATUS IS: NNNN0023`, `ABENDING PROGRAM` | `ErrorPathsJdbcIT.intR08_missingAccountStops`, `StopConditionsTest.intR08_missingAccountStops` |
| A balance with no rate for its group and none for DEFAULT: step FAILED (INT-R08) | met | `ErrorPathsJdbcIT.intR08_defaultRateMissingStops`, `StopConditionsTest.intR08_defaultRateMissingStops` |
| An account with two cards: step FAILED naming INT-H03; with multiCard=FIRST_BY_CARD_NUM the lower card number is used | met | `ErrorPathsJdbcIT.intH03_twoCardsStops`, `MultiCardSwitchJdbcIT.intH03_firstByCardNumberWhenSwitched` (switch set by property), `StopConditionsTest.intH03_*` |
| Interest above 999999999.99: step FAILED naming INT-H05; nothing is truncated | met | `ErrorPathsJdbcIT.intH05_interestAboveFieldStops`, `StopConditionsTest.intH05_interestAboveFieldStops`, `intH05_largestInterestThatFitsIsWritten` |
| A 1,000,000th transaction in one run: step FAILED naming INT-H05 | met in the calculation only: a counter that has issued 999999 ids stops the run. Not run through the step with a million rows (section 2) | `StopConditionsTest.intH05_millionthTransactionStops`, `intH05_transaction999999IsTheLastThatFits` |
| Database assertions: accounts committed before the failing chunk keep their update; the failing account has neither transactions nor an update; interest_run.status is FAILED | met for every condition run through the step: account 2001 keeps 1010.00 and its transaction; accounts 2002 (failing, not the last) and 2003 keep 2400.00 and have no transaction | `ErrorPathsJdbcIT.assertStoppedAtAccount2002` |
| Build: `mvn -q -f java/interest/pom.xml verify` | met: 299 unit and 43 integration tests, 0 failures, BUILD SUCCESS (before this ticket: 282 and 36) | test-output.txt |
| Tests: one test per condition | met, see the table below | |
| Fixture cases: the seven cases still pass | met, 7 of 7 PASS | parity-report.md |

Conditions and their tests (the test summary of test-output.txt lists `StopConditionsTest` 17 tests, `ErrorPathsJdbcIT`
6, `MultiCardSwitchJdbcIT` 1):

| Condition | Stop reason, rule | Run-log lines after the record (CBACT04C lines) | Calculation test (`StopConditionsTest`) | Through the step on PostgreSQL |
|---|---|---|---|---|
| account missing | ACCOUNT_MISSING, INT-R08 | `ACCOUNT NOT FOUND: <id>`, `ERROR READING ACCOUNT FILE`, `FILE STATUS IS: NNNN0023`, `ABENDING PROGRAM` (375, 386, 643-646, 629) | `intR08_missingAccountStops` | `ErrorPathsJdbcIT.intR08_missingAccountStops` |
| card missing | CARD_MISSING, INT-R08 | `ACCOUNT NOT FOUND: <id>`, `ERROR READING XREF FILE`, status 23, abend line (396, 408) | `intR08_missingCardStops` | `intR08_missingCardStops` |
| no rate, none for DEFAULT | DEFAULT_RATE_MISSING, INT-R08 | the two fallback lines, `ERROR READING DEFAULT DISCLOSURE GROUP`, status 23, abend line (418-419, 455) | `intR08_defaultRateMissingStops` | `intR08_defaultRateMissingStops` |
| several cards | MULTI_CARD, INT-H03 | `INT-H03: THE ACCOUNT HAS SEVERAL CARDS`, abend line (no legacy text exists) | `intH03_twoCardsStops`, `intH03_firstByCardNumberWhenSwitched`, `intH03_defaultIsStop` | `intH03_twoCardsStops`; `MultiCardSwitchJdbcIT` |
| interest, account total or new balance above its field | OVERFLOW, INT-H05 | `INT-H05: A VALUE DOES NOT FIT ITS FIELD`, abend line (no legacy text exists) | `intH05_interestAboveFieldStops`, `intH05_accountTotalAboveFieldStops`, `intH05_newBalanceAboveFieldStops` | `intH05_interestAboveFieldStops` |
| 1,000,000th transaction | OVERFLOW, INT-H05 | as above | `intH05_millionthTransactionStops` | - |
| a transaction cannot be written | WRITE_TRANSACTION, INT-R08 | `ERROR WRITING TRANSACTION RECORD`, abend line (510) | - | `JdbcCalcStepIT.intH02_failedRunRestartsWithoutChargingTwice` |
| an account cannot be updated | UPDATE_ACCOUNT, INT-R08 | `ERROR RE-WRITING ACCOUNT FILE`, abend line (365) | - | `ErrorPathsJdbcIT.intR08_accountUpdateFailureStops`, `JdbcCalcStepIT.intR07_insertsAndUpdateOfAnAccountCommitTogether` |
| a file cannot be opened | OPEN_INPUT with the file, INT-R08, INT-T10 | the text of the file - `ERROR OPENING DALY REJECTS FILE` for the rates, as coded - and the abend line (245, 263, 281, 300, 318) | `intR08_openFailureTextOfEachFile`, `intT10_openFailureOfTheRatesKeepsTheCopiedText` | - (section 2) |
| status line format | INT-T08 | `FILE STATUS IS: NNNN00` + status; no status line where the legacy status is not determined | `intT08_statusLineFormat`, `intT08_noStatusLineWhereTheLegacyStatusIsNotDetermined` | the tests above |
| exit code | INT-T03 | one constant, 12 (`InterestExitCode.STOPPED`, REL-10 open) | - | `ErrorPathsJdbcIT.assertStoppedAtAccount2002` |

Changed: `domain/InterestCalculator` (stops go through `RunStop`; INT-H03 switch; range checks), `domain/StopReason`
(legacy message and status per reason; new READ_ACCOUNT, READ_CARD, READ_RATE, READ_BALANCES, UPDATE_ACCOUNT,
WRITE_TRANSACTION), `domain/InterestRunStoppedException` (carries the file of OPEN_INPUT), `domain/TransactionCounter`
(starting value, `isExhausted`), `batch/JdbcCalcInterestRun` (database failures become the stop exception),
`policy/InterestPolicy` and `application.yml` (switches `multiCard`, `overflow`; granted). New: `domain/RunStop`,
`domain/LegacyFile`, `domain/FieldRange`, `domain/AccountIdText`. No retry or skip logic.

## 2. What the ticket does not cover (gaps)

| Gap | State in this branch | Owner |
|---|---|---|
| **Host behaviour for INT-H03 and INT-H05** | unknown. The target stops (proposed default). The two run-log lines starting `INT-H03:` and `INT-H05:` are the target's own; the legacy has no text for them. No parity is claimed | business owner (INT-H03, G08), (INT-H05) |
| `overflow=WIDEN` | not built. The switch exists; the value is refused when the policy is built (`IllegalArgumentException` naming INT-H05), because wider fields need other record layouts (FI-1) and columns (FI-4) than the frozen ones | INT-H05 decision, then a ticket that may change FI-1/FI-4 |
| 1,000,000th transaction through the step | shown at the calculation only; a run with a million transactions was not made | 09 (gate) may add a volume test |
| Stops of the file adapters | `file/FileRunWorkspace` is outside this ticket's Allowed files: an input file that cannot be opened or an output that cannot be written still stops with OPEN_INPUT / WRITE_OUTPUT, but without the legacy `ERROR OPENING ...` line in the run log and without the file in the exception. Whether a stopped file run keeps its run log was not checked | 07 / 09 |
| Open, read and close failures in jdbc mode | built for: the balance cursor cannot be opened (`ERROR OPENING TRANSACTION CATEGORY BALANCE`), a balance, account, card or rate read fails (`ERROR READING ...`). Only the texts are tested (`StopConditionsTest`); none of these was forced through the step - only the two write failures were. The legacy close errors (CBACT04C:522-611) have no counterpart: the tables are not closed | 09 |
| File status in the status line | written only for the three conditions that determine it (record not found, 23). A database failure gets no `FILE STATUS IS` line, so the run log of such a stop is shorter than the legacy's | REL-10 owner |
| Lines after `ABENDING PROGRAM` | none written; the off-host stub's line is not reproduced (standards section 4, row z). What the host writes there is not in the source | REL-10 owner |
| Exit code | 12, unchanged, one constant; REL-10 is open | mainframe operations owner |
| Run log of a stopped run | holds the lines up to the stop, including those of the rolled-back chunk; a restart cuts them off (ticket 06) | - |

## 3. For the decision register

| Finding | Evidence | Suggested owner |
|---|---|---|
| A missing card displays `ACCOUNT NOT FOUND:` followed by the account id - the same text as a missing account (CBACT04C:396). Reproduced as coded | `StopConditionsTest.intR08_missingCardStops` | - (as coded) |
| The account id appears in the run log (`ACCOUNT NOT FOUND: <id>`), as in the legacy and as the ticket asks; it does not appear in the stop exception or the application log (S6.5) | `StopConditionsTest.intR08_missingAccountStops`, `ErrorPathsJdbcIT.assertStoppedAtAccount2002` | data owner: the run log is account data |
| INT-H05 is checked at four stores: interest of a balance, interest total of an account (WS-TOTAL-INT), new account balance (ACCT-CURR-BAL), transaction counter. The ticket names two; the other two overflow the same way in the source (CBACT04C:352, 467) | `StopConditionsTest.intH05_*` | business owner (INT-H05) |
| With `multiCard=STOP` the run stops at the first account with several cards, after earlier accounts were committed: a part-run is left that needs a restart after the data are corrected | `ErrorPathsJdbcIT.intH03_twoCardsStops` | business owner (INT-H03) |

## 4. Standards review checklist

Standards:

- [x] S1.1 Sources used in order; recorded output decides - PASS: no recorded output exists for these paths; texts taken from CBACT04C and cited by line in the tests
- [x] S1.2 Rule/code disagreements reproduced as coded and reported - PASS: section 3 (missing card text, INT-T10)
- [x] S2.1 Designed from rules; no method per paragraph - PASS: `RunStop`, `FieldRange`, `LegacyFile`
- [x] S2.2 Immutable business records; copybook names only at the mapping - PASS: no record changed
- [x] S2.3 Spring Batch shape, transaction and restart boundaries of the architecture - PASS: unchanged; a stop rolls back the chunk (`ErrorPathsJdbcIT`)
- [x] S2.4 Types and return values, no flags or result codes - PASS: `StopReason` enum, `LegacyFile` as data of the exception
- [x] S2.5 Constructor injection; no mutable static state - PASS: `TransactionCounter(issuedSoFar)` is step state
- [x] S3.1 `BigDecimal` with data-map precision and scale; no `double`/`float` - PASS: `FieldRange.fits`
- [x] S3.2 Formula: exact product, one division, mode from the policy - PASS: `InterestFormula` untouched; seven cases pass
- [x] S3.3 Rounding mode named on every other arithmetic step - PASS: `setScale(2, UNNECESSARY)` kept on the sums
- [x] S3.4 Range check before store; overflow goes to the policy - PASS: `InterestCalculator.stored`, `intH05StopsOnOverflow`; `StopConditionsTest.intH05_*`
- [x] S3.5 Encodings only in `record-codec` - PASS: none added
- [x] S3.6 Width, padding and FILLER only in codec and schema - FAIL in part: the 11 digits of the account id in a text are formatted in `domain/AccountIdText` (as the pilot did for TRAN-DESC), not in the codec, which this ticket may not change
- [x] S3.7 Code page and key order each in one place - N/A
- [x] S3.8 Clock and run parameters injected; `parmDate` is text - PASS: unchanged
- [x] S4.1 Every PRESENT behaviour touched has its test; new ones reported - PASS: rows b, p, q, s, z below
- [x] S5.1 Each quirk/open decision in one named place behind its switch, rule id in names - PASS: `InterestPolicy.intH03StopsOnSeveralCards`, `intH05StopsOnOverflow`
- [x] S5.2 Nothing fixed, tidied or added without a rule id - PASS: INT-T10 text kept; no retry, no skip
- [x] S6.1 Status and message mapping as in section 6 - PASS: `StopReason` (message, status where determined); FAIL in part: file adapters and close errors (section 2)
- [x] S6.2 One stop exception, one exit-code constant, no skip or retry - PASS: `InterestRunStoppedException`, `InterestExitCode.STOPPED`; `ErrorPathsJdbcIT`
- [x] S6.3 Restart as in the architecture - PASS: committed chunks kept after each stop (`assertStoppedAtAccount2002`)
- [x] S6.4 Outputs in the recorded format; failed run leaves no SYSTRAN - N/A: export is ticket 07
- [x] S6.5 Run log byte for byte; no account data in the application log - PASS: seven cases equal; stop messages hold no account id (asserted)
- [x] S7.1 A test per rule id, named for it - PASS: section 1
- [x] S7.2 Fixture and edge cases of each rule covered - PASS: boundary tests `intH05_largestInterestThatFitsIsWritten`, `intH05_transaction999999IsTheLastThatFits`
- [x] S7.3 Integration tests on PostgreSQL 15 in a container - PASS: `ErrorPathsJdbcIT`, `MultiCardSwitchJdbcIT`
- [x] S7.4 No assertion against the code's own output - PASS: expected values are literals worked from the source and the input
- [x] S8.1 No edit under `app/`, `fixtures/`, `tools/parity` - PASS
- [x] S8.2 No golden file re-recorded - PASS
- [x] S8.3 Every new dependency explained in the pull request - PASS: none
- [x] S8.4 Frozen shared interfaces unchanged - PASS: FI-1, FI-3, FI-4 untouched; FI-5 names `multiCard`, `overflow` as in the architecture
- [x] S8.5 Synthetic data only - PASS

Behaviours PRESENT in section 4:

- [x] a Truncation without ROUNDED - N/A
- [x] b High-order digit loss (range check, STOP) - PASS: `StopConditionsTest.intH05_*`, `ErrorPathsJdbcIT.intH05_interestAboveFieldStops`
- [x] c Intermediate precision of the COMPUTE - N/A
- [x] d Zoned overpunch signs - N/A
- [x] e Binary items stay out of data and domain - PASS: the status line is built from text; TWO-BYTES-BINARY is not modelled
- [x] f Non-numeric data rejected at the boundary - N/A
- [x] g Fixed width, padding, STRING - N/A
- [x] h Code page and collating order - N/A
- [x] i REDEFINES replaced by formatter and function - PASS: `RunStop.statusLine` replaces IO-STATUS-04
- [x] j FILLER and record length round trip - N/A
- [x] l No state carried between accounts except the counter - PASS
- [x] m Control break and end of file - N/A
- [x] o Level-88 result codes replaced by types - PASS: `StopReason`
- [x] p Indexed access through the ports; several cards to the policy - PASS: `intH03_*`
- [x] q File statuses and abend - PASS: section 1; in part, section 2
- [x] r PARM, generation, disposition, return code - N/A
- [x] s DISPLAY output in the run log - PASS: the lines of each stop asserted
- [x] t PARM text against the clock - N/A
- [x] u Timestamp format and hundredths - N/A
- [x] w PARM length checked - N/A
- [x] x No stale rate after a failed lookup - PASS: a missing DEFAULT rate stops (`intR08_defaultRateMissingStops`)
- [x] y Typed fields where COBOL mixed text and numbers - N/A
- [x] z Abend ends the step; stub output not reproduced - PASS: nothing after `ABENDING PROGRAM`
- [x] aa Dead and empty code not ported - PASS: the non-numeric / status-9 branch of 9910-DISPLAY-IO-STATUS is not built, no status of that kind can arise

## 5. What is wrong or missing in the standards skill (proposals only)

1. **Section 6 / S6.1 does not say what a stop writes for INT-H03 and INT-H05**, which have no legacy text. Proposed:
   "For a stop the legacy has no message for, the run log gets one line starting with the rule id, then
   `ABENDING PROGRAM`."
2. **S6.1 does not say whether `FILE STATUS IS: NNNN00xx` is written when the cause is a database failure.** Proposed:
   "The status line is written only where the condition determines the legacy status (record not found: 23)."
3. **S3.4 names TRAN-AMT and the counter only.** Proposed: add WS-TOTAL-INT (S9(09)V99) and ACCT-CURR-BAL (S9(10)V99).
4. **S6.5 against the legacy line `ACCOUNT NOT FOUND: <id>`.** Proposed: say that the run log may hold account ids
   where the legacy displays them, the application log never.
5. **S3.6**: allow the 11-digit account id of a text (TRAN-DESC, `ACCOUNT NOT FOUND:`) to be formatted in one named
   domain place, or give the codec a function for it.
6. **S5.1**: say what a switch value that is not built does - proposed: refused at start, naming the decision.

## 6. Method

Skills used: `interest-conversion-standards` (sections 3, 4, 5, 6, 7, 9), `interest-ticket-build` steps 2, 5-9,
`interest-ticket-verify` steps 1, 3, 6 (error path), 7-10, `interest-pitfalls` P1, P6, P7.

```sh
mvn -B -f java/interest/pom.xml clean verify                # 299 + 43 tests, BUILD SUCCESS
.agents/skills/interest-ticket-verify/parity-report.sh 08-error-paths $(ls fixtures/interest)
```

Failures hit:

| Symptom | Cause | Fix |
|---|---|---|
| `InterestCalculatorTest.intR08_missingAccountStops` expected the run log to hold the record only | the pilot's test was written before a stop wrote its legacy lines | the expected list extended by the four lines of CBACT04C:375, 386, 643-646, 629 |
| `JdbcCalcStepIT.intH02_failedRunRestartsWithoutChargingTwice` run-log expectation | same: a failed insert now writes `ERROR WRITING TRANSACTION RECORD`, `ABENDING PROGRAM` | the two lines added to the expected log of the failed execution; the log after the restart still equals the recorded one |
| `parity-report.sh 08-error-paths` printed its usage, exit 2 | the script needs at least one case; this ticket has no acceptance case of its own | called with all seven cases, which the ticket names under Verification |

Corrections to the three procedure skills:

| Skill | Step | What is wrong | Corrected text |
|---|---|---|---|
| interest-ticket-verify | 8 | `parity-report.sh <ticket>` without a case exits 2; a ticket whose only fixture requirement is "the seven cases still pass" has no case to name | add: "For a ticket with no acceptance case of its own, pass every case: `parity-report.sh <ticket> $(ls fixtures/interest)`." |
| interest-ticket-verify | 6 | The error path is checked with one unreadable input file only | add: "From ticket 08 the stop conditions are in `StopConditionsTest` and `ErrorPathsJdbcIT`; a new stop needs a row in both and the run-log lines cited from CBACT04C." |
| interest-ticket-build | 5 | no row for the stop classes | add: "`domain/RunStop`, `StopReason`, `LegacyFile` - every stop goes through `RunStop`, which writes the legacy lines; never throw `InterestRunStoppedException` directly from the calculation." |
| interest-pitfalls | - | nothing wrong; nothing new hit | - |

## 7. Dependencies

None added. The pom is unchanged.
