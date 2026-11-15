---
name: interest-conversion-standards
description: Conversion standards for the interest stream (INTCALC -> CBACT04C) moving to Java 21 / Spring Batch 5 / PostgreSQL 15. Every ticket session under modernization/design/tickets must follow them and fill in the review checklist at the end. Use when implementing or reviewing any interest ticket, or when a question comes up about how a CBACT04C behaviour is reproduced in Java.
---

# Interest stream conversion standards

> **PROPOSED - not confirmed. Nothing here has been run.** These standards were written by an agent from the
> rulebook, the design, the data map, `CBACT04C.cbl`, its copybooks, `INTCALC.jcl` and `tools/build.sh`. No Java
> exists yet and no standard has been applied to code. **The pilot ticket
> (`modernization/design/tickets/01-pilot-thin-slice.md`) is the first test.**
> **Owner who must confirm: the target architect.**

This file is the only place the standards are written. Ticket sessions follow it and **do not edit it**: a standard
that is wrong or missing is reported in the pull request as a proposed change, for the target architect.
The human-readable summary is `modernization/design/standards.md`; if the two differ, this file is right.

Sources, all read-only: rules `modernization/rules/interest/rules.yaml` (INT-R, INT-Q, INT-H, INT-T ids) and
`decisions.md`; `modernization/design/architecture.md` (version 0.1.0); `modernization/design/data-map-interest.md`
(rows are cited as `copybook/FIELD`, encoding rules as E1-E6, gaps as G01-G12); `app/cbl/CBACT04C.cbl` (cited as
`CBACT04C:lines`). Each standard has an id (S1.1 ...) used in the checklist.

## 1. What to convert from, in this order

1. The **ticket** and the **rules** it names: what to build and why.
2. **`architecture.md` and the data map**: the shape of the target (steps, ports, tables, types, switches).
3. The **COBOL lines the rule cites**: the exact behaviour.
4. The **recorded legacy output** (`fixtures/interest/<case>/expected`): what is correct.

| Id | Standard | Cites | Do | Do not |
|---|---|---|---|---|
| S1.1 | Work down the four sources in that order; a later source settles what an earlier one leaves open, and the recorded output overrides all of them. | INT-R05, INT-Q01; `CBACT04C:464-465` | Rule INT-R05 gives the formula, lines 464-465 give the truncation, i02-tie-half-cent proves 12.35. | Start from the COBOL listing and port it top to bottom; or decide 12.36 is "right" because the rule text says "divided by 1200". |
| S1.2 | Where rule text and code disagree, reproduce the code and report the disagreement in the pull request under "For the decision register" (rule id, lines, what differs). | INT-Q04; `CBACT04C:188-222` | The comment says the last account is updated at end of file; the code never does it - reproduce the code, report it. | Follow the comment or the rule text and leave the difference unmentioned. |

## 2. Idiomatic Java, not translated COBOL

| Id | Standard | Cites | Do | Do not |
|---|---|---|---|---|
| S2.1 | Design from the rules. One unit of code per rule or decision, named for what it decides. No method per paragraph, no emulated `GO TO` or `PERFORM THRU` (the program has none: behaviour n). | INT-R02, INT-R03; `CBACT04C:415-460` | `RateLookup.findRate(group, type, category)` returning `Optional<BigDecimal>`, and one `rateFor(...)` that applies the DEFAULT fallback. | `p1200GetInterestRate()` and `p1200AGetDefaultIntRate()`. |
| S2.2 | Each copybook layout becomes one immutable `record` named for its business meaning; a copybook field name appears once, in the codec/row mapping that cites the data-map row. No class mirrors WORKING-STORAGE and no byte overlay stands in for a REDEFINES. | `CVTRA01Y/TRAN-CAT-BAL`, `CVACT01Y/ACCT-CURR-BAL`; `CBACT04C:166-173` | `record CategoryBalance(long accountId, String typeCode, int categoryCode, BigDecimal balance)`; `"TRAN-CAT-BAL"` appears only in the mapping. | `class WsMiscVars { String wsLastAcctNum; BigDecimal wsMonthlyInt; char wsFirstTime; }` or `getTranCatBal()` in the domain. |
| S2.3 | Reading, processing and writing follow `architecture.md` sections 2, 5 and 6: `calcInterestStep` reads one item = one account with all its balances in key order, the processor returns the result, the writer inserts the transactions and updates three account columns, a chunk is whole accounts, and the restart state (last account, transaction counter, `seq_no`, run-log length) is saved with the commit. The calculation uses only the ports of FI-3. | INT-R01, INT-R07; `CBACT04C:188-222`; `CVTRA01Y/TRAN-CAT-KEY` | `ItemReader<AccountBalances>` -> `ItemProcessor<AccountBalances, AccountInterest>` -> `ItemWriter<AccountInterest>`; total interest is a local sum. | A tasklet with `while (!endOfFile)` that keeps `lastAccountId` and `totalInterest` in fields and writes as it goes. |
| S2.4 | A type or a return value replaces every flag, switch and result code (`END-OF-FILE`, `WS-FIRST-TIME`, `APPL-RESULT` and its level-88 names). | `CBACT04C:133-135, 170, 188-198, 328-347` | End of input is the reader returning `null`; a missing rate is `Optional.empty()`; a failure is an exception (S6.2). | `int applResult = 16; if (applResult == APPL_EOF) endOfFile = 'Y';` |
| S2.5 | Constructor injection only; no mutable static state. The transaction counter, the clock and the policy are injected or held in step scope. | INT-Q02; `CBACT04C:173, 474` | `InterestCalculator(RateLookup rates, InterestPolicy policy, Clock clock)`; the counter comes from the step's restart state. | `private static int tranIdSuffix;` or `@Autowired` on a field. |

## 3. Numbers and data

| Id | Standard | Cites | Do | Do not |
|---|---|---|---|---|
| S3.1 | Money and rates are `BigDecimal` with the precision and scale of their data-map row: balance and interest (11,2), rate (6,2), account amounts (12,2). No `double` or `float` anywhere, including tests. | `CVTRA01Y/TRAN-CAT-BAL`, `CVTRA02Y/DIS-INT-RATE`, `CVTRA05Y/TRAN-AMT`, `CVACT01Y/ACCT-CURR-BAL`; INT-T06 | `new BigDecimal("1235.50")` | `double interest = balance * rate / 1200;` or `BigDecimal.valueOf(12.355)`. |
| S3.2 | The formula is one exact multiplication and one division that goes straight to scale 2 with an explicit mode. The mode comes from `interest.policy.rounding`; the default TRUNCATE is `RoundingMode.DOWN` (towards zero, also for negative balances). | INT-R05, INT-Q01, INT-Q05; `CBACT04C:464-465` | `balance.multiply(rate).divide(TWELVE_HUNDRED, 2, policy.roundingMode())` | `divide(TWELVE_HUNDRED)` with no mode; `MathContext.DECIMAL64`; `RoundingMode.FLOOR` (wrong for -3.005); a literal `HALF_EVEN`. |
| S3.3 | Every other arithmetic step also names its mode. The account total and the new balance are sums of scale-2 values, so the mode is `RoundingMode.UNNECESSARY` and the result is asserted to be scale 2. | INT-R07; `CBACT04C:352, 467` | `total.add(interest).setScale(2, RoundingMode.UNNECESSARY)` | `setScale(2)` or a silent `HALF_UP` "to be safe". |
| S3.4 | Before a value is stored, check it against the PIC of its row. A value that does not fit goes to `interest.policy.overflow` (default STOP, naming INT-H05). Never cut high-order digits to imitate COBOL: the source does not determine that behaviour. | INT-H05; `CVTRA05Y/TRAN-AMT`, `CVTRA05Y/TRAN-ID`; `CBACT04C:464-467, 474` | `policy.checkFits(interest, 9, 2)` and the same for the counter above 999999. | `interest.remainder(new BigDecimal("1000000000"))`, or letting PostgreSQL raise "numeric field overflow". |
| S3.5 | EBCDIC, zoned overpunch, packed and binary are handled only in `record-codec`, by rules E1-E5. Business code sees `BigDecimal`, `long`, `int` and trimmed `String`. A byte that breaks E1/E2 rejects the record at the boundary; no zero is substituted. | E1, E2, E5; `CVTRA01Y/TRAN-CAT-BAL`; `CBACT04C:326, 373` | `codec.decode(layout, bytes)` in `load`; `codec.encode(...)` in `export`. | `if (last == '}' ...)` or `new String(bytes, "Cp037")` in `domain`, `policy` or `port`. |
| S3.6 | Width, padding and FILLER belong to the codec and the schema. FILLER is raw bytes (E4) written back unchanged; a new transaction's FILLER and the `TRAN-DESC` tail are spaces (G05). The calculation updates only the three account columns. | E3, E4; `CVACT01Y/FILLER`, `CVTRA05Y/TRAN-DESC`; INT-T04; `CBACT04C:356, 485-489` | `"Int. for a/c " + elevenDigits(accountId)` in the domain; padding to 100 in the codec. | `String.format("%-100s", desc)` in the domain; rewriting the whole account row from a decoded object. |
| S3.7 | Code page and key order are fixed in one place each: the codec option (`fixture` ISO-8859-1 or `host`, G01) and the reader's `ORDER BY trancat_acct_id, trancat_type_cd COLLATE "C", trancat_cd`. | E3, E6; `CVTRA01Y/TRAN-CAT-KEY`; INT-R01; `CBACT04C:28-32` | One ordered query in the `BalanceSource` adapter. | `balances.sort(...)` in Java, or an `ORDER BY` without the collation. |
| S3.8 | Time and the run's date parameters are injected. Timestamps come from an injected `java.time.Clock`, read once per written transaction, hundredths cut (not rounded) and followed by `0000`. `parmDate` is a job parameter of exactly 10 characters, used as text for the id prefix and never parsed as a date; another length is refused. Any frozen business date (switch `timestampSource=CYCLE_DATE`) is a job parameter too. | INT-Q02, INT-Q03, INT-T05; `CBACT04C:176-178, 476-480, 613-624`; `CVTRA05Y/TRAN-ID` | `clock.instant()` in one `TimestampFormatter`; tests use `Clock.fixed(...)` at the frozen fixture time 2022-07-05 10:00:00.00. | `LocalDateTime.now()`, `System.currentTimeMillis()`, `LocalDate.parse(parmDate)`. |

## 4. COBOL behaviours with no Java equivalent

Verified against `app/cbl/CBACT04C.cbl` (652 lines), the five copybooks, `app/jcl/INTCALC.jcl` and
`tools/build.sh:16` (`cobc -m -std=ibm -fsign=EBCDIC`, GnuCOBOL 3.1.2 in the estate container). Nothing was executed
for this table; the fixture cases named are recorded legacy runs.

| Id | Standard | Cites | Do | Do not |
|---|---|---|---|---|
| S4.1 | Every behaviour marked PRESENT below is reproduced as its "Java standard" column says and proven by the test named in its row. A behaviour found later that is not in the table is reported in the pull request, not handled silently. | rows a-aa | Add `intQ01_truncatesHalfCent` with the ticket that implements the formula. | Reproduce a behaviour "from experience" without a row. |

Line numbers are `app/cbl/CBACT04C.cbl` unless a file is named. "Search" means
`grep -nE` over CBACT04C.cbl and the five copybooks returned nothing. Test names are the names ticket
sessions must use; none exists yet.

| | Behaviour | In this stream | Evidence | What the legacy does | Java standard | Test (fixture case) | Off-host run cannot settle |
|---|---|---|---|---|---|---|---|
| a | Decimal truncation, no ROUNDED | PRESENT | 464-465; search `ROUNDED`: nothing | 12.355 is stored in `WS-MONTHLY-INT` S9(09)V99 as 12.35 | S3.2: one `divide(1200, 2, mode)` with the mode from `interest.policy.rounding`, default TRUNCATE = `RoundingMode.DOWN` | `intQ01_truncatesHalfCent` (i02-tie-half-cent), `intQ01_nonTieUnchanged` (i07-nontie-rounding) | - |
| b | High-order digits lost, no ON SIZE ERROR | PRESENT (not reachable by any fixture) | 352, 464-467, 474; search `SIZE ERROR`: nothing | Interest above 999,999,999.99, an account total or balance beyond its PIC, or transaction 1,000,000 would lose leading digits silently | S3.4: range check against the PIC of the data-map row before every store; `interest.policy.overflow=STOP` fails the run naming INT-H05. Never `setScale`/`remainder` to imitate the loss | `intH05_interestAboveFieldStops`, `intH05_millionthTransactionStops` (no fixture) | The legacy result of an overflow: **confirm on the mainframe** - mainframe application owner |
| c | Intermediate precision inside COMPUTE | PRESENT | 464-465; operands CVTRA01Y:9, CVTRA02Y:9 | `(S9(09)V99 * S9(04)V99) / 1200`: product has 4 decimals and at most 17 digits; the quotient is cut to 2 decimals | S3.2: multiply exactly, divide once, straight to scale 2. No intermediate `setScale`, no `MathContext` | `intR05_formulaExactProductThenDivide` (i01-baseline, i07-nontie-rounding) | Truncating an exact quotient gives the same cents at any intermediate precision of 2 or more decimals, so ARITH should not matter - reasoning, not a host run: **confirm on the mainframe** - mainframe application owner |
| d | Sign handling: unsigned fields that drop a sign; zoned overpunch | Overpunch PRESENT; sign-dropping move ABSENT | CVTRA01Y:9, CVTRA02Y:9, CVACT01Y:7-9 and 13-14, CVTRA05Y:10; `tools/build.sh:16` (`-fsign=EBCDIC`). Every MOVE/ADD into an unsigned field (474, 483, 491, 621) has an unsigned source | Last byte of each S9 field carries digit and sign (`{A-I` plus, `}J-R` minus); results are written C/D signed | S3.5: data-map rule E2 in `record-codec` only; domain sees a signed `BigDecimal` | `e2_overpunchRoundTrip`, `intQ05_negativeInterestIsDSigned` (i06-negative-balance) | F-signed positives and minus zero in host data (G02, G03; NUMPROC): **confirm on the mainframe** - mainframe data owner |
| e | Packed and binary fields; binary truncation by compile option | Packed ABSENT; binary PRESENT in storage only, on no dataset | Search `COMP-3|PACKED`: nothing. Binary: 125, 133, 138-139, 177; data-map rule E5 | Binary items hold 0, 8, 12, 16, 999 and one status byte; `PARM-LENGTH` is never read | S3.5: the codec refuses a field whose USAGE is not DISPLAY. No binary type in the domain; the result codes become return values (o) | `e5_codecRefusesNonDisplayUsage` (no fixture) | TRUNC option and byte order affect only the `9x` branch of the status line (636-642): **confirm on the mainframe** - mainframe application owner |
| f | Non-numeric data in a numeric field | PRESENT | 636 (`IO-STATUS NOT NUMERIC`); 373/394/416 `READ INTO` move bytes unchecked into 9 and S9 fields; 483 | A bad digit in `TRAN-CAT-BAL` or `DIS-INT-RATE` reaches the COMPUTE unchecked | S3.5: rules E1/E2 reject the record at the boundary; the run stops before any calculation. No substitution of zero | `e1_nonDigitRejectsRecord` (no fixture) | S0C7 or a computed value on the host (NUMPROC, ZONEDATA): **confirm on the mainframe** - mainframe application owner |
| g | Fixed width: padding, truncation on MOVE, comparison with padding, STRING, reference modification | PRESENT, except right truncation on MOVE: ABSENT (every sending item fits its receiver) | 437, 484 (literal padded); 476-480, 485-489 (STRING); 638, 645 (reference modification); 194, 422 (comparisons) | `TRAN-ID` is filled exactly (10 + 6). `TRAN-DESC` gets 24 of 100 bytes; STRING does not pad (INT-T04, G05). `'DEFAULT'` is padded to 10 for the key | S3.6: the domain holds trimmed values; width and padding come from the data-map row in the codec. The fallback group is the constant `DEFAULT` compared as CHAR(10). `TRAN-DESC` tail and FILLER are written as spaces | `intR06_transactionFieldsFixedWidth` (i01-baseline), `intR03_defaultGroupKeyPadded` (i04-default-group-fallback) | Bytes of `TRAN-DESC` 25-100 and the FILLER on the host (G05): **confirm on the mainframe** - mainframe application owner |
| h | EBCDIC against ASCII: code page and collating order | PRESENT | Data-map rules E3, E6; gaps G01, G06; `tools/parity/record.py:51` (fixtures are ISO-8859-1); key 28-32 | TCATBALF is read in EBCDIC key order; `TRANCAT-TYPE-CD` is X(02), so letters sort before digits on the host | S3.7: code page only in the codec (`format=fixture` or `host`). Read `ORDER BY trancat_acct_id, trancat_type_cd COLLATE "C", trancat_cd` and nowhere else | `intR01_readsInKeyOrder` (i05-multi-category) | Host CCSID (G01) and the order of mixed letter/digit type codes (G06); the fixtures hold digits only: **confirm on the mainframe** - mainframe data owner |
| i | REDEFINES | PRESENT in the program, ABSENT in the copybooks | 126, 151; search `REDEFINES` in the five copybooks: nothing | A 2-byte binary is read as two characters to print a status; the 26-byte timestamp is filled through a redefinition | S2.2: no byte overlay. The timestamp is one formatter (u), the status line one function (q) | `intT05_timestampFormat`, `intT08_statusLineFormat` | - |
| j | FILLER and fixed record length across a round trip | PRESENT | CVTRA01Y:10, CVACT03Y:8, CVTRA02Y:10, CVACT01Y:17, CVTRA05Y:18; 356, 373; `app/jcl/INTCALC.jcl:39` (LRECL=350) | The 178-byte account FILLER is read and rewritten untouched; the transaction FILLER is never set | S3.6: rule E4 - FILLER is `BYTEA`, written back byte for byte; the calculation updates three columns only; every output record has its full length | `e4_accountFillerRoundTrip` (i01-baseline: ACCTDATA compared field by field) | See g (G05) |
| k | Tables: OCCURS, subscripts from 1, OCCURS DEPENDING ON | ABSENT | Search `OCCURS` in CBACT04C.cbl and the five copybooks: nothing | - | - | - | - |
| l | WORKING-STORAGE kept from record to record; fields used before set | PRESENT | 167-173 (`WS-LAST-ACCT-NUM`, `WS-TOTAL-INT`, `WS-FIRST-TIME`, `WS-TRANID-SUFFIX`); 352 uses the `ACCOUNT-RECORD` of the previous account; `TRAN-RECORD` FILLER and `TRAN-DESC` tail are never set | The account total, the account and card read at the break, and the counter survive between balances | S2.3: the item is one account with all its balances, so nothing is carried between items except the transaction counter, which lives in the step's restart state | `intR07_totalIsPerAccount` (i05-multi-category), `intQ02_counterRunsAcrossAccounts` (i01-baseline) | Unset bytes on the host: see g (G05) |
| m | Control break and end of file | PRESENT | 188-222, 339-340 | Account change triggers the update of the previous account; end of file ends the loop before the last update (INT-Q04). An empty file writes nothing (INT-T09) | S2.3, S5.1: the reader groups by account; the processor returns the result; "is this the account that ends the input" is decided once and passed to `interest.policy.updateLastAccount` | `intR07_updatesAccountAtBreak` (i01, i02, i07), `intQ04_lastAccountNotUpdated` (all seven), `intT09_emptyInputWritesNothing` (no fixture) | - |
| n | GO TO, PERFORM THRU, fall-through | ABSENT | Search `GO TO|THRU|THROUGH`: nothing. Every paragraph is PERFORMed singly; 232 `GOBACK` ends the mainline before the first paragraph | - | S2.1 still applies: no method per paragraph | - | - |
| o | Level-88 condition names | PRESENT | 134-135 (`APPL-AOK`, `APPL-EOF`) on `APPL-RESULT` | Every I/O sets 0, 8, 12 or 16 and tests the 88 | S2.4: end of input is the reader returning no item; a missing rate is an empty `Optional`; a failure is an exception. No result-code field | Covered by `intR03_*` and `intR08_*` tests | - |
| p | Indexed files: sequential and random access, alternate index, START, duplicate keys, REWRITE after READ | PRESENT, except START: ABSENT (search `START `: only the DISPLAY at 181) | 28-51 (SELECTs); 394-395 (read by alternate key); 356, 373 (REWRITE after random READ); `app/jcl/XREFFILE.jcl:72-92` (NONUNIQUEKEY) | TCATBALF sequential by key; ACCTDATA and DISCGRP random by primary key; CARDXREF random by the non-unique account key; the REWRITE at the break writes the previous account | S2.3: the ports of FI-3 - `BalanceSource` (ordered), `AccountStore`, `RateLookup`, `CardLookup` (by `card_xref_acct_ix`). Two cards for one account go to `interest.policy.multiCard`, default STOP (INT-H03) | `intR02_rateByGroupTypeCategory` (i01), `intR06_cardNumberOfAccount` (i01), `intH03_twoCardsStops` (no fixture) | Status 02, 00 or an open error for an account with several cards (INT-H03, G08, G12): **confirm on the mainframe** - mainframe application owner |
| q | File status codes and abends | PRESENT | 234-323, 327-347, 357-369, 378-390, 400-412, 422-439, 446-459, 501-514, 522-611, 628-648 | Status 00 continues; 10 on TCATBALF ends the input; 23 on DISCGRP falls back to DEFAULT; anything else prints a message and the status line and abends with user code 999 | S6.1-S6.3: the table in section 6 | `intR08_*` one per stop condition (no fixture has an abend) | U0999 on the host against RC 12 from `tools/stubs/CEE3ABD.cbl`: **confirm on the mainframe** - mainframe operations owner (REL-10) |
| r | JCL: PARM, GDG generation, DISP, step return code | PRESENT | `app/jcl/INTCALC.jcl:22` (PARM), `:37-41` (`DISP=(NEW,CATLG,DELETE)`, `SYSTRAN(+1)`, LRECL 350), `:27-36` (DISP=SHR) | PARM text becomes the first 10 bytes of `TRAN-ID`; a normal end catalogs the new generation and returns 0; an abend deletes it but keeps the accounts already rewritten | S6.4: `parmDate` is a job parameter; the export writes `SYSTRAN.G0001V00` and `rc.json` in the fixture format; a failed run leaves no export | `intQ02_idIsParmPlusCounter` (i01-baseline), `rcJson_matchesRecorded` (all seven) | `tools/runjcl/runjcl.py:598-599` reads only the first DISP subparameter, so the abnormal-end disposition is not modelled; generation numbering restarts per case: **confirm on the mainframe** - mainframe operations owner |
| s | DISPLAY output compared by parity | PRESENT | 181, 193, 230, 418-419; `tools/parity/compare.py:93-95` | Start line, each balance record as read (50 bytes, overpunch and trailing spaces included), the two fallback lines, end line | S6.5: `RunLog` writes these lines byte for byte to `sysout/INTCALC.STEP15.txt`; the record echo is the codec's image of the record, not a formatted amount | `sysout_matchesRecorded` (all seven; i04 for the fallback lines) | Where DISPLAY goes on the host (stream ask A10): **confirm on the mainframe** - mainframe operations owner |
| t | Business date from PARM against the system clock | PRESENT | 176-178, 476-480 (PARM); 496-498, 614 (clock) | The PARM text is used only in `TRAN-ID`; both timestamps come from the clock. No date arithmetic | S3.8: `parmDate` is a 10-character string, never parsed as a date; time comes from the injected `Clock` | `intQ02_idIsParmPlusCounter`, `intQ03_timestampsFromRunClock` (i01-baseline: PARM 2022071800, clock 2022-07-05) | - |
| u | CURRENT-DATE, hundredths, timestamp format | PRESENT | 140-165, 613-624 | `YYYY-MM-DD-HH.MM.SS.hh0000`: hundredths from CURRENT-DATE, then the literal `0000`; read again for every transaction | S3.8: one formatter; hundredths cut from the clock, not rounded; clock read once per written transaction | `intT05_timestampFormat` (i01-baseline: `2022-07-05-10.00.00.000000`) | The harness freezes the clock, so differing timestamps inside one run and the time zone of the host clock are not exercised: **confirm on the mainframe** - mainframe operations owner |
| v | Two-digit years and Julian dates | ABSENT | Search `ACCEPT|DAY-OF|JULIAN`: nothing; the PARM (`INTCALC.jcl:22`) and CURRENT-DATE carry four-digit years | - | - | - | - |
| w | *Added:* PARM length is never checked | PRESENT | 176-178, 476 | `PARM-DATE` is read as 10 bytes whatever `PARM-LENGTH` says; a shorter PARM puts storage residue into `TRAN-ID` | S3.8: the job refuses a `parmDate` that is not exactly 10 characters and reports it; it does not pad | `intQ02_shortParmRefused` (no fixture) | What a short PARM yields on the host: **confirm on the mainframe** - mainframe application owner |
| x | *Added:* a failed keyed READ leaves the record area as it was | PRESENT | 416-420, 436-439 | After status 23 `DIS-GROUP-RECORD` still holds the previous rate until the DEFAULT read replaces it or the run abends | S2.4: `RateLookup` returns `Optional`; no rate variable outlives one balance | `intR03_fallsBackToDefault` (i04-default-group-fallback) | - |
| y | *Added:* alphanumeric and numeric items mixed | PRESENT | 194 (9(11) compared with X(11), first time with spaces); 483 (`'05'` into 9(04) gives `0005`); 621 (X(02) into 9(02)) | Comparison and moves work on the characters | S2.2: typed fields - account id is one numeric type everywhere; the category code is the constant 5 encoded by rule E1 as `0005` | `intR06_categoryCodeIs0005` (i01-baseline) | - |
| z | *Added:* abend by `CALL 'CEE3ABD'` with no STOP RUN after it | PRESENT | 628-632; `tools/stubs/CEE3ABD.cbl` | On the host the call does not return. Off-host the stub prints `CEE3ABD: USER ABEND U...` and ends with RC 12 | S6.2: one exception type ends the step; nothing runs after it. The stub's line and RC 12 are harness behaviour and no golden file holds them | `intR08_*` assert the legacy lines up to `ABENDING PROGRAM` only | See q |
| aa | *Added:* dead and empty code | PRESENT | 219-220 (branch never reached), 518-520 (`1400-COMPUTE-FEES` is empty), 172 and 192 (`WS-RECORD-COUNT` never used) | Nothing | S5.2: not ported. INT-Q04 and INT-H01 keep the questions open; INT-T07 has no counterpart | `intH01_noFeeTransaction` (i01-baseline) | - |

**What the off-host run cannot settle.** The golden files come from GnuCOBOL 3.1.2 with `-std=ibm -fsign=EBCDIC`,
not from Enterprise COBOL on z/OS. `tools/build.sh` sets no equivalent of the host options ARITH, TRUNC, NUMPROC or
ZONEDATA, and the estate holds no host compile listing, so the options the production load module was built with are
unknown: **confirm on the mainframe - mainframe application owner.** Until then rows b, c, d, e, f, g, h, p, q, r, s,
u and w carry the open point named in their last column. For row c the standard does not depend on the answer
(the argument is in the row), but it is an argument, not a host run.

## 5. Quirks and open decisions

Rules of type QUIRK (INT-Q01 to INT-Q06) and HOLE (INT-H01 to INT-H06) are OPEN in
`modernization/rules/interest/decisions.md`. The legacy behaviour is what is built.

| Id | Standard | Cites | Do | Do not |
|---|---|---|---|---|
| S5.1 | Each QUIRK or OPEN behaviour is reproduced in exactly one named place, behind its switch of `architecture.md` section 8 (FI-5), read only through `interest-policy`, with the legacy value as default. The rule id is in the name of that code and of its tests. | INT-Q04; `CBACT04C:188-222`; switch `interest.policy.updateLastAccount` | `policy.updatesAccountEndingInput()` (Javadoc: INT-Q04) called once in the processor; test `intQ04_lastAccountNotUpdated`. | `if (isLast) { /* legacy bug */ }` in the writer and again in the export; a second property name for the same decision. |
| S5.2 | Nothing that changes output is fixed or tidied, and no behaviour is added without a rule id that traces to it. Dead and empty legacy code is not ported; an improvement is proposed in the pull request for the register. | INT-Q06, INT-H01, INT-H04, INT-T07; `CBACT04C:214-217, 219-220, 518-520` | Write the 0.00 transaction (INT-Q06); charge closed accounts (INT-H04); write no fee (INT-H01). | Skip zero amounts "because they are noise", add an active-status check, or add a retry: none has a rule id. |

## 6. Errors and operations

The legacy has one failure path: message, status line, `ABENDING PROGRAM`, user abend 999 (INT-R08,
`CBACT04C:628-632`). The target has one too. Class and constant names below are proposals for ticket 08; the exit
code value is REL-10 (OPEN, mainframe operations owner).

| Legacy event | `CBACT04C` lines | Legacy outcome | Java | Job and exit | Run log (`sysout/INTCALC.STEP15.txt`) |
|---|---|---|---|---|---|
| Normal end, `GOBACK` | 181, 230-232 | RC 0 (INT-T03) | - | `COMPLETED`, exit 0, `rc.json` STEP15 = 0 | start line, records, end line (INT-T02) |
| TCATBALF status 00 | 326-329 | next balance | reader returns the next account | - | the 50-byte record echo (193) |
| TCATBALF status 10 | 330-340 | end of input; last account not updated (INT-Q04, INT-T09) | reader returns `null` | - | - |
| TCATBALF other status | 341-345 | abend 999 | `InterestRunStoppedException(READ_BALANCES)` | `FAILED`, exit `STOPPED` | `ERROR READING TRANSACTION CATEGORY FILE`, status line, `ABENDING PROGRAM` |
| ACCTFILE read, record missing (23) | 373-390 | `ACCOUNT NOT FOUND: ` + id, then abend 999 | `InterestRunStoppedException(ACCOUNT_MISSING)` | `FAILED`, exit `STOPPED` | `ACCOUNT NOT FOUND: <id>`, `ERROR READING ACCOUNT FILE`, `FILE STATUS IS: NNNN0023`, `ABENDING PROGRAM` |
| XREFFILE read by account, record missing (23) | 394-412 | same message, abend 999 | `InterestRunStoppedException(CARD_MISSING)` | `FAILED`, exit `STOPPED` | `ACCOUNT NOT FOUND: <id>`, `ERROR READING XREF FILE`, status line, `ABENDING PROGRAM` |
| XREFFILE: several cards for one account | 394-395; INT-H03, G08 | not determined by the source | `InterestRunStoppedException(MULTI_CARD)` when `multiCard=STOP` | `FAILED`, exit `STOPPED` | a line naming INT-H03 (no legacy text exists) |
| DISCGRP status 23 | 416-439 | fallback to DEFAULT (INT-R03); not an error | `Optional.empty()` from `RateLookup`, then the DEFAULT lookup | - | `DISCLOSURE GROUP RECORD MISSING`, `TRY WITH DEFAULT GROUP CODE` |
| DISCGRP other status | 422-434 | abend 999 | `InterestRunStoppedException(READ_RATE)` | `FAILED`, exit `STOPPED` | `ERROR READING DISCLOSURE GROUP FILE`, status line, `ABENDING PROGRAM` |
| DEFAULT rate missing or unreadable | 444-459 | abend 999 | `InterestRunStoppedException(DEFAULT_RATE_MISSING)` | `FAILED`, exit `STOPPED` | `ERROR READING DEFAULT DISCLOSURE GROUP`, `FILE STATUS IS: NNNN0023`, `ABENDING PROGRAM` |
| ACCTFILE rewrite fails | 356-369 | abend 999 | `InterestRunStoppedException(UPDATE_ACCOUNT)` | `FAILED`, exit `STOPPED` | `ERROR RE-WRITING ACCOUNT FILE`, status line, `ABENDING PROGRAM` |
| TRANSACT write fails | 500-514 | abend 999 | `InterestRunStoppedException(WRITE_TRANSACTION)` | `FAILED`, exit `STOPPED` | `ERROR WRITING TRANSACTION RECORD`, status line, `ABENDING PROGRAM` |
| Open fails (five files) | 234-323 | abend 999 before any work | `InterestRunStoppedException(OPEN_<file>)`: the table or output location of that file cannot be reached | `FAILED`, exit `STOPPED` | the legacy text of that file, including `ERROR OPENING DALY REJECTS FILE` for the rates (INT-T10), then `ABENDING PROGRAM` |
| Close fails (five files) | 522-611 | abend 999 after all work | `InterestRunStoppedException(CLOSE_<file>)` from the export step | `FAILED`, exit `STOPPED` | `ERROR CLOSING ...` of that file, `ABENDING PROGRAM` |
| Amount or counter does not fit | 464-467, 474; INT-H05 | not determined by the source | `InterestRunStoppedException(OVERFLOW)` when `overflow=STOP` | `FAILED`, exit `STOPPED` | a line naming INT-H05 (no legacy text exists) |
| `CALL 'CEE3ABD'`, code 999 | 628-632 | U0999 on the host; off-host stub RC 12 | the exception above ends the step | exit `STOPPED` (value: REL-10) | nothing after `ABENDING PROGRAM`; the stub's line is not reproduced |

| Id | Standard | Cites | Do | Do not |
|---|---|---|---|---|
| S6.1 | Every row above is implemented as written; a file status is never a field passed around. The status line keeps the legacy format `FILE STATUS IS: NNNN` + four digits and is written only where the condition determines the legacy status (a missing record is 23). A database or file-system failure writes the legacy message of the operation it replaces to the run log and its own cause to the application log. | INT-R08, INT-T08; `CBACT04C:635-648` | `runLog.line("FILE STATUS IS: NNNN0023")` for a missing account. | Invent a status such as `0099` for a `SQLException`, or copy the 9x binary-byte branch. |
| S6.2 | One exception type stops the run; it carries the reason and the rule id. It is never caught to continue: no skip, no retry (`architecture.md` section 6). The exit code is one constant. | INT-R08, INT-T03; `CBACT04C:628-632` | `throw new InterestRunStoppedException(StopReason.ACCOUNT_MISSING)`; the step ends `FAILED`. | `catch (Exception e) { log.warn(...); continue; }`, `.faultTolerant().skip(...)`, `System.exit(12)` in several places. |
| S6.3 | Restart follows `architecture.md` section 6 (REL-11, proposed): `loadExtractsStep` and `exportDatasetsStep` repeat; `calcInterestStep` continues after the last committed account with the saved counter and the run log cut back. A second run with the same PARM is not a restart and is not blocked while `rerunGuard=OFF` (INT-H02). | INT-H02; `CBACT04C:352-356, 474` | Restart test: stop after a chunk, restart, compare with an uninterrupted run. | Restart from the first account; reset the counter to 000001 on restart. |
| S6.4 | A normal run writes the outputs in the recorded format (FI-2): `datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt` (350-byte records), the ACCTDATA image, `rc.json`. A failed run leaves no SYSTRAN generation in place (legacy `DISP=(NEW,CATLG,DELETE)`) but keeps committed account updates. | INT-T09; `app/jcl/INTCALC.jcl:22, 37-41`; `CVTRA05Y/TRAN-ID` | Export to a temporary directory, move on success. | Write SYSTRAN while calculating, or delete committed rows on failure. |
| S6.5 | Two logs, kept apart. The **run log** (`RunLog` port) is the legacy SYSOUT, byte for byte, and is a parity output; it contains what the legacy printed, including the balance record echo and `ACCOUNT NOT FOUND: <id>`. The **application log** (SLF4J) carries run id, step, rule id, counts, sums, exit status and stop reason - never an account id, card number, balance, amount or record image. | INT-T02; `CBACT04C:181, 193, 230, 375, 418-419`; `CVTRA01Y/FILLER` | `log.info("run {} step {} accounts={} transactions={}", ...)`; `runLog.echo(recordImage)`. | `log.info("account {} interest {}", accountId, amount)`; formatting the echo as `12.35` instead of the record image. |

## 7. Tests

| Id | Standard | Cites | Do | Do not |
|---|---|---|---|---|
| S7.1 | At least one test per rule id a ticket implements (INT-R01 to R08, INT-Q01 to Q06, INT-H01 to H05), named `<ruleId>_<whatItProves>` in camelCase. INT-H06 is outside the job (scheduler order) and has no test; say so in the checklist. A switch has a test for the default and for each other value. | INT-Q01; `CBACT04C:464-465` | `intQ01_truncatesHalfCent`, `intQ01_halfUpWhenSwitched`. | `testCalculate1`, or one test "covering everything". |
| S7.2 | Each rule's tests cover the fixture cases and edge cases listed for that rule in `rules.yaml` and in the ticket's Verification block; behaviours with no fixture (section 4: b, e, f, m empty input, p two cards, q, w) get synthetic data under `java/interest/src/test`. | INT-R03, INT-Q05; `fixtures/interest/i04-default-group-fallback`, `i06-negative-balance` | `intQ05_negativeBalanceGivesNegativeInterest` reads the i06 input. | Test only i01-baseline. |
| S7.3 | Integration tests (steps, JDBC adapters, restart) run against PostgreSQL 15 in a container - the `db` service of `docker-compose.yml` (`postgres:15`) or the same image - with the schema of FI-4. | `CVTRA01Y/TRAN-CAT-KEY` (`COLLATE "C"`), `CVACT01Y/FILLER` (`BYTEA`); E6 | `intR01_readsInKeyOrder` against `postgres:15`. | H2 or another in-memory database, or mocks of `JdbcTemplate` in place of an integration test. |
| S7.4 | Expected values come from the recorded legacy output (`fixtures/interest/<case>/expected`, compared with `tools/parity/compare.py`) or are written by hand in the test with the arithmetic shown. Never from the code under test. | INT-Q01; `fixtures/interest/i02-tie-half-cent`; FI-2 | `assertEquals(new BigDecimal("12.35"), ...)` with the comment `1235.50 * 12.00 / 1200 = 12.355`. | Run the job, save its output as `expected.txt`, assert against it; or compute the expectation with the same formula class. |

## 8. Guardrails

| Id | Standard | Cites | Do | Do not |
|---|---|---|---|---|
| S8.1 | No edit under `app/`, `fixtures/` (inputs and `expected`) or `tools/parity`. | FI-1, FI-2; `CVTRA05Y/TRAN-AMT` | A parity difference is fixed in `java/interest`. | Change a copybook PIC or an expected file so the Java output passes. |
| S8.2 | Golden files are not re-recorded (`make record`) by a ticket session. | INT-Q01; `fixtures/interest/i02-tie-half-cent` | Report a suspected wrong golden file in the pull request. | `make record CHAIN=interest`. |
| S8.3 | No new dependency without one sentence in the pull request saying why and what was considered instead. | E2 (the codec is built here, from the copybooks) | "Adds `org.testcontainers:postgresql` (test scope) for S7.3." | Pull in a COBOL-record or EBCDIC library unannounced. |
| S8.4 | Frozen shared interfaces FI-1 to FI-6 are not changed by a ticket other than the one that owns them; a needed change is reported. | `architecture.md` section 9; INT-H03 (`CardLookup`) | Propose a second `CardLookup` method in the pull request. | Add a parameter to a port, rename a switch, or alter `db/postgres/02-interest.sql` from another ticket. |
| S8.5 | Synthetic data only, in tests, examples, logs and pull requests. | `CVACT03Y/XREF-CARD-NUM`, `CVACT01Y/ACCT-ID` | Account 00000002003 and the card numbers of the fixtures. | A real card number, name or customer reference. |

## 9. Review checklist

The ticket session copies this list into its pull request and marks each line `PASS`, `FAIL` (with the reason) or
`N/A` (with the reason: outside this ticket's scope). The reviewer checks it against the diff.

Standards:

- [ ] S1.1 Sources used in order; recorded output decides - PASS / FAIL
- [ ] S1.2 Rule/code disagreements reproduced as coded and reported - PASS / FAIL
- [ ] S2.1 Designed from rules; no method per paragraph - PASS / FAIL
- [ ] S2.2 Immutable business records; copybook names only at the mapping - PASS / FAIL
- [ ] S2.3 Spring Batch shape, transaction and restart boundaries of the architecture - PASS / FAIL
- [ ] S2.4 Types and return values, no flags or result codes - PASS / FAIL
- [ ] S2.5 Constructor injection; no mutable static state - PASS / FAIL
- [ ] S3.1 `BigDecimal` with data-map precision and scale; no `double`/`float` - PASS / FAIL
- [ ] S3.2 Formula: exact product, one division, mode from the policy - PASS / FAIL
- [ ] S3.3 Rounding mode named on every other arithmetic step - PASS / FAIL
- [ ] S3.4 Range check before store; overflow goes to the policy - PASS / FAIL
- [ ] S3.5 Encodings only in `record-codec` - PASS / FAIL
- [ ] S3.6 Width, padding and FILLER only in codec and schema - PASS / FAIL
- [ ] S3.7 Code page and key order each in one place - PASS / FAIL
- [ ] S3.8 Clock and run parameters injected; `parmDate` is text - PASS / FAIL
- [ ] S4.1 Every PRESENT behaviour touched has its test; new ones reported - PASS / FAIL
- [ ] S5.1 Each quirk/open decision in one named place behind its switch, rule id in names - PASS / FAIL
- [ ] S5.2 Nothing fixed, tidied or added without a rule id - PASS / FAIL
- [ ] S6.1 Status and message mapping as in section 6 - PASS / FAIL
- [ ] S6.2 One stop exception, one exit-code constant, no skip or retry - PASS / FAIL
- [ ] S6.3 Restart as in the architecture - PASS / FAIL
- [ ] S6.4 Outputs in the recorded format; failed run leaves no SYSTRAN - PASS / FAIL
- [ ] S6.5 Run log byte for byte; no account data in the application log - PASS / FAIL
- [ ] S7.1 A test per rule id, named for it - PASS / FAIL
- [ ] S7.2 Fixture and edge cases of each rule covered - PASS / FAIL
- [ ] S7.3 Integration tests on PostgreSQL 15 in a container - PASS / FAIL
- [ ] S7.4 No assertion against the code's own output - PASS / FAIL
- [ ] S8.1 No edit under `app/`, `fixtures/`, `tools/parity` - PASS / FAIL
- [ ] S8.2 No golden file re-recorded - PASS / FAIL
- [ ] S8.3 Every new dependency explained in the pull request - PASS / FAIL
- [ ] S8.4 Frozen shared interfaces unchanged - PASS / FAIL
- [ ] S8.5 Synthetic data only - PASS / FAIL

Behaviours PRESENT in section 4 (k, n and v are ABSENT and have no line):

- [ ] a Truncation without ROUNDED - PASS / FAIL
- [ ] b High-order digit loss (range check, STOP) - PASS / FAIL
- [ ] c Intermediate precision of the COMPUTE - PASS / FAIL
- [ ] d Zoned overpunch signs - PASS / FAIL
- [ ] e Binary items stay out of data and domain - PASS / FAIL
- [ ] f Non-numeric data rejected at the boundary - PASS / FAIL
- [ ] g Fixed width, padding, STRING - PASS / FAIL
- [ ] h Code page and collating order - PASS / FAIL
- [ ] i REDEFINES replaced by formatter and function - PASS / FAIL
- [ ] j FILLER and record length round trip - PASS / FAIL
- [ ] l No state carried between accounts except the counter - PASS / FAIL
- [ ] m Control break and end of file (INT-Q04, INT-T09) - PASS / FAIL
- [ ] o Level-88 result codes replaced by types - PASS / FAIL
- [ ] p Indexed access through the ports; several cards to the policy - PASS / FAIL
- [ ] q File statuses and abend - PASS / FAIL
- [ ] r PARM, generation, disposition, return code - PASS / FAIL
- [ ] s DISPLAY output in the run log - PASS / FAIL
- [ ] t PARM text against the clock - PASS / FAIL
- [ ] u Timestamp format and hundredths - PASS / FAIL
- [ ] w PARM length checked - PASS / FAIL
- [ ] x No stale rate after a failed lookup - PASS / FAIL
- [ ] y Typed fields where COBOL mixed text and numbers - PASS / FAIL
- [ ] z Abend ends the step; stub output not reproduced - PASS / FAIL
- [ ] aa Dead and empty code not ported - PASS / FAIL
