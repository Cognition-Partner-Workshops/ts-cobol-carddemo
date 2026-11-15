# Ticket 03 record codec - report

Ticket: `modernization/design/tickets/03-record-codec.md` (INT-T03). Branch `demo/05-g1-groundwork`, after ticket 02 on
the same branch. Code revision the evidence was produced from: `2e37982`.

Read this with [test-output.txt](test-output.txt) (search for `ticket 03`: the record counts per file) and
[parity-report.md](parity-report.md).

## 1. Result

| Acceptance line of the ticket | Result | Where it is shown |
|---|---|---|
| Every record of every file under fixtures/interest/*/input and */expected/datasets: decode then encode returns the identical bytes | met: 228 records of 56 files, 0 differences | `FixtureRoundTripTest.everyFixtureRecordRoundTripsByteForByte_fixtureCodePage`; test-output.txt, table below |
| The same records in cp037 with zoned signs X'C0'-X'C9', X'D0'-X'D9': decoded values equal those of the fixture code page | met: 228 records, 454 numeric fields equal, 228 sign bytes checked, every text field equal; each record also re-encodes to the same cp037 bytes | `FixtureRoundTripTest.everyFixtureRecordInCp037DecodesToTheSameValuesAndRoundTrips`, `valuesOfOneRecordedAccountInBothCodePages` |
| 0000010000{ is 1000.00 as S9(09)V99; 0000000300} is -30.00 and encodes back to the same 11 bytes | met | `ZonedDecimalTest.intT06_observableValuesOfTheTicket` and rows 1, 2 of `e1e2_decodesAndEncodesTheSameBytesInBothCodePages` |
| An F-signed digit decodes as positive and encodes with a C sign (G02) | met, in both code pages; minus zero comes back plus zero (G03) | `ZonedDecimalTest.g02g03_fSignedAndMinusZeroComeBackCSigned` |
| Error: a non-digit in a PIC 9 field | met | `RecordCodecErrorTest.e1_nonDigitInAPic9Field`, `e2_nonDigitInASignedField`; `ZonedDecimalTest.e1e2_nonDigit...` (16 rows) |
| Error: a byte outside the code page in a PIC X field (G07) | met | `RecordCodecErrorTest.g07_byteOutsideTheCodePageInAPicXField`; `TextAndFillerTest.g07_...` (8 rows) |
| Error: a record of the wrong length | met | `RecordCodecErrorTest.recordOfTheWrongLength_bytes`, `_fixtureLineLongerThanTheRecord`, `_hostFileThatDoesNotEndOnARecord` |
| Error: a field with USAGE other than DISPLAY | met | `RecordCodecErrorTest.e5_fieldWithUsageOtherThanDisplay`; `CopybookTest.e5_fieldThatIsNotPlainDisplayIsRefusedNotGuessed` (12 rows) |
| Each error names file, record number and field | met: the tests compare the whole message, for example `RECORD_REJECTED (INT-R08): file AWS.M2.CARDDEMO.TCATBALF.PS.txt, record 4, field TRANCAT-CD: not numeric: byte X'58' at position 3 of the field (E1)`; the same from the job's command line in section 6 | `RecordCodecErrorTest` |
| Build: `mvn -q -f java/interest/pom.xml verify` | met: 162 unit tests and 12 integration tests, 0 failures, BUILD SUCCESS (starting branch: 60 and 12) | test-output.txt |
| Tests: round-trip tests over all fixture files; table-driven tests for E1-E4 | met | `FixtureRoundTripTest`; `ZonedDecimalTest` (E1, E2), `TextAndFillerTest` (E3, E4) |
| `make parity-java CASE=i01-baseline` still passes | met | parity-report.md, "Acceptance run" |

Records round-tripped per file (from test-output.txt; the counts are also asserted against `wc -l` figures written by hand in the test):

| File (7 cases each) | Layout | Records |
|---|---|---|
| input/AWS.M2.CARDDEMO.ACCTDATA.PS.txt | CVACT01Y | 10 |
| expected/datasets/AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS.txt | CVACT01Y | 10 |
| input/AWS.M2.CARDDEMO.CARDXREF.PS.txt | CVACT03Y | 10 |
| input/AWS.M2.CARDDEMO.TCATBALF.PS.txt | CVTRA01Y | 12 |
| input/AWS.M2.CARDDEMO.DISCGRP.PS.txt | CVTRA02Y | 105 |
| expected/datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt | CVTRA05Y | 11 |
| input/AWS.M2.CARDDEMO.TRANCATG.PS.txt | CVTRA04Y (not one of the five; read from app/cpy by the test) | 35 |
| input/AWS.M2.CARDDEMO.TRANTYPE.PS.txt | CVTRA03Y (not one of the five; read from app/cpy by the test) | 35 |
| **Total** | | **228** |

**Fixture records that do not round-trip: none.**

Parity: i01, i02, i05, i06, i07 PASS; i03 and i04 FAIL exactly as before the codec (the job stops on rules ticket 04
builds). The result line of every case is identical to the one in `../02-schema/parity-report.md`, taken before this ticket.

Changed: `java/interest/src/main/java/com/carddemo/interest/codec/**`, `.../file/FileRunWorkspace.java`,
`java/interest/src/test/java/com/carddemo/interest/codec/**` and this folder. The ports (FI-3), the copybooks (FI-1), the
fixtures and comparator (FI-2), the Makefile (FI-6), the calculation, the batch package and the pom are untouched.

## 2. What the ticket does not cover (gaps)

| Gap | State in this branch | Owner |
|---|---|---|
| `format=host` of the job | still refused by `InterestJobParameters` ("the host format is ticket 03"). That class is in package `batch`, outside this ticket's Allowed files, and the file adapter has no host reader or writer. The codec is complete for the host format (`CodePage.host()`, `RecordImage.hostRecords`) and proven by tests only | 05 / 07 (loader and export), with a one-line change in `batch` |
| The host code page | a parameter of `CodePage.host(name)`, default IBM037; REL-08 and G01 stay open. No job parameter carries it yet | mainframe data owner; 05 / 07 |
| Layouts are carried in the jar | `CardDemoLayouts` holds the data description lines of the five copybooks as text, parsed by `Copybook` at class load; the job does not read `app/cpy` at run time. `CopybookTest` reads `app/cpy` and fails when a copybook and its copy differ. A build step that packages `app/cpy/*.cpy` as resources would remove the copy; `pom.xml` and `src/main/resources` are outside the Allowed files (finding 1) | architect |
| Copybook clauses | handled: levels, groups, FILLER, PIC X/9/S9/V with counts, USAGE DISPLAY, VALUE, level 88. Refused per field, never guessed: COMP, COMP-1..5, BINARY, PACKED-DECIMAL, POINTER, INDEX, NATIONAL, OCCURS, REDEFINES, SIGN LEADING / SEPARATE, SYNC, JUSTIFIED, edited pictures, any other clause | - |
| G02, G03 on a host file | a whole-record decode and encode turns F-signed into C-signed and -0 into +0, as E2 says, so such a record differs by one byte per amount. The account update avoids it: it replaces three fields and keeps every other byte | 05 (RC3 counts them) |
| G06 key order | the file adapter still orders balances by the fixture bytes of the key; unchanged by this ticket | 06 |
| Database, calculation | not touched (Excluded work) | 04, 05, 06 |
| TRANCATG, TRANTYPE | not read by CBACT04C and not among the five layouts; round-tripped in the test only, with the copybooks parsed from `app/cpy` | - |

## 3. For the decision register

| # | Finding | Evidence | Suggested owner |
|---|---|---|---|
| 1 | **"Copybook-driven" at run time needs the copybooks in the jar.** The ticket allows only `codec/**` Java, so the five data descriptions are text constants guarded by a test against `app/cpy`. Decide: keep the guarded copy, or package `app/cpy` as a resource (pom change). | `CardDemoLayouts`, `CopybookTest.fi1_layoutCarriedInTheJarEqualsTheCopybookOfTheEstate` | architect |
| 2 | **The data map counts 43 fields, the copybooks hold 41 elementary fields.** E5 says "none of the 43 fields"; 43 counts the two key groups TRAN-CAT-KEY and DIS-GROUP-KEY. G07 says "every PIC X field (20 fields)" - the five layouts hold 20 PIC X fields plus 5 FILLER. Wording only. | `CopybookTest.e5_theFiveLayoutsHold41ElementaryFieldsAllDisplay` | data architect |
| 3 | **What "not a character of the code page" means (G07) is now fixed in code:** a byte whose character is a control character (U+0000-U+001F, U+007F-U+009F) or has no mapping. cp037 and ISO-8859-1 map all 256 bytes, so without this rule nothing would ever be rejected. The pilot rejected only bytes below X'20'. A host field holding X'FF' or a tab is therefore rejected. Needs the data owner's confirmation with the trial-load profile. | `CodePage.text`; `TextAndFillerTest.g07_*` | data owner |
| 4 | **Zoned signs in the host format are read by nibble, not through the code page.** In cp037 the braces are X'C0' and X'D0', in cp273 they are X'43' and X'DC'. A codec that translated first and then looked for "{" would be wrong outside cp037. So REL-08 changes text only, never amounts. | `ZonedDecimalTest.rel08_hostCodePageIsAParameter_zonedBytesDoNotDependOnIt` | mainframe data owner |
| 5 | **Every field of every input record is now validated, not only the fields the job uses.** A bad byte in, say, ACCT-OPEN-DATE or XREF-CUST-ID now stops the run (RECORD_REJECTED), where the pilot and CBACT04C would not notice. This is what E1-E3 prescribe at the boundary, and it is stricter than the legacy. Balance records are still decoded when read, account, card and rate records when the run opens. | `RecordLayout.decode`; `FileRunWorkspace` | business owner |
| 6 | **A fixture line cannot show a short record.** The recorded format strips trailing spaces, so a truncated record and a record ending in spaces look the same; only a line longer than the record is rejected. In the host format a file whose size is not a multiple of the record length is rejected. | `RecordCodecErrorTest.recordOfTheWrongLength_*` | - |
| 7 | **A copybook entry running past column 72 would merge with the next entry.** The reader refuses such a field ("clause ... is not handled") instead of dropping the next field silently. | `CopybookTest.entryCutAtColumn72IsRefusedNotMergedSilently` | - |

## 4. Standards review checklist

Standards:

- [x] S1.1 Sources used in order; recorded output decides - PASS: layouts from the copybooks, rules E1-E5 from the data map, proven on all recorded files (`FixtureRoundTripTest`)
- [x] S1.2 Rule/code disagreements reproduced as coded and reported - PASS: findings 2 and 3 report where the data map's wording and the code differ; no behaviour of CBACT04C is changed
- [x] S2.1 Designed from rules; no method per paragraph - PASS: `RecordLayout` has one decode and one encode per encoding rule; nothing mirrors a paragraph
- [x] S2.2 Immutable business records; copybook names only at the mapping - PASS: copybook field names appear only in package `codec` (`AccountRecord` etc. map them to `Account`, `CategoryBalance`, ...); `DecodedRecord`, `LayoutField`, `RecordSource` are immutable
- [x] S2.3 Spring Batch shape, transaction and restart boundaries of the architecture - N/A: no job code changed
- [x] S2.4 Types and return values, no flags or result codes - PASS: `FieldKind` enum, BigDecimal/String/byte[] values; rejection is an exception, not a code
- [x] S2.5 Constructor injection; no mutable static state - PASS: the static fields of `CardDemoLayouts` and `CodePage` are immutable constants; no Spring bean added
- [x] S3.1 `BigDecimal` with data-map precision and scale; no `double`/`float` - PASS: numbers decode to BigDecimal with the scale of the PIC (`ZonedDecimalTest`); no double or float in the package
- [x] S3.2 Formula: exact product, one division, mode from the policy - N/A: no calculation
- [x] S3.3 Rounding mode named on every other arithmetic step - PASS: the only scale change is `setScale(scale, RoundingMode.UNNECESSARY)` in `RecordLayout.encodeNumber`; a value with more decimals is refused (`ZonedDecimalTest.e1e2_valueThatDoesNotFitIsRefusedNotCutOrRounded`)
- [x] S3.4 Range check before store; overflow goes to the policy - PASS for the codec: a value that does not fit is refused, not cut (same test); the policy switch is ticket 04's
- [x] S3.5 Encodings only in `record-codec` - PASS: zoned digits, signs and text bytes only in `CodePage` and `RecordLayout`; `FileRunWorkspace` calls them
- [x] S3.6 Width, padding and FILLER only in codec and schema - PASS: `RecordLayout.encodeField`, `RecordImage`; `TextAndFillerTest`, `RecordLayoutTest.e4_accountFillerRoundTrip`
- [x] S3.7 Code page and key order each in one place - PASS: `CodePage` is the one place (`FixtureCodePage.CHARSET` now points to it); key order unchanged in `FileRunWorkspace`
- [x] S3.8 Clock and run parameters injected; `parmDate` is text - N/A: not touched
- [x] S4.1 Every PRESENT behaviour touched has its test; new ones reported - PASS: rows d, e, f, g, h, j below; none new
- [x] S5.1 Each quirk/open decision in one named place behind its switch, rule id in names - PASS: REL-08 in `CodePage.DEFAULT_HOST_CHARSET` / `CodePage.host(name)`; G02/G03 in `RecordLayout.encodeNumber`
- [x] S5.2 Nothing fixed, tidied or added without a rule id - PASS with a note: stricter validation of unused fields follows E1-E3 and is reported as finding 5
- [x] S6.1 Status and message mapping as in section 6 - PASS: rejections are `InterestRunStoppedException(RECORD_REJECTED)` as before; only the detail text gained file, record and field
- [x] S6.2 One stop exception, one exit-code constant, no skip or retry - PASS: no new exception type for records; command-line run in section 6 exits 12
- [x] S6.3 Restart as in the architecture - N/A
- [x] S6.4 Outputs in the recorded format; failed run leaves no SYSTRAN - PASS: parity-report.md; section 6 (only `input` left after a rejected record)
- [x] S6.5 Run log byte for byte; no account data in the application log - PASS: SYSOUT compares equal (parity-report.md). The rejection message holds file, record number, field name and one byte in hex, never a field value
- [x] S7.1 A test per rule id, named for it - PASS: `intT06_*`, `e1e2_*`, `e3_*`, `e4_*`, `e5_*`, `g02g03_*`, `g07_*`, `rel08_*`, `fi1_*`
- [x] S7.2 Fixture and edge cases of each rule covered - PASS: all 228 fixture records; largest and smallest values, -0, F sign, wrong bytes in each position class
- [x] S7.3 Integration tests on PostgreSQL 15 in a container - PASS: `InterestCalcJobIT` unchanged, 12 tests (test-output.txt)
- [x] S7.4 No assertion against the code's own output - PASS: expected bytes and values are written by hand (text and hex); the cp037 records are made with the JDK charset, not the codec; record counts from `wc -l`
- [x] S8.1 No edit under `app/`, `fixtures/`, `tools/parity` - PASS: `git diff --stat origin/demo/05b-integration -- app fixtures tools modernization .agents` is empty
- [x] S8.2 No golden file re-recorded - PASS
- [x] S8.3 Every new dependency explained in the pull request - PASS: none (section 7)
- [x] S8.4 Frozen shared interfaces unchanged - PASS: FI-1, FI-2, FI-3, FI-6 untouched; FI-4 is ticket 02's
- [x] S8.5 Synthetic data only - PASS: test records use the fixture's synthetic ids (00000002001, 4000000000002001) and made-up text

Behaviours PRESENT in section 4:

- [x] a Truncation without ROUNDED - N/A: calculation
- [x] b High-order digit loss (range check, STOP) - PASS for the codec: refused, not cut (`ZonedDecimalTest`, `RecordLayoutTest.intQ02_idLongerThanTheFieldIsRefused`)
- [x] c Intermediate precision of the COMPUTE - N/A
- [x] d Zoned overpunch signs - PASS: `ZonedDecimalTest` (21 value rows in two code pages, G02, G03)
- [x] e Binary items stay out of data and domain - PASS: COMP and COMP-3 are refused (`CopybookTest.e5_*`, `RecordCodecErrorTest.e5_*`)
- [x] f Non-numeric data rejected at the boundary - PASS: `ZonedDecimalTest.e1e2_nonDigit*`, `RecordCodecErrorTest.e1_*`, `FileRunWorkspaceTest.e1_badDigitInABalanceRejectsTheRecord` (unchanged, passes)
- [x] g Fixed width, padding, STRING - PASS: `TextAndFillerTest.e3_*`, `RecordLayoutTest.intR06_transactionFieldsFixedWidth`
- [x] h Code page and collating order - PASS for the code page: `CodePage`, `TextAndFillerTest`, `rel08_*`; collating order not touched
- [x] i REDEFINES replaced by formatter and function - N/A: none in the five layouts; a REDEFINES field is refused
- [x] j FILLER and record length round trip - PASS: `TextAndFillerTest.e4_*`, `FixtureRoundTripTest`, `RecordCodecErrorTest.recordOfTheWrongLength_*`
- [x] l No state carried between accounts except the counter - N/A
- [x] m Control break and end of file (INT-Q04, INT-T09) - N/A; parity unchanged
- [x] o Level-88 result codes replaced by types - N/A
- [x] p Indexed access through the ports; several cards to the policy - N/A: ports untouched
- [x] q File statuses and abend - N/A
- [x] r PARM, generation, disposition, return code - N/A
- [x] s DISPLAY output in the run log - PASS: SYSOUT equal on the five passing cases (parity-report.md); the echoed record image is still the input record
- [x] t PARM text against the clock - N/A
- [x] u Timestamp format and hundredths - N/A: the codec writes the 26 characters it is given (INT-T05 is tested in the domain)
- [x] w PARM length checked - N/A
- [x] x No stale rate after a failed lookup - N/A
- [x] y Typed fields where COBOL mixed text and numbers - PASS: numbers and text have different accessors; a wrong kind is refused (`TextAndFillerTest.valuesAreReadAndSetByTheKindOfTheField`)
- [x] z Abend ends the step; stub output not reproduced - N/A
- [x] aa Dead and empty code not ported - PASS: the pilot's `Field` class and its test are removed, replaced by the layout codec

## 5. What is wrong or missing in the standards skill (proposals only)

1. **S3.5/S3.6 do not say what the codec validates.** Proposal: "the codec decodes and validates every field of a record,
   used by the job or not; a record with a bad byte in any field is rejected" - or the opposite. Today both readings
   satisfy the text (finding 5).
2. **Section 4 row f / G07 has no definition of "not a character".** Proposal: add the rule of finding 3 (control
   characters and unmapped bytes) to row f, or name the fields where low-values are legal.
3. **Row d should say that host sign bytes are read by zone and digit nibble**, never by translating to characters first
   (finding 4). Today it only names the overpunch characters.
4. **No standard for where the layouts come from at run time.** Proposal for S3.6: "layouts are read from the copybook
   text; when a copy is carried in the jar a test compares it with `app/cpy`".
5. **Error text.** Section 6 gives the stop reasons but not the form of the detail. Proposal: "a rejection names file,
   record number (from 1) and copybook field, and never prints a field value" (S6.5 then holds by construction).
6. **S7.4** could name the accepted independent sources for expected bytes: hand-written text or hex, the JDK charset for
   whole-record translation, `wc -l` for counts.

## 6. Method

Skills used: `interest-conversion-standards` (sections 3, 4 rows b/d/e/f/g/h/j/y, 6, 7, 8, 9), `interest-ticket-build`
(steps 1-9: JDK 21, build, unit and integration tests, scope check), `interest-ticket-verify` (steps 1-6, 8-10),
`interest-pitfalls` (JDK, db network, record length, tracked `.pyc`, stale compare.md).

```sh
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
mvn -B -f java/interest/pom.xml clean verify                                  # test-output.txt
.agents/skills/interest-ticket-verify/parity-report.sh 03-record-codec i01-baseline   # parity-report.md
```

Error path from the command line (`interest-ticket-verify` step 6), with the second balance record of i01 damaged
(`0000000200201000100X0024000{`):

```
com.carddemo.interest.domain.InterestRunStoppedException: RECORD_REJECTED (INT-R08): file AWS.M2.CARDDEMO.TCATBALF.PS.txt, record 2, field TRAN-CAT-BAL: not numeric: byte X'58' at position 3 of the field (E2)
... c.c.interest.batch.CalcInterestRun       : run bad-1794770020 step calcInterestStep stopped: accounts=0 transactions=0; no output kept
job exit=12
input
```

Failures hit:

| Symptom | Cause | Fix |
|---|---|---|
| Five new tests failed on the first run; the codec was right in four of them | hand-written expectations were wrong: S9(10)V99 `00000240000{` is 24000.00, not 2400.00; `00000003000}` as S9(10)V99 is -300.00; 10 as S9(09)V99 is `0000000100{`; the data map's "43 fields" counts two groups | the expectations were recomputed by hand from the PIC (digits, two decimals) and corrected; no assertion was removed |
| The fifth: a test copybook with `COMP-3.` past column 72 parsed as a DISPLAY field and swallowed the next entry | columns 73-80 are not data; the cut entry merged with the following one | the reader now refuses a field with a clause it does not know (finding 7); test `entryCutAtColumn72IsRefusedNotMergedSilently` |

Design in short: `Copybook` reads data description text into a `RecordLayout` (fields with offset, length, kind, scale);
`RecordLayout` decodes bytes to a `DecodedRecord` and back, or replaces one field in place; `CodePage` holds the bytes of
digits, signs, space and text for the fixture and the host format; `RecordImage` handles the line form of the fixture
files and the fixed-block form of host files; `RecordSource` carries file and record number into every message. The
five `*Record` classes keep their role (copybook names to business records) on top of it, and `FileRunWorkspace` passes
each record's file name and number.

## 7. Dependencies

None. `junit-jupiter-params` (`@ParameterizedTest`) is already on the test class path through `spring-boot-starter-test`.
The cp037 charset (`IBM037`) is in the JDK's `jdk.charsets` module, present in the `eclipse-temurin:21-jre` image the
parity run uses; considered and not used: a hand-written EBCDIC table (a second source of truth for REL-08).
