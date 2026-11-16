# Shared skills and stream annexes

> **PROPOSED - not approved.** Written by an agent in step 5c from the four interest skills on
> `demo/05b-integration` at `b92b9d1`. No application code, ticket, rule or design document was changed, and
> `carddemo-parity` was not edited. The standards stay PROPOSED.
> **Owner who must confirm: the target architect.**

## 1. Classification

Each item of the four interest skills is one of:

* **SHARED** - true for any batch stream of this estate; the text and its example name nothing of one stream.
* **STREAM** - true only for interest: the rule itself is a fact about `CBACT04C`, `INTCALC` or `java/interest`.
* **MIXED** - the rule is shared; its citation, example, path or value is interest's.

Classified by reading the text only. Whether a shared rule holds for a second stream is not proven here; section 6
says what the posting draft found.

### 1.1 Standards (`interest-conversion-standards`, sections 1 to 8)

| Id | Class | Reason |
|---|---|---|
| S1.1 | MIXED | source order is general; example cites INT-R05 and i02 |
| S1.2 | MIXED | "reproduce the code, report it" is general; example is INT-Q04 |
| S2.1 | MIXED | design from rules is general; "program has none" and example are CBACT04C's |
| S2.2 | MIXED | records per layout is general; fields and example are interest's |
| S2.3 | MIXED | follow the architecture, ports only; the step, item and restart state are interest's |
| S2.4 | MIXED | types replace flags; the flag names are CBACT04C's |
| S2.5 | MIXED | constructor injection is general; the counter and constructor are interest's |
| S3.1 | MIXED | `BigDecimal` at data-map scale; the four precisions are interest's |
| S3.2 | STREAM | the interest formula and its rounding switch |
| S3.3 | MIXED | name every rounding mode; the sums named are interest's |
| S3.4 | MIXED | check against the PIC before store; switch, default and INT-H05 are interest's |
| S3.5 | MIXED | encodings only in the codec; cites and line numbers are interest's |
| S3.6 | MIXED | width and FILLER in codec and schema; `TRAN-DESC`, three columns, id digits are interest's |
| S3.7 | MIXED | one place each for code page and key order; the `ORDER BY` and adapter are interest's |
| S3.8 | MIXED | injected clock and parameters; `parmDate` and the stamp format are interest's |
| S4.1 | MIXED | reproduce every PRESENT behaviour; the rows are CBACT04C's |
| S5.1 | MIXED | one named place behind a switch; switch and example are INT-Q04 |
| S5.2 | MIXED | fix nothing without a rule id; the examples are interest rules |
| S6.1 | MIXED | implement the stream's status table; status-line text and rule ids are interest's |
| S6.2 | MIXED | one stop exception, one exit code; class name and rule ids are interest's |
| S6.3 | MIXED | restart follows the architecture; the three steps and INT-H02 are interest's |
| S6.4 | MIXED | recorded output format (FI-2); SYSTRAN, ACCTDATA and the DISP are INTCALC's |
| S6.5 | MIXED | run log against application log; the echo and message are CBACT04C's |
| S7.1 | MIXED | a test per rule id; the id ranges are interest's |
| S7.2 | MIXED | cover the rule's cases; behaviours and path are interest's |
| S7.3 | MIXED | PostgreSQL 15 in a container; FI-4, cites and example are interest's |
| S7.4 | MIXED | expectations from recorded output; path and arithmetic are interest's |
| S8.1 | MIXED | no edit of estate, fixtures, comparator; "fixed in `java/interest`" |
| S8.2 | MIXED | no re-recording; example names the interest chain |
| S8.3 | SHARED | dependency rule and example name no stream |
| S8.4 | MIXED | frozen interfaces; FI numbers, `CardLookup`, the SQL file are interest's |
| S8.5 | MIXED | synthetic data only; the account in the example is an interest fixture |

Standards: 32 - SHARED 1, STREAM 1, MIXED 30.

### 1.2 COBOL behaviours (`interest-conversion-standards`, section 4, rows a to aa)

For every row the behaviour (the question asked of a program) is general and the answer is CBACT04C's: presence,
evidence lines, what the legacy does, the Java standard as worded, the test and the open point.

| Row | Class | Reason |
|---|---|---|
| a Decimal truncation, no ROUNDED | MIXED | general question; evidence 464-465, i02 |
| b High-order digits lost, no ON SIZE ERROR | MIXED | general question; INT-H05 |
| c Intermediate precision inside COMPUTE | MIXED | general question; the interest COMPUTE |
| d Sign handling, zoned overpunch | MIXED | general question; interest fields |
| e Packed and binary fields | MIXED | general question; "packed ABSENT" is interest's answer |
| f Non-numeric data in a numeric field | MIXED | general question; interest reads |
| g Fixed width, padding, STRING, reference modification | MIXED | general question; `TRAN-ID`, `TRAN-DESC` |
| h EBCDIC against ASCII, collating order | MIXED | general question; TCATBALF key |
| i REDEFINES | MIXED | general question; lines 126, 151 |
| j FILLER and record length across a round trip | MIXED | general question; five copybooks |
| k Tables: OCCURS | MIXED | general question; ABSENT is interest's answer |
| l WORKING-STORAGE kept between records | MIXED | general question; interest fields |
| m Control break and end of file | MIXED | general question; INT-Q04, INT-T09 |
| n GO TO, PERFORM THRU, fall-through | MIXED | general question; ABSENT is interest's answer |
| o Level-88 condition names | MIXED | general question; `APPL-RESULT` |
| p Indexed files | MIXED | general question; interest's four files, INT-H03 |
| q File status codes and abends | MIXED | general question; CBACT04C lines |
| r JCL: PARM, GDG, DISP, return code | MIXED | general question; `INTCALC.jcl` |
| s DISPLAY output compared by parity | MIXED | general question; `INTCALC.STEP15` |
| t Business date from PARM against the clock | MIXED | general question; PARM 2022071800 |
| u CURRENT-DATE, hundredths, timestamp format | MIXED | general question; lines 613-624 |
| v Two-digit years and Julian dates | MIXED | general question; ABSENT is interest's answer |
| w PARM length never checked (added) | MIXED | found in CBACT04C; asks of any program with a PARM |
| x Failed keyed READ leaves the record area (added) | MIXED | found in CBACT04C; asks of any keyed READ |
| y Alphanumeric and numeric items mixed (added) | MIXED | found in CBACT04C; asks of any program |
| z Abend by `CALL 'CEE3ABD'` (added) | MIXED | found in CBACT04C; stub is the harness's |
| aa Dead and empty code (added) | MIXED | found in CBACT04C; asks of any program |

Behaviours: 27 - SHARED 0, STREAM 0, MIXED 27.

### 1.3 Other parts of `interest-conversion-standards`

| Part | Class | Reason |
|---|---|---|
| Banner (PROPOSED, owner) | MIXED | status and owner carry over; "applied by the pilot ticket" is interest's |
| "Only place the standards are written" paragraph | MIXED | authority rule is general; names `standards.md` of interest |
| Sources paragraph | STREAM | interest's rules, data map and program |
| Section 1, the four sources in order | MIXED | order is general; the fixture path is interest's |
| Section 4 preamble (what was verified, how "Search" reads) | STREAM | describes the CBACT04C check |
| Section 4, "What the off-host run cannot settle" | MIXED | GnuCOBOL against host options is general; the row list is interest's |
| Section 5 intro (QUIRK and HOLE rules are OPEN) | STREAM | interest's rule ids and register |
| Section 6 intro (one failure path, abend 999) | STREAM | CBACT04C's failure path |
| Section 6 table of legacy events (17 rows) | STREAM | every row is a CBACT04C line range |
| Section 9 checklist, procedure | SHARED | copy, mark PASS / FAIL / N/A |
| Section 9 checklist, standards lines | MIXED | one line per id; five lines carry interest wording |
| Section 9 checklist, behaviour lines | STREAM | only the rows PRESENT in CBACT04C |

Other parts: 12 - SHARED 1, STREAM 6, MIXED 5.

### 1.4 Build steps (`interest-ticket-build`)

| Step | Class | Reason |
|---|---|---|
| Intro (commands only, run from the root) | MIXED | general; names the pilot and the interest skills |
| 1 Check the tools | SHARED | names no stream |
| 2 Put Maven on a JDK 21 | MIXED | general; names `make parity-java` |
| 3 Check nobody has the ticket, branch | SHARED | names no stream |
| 4 Read in this order | MIXED | order is S1.1; the paths are interest's |
| 5 Know the layout | MIXED | `db` service and fixtures layout are shared; the module layout is interest's |
| 6 Start PostgreSQL 15 | MIXED | command is shared; "tests use Testcontainers" is about interest's tests |
| 7 Build | MIXED | Maven command by module path; jar and second main class are interest's |
| 8 Run the tests | MIXED | same command by path; figures and test class are interest's |
| 9 Before every commit | MIXED | scope check is general; allowed files and the two cases are interest's |
| 10 Go on with verify | SHARED | pointer only |

Build: 11 - SHARED 3, STREAM 0, MIXED 8.

### 1.5 Verify steps (`interest-ticket-verify`)

| Step | Class | Reason |
|---|---|---|
| Intro | MIXED | general; names the pilot and the interest skills |
| 1 Run the job on a case and compare | MIXED | "one target per stream" is general; `make parity-java` runs interest only |
| 2 Know the files the comparator reads | MIXED | recorded layout is shared; file names are interest's |
| 3 Compare each case with the comparator | MIXED | `compare.py <chain> <case>` is shared; results are interest's |
| 4 Run the job by hand | STREAM | interest's jar, parameters and switches |
| 5 Read a difference | MIXED | how a report reads is shared; the five rows are interest's |
| 6 Check the error path | STREAM | interest's stop, classes and input file |
| 7 Check the database assertions | MIXED | job-repository query is general; schema, tables, `interest_run` are interest's |
| 8 Write the evidence | MIXED | commit first, one folder per ticket; script and exceptions table are interest's |
| 9 Write the ticket's report | MIXED | seven sections are general; the model file is the pilot's |
| 10 Commit the evidence, open the pull request | SHARED | names no stream |
| `parity-report.sh` | STREAM | every path in it is interest's |

Verify: 12 - SHARED 1, STREAM 3, MIXED 8.

### 1.6 Pitfalls (`interest-pitfalls`)

| Pitfall | Class | Reason |
|---|---|---|
| How to use | SHARED | names no stream |
| P1 Maven runs on the wrong JDK | MIXED | any Java 21 module; the command path is interest's |
| P2 Integration test cannot reach Docker | MIXED | Testcontainers 1.21.3 against Docker 29; the test and pom are interest's |
| P3 Job cannot reach the `db` service | MIXED | compose fact is shared; the jar and fix target are interest's |
| P4 rc.json differs although the job ended normally | MIXED | harness records setup steps; step names and writer are interest's |
| P5 `StringIndexOutOfBoundsException` from `codec/Field` | STREAM | a class of `java/interest` |
| P6 Changed `.pyc` under `tools/parity` | MIXED | repository fact; the reproducing command is interest's |
| P7 Comparator report holds old results | MIXED | comparator fact; the paths are interest's |
| P8 A test that reads JSON cannot use Jackson | STREAM | dependencies of the interest module |
| P9 No schema `carddemo` for the tables | MIXED | `CREATE SCHEMA` is only in the 01 script; the 02 script and test are interest's |
| P10 rc.json written outside package `file` | STREAM | interest's packages |
| P11 `compare.py` cannot compare two directories | MIXED | comparator fact; the fix target is interest's |
| P12 Package-private class a later ticket needs | MIXED | general rule; the classes are interest's |
| Reported, not reproduced: `search_path` is `batch` | STREAM | interest's `application.yml` |

Pitfalls: 14 - SHARED 1, STREAM 4, MIXED 9.

### 1.7 Counts

| Group | Items | SHARED | STREAM | MIXED |
|---|---|---|---|---|
| Standards S1.1 to S8.5 | 32 | 1 | 1 | 30 |
| COBOL behaviours a to aa | 27 | 0 | 0 | 27 |
| Other parts of the standards skill | 12 | 1 | 6 | 5 |
| Build steps | 11 | 3 | 0 | 8 |
| Verify steps | 12 | 1 | 3 | 8 |
| Pitfalls | 14 | 1 | 4 | 9 |
| **Total** | **108** | **7** | **14** | **87** |
