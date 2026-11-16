---
name: carddemo-conversion-standards
description: Conversion standards shared by every batch stream of the CardDemo estate moving to Java 21 / Spring Batch 5 / PostgreSQL 15 - the part of each standard that holds for any stream, with its id. The citations, examples and values of one stream are in that stream's annex (carddemo-stream-<stream>). Use when implementing or reviewing a ticket of any stream, together with the annex of the stream.
---

# CardDemo conversion standards (shared)

> **PROPOSED - not confirmed. Proven on one stream only: interest.** These are the standards of
> `interest-conversion-standards`, split in step 5c into the part that names no stream (this file) and the part that
> is interest's (`.agents/skills/carddemo-stream-interest/SKILL.md`). No standard was added, dropped or renumbered;
> every one was PROPOSED and stays PROPOSED. The posting stream has a draft annex that is not proven.
> The corrections still proposed are in section 4 of `modernization/design/skills.md` and section 6 of
> `modernization/design/build-run.md`.
> **Owner who must confirm: the target architect.**

This file and the annex of the stream are together the only place the standards are written. Ticket sessions follow
them and **do not edit them**: a standard that is wrong or missing is reported in the pull request as a proposed
change, for the target architect.

## How to read a standard

1. Read the shared text here, by id (S1.1 ...).
2. Read the row with the same id in section 2 of the stream's annex,
   `.agents/skills/carddemo-stream-<stream>/SKILL.md`: the standard as it reads for that stream, what it cites, and a
   do / do-not example.
3. If the two differ for a stream, the annex is right for that stream: it is the text that was applied to its code.

Stream sources, all read-only and named in the annex: the rules (`modernization/rules/<stream>/rules.yaml`, with the
stream's rule-id prefix) and `decisions.md`; the architecture; the data map (rows cited as `copybook/FIELD`,
encoding rules as E1-E6, gaps as G-numbers); the programs under `app/cbl`.

## 1. What to convert from, in this order

1. The **ticket** and the **rules** it names: what to build and why.
2. **The architecture and the data map**: the shape of the target (steps, ports, tables, types, switches).
3. The **COBOL lines the rule cites**: the exact behaviour.
4. The **recorded legacy output** (`fixtures/<stream>/<case>/expected`): what is correct.

| Id | Standard | The annex of the stream supplies |
|---|---|---|
| S1.1 | Work down the four sources in that order; a later source settles what an earlier one leaves open, and the recorded output overrides all of them. | citation, example |
| S1.2 | Where rule text and code disagree, reproduce the code and report the disagreement in the pull request under "For the decision register" (rule id, lines, what differs). | citation, example |

## 2. Idiomatic Java, not translated COBOL

| Id | Standard | The annex of the stream supplies |
|---|---|---|
| S2.1 | Design from the rules. One unit of code per rule or decision, named for what it decides. No method per paragraph, no emulated `GO TO` or `PERFORM THRU`. | whether the program has GO TO or PERFORM THRU; citation, example |
| S2.2 | Each copybook layout becomes one immutable `record` named for its business meaning; a copybook field name appears once, in the codec/row mapping that cites the data-map row. No class mirrors WORKING-STORAGE and no byte overlay stands in for a REDEFINES. | citation, example |
| S2.3 | Reading, processing and writing follow the stream's architecture (annex: the document, its sections, the step, the item, the chunk and the restart state saved with the commit). The calculation uses only the ports of that architecture. **With file adapters (no table):** the same reader, processor and writer shape and the same ports; there is no restart state to save. | the architecture sections, the step, the item, the restart state; citation, example |
| S2.4 | A type or a return value replaces every flag, switch and result code. | the flags and result codes of the program; citation, example |
| S2.5 | Constructor injection only; no mutable static state. The clock and the policy are injected or held in step scope; so is any counter the stream carries from item to item (annex). | the counter; citation, example |

## 3. Numbers and data

| Id | Standard | The annex of the stream supplies |
|---|---|---|
| S3.1 | Money and rates are `BigDecimal` with the precision and scale of their data-map row. No `double` or `float` anywhere, including tests. | the precision and scale of each amount; citation, example |
| S3.2 | *Stream standard.* The id is kept for the formula of the stream's calculation; the annex holds the standard. There is no shared text. | the whole standard |
| S3.3 | Every arithmetic step outside the stream's formula (S3.2) also names its mode. Where the operands are sums of scale-2 values, the mode is `RoundingMode.UNNECESSARY` and the result is asserted to be scale 2. | which sums; citation, example |
| S3.4 | Before a value is stored, check it against the PIC of its row. A value that does not fit goes to the stream's overflow switch (annex: its name, its default and the rule id it names). Never cut high-order digits to imitate COBOL. | the switch, its default, the rule id; citation, example |
| S3.5 | EBCDIC, zoned overpunch, packed and binary are handled only in `record-codec`, by rules E1-E5. Business code sees `BigDecimal`, `long`, `int` and trimmed `String`. A byte that breaks E1/E2 rejects the record at the boundary; no zero is substituted. | citation, example; the encoding rules E1-E6 are in the data map of the stream |
| S3.6 | Width, padding and FILLER belong to the codec and the schema. FILLER is raw bytes (E4) written back unchanged. The digits of an id inside a text are a domain matter; the width of the field that holds the text stays in the codec. | what the job leaves as spaces, which columns it updates, which ids it formats; citation, example |
| S3.7 | Code page and key order are fixed in one place each: the codec option (`fixture` ISO-8859-1 or `host`) and the `ORDER BY` of the reader of the driving file, with `COLLATE "C"` on its text keys. **With file adapters:** there is no query, so the key order is fixed once in the adapter that reads the driving file, by ordering the records on the bytes of the key, and nowhere else. | the `ORDER BY`, the adapter, the gap id of the code page; citation, example |
| S3.8 | Time and the run's date parameters are injected. Timestamps come from an injected `java.time.Clock`. Any business date the run is given is a job parameter. | the timestamp format, the PARM and its checks; citation, example |

## 4. COBOL behaviours with no Java equivalent

| Id | Standard | The annex of the stream supplies |
|---|---|---|
| S4.1 | Every behaviour marked PRESENT in the behaviours table of the stream's annex is reproduced as its "Java standard" column says and proven by the test named in its row. A behaviour found later that is not in the table is reported in the pull request, not handled silently. | the behaviours table |

The table below is the list of questions to ask of every program of a stream. The answers - PRESENT or ABSENT, the
lines that show it, what the legacy does, the Java standard as worded for the stream, the test and what the off-host
run cannot settle - are a table in the stream's annex, one row per letter, none left out. A stream that finds a
behaviour with no letter adds a row to its annex and reports it for this list.

| | Behaviour | Standards that answer it when it is PRESENT |
|---|---|---|
| a | Decimal truncation, no ROUNDED | S3.2 |
| b | High-order digits lost, no ON SIZE ERROR | S3.4 |
| c | Intermediate precision inside COMPUTE | S3.2 |
| d | Sign handling: unsigned fields that drop a sign; zoned overpunch | S3.5 |
| e | Packed and binary fields; binary truncation by compile option | S3.5 |
| f | Non-numeric data in a numeric field | S3.5 |
| g | Fixed width: padding, truncation on MOVE, comparison with padding, STRING, reference modification | S3.6 |
| h | EBCDIC against ASCII: code page and collating order | S3.7 |
| i | REDEFINES | S2.2 |
| j | FILLER and fixed record length across a round trip | S3.6 |
| k | Tables: OCCURS, subscripts from 1, OCCURS DEPENDING ON | - |
| l | WORKING-STORAGE kept from record to record; fields used before set | S2.3 |
| m | Control break and end of file | S2.3, S5.1 |
| n | GO TO, PERFORM THRU, fall-through | S2.1 |
| o | Level-88 condition names | S2.4 |
| p | Indexed files: sequential and random access, alternate index, START, duplicate keys, REWRITE after READ | S2.3 |
| q | File status codes and abends | S6.1, S6.2, S6.3 |
| r | JCL: PARM, GDG generation, DISP, step return code | S6.4 |
| s | DISPLAY output compared by parity | S6.5 |
| t | Business date from PARM against the system clock | S3.8 |
| u | CURRENT-DATE, hundredths, timestamp format | S3.8 |
| v | Two-digit years and Julian dates | - |
| w | PARM length is never checked *(added from the first stream)* | S3.8 |
| x | a failed keyed READ leaves the record area as it was *(added from the first stream)* | S2.4 |
| y | alphanumeric and numeric items mixed *(added from the first stream)* | S2.2 |
| z | abend by `CALL 'CEE3ABD'` with no STOP RUN after it *(added from the first stream)* | S6.2 |
| aa | dead and empty code *(added from the first stream)* | S5.2 |

**What the off-host run cannot settle.** The golden files come from GnuCOBOL 3.1.2 with `-std=ibm -fsign=EBCDIC`,
not from Enterprise COBOL on z/OS. `tools/build.sh` sets no equivalent of the host options ARITH, TRUNC, NUMPROC or
ZONEDATA, and the estate holds no host compile listing, so the options the production load module was built with are
unknown: **confirm on the mainframe - mainframe application owner.** Until then the rows a stream's annex marks carry
the open point named in their last column.

## 5. Quirks and open decisions

Rules of type QUIRK and HOLE are OPEN in the stream's `decisions.md`. The legacy behaviour is what is built.

| Id | Standard | The annex of the stream supplies |
|---|---|---|
| S5.1 | Each QUIRK or OPEN behaviour is reproduced in exactly one named place, behind its switch of the stream's architecture, read only through the stream's policy module, with the legacy value as default. The rule id is in the name of that code and of its tests. | the switches and the policy module; citation, example |
| S5.2 | Nothing that changes output is fixed or tidied, and no behaviour is added without a rule id that traces to it. Dead and empty legacy code is not ported; an improvement is proposed in the pull request for the register. | citation, example |

## 6. Errors and operations

The legacy failure path of the stream's programs, and the table of legacy events (event, lines, legacy outcome, Java,
job and exit, run log), are in the stream's annex.

| Id | Standard | The annex of the stream supplies |
|---|---|---|
| S6.1 | Every row of the legacy-event table of the stream's annex is implemented as written; a file status is never a field passed around. The status line keeps the legacy format and is written only where the condition determines the legacy status. A database or file-system failure writes the legacy message of the operation it replaces to the run log and its own cause to the application log. | the legacy-event table, the status-line format; citation, example |
| S6.2 | One exception type stops the run; it carries the reason and the rule id. It is never caught to continue: no skip, no retry (the stream's architecture). The exit code is one constant. | the exception and its reasons; citation, example |
| S6.3 | Restart follows the stream's architecture (annex: the section, which steps repeat, which continue, and the decision ids). **With file adapters:** there is no restart state and no restart; a stopped run is launched again from the start with a new `runId`. | the restart of each step; citation, example |
| S6.4 | A normal run writes the outputs in the recorded format (`carddemo-parity`, workflow step 3: `datasets/<DSN>.txt`, `sysout/<JOB>.<STEP>.txt`, `rc.json`). The annex names the files and says what a failed run leaves. | the files, and what a failed run leaves; citation, example |
| S6.5 | Two logs, kept apart. The **run log** (`RunLog` port) is the legacy SYSOUT, byte for byte, and is a parity output; it contains what the legacy printed. The **application log** (SLF4J) carries run id, step, rule id, counts, sums, exit status and stop reason - never an account id, card number, balance, amount or record image. | what the legacy printed; citation, example |

## 7. Tests

| Id | Standard | The annex of the stream supplies |
|---|---|---|
| S7.1 | At least one test per rule id a ticket implements, named `<ruleId>_<whatItProves>` in camelCase. A rule that is outside the job has no test; say so in the checklist. A switch has a test for the default and for each other value. | the rule ids, and the rule with no test; citation, example |
| S7.2 | Each rule's tests cover the fixture cases and edge cases listed for that rule in `rules.yaml` and in the ticket's Verification block; behaviours with no fixture (annex: which) get synthetic data under `java/<stream>/src/test`. | the behaviours with no fixture; citation, example |
| S7.3 | Integration tests (steps, JDBC adapters, restart) run against PostgreSQL 15 in a container - the `db` service of `docker-compose.yml` (`postgres:15`) or the same image - with the stream's schema (annex). | the schema; citation, example |
| S7.4 | Expected values come from the recorded legacy output (`fixtures/<stream>/<case>/expected`, compared with `tools/parity/compare.py`) or are written by hand in the test with the arithmetic shown. Never from the code under test. | citation, example |

## 8. Guardrails

| Id | Standard | The annex of the stream supplies |
|---|---|---|
| S8.1 | No edit under `app/`, `fixtures/` (inputs and `expected`) or `tools/parity`. | where a difference is fixed; citation, example |
| S8.2 | Golden files are not re-recorded (`make record`) by a ticket session. | citation, example |
| S8.3 | No new dependency without one sentence in the pull request saying why and what was considered instead. | nothing |
| S8.4 | Frozen shared interfaces of the stream's architecture (annex: their ids) are not changed by a ticket other than the one that owns them; a needed change is reported. | the interface ids; citation, example |
| S8.5 | Synthetic data only, in tests, examples, logs and pull requests. | citation, example |

## 9. Review checklist

The ticket session copies this list into its pull request and marks each line `PASS`, `FAIL` (with the reason) or
`N/A` (with the reason: outside this ticket's scope). The reviewer checks it against the diff.

Where the stream's annex has its own checklist (interest has), copy that one: it has these lines in the stream's
wording and one line per behaviour PRESENT in the stream.

Standards:

- [ ] S1.1 Sources used in order; recorded output decides - PASS / FAIL / N/A
- [ ] S1.2 Rule/code disagreements reproduced as coded and reported - PASS / FAIL / N/A
- [ ] S2.1 Designed from rules; no method per paragraph - PASS / FAIL / N/A
- [ ] S2.2 Immutable business records; copybook names only at the mapping - PASS / FAIL / N/A
- [ ] S2.3 Spring Batch shape, transaction and restart boundaries of the architecture - PASS / FAIL / N/A
- [ ] S2.4 Types and return values, no flags or result codes - PASS / FAIL / N/A
- [ ] S2.5 Constructor injection; no mutable static state - PASS / FAIL / N/A
- [ ] S3.1 `BigDecimal` with data-map precision and scale; no `double`/`float` - PASS / FAIL / N/A
- [ ] S3.2 The stream's formula as its annex states it - PASS / FAIL / N/A
- [ ] S3.3 Rounding mode named on every other arithmetic step - PASS / FAIL / N/A
- [ ] S3.4 Range check before store; overflow goes to the policy - PASS / FAIL / N/A
- [ ] S3.5 Encodings only in `record-codec` - PASS / FAIL / N/A
- [ ] S3.6 Width, padding and FILLER only in codec and schema - PASS / FAIL / N/A
- [ ] S3.7 Code page and key order each in one place - PASS / FAIL / N/A
- [ ] S3.8 Clock and run parameters injected - PASS / FAIL / N/A
- [ ] S4.1 Every PRESENT behaviour touched has its test; new ones reported - PASS / FAIL / N/A
- [ ] S5.1 Each quirk/open decision in one named place behind its switch, rule id in names - PASS / FAIL / N/A
- [ ] S5.2 Nothing fixed, tidied or added without a rule id - PASS / FAIL / N/A
- [ ] S6.1 Status and message mapping as in the legacy-event table of the annex - PASS / FAIL / N/A
- [ ] S6.2 One stop exception, one exit-code constant, no skip or retry - PASS / FAIL / N/A
- [ ] S6.3 Restart as in the architecture - PASS / FAIL / N/A
- [ ] S6.4 Outputs in the recorded format; a failed run leaves what the annex says - PASS / FAIL / N/A
- [ ] S6.5 Run log byte for byte; no account data in the application log - PASS / FAIL / N/A
- [ ] S7.1 A test per rule id, named for it - PASS / FAIL / N/A
- [ ] S7.2 Fixture and edge cases of each rule covered - PASS / FAIL / N/A
- [ ] S7.3 Integration tests on PostgreSQL 15 in a container - PASS / FAIL / N/A
- [ ] S7.4 No assertion against the code's own output - PASS / FAIL / N/A
- [ ] S8.1 No edit under `app/`, `fixtures/`, `tools/parity` - PASS / FAIL / N/A
- [ ] S8.2 No golden file re-recorded - PASS / FAIL / N/A
- [ ] S8.3 Every new dependency explained in the pull request - PASS / FAIL / N/A
- [ ] S8.4 Frozen shared interfaces unchanged - PASS / FAIL / N/A
- [ ] S8.5 Synthetic data only - PASS / FAIL / N/A

Behaviours: one line per row marked PRESENT in the behaviours table of the stream's annex, in the form
`- [ ] <letter> <behaviour> - PASS / FAIL / N/A`.
