# Report - ticket 03-record-codec (INT-T03)

Branch `demo/05-03-record-codec`, base `demo/05b-integration`. Code revision `74feee47db2a500bf398dafcb2ee22c09dc011dc`.
Evidence in this directory: `test-output.txt` (`mvn -B clean verify`), `parity-report.md` (all seven cases).
Tests are under `java/interest/src/test/java/com/carddemo/interest/codec/`.

## 1. Result

| # | Acceptance line of the ticket | Met | Shown by |
|---|---|---|---|
| 1 | Every record of every file under `fixtures/interest/*/input` and `*/expected/datasets`: decode then encode returns the identical bytes | **met** | `FixtureRoundTripTest.everyFixtureRecordDecodesAndEncodesToTheSameBytes`: 56 files, 228 records, 0 not identical. Count per file: lines `ROUNDTRIP fixture` in `test-output.txt`; totals below |
| 2 | The same records in cp037 with sign bytes X'C0'-X'C9' and X'D0'-X'D9' decode to the values of the fixture code page | **met** | `FixtureRoundTripTest.theSameRecordsInCp037DecodeToTheSameValues`: 228 records, sign bytes seen C0 C1 C2 C3 C4 C5 C7 D0 (line `ROUNDTRIP cp037 total`). The host image is built in the test, not by the codec. C6, C8, C9 and D1-D9 do not occur in the fixtures; `EncodingRulesTest.e2_signedZoned` covers C1, C9, D1, D9 with synthetic rows |
| 3 | `0000010000{` = 1000.00 as S9(09)V99; `0000000300}` = -30.00 and back to the same 11 bytes; an F-signed digit is positive and is written C-signed (G02) | **met** | `EncodingRulesTest.e2_signedZoned` rows 1, 2, 12, 13 (fixture) and the host rows with X'F0', X'F5' |
| 4 | Non-digit in PIC 9, byte outside the code page in PIC X (G07), wrong record length, USAGE other than DISPLAY: each an error naming file, record number and field | **met** | `RecordRejectionTest`: `e1_nonDigitInAPic9Field...`, `e3_g07_byteOutsideTheCodePage...`, `wrongRecordLengthNamesFileRecordAndField`, `e5_usageOtherThanDisplay...`. Each asserts the whole message, for example `RECORD_REJECTED (INT-R08): file AWS.M2.CARDDEMO.TCATBALF.PS.txt record 7 field TRANCAT-CD: not numeric (E1)` |
| 5 | Database assertions: none | n/a | no database code touched |

Verification lines: `mvn verify` BUILD SUCCESS, `Tests run: 168` (unit) and `Tests run: 12` (integration), 0 failures; `make parity-java CASE=i01-baseline` PASS (4 records, 50 fields, 0 differences).

Records round-tripped, by copybook (per file in `test-output.txt`):

| Copybook | Files | Records | Not identical |
|---|---|---|---|
| CVACT01Y (ACCTDATA input and KSDS output) | 14 | 20 | 0 |
| CVACT03Y (CARDXREF) | 7 | 10 | 0 |
| CVTRA01Y (TCATBALF) | 7 | 12 | 0 |
| CVTRA02Y (DISCGRP) | 7 | 105 | 0 |
| CVTRA05Y (SYSTRAN output) | 7 | 11 | 0 |
| CVTRA03Y (TRANTYPE), not one of the five | 7 | 35 | 0 |
| CVTRA04Y (TRANCATG), not one of the five | 7 | 35 | 0 |
| Total | 56 | 228 | 0 |

Gaps asked for by the Deliverables line: **no fixture record fails to round-trip.**

## 2. What the ticket does not cover (gaps)

- **The job still reads and writes the fixture format only.** `FileRunWorkspace` builds its codecs with `CodePage.fixture()`. The host code page works in the codec (acceptance line 2) but no run parameter selects it: that parameter belongs in `batch/`, outside this ticket's Allowed files, and REL-08 is still open. `RecordImage.fromBlock` (host files, no line ends) is tested but not called by the job.
- **TRANTYPE and TRANCATG.** They lie under `fixtures/interest/*/input` but INTCALC does not read them and their copybooks (CVTRA03Y, CVTRA04Y) are not among the five. The tests round-trip them with layouts parsed from `app/cpy`; the production code has no class for them.
- **i03-zero-rate-skipped and i04-default-group-fallback fail parity**, as they did on the base branch: the job stops with ZERO_RATE_NOT_BUILT (INT-R04) and RATE_FALLBACK_NOT_BUILT (INT-R03). Ticket 04 owns both.
- **COMP and COMP-3** are refused by name, not decoded (rule E5, excluded work).
- **REDEFINES, OCCURS, SIGN SEPARATE, level 66/88** are refused by `CopybookParser`; none occurs in the five copybooks.
- **Sign bytes C6, C8, C9, D1-D9** do not occur in any fixture; only synthetic rows cover them.

## 3. For the decision register

| Finding | Evidence | Suggested owner |
|---|---|---|
| REL-08 stays open. The host code page is the argument of `CodePage.host(Charset)`; `CodePage.host()` uses IBM037 and the constant is named `UNCONFIRMED_HOST_CHARSET` | `EncodingRulesTest.rel08_theHostCodePageIsAParameterWithCp037AsDefault` | mainframe data owner |
| G07: ISO-8859-1 and cp037 give every one of the 256 bytes a character, so "a byte outside the code page" can never happen by the table alone. The codec treats **control characters** (X'00'-X'1F', X'7F'-X'9F' in ISO-8859-1 and the bytes cp037 maps to them) in a PIC X field as outside the code page and rejects the record. This is my reading, not a recorded behaviour: COBOL would carry such bytes through | `RecordRejectionTest.e3_g07_...`, `CodePage.isPrintable` | data owner / design (data-map G07) |
| A fixture line longer than the record length was cut by the pilot; it is now rejected (wrong record length). No fixture has such a line | `RecordRejectionTest.aLineLongerThanTheRecordIsNotCut` | design |
| The pilot checked only the fields the job uses; the codec now checks every field of a record, so a bad byte in a field the job only carries (for example ACCT-OPEN-DATE) stops the run. All fixtures pass | `AccountRecord.decode`, round-trip test | design (S3.5) |
| G02/G03: an F-signed or minus-zero input is not byte-identical after a rewrite (C-signed plus zero comes back). No fixture record has either | `EncodingRulesTest.e2_signedZoned` G02 and G03 rows | none - as the data map says |

## 4. Standards review checklist (`.agents/skills/interest-conversion-standards/SKILL.md`, section 9)

Standards:

- [x] S1.1 Sources used in order; recorded output decides - PASS: `FixtureRoundTripTest` asserts against the recorded files
- [x] S1.2 Rule/code disagreements reproduced as coded and reported - PASS: none found; G07 reading reported in section 3
- [x] S2.1 Designed from rules; no method per paragraph - PASS: `RecordCodec` has one method per encoding rule E1-E4
- [x] S2.2 Immutable business records; copybook names only at the mapping - PASS: `RecordValues`, `FieldSpec`, `RecordSource` are immutable; copybook names only in the five `*Record` classes
- [ ] S2.3 Spring Batch shape, transaction and restart boundaries - N/A: `batch/` not touched
- [x] S2.4 Types and return values, no flags or result codes - PASS: `FieldKind` enum, `Optional`, `InterestRunStoppedException`
- [x] S2.5 Constructor injection; no mutable static state - PASS: codecs take `CodePage` in the constructor; statics are final layouts. Note: the static `RecordLayout.Builder COPYBOOK` in each `*Record` is used only during class initialisation
- [x] S3.1 `BigDecimal` with data-map precision and scale; no `double`/`float` - PASS: `EncodingRulesTest.e2_signedZoned` asserts with `equals` (scale included)
- [ ] S3.2 Formula - N/A: calculation not touched
- [x] S3.3 Rounding mode named on every other arithmetic step - PASS: `RoundingMode.UNNECESSARY` in `RecordCodec.encodeSigned`
- [x] S3.4 Range check before store; overflow goes to the policy - PASS for the codec part: `EncodingRulesTest.valuesThatDoNotFitAreRefusedNotCut`; the policy is not touched
- [x] S3.5 Encodings only in `record-codec` - PASS: `FileRunWorkspace` no longer slices or pads; `git grep -n "substring\|repeat" java/interest/src/main/java/com/carddemo/interest/file` prints nothing
- [x] S3.6 Width, padding and FILLER only in codec and schema - PASS: `RecordCodec`, `RecordImage`; `RecordLayoutTest.e4_accountFillerRoundTrip`
- [x] S3.7 Code page and key order each in one place - PASS: `CodePage`; `CategoryBalanceRecord.KEY_ORDER` used once in `FileRunWorkspace`; `EncodingRulesTest.e6_keyOrderIsTheByteOrderOfTheCodePage`
- [ ] S3.8 Clock and run parameters injected - N/A: not touched
- [x] S4.1 Every PRESENT behaviour touched has its test - PASS: rows d, f, g, h, j below
- [x] S5.1 Each quirk/open decision in one named place - PASS: REL-08 in `CodePage.UNCONFIRMED_HOST_CHARSET`
- [x] S5.2 Nothing fixed, tidied or added without a rule id - PASS with a note: control characters rejected under G07 (section 3)
- [x] S6.1 Status and message mapping - PASS: rejections use the existing `StopReason.RECORD_REJECTED` (INT-R08)
- [x] S6.2 One stop exception, no skip or retry - PASS: only `InterestRunStoppedException` is thrown for data
- [ ] S6.3 Restart - N/A: not touched
- [x] S6.4 Outputs in the recorded format; failed run leaves no SYSTRAN - PASS: parity i01-baseline; `FileRunWorkspaceTest` unchanged and green
- [x] S6.5 Run log byte for byte; no account data in the application log - PASS: `RecordRejectionTest.s6_5_theRejectionHoldsNoFieldValue`; sysout compared in parity
- [x] S7.1 A test per rule id, named for it - PASS: tests named `e1_`, `e2_`, `e3_g07_`, `e4_`, `e5_`, `e6_`, `intR07_`, `intT04_g05_`
- [x] S7.2 Fixture and edge cases of each rule covered - PASS: all 228 fixture records plus synthetic edge rows
- [ ] S7.3 Integration tests on PostgreSQL 15 - N/A: no database work; `InterestCalcJobIT` still passes (12 tests)
- [x] S7.4 No assertion against the code's own output - PASS: expected bytes are fixture lines or hex literals; the cp037 image is built by the test. One exception stated: `RecordLayoutTest.intT04_g05_theSameTransactionInCp037` compares the host record with the fixture record put through the JDK cp037 table, and the fixture record is itself asserted against the recorded SYSTRAN line in the test above it
- [x] S8.1 No edit under `app/`, `fixtures/`, `tools/parity` - PASS: `git diff --stat origin/demo/05b-integration -- app fixtures tools modernization .agents db docker-compose.yml` prints nothing
- [x] S8.2 No golden file re-recorded - PASS: same command
- [x] S8.3 Every new dependency explained - PASS: none added (section 7)
- [x] S8.4 Frozen shared interfaces unchanged - PASS: no file under `port/` in the diff; copybooks read only
- [x] S8.5 Synthetic data only - PASS: fixture records and `SYNTH-*` fields

Behaviours PRESENT in section 4:

- [ ] a Truncation without ROUNDED - N/A: calculation
- [ ] b High-order digit loss - N/A: calculation; the codec refuses a value wider than its field (`valuesThatDoNotFitAreRefusedNotCut`)
- [ ] c Intermediate precision of the COMPUTE - N/A
- [x] d Zoned overpunch signs - PASS: `EncodingRulesTest.e2_signedZoned`, `FixtureRoundTripTest`
- [x] e Binary items stay out of data and domain - PASS: `RecordRejectionTest.e5_usageOtherThanDisplayNamesFileRecordAndField`
- [x] f Non-numeric data rejected at the boundary - PASS: `RecordRejectionTest.e1_...`, `e1e2_...`; `FileRunWorkspaceTest.e1_badDigitInABalanceRejectsTheRecord`
- [x] g Fixed width, padding, STRING - PASS: `EncodingRulesTest.e3_text`, `RecordLayoutTest.intR06_transactionFieldsFixedWidth`
- [x] h Code page and collating order - PASS: `theSameRecordsInCp037DecodeToTheSameValues`, `e6_keyOrderIsTheByteOrderOfTheCodePage`
- [ ] i REDEFINES replaced by formatter and function - N/A: not touched; the parser refuses REDEFINES (`CopybookLayoutTest`)
- [x] j FILLER and record length round trip - PASS: `EncodingRulesTest.e4_fillerComesBackUnchanged`, `RecordRejectionTest.wrongRecordLength...`
- [ ] l No state carried between accounts - N/A
- [ ] m Control break and end of file - N/A: `FileRunWorkspaceTest.intQ04`, `intT09` unchanged and green
- [ ] o Level-88 result codes replaced by types - N/A
- [ ] p Indexed access through the ports - N/A: ports unchanged
- [ ] q File statuses and abend - N/A
- [ ] r PARM, generation, disposition, return code - N/A
- [ ] s DISPLAY output in the run log - N/A: the echo of the balance record is still the 50-byte image (`FileRunWorkspaceTest.intR01`)
- [ ] t PARM text against the clock - N/A
- [ ] u Timestamp format and hundredths - N/A
- [ ] w PARM length checked - N/A
- [ ] x No stale rate after a failed lookup - N/A
- [x] y Typed fields where COBOL mixed text and numbers - PASS: `RecordValues` gives `long`, `BigDecimal`, `String`, `byte[]` by field kind
- [ ] z Abend ends the step - N/A
- [ ] aa Dead and empty code not ported - N/A

## 5. What is wrong or missing in the standards skill (not edited)

- Proposal: S3.5 and data-map G07 say "a byte outside the code page" is rejected, but ISO-8859-1 and cp037 map all 256 bytes. The standard should say which bytes count as outside (this ticket chose control characters) or say that PIC X carries any byte.

Skills `interest-ticket-build`, `interest-ticket-verify`, `interest-pitfalls`: no correction; every command worked as written.

## 6. Method: commands run and failures hit

```sh
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
mvn -B -f java/interest/pom.xml clean verify          # test-output.txt
.agents/skills/interest-ticket-verify/parity-report.sh 03-record-codec i01-baseline   # parity-report.md
git checkout -- tools/parity/__pycache__
git diff --stat origin/demo/05b-integration -- app fixtures tools modernization .agents db docker-compose.yml   # prints nothing
```

| Symptom | Cause | Fix |
|---|---|---|
| `EncodingRulesTest.e2_signedZoned:105 expected: <99999999.99> but was: <999999999.99>` (2 rows) | my test rows: `9999999999I` in S9(09)V99 is 999999999.99, I wrote one 9 too few | corrected the expected values in the test; the codec was right |

No other failure. Pitfalls P1 (JDK 21) and P6 (`__pycache__`) were avoided as the skill says.

## 7. Dependencies (S8.3)

None added. cp037 and the other EBCDIC tables come from the JDK (`jdk.charsets`); a COBOL copybook library (for example JRecord or cb2java) was considered and not taken, because the five layouts need only PIC X, 9 and S9..V9 with USAGE DISPLAY.
