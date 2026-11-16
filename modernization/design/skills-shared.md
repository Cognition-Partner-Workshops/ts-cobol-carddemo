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

Classified by reading the text only. Whether a shared rule holds for a second stream is not proven here; section 5
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

## 2. The skills after the split

| Skill | What it holds | State |
|---|---|---|
| `.agents/skills/carddemo-conversion-standards/SKILL.md` | the 32 standards by id with the text that names no stream, the list of 27 COBOL behaviours to ask of any program, the checklist template | PROPOSED; proven on interest only |
| `.agents/skills/carddemo-ticket-build/SKILL.md` | build steps 1 to 10 with `<stream>` in every path | proven on interest only |
| `.agents/skills/carddemo-ticket-verify/SKILL.md` | verify steps 1 to 10 with `<stream>`, `<parity-target>`, `<out>`; the combined check | proven on interest only |
| `.agents/skills/carddemo-pitfalls/SKILL.md` | P1, P2, P3, P4, P6, P7, P9, P11, P12 without interest's names | seen on interest only |
| `.agents/skills/carddemo-stream-interest/SKILL.md` | the interest annex: stream facts, every standard as it reads for interest with its Cites, Do and Do not, the behaviours table, the legacy-event table, the checklist, the interest text of the build and verify steps, all pitfalls with their reproducing commands | PROPOSED; followed in section 4 |
| `.agents/skills/carddemo-stream-posting/SKILL.md` | the posting annex | **DRAFT - NOT PROVEN** |
| `.agents/skills/interest-conversion-standards`, `interest-ticket-build`, `interest-ticket-verify`, `interest-pitfalls` (`SKILL.md` each) | a pointer to the shared skill and the annex section; name and description kept | references still resolve |
| `.agents/skills/interest-ticket-verify/parity-report.sh` | unchanged, same path | interest only |
| `.agents/skills/carddemo-parity/SKILL.md` | unchanged | - |

How the split was made, so that nothing is lost:

* A standard keeps its id, its section number and its status. Where its text names nothing of interest it is in
  the shared skill word for word (S1.1, S1.2, S2.2, S3.5, S5.2, S8.1, S8.2, S8.3, S8.5). Where it does, the shared
  skill has the text without the interest names, and the annex has the standard **exactly as it was**, so the
  meaning applied by tickets 01 to 10 is still written down. Cites, Do and Do not are in the annex unchanged.
  The annex wins for interest if the two ever differ.
* S3.2 (the interest formula) is STREAM: its id is kept in the shared skill with no text, the standard is in the annex.
* Build steps, verify steps and pitfalls keep their numbers. The annex carries the original text of every MIXED and
  STREAM step and pitfall (with the three skill names updated); the shared skill carries the general step.
* The standards were PROPOSED with the target architect as owner. Both the shared skill and the annex say so.

Checked by script: every non-empty line of the four old skills is in a new skill, except their titles, front
matter, the five-column table headers and two sentences that only named the old skills (`interest-ticket-verify`
intro line 10, `interest-pitfalls` "How to use" line 1, both reworded in the shared skills). Every cell of every
standard row is in a new skill.

Added in step 5c, in no old skill: the placeholder paragraphs; section 1 of the interest annex (stream facts, read
from `chains.json`, `INTCALC.jcl`, the fixtures and the `Makefile`); the rows of shared build step 5 for
`fixtures/`, `modernization/rules/`, `db/postgres/` and the comparator; "Combined check of a stream" in
`carddemo-ticket-verify` (the four commands of `build-run.md` section 3); in shared P6 the sentence that
`make parity` also rewrites `tools/runjcl/__pycache__` (seen in section 4). No standard was added.

## 3. Where each section went

"Annex" is `.agents/skills/carddemo-stream-interest/SKILL.md`. "CS", "TB", "TV", "PF" are the shared skills
`carddemo-conversion-standards`, `carddemo-ticket-build`, `carddemo-ticket-verify`, `carddemo-pitfalls`.

### 3.1 `interest-conversion-standards`

| Old section | New: shared | New: annex |
|---|---|---|
| Front matter (name, description) | kept in the pointer file; CS has its own | Annex front matter |
| Banner (PROPOSED, owner) | CS banner | 2, "Head ... before the split" (word for word) |
| "This file is the only place ..." paragraph | CS, paragraph after the banner | 2, same block |
| Sources paragraph | CS, "How to read a standard" | 2, intro and same block |
| 1. What to convert from - the four sources | CS 1 | 2.1 |
| S1.1, S1.2 | CS 1 | 2.1 |
| S2.1 to S2.5 | CS 2 | 2.2 |
| S3.1, S3.3 to S3.8 | CS 3 | 2.3 |
| S3.2 | CS 3 (id only) | 2.3 (the standard) |
| 4. preamble ("Verified against ...") | - | 2.4 |
| S4.1 | CS 4 | 2.4 |
| "Line numbers are ..." paragraph | - | 2.4 |
| Behaviours table, rows a to aa | CS 4 (behaviour and the standards that answer it) | 2.4 (every row, every column) |
| "What the off-host run cannot settle" | CS 4 (without the row list) | 2.4 (word for word) |
| 5. intro (QUIRK and HOLE are OPEN) | CS 5 (without the ids) | 2.5 |
| S5.1, S5.2 | CS 5 | 2.5 |
| 6. intro (one failure path) | CS 6 (pointer) | 2.6 |
| 6. table of legacy events, 17 rows | - | 2.6 |
| S6.1 to S6.5 | CS 6 | 2.6 |
| S7.1 to S7.4 | CS 7 | 2.7 |
| S8.1 to S8.5 | CS 8 | 2.8 |
| 9. checklist, procedure | CS 9 | 2.9 |
| 9. checklist, 32 standards lines | CS 9 (four lines without interest's words: S3.2, S3.8, S6.1, S6.4) | 2.9 (word for word) |
| 9. checklist, 24 behaviour lines | CS 9 (the form of a line) | 2.9 (word for word) |

### 3.2 `interest-ticket-build`

| Old section | New: shared | New: annex |
|---|---|---|
| Front matter | pointer file; TB has its own | - |
| Intro | TB intro | 3, first block |
| Step 1 Check the tools | TB 1 | 3 (versions the pilot was proven with) |
| Step 2 JDK 21 | TB 2 | 3 |
| Step 3 Nobody has the ticket | TB 3 | - |
| Step 4 Read in this order | TB 4 | 3 |
| Step 5 Layout | TB 5 (rows true for every stream) | 3 (the layout table of `java/interest`); 1 |
| Step 6 Start PostgreSQL 15 | TB 6 | 3 (Testcontainers sentence) |
| Step 7 Build | TB 7 | 3 (second main class) |
| Step 8 Run the tests | TB 8 | 3 (pilot figures, test class); 1 |
| Step 9 Before every commit | TB 9 | 3 (`InterestJobConfig`) |
| Step 10 Go on with verify | TB 10 | - |

### 3.3 `interest-ticket-verify`

| Old section | New: shared | New: annex |
|---|---|---|
| Front matter | pointer file; TV has its own | - |
| Intro | TV intro | 4, first block |
| Step 1 Run the job on a case | TV 1 | 4 (what `parity-java` does); 1 |
| Step 2 Files the comparator reads | TV 2 | 4 |
| Step 3 Compare each case | TV 3 | 4 (pilot results) |
| Step 4 Run the job by hand | TV 4 (pointer) | 4 (the step) |
| Step 5 Read a difference | TV 5 | 4 (the five rows) |
| Step 6 Error path | TV 6 (pointer) | 4 (the step) |
| Step 7 Database assertions | TV 7 (table list) | 4 (job repository, `interest_run`) |
| Step 8 Write the evidence | TV 8 | 4 (`parity-report.sh`, the table of ticket cases) |
| Step 9 Write the ticket's report | TV 9 | 4 (the pilot's report as the model) |
| Step 10 Commit, pull request | TV 10 | - |
| `parity-report.sh` | - | unchanged at `.agents/skills/interest-ticket-verify/parity-report.sh`; cited in 1 and 4 |
| (new) Combined check | TV, "Combined check of a stream" | 4, "The combined check, as recorded for interest" |

### 3.4 `interest-pitfalls`

| Old section | New: shared | New: annex |
|---|---|---|
| Front matter | pointer file; PF has its own | - |
| Intro | PF intro | 5, first block |
| How to use | PF "How to use" | - |
| P1 | PF P1 | 5 P1 (reproducing command) |
| P2 | PF P2 | 5 P2 |
| P3 | PF P3 | 5 P3 |
| P4 | PF P4 | 5 P4 |
| P5 | - (number kept free) | 5 P5 |
| P6 | PF P6 | 5 P6 |
| P7 | PF P7 | 5 P7 |
| P8 | - (number kept free) | 5 P8 |
| P9 | PF P9 | 5 P9 |
| P10 | - (number kept free) | 5 P10 |
| P11 | PF P11 | 5 P11 |
| P12 | PF P12 | 5 P12 |
| Reported, not reproduced (pilot: none; ticket 05: `search_path`) | PF "Pitfalls of one stream" (pointer) | 5 "Reported, not reproduced" |


## 4. The shared skills proven on interest

A fresh clone of `demo/05c-shared-skills` at `39e5fb4` in an empty directory: no `java/interest/target`, no `work/`,
`loadlib/` or `datasets/`, a Docker Compose project of its own (new `db` container, new volume, new `estate`
image), a shell without `JAVA_HOME`. Only the four shared skills and the interest annex were followed, with
`<stream>` = `interest`. Tools: JDK 21.0.13, Maven 3.6.3, Docker Engine 29.9.2, Docker Compose v5.6.1,
Python 3.10.12, GNU Make 4.3, PostgreSQL 15.20. Full transcript:
`modernization/design/evidence/05c-shared-skills-check.txt`.

| Skill step | Command (with `<stream>` = `interest`) | Result |
|---|---|---|
| build 1 | `docker --version && docker compose version && python3 --version && make --version \| head -1` | versions above |
| build 2 | `export JAVA_HOME=...; mvn -v` | `Java version: 21.0.13.0.3` |
| build 3 | `git fetch origin; git ls-remote --heads origin 'demo/*'; gh pr list ...; git checkout -b ...` | 17 `demo/*` branches, the open pull requests listed, branch created |
| build 4 | `cat -A fixtures/interest/i01-baseline/expected/rc.json` | no final newline |
| build 6 | `docker compose up -d --wait db; ... psql ... "show server_version"` | `Healthy`, `15.20` |
| build 7 | `mvn -q -f java/interest/pom.xml -DskipTests package && ls java/interest/target/interest.jar` | jar listed |
| build 8 | `mvn -B -f java/interest/pom.xml verify` | **396 unit, 105 integration, 0 failures, 0 errors, BUILD SUCCESS** |
| build 8 | `mvn -B -f java/interest/pom.xml test -Dtest=InterestFormulaTest` | 8 tests, BUILD SUCCESS |
| build 9 | `git checkout -- tools/parity/__pycache__; git diff --stat origin/<base> -- ...; git status --short` | nothing printed |
| verify 1 | `make parity-java CASE=i01-baseline` (annex) | `PASS (4 records, 50 fields, 0 differences)`, `1 of 1 cases PASS` |
| verify 2 | `find work/parity-java/i01-baseline -type f \| sort && wc -l .../datasets/*` | two datasets of 2 records, `rc.json`, the SYSOUT file, `recon.md` |
| verify 3 | `python3 tools/parity/compare.py interest <case> --actual work/parity-java/<case>/actual --label java`, each case | table below |
| verify 4 | the job by hand with `--interest.policy.rounding=HALF_UP` on i02 (annex) | job exit 0; comparator `FAIL (... 3 differences)`, as the annex says |
| verify 6 | the job by hand without the CARDXREF input (annex) | `OPEN_INPUT (INT-R08)`, exit 12, no output directory |
| verify 7 | the table list (shared); job repository and `interest_run` (annex) | eight tables; `interestCalcJob COMPLETED`, three steps in order; run `EXPORTED` |
| verify 8 | `mvn -B -f java/interest/pom.xml clean verify` into `test-output.txt` | `# exit code 0`, 396 and 105, BUILD SUCCESS |
| verify 8 | `parity-report.sh 09-parity-gate $(ls fixtures/interest)` with `REPORT=work/...` (annex) | seven cases `0 0`, exit 0 |
| pitfalls | P3 `docker compose port db 5432`; P6 `git status --short`; P7 `grep -c '^## '` | `:0`; the tracked `.pyc` changed; 7 sections after one loop |
| combined | `mvn ... clean verify`; `make up && make build`; `make parity-java`; `make parity` | table below |

Every case under `fixtures/interest` through `tools/parity/compare.py`:

| Case | Comparator | Exit |
|---|---|---|
| i01-baseline | PASS (4 records, 50 fields, 0 differences) | 0 |
| i02-tie-half-cent | PASS (4 records, 50 fields, 0 differences) | 0 |
| i03-zero-rate-skipped | PASS (2 records, 25 fields, 0 differences) | 0 |
| i04-default-group-fallback | PASS (2 records, 25 fields, 0 differences) | 0 |
| i05-multi-category | PASS (3 records, 38 fields, 0 differences) | 0 |
| i06-negative-balance | PASS (2 records, 25 fields, 0 differences) | 0 |
| i07-nontie-rounding | PASS (4 records, 50 fields, 0 differences) | 0 |

Against the combined check recorded in `build-run.md`, section 3:

| Command | Recorded in step 5b (`079b641`) | Step 5c, clean checkout | Equal |
|---|---|---|---|
| `mvn -B -f java/interest/pom.xml clean verify` | 396 unit, 105 integration, 0 failures, 0 errors, BUILD SUCCESS | 396 unit, 105 integration, 0 failures, 0 errors, BUILD SUCCESS | yes |
| `make up && make build` | exit 0 | exit 0 | yes |
| `make parity-java` | 7 of 7 cases PASS, exit 0 | 7 of 7 cases PASS, exit 0 | yes |
| `make parity` (legacy chains) | all 24 cases PASS, exit 0 | all 24 cases PASS, exit 0 | yes |

**Corrections the check needed: none to reach the result.** Three things were changed in the skills afterwards
and are in the transcript's addendum: build step 8 and verify step 8 were first run in a slightly different
form than printed (an added `| tail -6`; double for single quotes) and were run again exactly as printed; the
first command of the combined check now uses the grep of verify step 8, so that both test totals show, and
was run in that form. The outputs quoted in the shared skills are from this run.

What the check does not show: that an agent with no knowledge of interest would have been able to follow the
skills. The session that ran it wrote them. A replay by another session is the test for that.

## 5. What the posting draft found

`.agents/skills/carddemo-stream-posting/SKILL.md`, **DRAFT - NOT PROVEN**: written from the source, nothing run
except `make parity`, where the seven posting cases of the legacy chain passed.

**Standards (32).** UNCHANGED 12: S1.2, S2.2, S2.4, S3.3, S3.5, S5.2, S7.3, S7.4, S8.1, S8.2, S8.3, S8.5.
Need a posting CITATION 18: S1.1, S2.1, S2.3, S2.5, S3.1, S3.2, S3.4, S3.6, S3.7, S3.8, S4.1, S5.1, S6.1, S6.3,
S6.4, S7.1, S7.2, S8.4. GAP 2: S6.2 (a reject path and return code 4 beside the abend), S6.5 (the report dataset).

**Behaviours (27).** ABSENT in the two programs 5: a, c, k, v, w. PRESENT 22: shared standard UNCHANGED 8 (e, f,
i, o, x, y, z, aa), needs a CITATION 10 (b, d, g, j, m, p, q, s, t, u), leaves a GAP 4 (h, l, n, r).

**Missing from the shared standards** - eleven proposals for the target architect, each with its line, in section
4 of the draft:

1. Utility steps (IDCAMS REPRO, SORT with `INCLUDE COND` and a zoned sort key) - `TRANREPT.jcl:23-55`.
2. A printed report as an output: edited pictures, page breaks, totals - `CBTRN03C:274-374`, `CVTRA07Y:30-66`.
3. A rejected record that does not stop the run - `CBTRN02C:208-216, 446-451`.
4. A normal end with return code 4 - `CBTRN02C:229-230`.
5. The record area used after end of file (the last amount is counted twice) - `CBTRN03C:172-174, 197-203`.
6. `NEXT SENTENCE` - `CBTRN03C:177`.
7. Run dates from a parameter file, given a second time in the JCL - `CBTRN03C:220-243`, `TRANREPT.jcl:43-44`.
8. The order of validations, last reason wins - `CBTRN02C:403-420`.
9. An error that is noted and ignored - `CBTRN02C:554-559`.
10. Create-or-update of a keyed record; `OPEN OUTPUT` of an indexed file - `CBTRN02C:256, 474-528`.
11. A stream of two jobs and two programs.

**Missing from the shared procedure.** Every command that builds, runs or reports on Java works for interest
only (section 5 of the draft): `make parity-java`, `recon-java`, `sidebyside-java`, `parity-naive`, `rules`,
`design`, `parity-report.sh`, and every `mvn -f java/<stream>/pom.xml` until `java/posting` exists. Only the
legacy side takes `posting` today: `make run`, `make parity`, `tools/parity/compare.py posting <case>`.

**Assumed, not decided.** The shared skills write the module as `java/<stream>` with the jar
`java/<stream>/target/<stream>.jar`, the evidence as `java/<stream>/evidence/<ticket>/`, and the design documents
as "the stream's architecture and data map". Interest has `modernization/design/architecture.md`,
`data-map-interest.md` and `tickets/` with no stream in the path of two of the three; where posting's go is for
the owner to say. The encoding rules E1-E6 and the record codec are in interest's data map and module; four of
posting's copybooks are the same ones, so whether the codec is shared is a design decision, not made here.

## 6. What a new stream writes, and what it reuses

**Writes.**

| What | Where | Notes |
|---|---|---|
| Its annex | `.agents/skills/carddemo-stream-<stream>/SKILL.md` | the sections of the interest annex: 1 stream facts (programs, jobs, datasets, copybooks, cases, rule-id prefix, `<parity-target>`, `<out>`); 2 for every standard id the citation and a do / do-not example, and the stream's wording where the shared text leaves a value open; the behaviours table, one row per letter with the lines; the legacy-event table; its checklist; 3 and 4 its layout, job parameters, error path, database queries, report script; 5 its pitfalls |
| Its rules | `modernization/rules/<stream>/rules.yaml`, `decisions.md` | with its own rule-id prefix; every QUIRK and HOLE gets a switch (S5.1) |
| Its design | architecture, data map, tickets | location to be decided (section 5) |
| Its stream standard S3.2 | its annex | the formula or decision logic of its calculation |
| Its code and tests | `java/<stream>` | proving itself with `tools/parity/compare.py <stream> <case>` |
| Its parity target | `Makefile` | today's `parity-java` is interest's; a second stream needs its own or a parameter. The `Makefile` was not changed in this step |
| Its own pitfalls | section 5 of its annex | a pitfall whose cause is not in the stream's code is proposed for `carddemo-pitfalls` |
| Proposals | its pull requests | a behaviour with no letter, a standard that does not fit: for the target architect; it does not edit the shared skills |

**Reuses unchanged.**

* `carddemo-conversion-standards`: the 32 ids and their shared text, the 27 behaviour questions, the checklist form.
* `carddemo-ticket-build` and `carddemo-ticket-verify`: every step, with its own values for the placeholders.
* `carddemo-pitfalls`: P1, P2, P3, P4, P6, P7, P9, P11, P12.
* `carddemo-parity`, `tools/parity/compare.py`, the fixtures, `make up`, `make build`, `make parity`.
* The evidence layout (`test-output.txt`, `parity-report.md`, `report.md` with seven sections, one folder per ticket).

**Does not reuse.** The interest annex, `parity-report.sh`, the `parity-java`, `recon-java` and `sidebyside-java`
targets, and the stream standard S3.2 of interest.

## 7. What is proven and what is not

* **The shared skills are proven on one stream only: interest.** They were written from interest's skills and
  checked by following them on interest. That shows the split lost nothing interest needs. It does not show that
  the shared text is right for another stream.
* The posting annex is a draft from reading the source. It already shows that 18 of the 32 shared standards need
  a value posting does not have yet, that 2 standards and 4 behaviour rows have gaps, and that eleven behaviours
  of two programs have no standard. A posting pilot ticket is the first real test of the shared skills.
* The classification in section 1 is by the text. 87 of 108 items are MIXED: most of what the pilot learned is a
  general rule that was only ever written with interest's example. Until a second stream has applied a rule, its
  SHARED or MIXED label is a claim.
* The standards are still PROPOSED. The thirteen corrections still open in `skills.md` section 4 and the
  proposals of tickets 02 to 10 in `build-run.md` section 6 were not applied and not touched; they now refer to
  text that is in the shared skill and the interest annex under the same ids.
* Not done in this step: no change to `app/`, `fixtures/`, the comparator, `java/` or the `Makefile`; no ticket,
  rule or design document changed; `modernization/design/skills.md`, `standards.md` and `build-run.md` still name
  the four interest skills, which now point on.
