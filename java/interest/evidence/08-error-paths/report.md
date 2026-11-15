# Ticket 08-error-paths - report

Branch `demo/05-08-error-paths`, base `demo/05b-integration`. Code revision `5c4976dceb0ca8cd7386fe0adf28bbaec44dd3f7`.
Build `mvn -B -f java/interest/pom.xml clean verify`: BUILD SUCCESS, 224 unit and 90 integration tests, 0 failures
(`test-output.txt`). Base: 213 and 80. Fixture cases: 7 of 7 PASS (`parity-report.md`).

## 1. Result

| Acceptance line | Met | Shown by |
|---|---|---|
| A balance whose account is missing: step FAILED, non-zero exit code, the log holds ACCOUNT NOT FOUND and the account id (INT-R08) | yes | `ErrorPathsIT.intR08_missingAccountStops` (run log `ACCOUNT NOT FOUND: 00000002009`, exit code 12); `InterestCalculatorTest.intR08_missingAccountStops`. "The log" is the run log (legacy SYSOUT); the application log holds no account id (S6.5) |
| No rate for the group and none for DEFAULT: step FAILED (INT-R08) | yes | `ErrorPathsIT.intR08_noRateForTheGroupAndNoneForDefaultStops`; `InterestCalculatorTest.intR03_missingDefaultRateStops` |
| An account with two cards: step FAILED naming INT-H03 | yes | `ErrorPathsIT.intH03_twoCardsStops`; `InterestCalculatorTest.intH03_twoCardsStops` |
| ... with multiCard=FIRST_BY_CARD_NUM the lower card number is used | **in part** | The behaviour is built and tested (`MultiCardFirstByCardNumIT.intH03_lowerCardNumberWhenSwitched`, `InterestCalculatorTest.intH03_lowerCardNumberWhenSwitched`). It **cannot be switched on by the property** `interest.policy.multiCard`: `InterestPolicy` is the only reader of `interest.policy.*` and `policy/` is not in the ticket's Allowed files. The tests give the decision to the step as a bean |
| Interest above 999999999.99: step FAILED naming INT-H05; nothing is truncated | yes | `ErrorPathsIT.intH05_interestAboveFieldStops`; `InterestCalculatorTest.intH05_interestAboveFieldStops` and four edge tests |
| A 1,000,000th transaction in one run: step FAILED naming INT-H05 | yes, with a limit | `InterestCalculatorTest.intH05_millionthTransactionStops`; `ErrorPathsIT.intH05_millionthTransactionStops` puts the counter of the saved restart state at 999997 instead of writing 999,999 transactions first |
| Database assertions: committed accounts keep their update, the failing account has neither transactions nor an update, interest_run.status is FAILED | yes | `ErrorPathsIT.assertStopped`, called by eight of its nine tests |
| Fixture cases: the seven still pass | yes | `parity-report.md` |

Also built, left by tickets 04 and 06: the legacy lines of a missing DEFAULT rate; stop reasons `UPDATE_ACCOUNT` and
`OVERFLOW` (and `READ_BALANCES`, `READ_ACCOUNT`, `READ_CARD`, `READ_RATE`, `WRITE_TRANSACTION`); a database failure of
the JDBC adapters becomes the stop exception without the driver's message (`batch/DatabaseFailureStops`).

Error path from the command line (interest-ticket-verify step 6): not run. The error lines of this ticket need loaded
tables and `make parity-java` runs the file adapters; they are shown by `ErrorPathsIT` on PostgreSQL 15, as the step says
for ticket 08. Database assertions (step 7) are in the same class.

## 2. What the ticket does not cover

| Left | Why | Owner |
|---|---|---|
| Binding `interest.policy.multiCard` and `interest.policy.overflow` | `policy/InterestPolicy.java` and `application.yml` are outside the Allowed files. Needed: two components `multiCard` (STOP, FIRST_BY_CARD_NUM) and `overflow` (STOP) on `InterestPolicy`, the lines `multiCard: STOP  # INT-H03` and `overflow: STOP  # INT-H05` in `application.yml`, and `InterestJobConfig.holeDecisions()` built from them | coordinator, between waves |
| `overflow=WIDEN` | needs wider columns and layouts (FI-1, FI-4); INT-H05 is OPEN | target architect |
| `JdbcAccountStore` and `JdbcTransactionSink` still raise `WRITE_OUTPUT` and `RECORD_REJECTED` themselves | `jdbc/calc` is outside the Allowed files; the step translates `WRITE_OUTPUT` of the account update to `UPDATE_ACCOUNT`, and the domain's INT-H05 check runs before the adapters' picture check | ticket 06 owner |
| Legacy message per file for a failed open or close; legacy lines for the stops of the file adapters and the loader (`OPEN_INPUT`, `RECORD_REJECTED`, `WRITE_OUTPUT`) | `file/`, `load/`, `export/` are outside the Allowed files | tickets 05, 07 or a follow-up |
| A database failure reading the DEFAULT rate writes `ERROR READING DISCLOSURE GROUP FILE` | the decorator of `RateLookup` cannot tell the group read from the DEFAULT read; missing DEFAULT rate (status 23) has the right message | follow-up |
| `rc.json` of a stopped run (STEP15 = 12) | export, outside the Allowed files; a stopped run writes no rc.json | ticket 07 owner |
| `loadExtractsStep` and `exportDatasetsStep` as steps of the job; `make parity-java` through the tables | not this ticket | ticket 09 |
| Retry or skip | excluded work | - |
| Parity for any stop | no recorded legacy output of an abend; INT-H03 and INT-H05 on the host are unknown | mainframe application owner |

## 3. For the decision register

1. **INT-H03 and INT-H05 stay OPEN.** The proposed default STOP is built. What CBACT04C does on the host for an account
   with several cards (status 02, 00, or an abend) and for an overflow (compiler options ARITH/TRUNC unknown) is not known.
   No parity is claimed. Owner: mainframe application owner.
2. **The run log lines of the two holes are invented.** No legacy text exists. Built: `INT-H03 ACCOUNT HAS SEVERAL CARDS: <11-digit id>`
   and `INT-H05 VALUE DOES NOT FIT FIELD: <field>`, each followed by `ABENDING PROGRAM`. The wording needs an owner's
   yes. Owner: target architect with operations.
3. **INT-H05 does not name ACCT-CURR-BAL.** The rule cites the interest (S9(09)V99), WS-TOTAL-INT and the counter.
   `ADD WS-TOTAL-INT TO ACCT-CURR-BAL` (`CBACT04C:352`) has no ON SIZE ERROR either; a balance beyond S9(10)V99 would lose
   its leading digit. Built: stops with OVERFLOW (INT-H05), field `ACCT-CURR-BAL`
   (`intH05_newBalanceAboveFieldStops`). Proposed: add line 352 to the sources of INT-H05. Owner: rules owner.
4. **A missing card displays `ACCOUNT NOT FOUND:`.** `CBACT04C:397` uses the text of the account read for the
   cross-reference read. Reproduced as coded (S1.2). Owner: mainframe application owner (is the text relied on by operations?).
5. **A stop keeps the run log lines of the rolled-back chunk.** The run log file of a FAILED step holds the echo lines
   of the accounts of the failing chunk although their rows were rolled back; the legacy would have committed those
   accounts (it has no chunk). The restart cuts the lines off. Operations reading the SYSOUT of a failed run see accounts
   that are not posted. Owner: mainframe operations owner (REL-11).
6. **REL-10 unchanged.** The exit code of every stop is the one constant `InterestExitCode.STOPPED` (12).
7. **A database failure has no legacy status.** The run log gets the legacy message without `FILE STATUS IS:`; the
   application log gets the exception class and SQLSTATE only. Whether operations need a status line here is open.

## 4. Standards review checklist

Standards:

- [x] S1.1 Sources used in order; recorded output decides - PASS: ticket, INT-R08/H03/H05, architecture section 8, `CBACT04C:350-459, 628-648`; no recorded output exists for a stop, so the messages come from the COBOL lines; the seven recorded cases still pass
- [x] S1.2 Rule/code disagreements reproduced as coded and reported - PASS: the legacy displays `ACCOUNT NOT FOUND:` also for a missing card (`CBACT04C:397`) - reproduced, reported in section 3
- [x] S2.1 Designed from rules; no method per paragraph - PASS: `RunStops` (one method per stop condition), `HoleDecisions` (INT-H03, INT-H05), `DatabaseFailureStops`; no `p9999AbendProgram`
- [x] S2.2 Immutable business records; copybook names only at the mapping - PASS with one remark: `HoleDecisions` is a record; the field names `TRAN-AMT`, `WS-TOTAL-INT`, `ACCT-CURR-BAL`, `TRAN-ID` appear as text in the INT-H05 run log line (`InterestCalculator`), not as accessors
- [x] S2.3 Spring Batch shape, transaction and restart boundaries of the architecture - PASS: reader/processor/writer unchanged; a stop rolls back the chunk in work, committed chunks stay (`ErrorPathsIT`)
- [x] S2.4 Types and return values, no flags or result codes - PASS: a stop is the exception `RunStops` returns; `intH03CardNumber` returns `Optional`; no status field is passed around
- [x] S2.5 Constructor injection; no mutable static state - PASS: `HoleDecisions` injected into `InterestCalculator`, `CalcInterestRun`, `CalcInterestTableRun`
- [x] S3.1 `BigDecimal` with data-map precision and scale; no `double`/`float` - PASS: `grep -rnE '\b(double|float)\b' java/interest/src` finds nothing in the changed files
- [x] S3.2 Formula: exact product, one division, mode from the policy - PASS (unchanged; the INT-H05 check wraps the result)
- [x] S3.3 Rounding mode named on every other arithmetic step - PASS (unchanged, `RoundingMode.UNNECESSARY`)
- [ ] S3.4 Range check before store; overflow goes to the policy - FAIL in part: the check is made before the store for TRAN-AMT, WS-TOTAL-INT, ACCT-CURR-BAL and the counter and stops naming INT-H05 (`intH05_*`), but it goes to `domain/HoleDecisions`, not to `InterestPolicy.checkFits`: `policy/` is outside the ticket's Allowed files
- [x] S3.5 Encodings only in `record-codec` - PASS: no encoding in the changed code
- [x] S3.6 Width, padding and FILLER only in codec and schema - PASS: the domain formats only the 11 digits of the account id in `ACCOUNT NOT FOUND:` (`RunStops.elevenDigits`), as S3.6 allows for an id inside a text
- [ ] S3.7 Code page and key order each in one place - N/A: not touched
- [ ] S3.8 Clock and run parameters injected; `parmDate` is text - N/A: not touched
- [x] S4.1 Every PRESENT behaviour touched has its test; new ones reported - PASS: b, p, q, z below; new finding (ACCT-CURR-BAL overflow is not named by INT-H05) in section 3
- [ ] S5.1 Each quirk/open decision in one named place behind its switch, rule id in names - FAIL in part: INT-H03 and INT-H05 are each in one named place (`HoleDecisions.intH03CardNumber`, `intH05Fits`, `intH05HasNextTransaction`) with the proposed default STOP, but not behind `interest.policy.multiCard` / `interest.policy.overflow` read through `InterestPolicy`: `policy/InterestPolicy.java` is outside the Allowed files. The decision reaches the step as the bean `HoleDecisions`, fixed to STOP in `InterestJobConfig`
- [x] S5.2 Nothing fixed, tidied or added without a rule id - PASS: no retry, no skip; WIDEN is not built
- [x] S6.1 Status and message mapping as in section 6 - PASS for the rows built: account missing, card missing, several cards, DEFAULT rate missing, rewrite fails, write fails, TCATBALF other status, amount or counter does not fit; status line only for a missing record (23). Not built: one message per file for a failed open and close (file adapters and export, outside the Allowed files); a database failure of the DEFAULT rate read writes `ERROR READING DISCLOSURE GROUP FILE`, not `... DEFAULT DISCLOSURE GROUP` (the decorator cannot tell the two reads apart)
- [x] S6.2 One stop exception, one exit-code constant, no skip or retry - PASS: `InterestRunStoppedException` only; a `DataAccessException` of the adapters becomes it (`DatabaseFailureStops`); `InterestExitCode.STOPPED` asserted in `ErrorPathsIT.assertStopped`
- [x] S6.3 Restart as in the architecture - PASS: `CalcInterestRestartIT` (5 tests) still passes; the lines of a stop are cut off by the restart (`rel11_restartEndsWithTheRowsOfAnUninterruptedRun`)
- [x] S6.4 Outputs in the recorded format; failed run leaves no SYSTRAN - PASS for what the ticket touches: committed account updates stay, nothing of the failing chunk stays. An `rc.json` of a stopped run is not written (export, outside the Allowed files)
- [x] S6.5 Run log byte for byte; no account data in the application log - PASS: the run log lines of each stop are asserted line by line; `assertStopped` asserts no account id in the captured application log; the driver's message is neither logged nor kept as the cause (`intR08_refused*WithoutTheDriversRowDetail`)
- [x] S7.1 A test per rule id, named for it - PASS: `intR08_*`, `intH03_*`, `intH05_*`; default and other value of multiCard tested; overflow has the one value STOP
- [x] S7.2 Fixture and edge cases of each rule covered - PASS: no fixture exists for these rules; synthetic data under `java/interest/src/test` (b, p two cards, q); edges: largest interest that fits, negative overflow, account total, new balance, id 999999 then stop
- [x] S7.3 Integration tests on PostgreSQL 15 in a container - PASS: `ErrorPathsIT`, `MultiCardFirstByCardNumIT` on `postgres:15` with both init scripts
- [x] S7.4 No assertion against the code's own output - PASS: run log lines are written by hand from `CBACT04C:356-459, 628-648`; amounts with the arithmetic shown
- [x] S8.1 No edit under `app/`, `fixtures/`, `tools/parity` - PASS: `git diff --stat origin/demo/05b-integration -- app fixtures tools` prints nothing
- [x] S8.2 No golden file re-recorded - PASS
- [x] S8.3 Every new dependency explained in the pull request - PASS: none
- [x] S8.4 Frozen shared interfaces unchanged - PASS: ports, schema, record classes, pom, Makefile, switch names untouched
- [x] S8.5 Synthetic data only - PASS: accounts 2001-2010 and card numbers 400000000000xxxx of the fixtures' pattern

Behaviours PRESENT in section 4:

- [ ] a Truncation without ROUNDED - N/A: not touched; i02 and i07 still pass
- [x] b High-order digit loss (range check, STOP) - PASS: `intH05_interestAboveFieldStops`, `intH05_millionthTransactionStops` (unit and `ErrorPathsIT`)
- [ ] c Intermediate precision of the COMPUTE - N/A: not touched
- [ ] d Zoned overpunch signs - N/A: not touched
- [ ] e Binary items stay out of data and domain - N/A: the 9x binary-byte branch of `9910-DISPLAY-IO-STATUS` is not copied (S6.1)
- [ ] f Non-numeric data rejected at the boundary - N/A: not touched
- [ ] g Fixed width, padding, STRING - N/A: not touched
- [ ] h Code page and collating order - N/A: not touched
- [ ] i REDEFINES replaced by formatter and function - N/A: not touched
- [ ] j FILLER and record length round trip - N/A: not touched
- [x] l No state carried between accounts except the counter - PASS: `TransactionCounter(int)` takes the saved counter; nothing else added
- [ ] m Control break and end of file (INT-Q04, INT-T09) - N/A: not touched
- [ ] o Level-88 result codes replaced by types - N/A: no result code added; see S2.4
- [x] p Indexed access through the ports; several cards to the policy - PASS for the behaviour (`intH03_twoCardsStops`, `intH03_lowerCardNumberWhenSwitched`, unit and IT); the decision is in `HoleDecisions`, not in the policy (see S5.1)
- [x] q File statuses and abend - PASS: one `intR08_*` test per stop condition built
- [ ] r PARM, generation, disposition, return code - N/A: not touched
- [x] s DISPLAY output in the run log - PASS: the lines of each stop are in the run log file of the step
- [ ] t PARM text against the clock - N/A: not touched
- [ ] u Timestamp format and hundredths - N/A: not touched
- [ ] w PARM length checked - N/A: not touched
- [x] x No stale rate after a failed lookup - PASS: unchanged; a missing DEFAULT rate stops (`intR03_missingDefaultRateStops`)
- [ ] y Typed fields where COBOL mixed text and numbers - N/A: not touched
- [x] z Abend ends the step; stub output not reproduced - PASS: the run log ends with `ABENDING PROGRAM` in every stop test; `intR08_nothingIsReturnedForAnAccountThatStops`
- [ ] aa Dead and empty code not ported - N/A: not touched

## 5. What is wrong or missing in the standards skill

1. **Section 6 has no row for a failed read of ACCTFILE or XREFFILE other than "record missing".** `CBACT04C:378-390`
   and `400-412` abend on every status but 00. Built: `READ_ACCOUNT` and `READ_CARD` with `ERROR READING ACCOUNT FILE` /
   `ERROR READING XREF FILE` and no status line. Proposed: add the two rows.
2. **S3.4 and S5.1 say the overflow and the several-cards decision go to the policy,** but ticket 08's Allowed files
   are `domain/**`, `batch/**` and tests. Proposed: either add `policy/**` to the Allowed files of ticket 08 or say in
   S5.1 that a ticket without `policy/` keeps the decision in one domain type and reports the binding.
3. **Section 6 says "a line naming INT-H03 / INT-H05 (no legacy text exists)"** without the text. Proposed: fix the
   wording (finding 2 above) so that operations can search for it.
4. **Section 6, row "DEFAULT rate missing or unreadable"** gives one reason for a missing record and a read failure,
   but the status line `NNNN0023` is right for the first only. Proposed: split the row (missing: with status 23;
   unreadable: `READ_RATE`, no status line).
5. **S6.1 does not say whether `ABENDING PROGRAM` follows the line of a hole.** Built: it does, because the target has
   one failure path. Proposed: say so.

## 6. Method

Commands, from the repository root, with `JAVA_HOME` on JDK 21: `make parity-java` on the base (7 PASS, kept in
`work/base-parity.txt`); `mvn -B -f java/interest/pom.xml verify`; `make parity-java`; the scope check of
interest-ticket-build step 9; after the commit `mvn -B -f java/interest/pom.xml clean verify` into `test-output.txt` and
`.agents/skills/interest-ticket-verify/parity-report.sh 08-error-paths <the seven cases>`.

Failures hit:

| Symptom | Cause | Fix |
|---|---|---|
| `ErrorPathsIT.intH05_interestAboveFieldStops`: expected `00000002007010001099999999999I` but was `000000020070100019999999999I` | the hand-written record image in the test had a digit too many: 999999999.99 in S9(09)V99 is ten `9` and the overpunch `I` | corrected the test, not the code (as pitfall P15) |
| `CalcInterestRestartIT.rel11_restartEndsWithTheRowsOfAnUninterruptedRun` and `InterestCalculatorTest.intR03_missingDefaultRateStops`, `intR08_missingAccountStops` asserted a run log without the lines of the stop | they were written when the legacy lines were left to ticket 08 | the expected lines now include the stop's lines from `CBACT04C:356-369, 373-390, 443-459, 628-648` |

Where a skill step did not fit: interest-ticket-verify step 6 (no command-line error run, see section 1); step 8's
result table (`parity-report.sh` marks every case passed on the command line as "acceptance case of the ticket"; here
they are regression cases, changed by hand); `switch-test-output.txt` is not written, because the step 4 command cannot
set `multiCard` (the property is not bound).

The integration test for the 1,000,000th transaction changes the saved restart state of a failed execution through
`JobRepository.updateExecutionContext`; it does not write 999,999 transactions.

## 7. Dependencies

None added.
