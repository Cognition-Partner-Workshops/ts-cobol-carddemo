# Ticket 03-record-codec - report

Code revision `68295010f0ce308b46a86d2d0de708e26e0835a1` on `demo/05-03-record-codec`, base `demo/05b-integration` (`6c5ee61`).
Evidence: `test-output.txt` (81 unit tests, 12 integration tests, BUILD SUCCESS) and `parity-report.md`.

## 1. Result

| Acceptance line | Met | Shown by |
|---|---|---|
| Every record of every file under fixtures/interest/*/input and */expected/datasets decodes and encodes to the identical bytes | yes for the files of the five layouts: 158 records in 42 files of 7 cases, 0 not identical. Not applicable to TRANCATG.PS and TRANTYPE.PS (see section 2) | `FixtureRoundTripTest.acceptance_everyFixtureRecordRoundTripsInTheFixtureCodePage`; the `ROUND-TRIP` lines in `test-output.txt` give the count per file |
| The same records in cp037 with sign bytes X'C0'-X'C9' and X'D0'-X'D9' decode to the same values | yes, 158 records | `FixtureRoundTripTest.acceptance_theSameRecordsInCp037DecodeToTheSameValues` (`CP037 total` line) |
| 0000010000{ is 1000.00; 0000000300} is -30.00 and encodes back; an F-signed digit is positive and comes back C-signed (G02) | yes | `RecordCodecTest.e2_signedZonedRoundTripInBothCodePages`, `e2_fSignedDigitIsPositiveAndComesBackCSigned` |
| Non-digit in PIC 9, byte outside the code page in PIC X (G07), wrong record length, USAGE other than DISPLAY: each an error naming file, record number and field | yes. A wrong length has no field; the error names file, record number and the layout | `RecordCodecTest.e1_*`, `e3_byteThatIsNotACharacterRejectsTheRecord`, `wrongRecordLength...`, `e5_*`; `RecordFormatTest`; `FileAdapterCodecTest`; command-line run in section 6 |
| Database assertions: none | n/a | |
| Fixture case: make parity-java CASE=i01-baseline still passes | yes | `parity-report.md` |

Records round-tripped per layout: CVACT01Y 20, CVACT03Y 10, CVTRA01Y 12, CVTRA02Y 105, CVTRA05Y 11.
Fixture records that do not round-trip: none.

Parity, all seven cases (same result as on the base before the change):

| Case | In ticket scope | Java exit | Comparator |
|---|---|---|---|
| i01-baseline | yes | 0 | PASS (4 records, 50 fields, 0 differences) |
| i02-tie-half-cent | no | 0 | PASS (4 records, 50 fields, 0 differences) |
| i03-zero-rate-skipped | no | 12 | FAIL (2 records, 25 fields, 4 differences) - also on the base: ZERO_RATE_NOT_BUILT, ticket 04 |
| i04-default-group-fallback | no | 12 | FAIL (2 records, 25 fields, 4 differences) - also on the base: RATE_FALLBACK_NOT_BUILT, ticket 04 |
| i05-multi-category | no | 0 | PASS (3 records, 38 fields, 0 differences) |
| i06-negative-balance | no | 0 | PASS (2 records, 25 fields, 0 differences) |
| i07-nontie-rounding | no | 0 | PASS (4 records, 50 fields, 0 differences) |

## 2. What the ticket does not cover

- **The job cannot be started with `format=host`.** The codec and `RecordFormat.host()` read and write EBCDIC fixed-length records, but `batch/InterestJobParameters` accepts only `format=fixture` and `FileRunWorkspace.open` has no format argument. Both are outside this ticket's Allowed files (batch belongs to tickets 06 and 08). The file adapter therefore uses the fixture format only.
- `AWS.M2.CARDDEMO.TRANCATG.PS.txt` and `TRANTYPE.PS.txt` lie under every `input` directory but are not of the five layouts and are not read by INTCALC (`tools/runjcl/chains.json` lists four inputs). They are not decoded; the round-trip test asserts that these two are the only files left out.
- Key order of host data (E6, G06): the file adapter still orders the image text; no EBCDIC collation is built.
- The run log echo (`RunLog.echo`) takes the fixture record image; what a host run echoes is not decided.
- COMP-3 and COMP: refused, as the ticket says (E5).
- i03 and i04 fail as on the base; their rules are ticket 04.

## 3. For the decision register

1. REL-08 / G01 stays open. `CodePage.host()` uses `IBM037`; `CodePage.host(Charset)` takes another page. Digits and sign bytes do not depend on the page; only PIC X text does. Owner: mainframe data owner.
2. G07 needs a precise rule. Every byte value is a character in ISO-8859-1 and in cp037, so "not a character of the code page" cannot be tested by the charset alone. The codec rejects control characters after translation (C0 and C1 controls: X'00'-X'1F', X'7F'-X'9F' in the fixture page; for cp037 mostly below X'40' plus X'FF'). The pilot rejected only X'00'-X'1F'. Owner: data owner.
3. Stricter input checking than the legacy program. The codec decodes every field of a record, so a non-digit in a field CBACT04C never reads (for example ACCT-CREDIT-LIMIT, INT-H04) now stops the run; the pilot checked only the fields it read. The ticket's acceptance asks for this; the legacy program would not notice. Owner: application owner.
4. A fixture line longer than the record length is rejected; the pilot and `tools/parity/record.py` (`ljust(lrecl)[:lrecl]`) cut it. No fixture file has such a line. Owner: coordinator.
5. G02/G03: an F-signed or minus-zero field that is decoded and encoded changes its last byte. `AccountRecord.withInterestPosted` therefore rewrites only the bytes of the three fields of INT-R07, so other fields of an account record keep their bytes. No fixture record holds an F sign or a minus zero.

## 4. Standards review checklist

Standards:

- S1.1 Sources used in order; recorded output decides - PASS: layouts from the copybooks (`CopybookTest`), bytes from the fixtures (`FixtureRoundTripTest`)
- S1.2 Rule/code disagreements reproduced as coded and reported - N/A: no rule of the calculation touched
- S2.1 Designed from rules; no method per paragraph - PASS: codec is built from data-map rules E1-E5, not from paragraphs
- S2.2 Immutable business records; copybook names only at the mapping - PASS: copybook field names only in `codec/*Record.java`; `RecordValues` is immutable
- S2.3 Spring Batch shape, transaction and restart boundaries - N/A: batch package not touched
- S2.4 Types and return values, no flags or result codes - PASS: `Optional`, `Kind` enum, one stop exception
- S2.5 Constructor injection; no mutable static state - PASS: static fields are final layouts and codecs
- S3.1 `BigDecimal` with data-map precision and scale; no `double`/`float` - PASS: `RecordCodec.signed`, scale from the picture
- S3.2 Formula - N/A: calculation not touched
- S3.3 Rounding mode named on every other arithmetic step - PASS: no rounding in the codec; a value with too many decimals is refused (`e2_valueThatDoesNotFitIsRefusedNotCutOrRounded`)
- S3.4 Range check before store; overflow goes to the policy - PASS for the codec: a value that does not fit is refused, not cut; the policy is ticket 04
- S3.5 Encodings only in `record-codec` - PASS: `CodePage` and `RecordCodec` only; pilot `Field` removed
- S3.6 Width, padding and FILLER only in codec and schema - PASS: `RecordCodec`, `RecordFormat`; `s36_withFieldReplacesOneFieldAndNoOtherByte`
- S3.7 Code page and key order each in one place - PASS: `FixtureCodePage`/`CodePage`; key order still in `FileRunWorkspace` (unchanged)
- S3.8 Clock and run parameters injected - N/A: not touched
- S4.1 Every PRESENT behaviour touched has its test; new ones reported - PASS: d, f, g, h, j below
- S5.1 Each quirk/open decision in one named place behind its switch - PASS for REL-08: `CodePage.DEFAULT_HOST_CHARSET` and `CodePage.host(Charset)`; no `interest.policy.*` switch touched
- S5.2 Nothing fixed, tidied or added without a rule id - PASS, with one thing to review: a fixture line longer than the record length is now rejected (ticket acceptance), where the pilot cut it
- S6.1 Status and message mapping - N/A: not touched; codec errors use the existing `RECORD_REJECTED`
- S6.2 One stop exception, one exit-code constant, no skip or retry - PASS: `InterestRunStoppedException` only
- S6.3 Restart - N/A
- S6.4 Outputs in the recorded format; failed run leaves no SYSTRAN - PASS: five cases identical to the legacy output; rejected-record run left only `input`
- S6.5 Run log byte for byte; no account data in the application log - PASS: run log unchanged; error messages name file, record number and field, never a value
- S7.1 A test per rule id, named for it - PASS: tests named `e1_`..`e5_`, `fi1_`, `s36_`, `acceptance_`
- S7.2 Fixture and edge cases of each rule covered - PASS: all 158 fixture records plus G02, G03, G05, G07 edge rows
- S7.3 Integration tests on PostgreSQL 15 in a container - N/A: no database in this ticket; the pilot's 12 pass
- S7.4 No assertion against the code's own output - PASS: expected bytes written by hand or read from fixtures; the cp037 records are made in the test without the codec
- S8.1 No edit under `app/`, `fixtures/`, `tools/parity` - PASS: `git diff --stat` on those paths is empty
- S8.2 No golden file re-recorded - PASS: `make record` not run
- S8.3 Every new dependency explained - PASS: none; cp037 is `IBM037` of the JDK (`jdk.charsets`)
- S8.4 Frozen shared interfaces unchanged - PASS: ports, pom, schema, Makefile untouched; public methods of the five record classes kept, new ones added
- S8.5 Synthetic data only - PASS

Behaviours PRESENT in section 4:

- a Truncation without ROUNDED - N/A: calculation
- b High-order digit loss - N/A here beyond the codec refusing a value that does not fit
- c Intermediate precision - N/A
- d Zoned overpunch signs - PASS: `RecordCodecTest.e2_*`, both code pages
- e Binary items stay out of data and domain - PASS: `e5_usageOtherThanDisplayIsRefused...`, `CopybookTest.e5_*`
- f Non-numeric data rejected at the boundary - PASS: `e1_nonDigitRejects...`, `FileAdapterCodecTest`
- g Fixed width, padding, STRING - PASS: `e3_textIsPaddedAndNeverCut`, `intR06_transactionFieldsFixedWidth`
- h Code page and collating order - PASS for the code page; collating order of host keys (G06) not built, see section 2
- i REDEFINES - N/A: none in the five copybooks; the parser refuses REDEFINES
- j FILLER and record length round trip - PASS: `e4_*`, `FixtureRoundTripTest`, wrong-length tests
- l, m, o, p, q, r, s, t, u, w, x, y, z, aa - N/A: calculation, batch and run-log behaviours, not touched

## 5. What is wrong or missing in the standards skill

- S3.5/S3.7 do not say what a record image is inside the Java code. The ports (`BalanceRead.recordImage`, `RunLog.echo`) carry a `String`. This ticket treats the image as one character per byte (ISO-8859-1) and the codec works on `byte[]`. Proposal: say so in S3.5.
- G07 / section 4 row f: add the rule which bytes are "not a character" (finding 2).
- Section 9 has no line for "errors name file, record number and field"; the tickets ask for it.

## 6. Method

- `git checkout -b demo/05-03-record-codec origin/demo/05b-integration`; read AGENTS.md, the four skills, the ticket, the data map.
- Base result first: `make parity-java` on the unchanged base gave i01, i02, i05, i06, i07 PASS and i03, i04 FAIL with exit 12.
- `mvn -B -f java/interest/pom.xml verify`, then the code commit, then `test-output.txt` (clean verify) and `parity-report.sh 03-record-codec i01-baseline` from that commit.
- Error path from the command line (verify step 6), a letter in DIS-TRAN-CAT-CD of the second DISCGRP record:
  ```
  com.carddemo.interest.domain.InterestRunStoppedException: RECORD_REJECTED (INT-R08): file AWS.M2.CARDDEMO.DISCGRP.PS.txt record 2 field DIS-TRAN-CAT-CD: is not numeric
  ... c.c.interest.batch.CalcInterestRun : run bad-1794740480 step calcInterestStep stopped before any work
  job exit=12
  input
  ```
- Verify step 7 (database assertions) does not apply: the ticket has none.
- Failures hit: two of my own new tests had wrong expected values (a FILLER compared across code pages although FILLER is raw; 1.5 written as `0000000150{` instead of `0000000015{`). Cause: the tests; fix: the tests. No parity failure.
- Design: the copybooks are not in the jar (the build file is frozen), so each record class declares its layout and `CopybookTest` checks it against `app/cpy/*.cpy` parsed by `Copybook`. `FileRunWorkspaceTest` is outside the Allowed files and unchanged; the adapter tests of this ticket are in `codec/FileAdapterCodecTest`.
- Removed: `codec/Field.java`, `codec/RecordImage.java` and `FieldTest` (replaced by `RecordCodec`, `RecordFormat`, `RecordCodecTest`). Nothing outside `codec` and `file` used them.

## 7. Dependencies

None added. cp037 is the JDK charset `IBM037` (module `jdk.charsets`, part of the `eclipse-temurin:21-jre` image: the cp037 tests ran under Maven; the jar run in the container uses the fixture page only).
