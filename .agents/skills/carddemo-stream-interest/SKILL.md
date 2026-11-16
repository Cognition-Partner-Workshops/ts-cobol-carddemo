---
name: carddemo-stream-interest
description: Annex of the interest stream (INTCALC -> CBACT04C) to the shared CardDemo skills - its programs, jobs, paths, fixture cases, datasets, copybooks and rule-id prefix, the standards as they read for interest with their citations and examples, the behaviours table of CBACT04C, the legacy-event table, the review checklist, and the interest part of the build and verify steps and of the pitfalls. Use with carddemo-conversion-standards, carddemo-ticket-build, carddemo-ticket-verify and carddemo-pitfalls when implementing, reviewing or proving any interest ticket, or when a question comes up about how a CBACT04C behaviour is reproduced in Java.
---

# Stream annex: interest

> **PROPOSED - not confirmed.** The standards in section 2 are those of `interest-conversion-standards`, moved here
> in step 5c without a change of id or meaning. **Owner who must confirm: the target architect.**
> This annex and the four shared skills were followed from a clean checkout in step 5c and gave the combined check
> recorded in `modernization/design/build-run.md` (`modernization/design/evidence/05c-shared-skills-check.txt`).

Read the shared skill first, then the section of this annex with the same subject. The shared skills hold what is
true for any batch stream; this annex holds what is true for interest only, and for each shared item the citation
and the example the shared skill left out. Ticket sessions **do not edit** this annex or the shared skills: a
change is proposed in the pull request, for the target architect.

| Shared skill | Section here |
|---|---|
| `carddemo-conversion-standards` | 2 |
| `carddemo-ticket-build` | 1 (values), 3 |
| `carddemo-ticket-verify` | 1 (values), 4 |
| `carddemo-pitfalls` | 5 |

## 1. The stream

| | |
|---|---|
| Stream, chain in `tools/runjcl/chains.json` | `interest` - "Monthly interest calculation: INTCALC (CBACT04C)" |
| Program | `app/cbl/CBACT04C.cbl` (652 lines), cited as `CBACT04C:lines` |
| Job, step | `app/jcl/INTCALC.jcl`, `STEP15 EXEC PGM=CBACT04C,PARM='2022071800'` |
| Setup jobs of the harness | `DEFGDGB`, `ACCTFILE`, `XREFFILE`, `TCATBALF`, `DISCGRP` |
| Copybooks | `CVTRA01Y` (category balance), `CVACT03Y` (card cross-reference), `CVTRA02Y` (disclosure group), `CVACT01Y` (account), `CVTRA05Y` (transaction) |
| Frozen clock | `2022-07-05 10:00:00.00` |
| Rule-id prefix | `INT-`: `INT-R01` to `INT-R08` (rules), `INT-Q01` to `INT-Q06` (quirks), `INT-H01` to `INT-H06` (holes), `INT-T01` to `INT-T11` (technical rules in `rules.yaml`; the tickets use `INT-T` numbers for their work items too) |
| Other ids used | frozen interfaces FI-1 to FI-6 and switches (`architecture.md` sections 8 and 9), release decisions REL-n (`decisions-release.md`), encoding rules E1-E6 and gaps G01-G12 (`data-map-interest.md`) |
| Rules | `modernization/rules/interest/rules.yaml`, `modernization/rules/interest/decisions.md`, `modernization/rules/interest/expected/` |
| Design | `modernization/design/architecture.md` (0.1.0), `modernization/design/data-map-interest.md`, `modernization/design/standards.md` (summary), `modernization/design/tickets/01-pilot-thin-slice.md` to `10-data-move-kit.md` |
| Maven module | `java/interest`, `com.carddemo:interest:0.1.0`, jar `java/interest/target/interest.jar` |
| Job | `interestCalcJob`; with profile `database`: `loadExtractsStep` -> `calcInterestStep` -> `exportDatasetsStep` |
| Schema script | `db/postgres/02-interest.sql` (six tables, FI-4), tests `db/postgres/tests/run.sh` |
| Policy switches | `interest.policy.*`, read only through `policy/InterestPolicy` |
| `<parity-target>` | `parity-java` (FI-6) |
| `<out>` | `work/parity-java` (`PARITY_JAVA_OUT`) |
| Other make targets | `recon-java`, `sidebyside-java` (ticket 10), `parity-naive` (half-even reference; must fail on `i02-tie-half-cent` only) |
| Job parameters of the parity run | `parmDate=2022071800 runId=<new each launch> format=fixture inputDir=fixtures/interest/<case>/input outputDir=<out>/<case>/actual`, with `--spring.profiles.active=database --interest.clock.frozen="2022-07-05 10:00:00.00"` |
| Evidence | `java/interest/evidence/<ticket>/`; the pilot's three files are directly in `java/interest/evidence/` |
| Report script | `.agents/skills/interest-ticket-verify/parity-report.sh <ticket> <case> [...]` |
| A unit test class for build step 8 | `InterestFormulaTest` |

**Datasets.**

| Dataset | Layout | Role |
|---|---|---|
| `AWS.M2.CARDDEMO.TCATBALF.PS` -> `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS` | `CVTRA01Y`, 50 bytes | input, read in key order |
| `AWS.M2.CARDDEMO.CARDXREF.PS` -> `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS` and `.AIX.PATH` | `CVACT03Y`, 50 bytes | input, read by account |
| `AWS.M2.CARDDEMO.DISCGRP.PS` -> `AWS.M2.CARDDEMO.DISCGRP.VSAM.KSDS` | `CVTRA02Y`, 50 bytes | input, read by key |
| `AWS.M2.CARDDEMO.ACCTDATA.PS` -> `AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS` | `CVACT01Y`, 300 bytes | input and output (rewritten) |
| `AWS.M2.CARDDEMO.SYSTRAN(+1)`, recorded as `SYSTRAN.G0001V00` | `CVTRA05Y`, 350 bytes | output, `DISP=(NEW,CATLG,DELETE)` |
| `sysout/INTCALC.STEP15.txt` | text | output (DISPLAY lines) |
| `rc.json` | text | output (`INTCALC`, steps `STEP05` to `STEP30`; only `STEP15` is the job's own) |

The input directory of a case also holds `TRANCATG.PS` and `TRANTYPE.PS`; the chain does not list them as inputs.

**Fixture cases** (`fixtures/interest/<case>`, each with a `README.md`):

| Case | What it proves |
|---|---|
| `i01-baseline` | two accounts, exact results; the last account is not updated |
| `i02-tie-half-cent` | 12.355 is truncated to 12.35 (no `ROUNDED`) |
| `i03-zero-rate-skipped` | a category at rate 0.00 writes no transaction |
| `i04-default-group-fallback` | a group with no rate falls back to `DEFAULT` and prints two lines |
| `i05-multi-category` | two balances of one account: two transactions, the balance grows by the total |
| `i06-negative-balance` | a negative balance gives negative interest |
| `i07-nontie-rounding` | sub-cent remainders that are not ties |

## 2. Standards: what the shared skill left out

Shared text: `.agents/skills/carddemo-conversion-standards/SKILL.md`. Each row below has the same id. Where the
shared text is the interest text without its interest names, the second column gives the standard exactly as it was
written for interest and applied by tickets 01 to 10; where it says *as shared, word for word*, nothing was left
out of the text. Cites, Do and Do not are unchanged. S3.2 is a STREAM standard: it exists only here.

Status and owner are those of the shared skill: **PROPOSED, owner the target architect.** Applied by the pilot ticket
(`modernization/design/tickets/01-pilot-thin-slice.md`; its checklist is section 4 of
`java/interest/evidence/PILOT-REPORT.md`) and by tickets 02 to 10 (`java/interest/evidence/<ticket>/report.md`,
section 4). The human-readable summary is `modernization/design/standards.md`; if it differs, the skills are right.

Sources, all read-only: rules `modernization/rules/interest/rules.yaml` (INT-R, INT-Q, INT-H, INT-T ids) and
`decisions.md`; `modernization/design/architecture.md` (version 0.1.0); `modernization/design/data-map-interest.md`
(rows are cited as `copybook/FIELD`, encoding rules as E1-E6, gaps as G01-G12); `app/cbl/CBACT04C.cbl` (cited as
`CBACT04C:lines`).

Head of `interest-conversion-standards` as it stood before the split, kept word for word (it says "this file": that
was the interest skill; the statement that no other ticket has applied the standards predates tickets 02 to 10):

> **PROPOSED - not confirmed. Applied once, by the pilot ticket.** These standards were written by an agent from
> the rulebook, the design, the data map, `CBACT04C.cbl`, its copybooks, `INTCALC.jcl` and `tools/build.sh`. The
> pilot ticket (`modernization/design/tickets/01-pilot-thin-slice.md`) applied them to the code in `java/interest`;
> its filled-in checklist is section 4 of `java/interest/evidence/PILOT-REPORT.md`. No other ticket has applied
> them. Corrections approved after the pilot are in the text; the ones still proposed are in section 4 of
> `modernization/design/skills.md`.
> **Owner who must confirm: the target architect.**

This file is the only place the standards are written. Ticket sessions follow it and **do not edit it**: a standard
that is wrong or missing is reported in the pull request as a proposed change, for the target architect.
The human-readable summary is `modernization/design/standards.md`; if the two differ, this file is right.

Sources, all read-only: rules `modernization/rules/interest/rules.yaml` (INT-R, INT-Q, INT-H, INT-T ids) and
`decisions.md`; `modernization/design/architecture.md` (version 0.1.0); `modernization/design/data-map-interest.md`
(rows are cited as `copybook/FIELD`, encoding rules as E1-E6, gaps as G01-G12); `app/cbl/CBACT04C.cbl` (cited as
`CBACT04C:lines`). Each standard has an id (S1.1 ...) used in the checklist.

### 2.1 What to convert from, in this order

1. The **ticket** and the **rules** it names: what to build and why.
2. **`architecture.md` and the data map**: the shape of the target (steps, ports, tables, types, switches).
3. The **COBOL lines the rule cites**: the exact behaviour.
4. The **recorded legacy output** (`fixtures/interest/<case>/expected`): what is correct.

| Id | Standard as it reads for interest | Cites | Do | Do not |
|---|---|---|---|---|
| S1.1 | *as shared, word for word* | INT-R05, INT-Q01; `CBACT04C:464-465` | Rule INT-R05 gives the formula, lines 464-465 give the truncation, i02-tie-half-cent proves 12.35. | Start from the COBOL listing and port it top to bottom; or decide 12.36 is "right" because the rule text says "divided by 1200". |
| S1.2 | *as shared, word for word* | INT-Q04; `CBACT04C:188-222` | The comment says the last account is updated at end of file; the code never does it - reproduce the code, report it. | Follow the comment or the rule text and leave the difference unmentioned. |

### 2.2 Idiomatic Java, not translated COBOL

| Id | Standard as it reads for interest | Cites | Do | Do not |
|---|---|---|---|---|
| S2.1 | Design from the rules. One unit of code per rule or decision, named for what it decides. No method per paragraph, no emulated `GO TO` or `PERFORM THRU` (the program has none: behaviour n). | INT-R02, INT-R03; `CBACT04C:415-460` | `RateLookup.findRate(group, type, category)` returning `Optional<BigDecimal>`, and one `rateFor(...)` that applies the DEFAULT fallback. | `p1200GetInterestRate()` and `p1200AGetDefaultIntRate()`. |
| S2.2 | *as shared, word for word* | `CVTRA01Y/TRAN-CAT-BAL`, `CVACT01Y/ACCT-CURR-BAL`; `CBACT04C:166-173` | `record CategoryBalance(long accountId, String typeCode, int categoryCode, BigDecimal balance)`; `"TRAN-CAT-BAL"` appears only in the mapping. | `class WsMiscVars { String wsLastAcctNum; BigDecimal wsMonthlyInt; char wsFirstTime; }` or `getTranCatBal()` in the domain. |
| S2.3 | Reading, processing and writing follow `architecture.md` sections 2, 5 and 6: `calcInterestStep` reads one item = one account with all its balances in key order, the processor returns the result, the writer inserts the transactions and updates three account columns, a chunk is whole accounts, and the restart state (last account, transaction counter, `seq_no`, run-log length) is saved with the commit. The calculation uses only the ports of FI-3. **With file adapters (no table, as in ticket 01):** the same reader, processor and writer shape and the same ports; there is no restart state to save. | INT-R01, INT-R07; `CBACT04C:188-222`; `CVTRA01Y/TRAN-CAT-KEY` | `ItemReader<AccountBalances>` -> `ItemProcessor<AccountBalances, AccountInterest>` -> `ItemWriter<AccountInterest>`; total interest is a local sum. | A tasklet with `while (!endOfFile)` that keeps `lastAccountId` and `totalInterest` in fields and writes as it goes. |
| S2.4 | A type or a return value replaces every flag, switch and result code (`END-OF-FILE`, `WS-FIRST-TIME`, `APPL-RESULT` and its level-88 names). | `CBACT04C:133-135, 170, 188-198, 328-347` | End of input is the reader returning `null`; a missing rate is `Optional.empty()`; a failure is an exception (S6.2). | `int applResult = 16; if (applResult == APPL_EOF) endOfFile = 'Y';` |
| S2.5 | Constructor injection only; no mutable static state. The transaction counter, the clock and the policy are injected or held in step scope. | INT-Q02; `CBACT04C:173, 474` | `InterestCalculator(RateLookup rates, InterestPolicy policy, Clock clock)`; the counter comes from the step's restart state. | `private static int tranIdSuffix;` or `@Autowired` on a field. |

### 2.3 Numbers and data

| Id | Standard as it reads for interest | Cites | Do | Do not |
|---|---|---|---|---|
| S3.1 | Money and rates are `BigDecimal` with the precision and scale of their data-map row: balance and interest (11,2), rate (6,2), account amounts (12,2). No `double` or `float` anywhere, including tests. | `CVTRA01Y/TRAN-CAT-BAL`, `CVTRA02Y/DIS-INT-RATE`, `CVTRA05Y/TRAN-AMT`, `CVACT01Y/ACCT-CURR-BAL`; INT-T06 | `new BigDecimal("1235.50")` | `double interest = balance * rate / 1200;` or `BigDecimal.valueOf(12.355)`. |
| S3.2 | The formula is one exact multiplication and one division that goes straight to scale 2 with an explicit mode. The mode comes from `interest.policy.rounding`; the default TRUNCATE is `RoundingMode.DOWN` (towards zero, also for negative balances). | INT-R05, INT-Q01, INT-Q05; `CBACT04C:464-465` | `balance.multiply(rate).divide(TWELVE_HUNDRED, 2, policy.roundingMode())` | `divide(TWELVE_HUNDRED)` with no mode; `MathContext.DECIMAL64`; `RoundingMode.FLOOR` (wrong for -3.005); a literal `HALF_EVEN`. |
| S3.3 | Every other arithmetic step also names its mode. The account total and the new balance are sums of scale-2 values, so the mode is `RoundingMode.UNNECESSARY` and the result is asserted to be scale 2. | INT-R07; `CBACT04C:352, 467` | `total.add(interest).setScale(2, RoundingMode.UNNECESSARY)` | `setScale(2)` or a silent `HALF_UP` "to be safe". |
| S3.4 | Before a value is stored, check it against the PIC of its row. A value that does not fit goes to `interest.policy.overflow` (default STOP, naming INT-H05). Never cut high-order digits to imitate COBOL: the source does not determine that behaviour. | INT-H05; `CVTRA05Y/TRAN-AMT`, `CVTRA05Y/TRAN-ID`; `CBACT04C:464-467, 474` | `policy.checkFits(interest, 9, 2)` and the same for the counter above 999999. | `interest.remainder(new BigDecimal("1000000000"))`, or letting PostgreSQL raise "numeric field overflow". |
| S3.5 | *as shared, word for word* | E1, E2, E5; `CVTRA01Y/TRAN-CAT-BAL`; `CBACT04C:326, 373` | `codec.decode(layout, bytes)` in `load`; `codec.encode(...)` in `export`. | `if (last == '}' ...)` or `new String(bytes, "Cp037")` in `domain`, `policy` or `port`. |
| S3.6 | Width, padding and FILLER belong to the codec and the schema. FILLER is raw bytes (E4) written back unchanged; a new transaction's FILLER and the `TRAN-DESC` tail are spaces (G05). The calculation updates only the three account columns. The digits of an id inside a text are a domain matter: the domain formats the 11-digit account id of the description and of a lookup key and the 6-digit counter of the transaction id; the width of the field that holds the text stays in the codec. | E3, E4; `CVACT01Y/FILLER`, `CVTRA05Y/TRAN-DESC`; INT-T04; `CBACT04C:356, 485-489` | `"Int. for a/c " + elevenDigits(accountId)` and the six-digit counter in the domain; padding to 100 in the codec. | `String.format("%-100s", desc)` in the domain; rewriting the whole account row from a decoded object. |
| S3.7 | Code page and key order are fixed in one place each: the codec option (`fixture` ISO-8859-1 or `host`, G01) and the reader's `ORDER BY trancat_acct_id, trancat_type_cd COLLATE "C", trancat_cd`. **With file adapters:** there is no query, so the key order is fixed once in the `BalanceSource` adapter, by ordering the records on the 17 bytes of `TRAN-CAT-KEY`, and nowhere else. | E3, E6; `CVTRA01Y/TRAN-CAT-KEY`; INT-R01; `CBACT04C:28-32` | One ordered query in the `BalanceSource` adapter. | `balances.sort(...)` in Java where a table is read, a sort anywhere outside the `BalanceSource` adapter, or an `ORDER BY` without the collation. |
| S3.8 | Time and the run's date parameters are injected. Timestamps come from an injected `java.time.Clock`, read once per written transaction, hundredths cut (not rounded) and followed by `0000`. `parmDate` is a job parameter of exactly 10 characters, used as text for the id prefix and never parsed as a date; another length is refused. Any frozen business date (switch `timestampSource=CYCLE_DATE`) is a job parameter too. | INT-Q02, INT-Q03, INT-T05; `CBACT04C:176-178, 476-480, 613-624`; `CVTRA05Y/TRAN-ID` | `clock.instant()` in one `TimestampFormatter`; tests use `Clock.fixed(...)` at the frozen fixture time 2022-07-05 10:00:00.00. | `LocalDateTime.now()`, `System.currentTimeMillis()`, `LocalDate.parse(parmDate)`. |

### 2.4 COBOL behaviours with no Java equivalent

Verified against `app/cbl/CBACT04C.cbl` (652 lines), the five copybooks, `app/jcl/INTCALC.jcl` and
`tools/build.sh:16` (`cobc -m -std=ibm -fsign=EBCDIC`, GnuCOBOL 3.1.2 in the estate container). Nothing was executed
for this table; the fixture cases named are recorded legacy runs.

| Id | Standard as it reads for interest | Cites | Do | Do not |
|---|---|---|---|---|
| S4.1 | Every behaviour marked PRESENT below is reproduced as its "Java standard" column says and proven by the test named in its row. A behaviour found later that is not in the table is reported in the pull request, not handled silently. | rows a-aa | Add `intQ01_truncatesHalfCent` with the ticket that implements the formula. | Reproduce a behaviour "from experience" without a row. |

Line numbers are `app/cbl/CBACT04C.cbl` unless a file is named. "Search" means
`grep -nE` over CBACT04C.cbl and the five copybooks returned nothing. Test names are the names ticket
sessions must use; those of the behaviours ticket 01 touched exist in `java/interest/src/test`, the others do not
yet.

| | Behaviour | In this stream | Evidence | What the legacy does | Java standard | Test (fixture case) | Off-host run cannot settle |
|---|---|---|---|---|---|---|---|
| a | Decimal truncation, no ROUNDED | PRESENT | 464-465; search `ROUNDED`: nothing | 12.355 is stored in `WS-MONTHLY-INT` S9(09)V99 as 12.35 | S3.2: one `divide(1200, 2, mode)` with the mode from `interest.policy.rounding`, default TRUNCATE = `RoundingMode.DOWN` | `intQ01_truncatesHalfCent` (i02-tie-half-cent), `intQ01_nonTieUnchanged` (i07-nontie-rounding) | - |
| b | High-order digits lost, no ON SIZE ERROR | PRESENT (not reachable by any fixture) | 352, 464-467, 474; search `SIZE ERROR`: nothing | Interest above 999,999,999.99, an account total or balance beyond its PIC, or transaction 1,000,000 would lose leading digits silently | S3.4: range check against the PIC of the data-map row before every store; `interest.policy.overflow=STOP` fails the run naming INT-H05. Never `setScale`/`remainder` to imitate the loss | `intH05_interestAboveFieldStops`, `intH05_millionthTransactionStops` (no fixture) | The legacy result of an overflow: **confirm on the mainframe** - mainframe application owner |
| c | Intermediate precision inside COMPUTE | PRESENT | 464-465; operands CVTRA01Y:9, CVTRA02Y:9 | `(S9(09)V99 * S9(04)V99) / 1200`: product has 4 decimals and at most 17 digits; the quotient is cut to 2 decimals | S3.2: multiply exactly, divide once, straight to scale 2. No intermediate `setScale`, no `MathContext` | `intR05_formulaExactProductThenDivide` (i01-baseline, i07-nontie-rounding) | Truncating an exact quotient gives the same cents at any intermediate precision of 2 or more decimals, so ARITH should not matter - reasoning, not a host run: **confirm on the mainframe** - mainframe application owner |
| d | Sign handling: unsigned fields that drop a sign; zoned overpunch | Overpunch PRESENT; sign-dropping move ABSENT | CVTRA01Y:9, CVTRA02Y:9, CVACT01Y:7-9 and 13-14, CVTRA05Y:10; `tools/build.sh:16` (`-fsign=EBCDIC`). Every MOVE/ADD into an unsigned field (474, 483, 491, 621) has an unsigned source | Last byte of each S9 field carries digit and sign (`{A-I` plus, `}J-R` minus); results are written C/D signed | S3.5: data-map rule E2 in `record-codec` only; domain sees a signed `BigDecimal` | `e2_overpunchRoundTrip`, `intQ05_negativeInterestIsDSigned` (i06-negative-balance) | F-signed positives and minus zero in host data (G02, G03; NUMPROC): **confirm on the mainframe** - mainframe data owner |
| e | Packed and binary fields; binary truncation by compile option | Packed ABSENT; binary PRESENT in storage only, on no dataset | Search `COMP-3` and `PACKED`: nothing. Binary: 125, 133, 138-139, 177; data-map rule E5 | Binary items hold 0, 8, 12, 16, 999 and one status byte; `PARM-LENGTH` is never read | S3.5: the codec refuses a field whose USAGE is not DISPLAY. No binary type in the domain; the result codes become return values (o) | `e5_codecRefusesNonDisplayUsage` (no fixture) | TRUNC option and byte order affect only the `9x` branch of the status line (636-642): **confirm on the mainframe** - mainframe application owner |
| f | Non-numeric data in a numeric field | PRESENT | 636 (`IO-STATUS NOT NUMERIC`); 373/394/416 `READ INTO` move bytes unchecked into 9 and S9 fields; 483 | A bad digit in `TRAN-CAT-BAL` or `DIS-INT-RATE` reaches the COMPUTE unchecked | S3.5: rules E1/E2 reject the record at the boundary; the run stops before any calculation. No substitution of zero | `e1_nonDigitRejectsRecord` (no fixture) | S0C7 or a computed value on the host (NUMPROC, ZONEDATA): **confirm on the mainframe** - mainframe application owner |
| g | Fixed width: padding, truncation on MOVE, comparison with padding, STRING, reference modification | PRESENT, except right truncation on MOVE: ABSENT (every sending item fits its receiver) | 437, 484 (literal padded); 476-480, 485-489 (STRING); 638, 645 (reference modification); 194, 422 (comparisons) | `TRAN-ID` is filled exactly (10 + 6). `TRAN-DESC` gets 24 of 100 bytes; STRING does not pad (INT-T04, G05). `'DEFAULT'` is padded to 10 for the key | S3.6: the domain holds trimmed values; width and padding come from the data-map row in the codec. The fallback group is the constant `DEFAULT` compared as CHAR(10). `TRAN-DESC` tail and FILLER are written as spaces | `intR06_transactionFieldsFixedWidth` (i01-baseline), `intR03_defaultGroupKeyPadded` (i04-default-group-fallback) | Bytes of `TRAN-DESC` 25-100 and the FILLER on the host (G05): **confirm on the mainframe** - mainframe application owner |
| h | EBCDIC against ASCII: code page and collating order | PRESENT | Data-map rules E3, E6; gaps G01, G06; `tools/parity/record.py:51` (fixtures are ISO-8859-1); key 28-32 | TCATBALF is read in EBCDIC key order; `TRANCAT-TYPE-CD` is X(02), so letters sort before digits on the host | S3.7: code page only in the codec (`format=fixture` or `host`). Read `ORDER BY trancat_acct_id, trancat_type_cd COLLATE "C", trancat_cd` and nowhere else; with file adapters the order is fixed once in the `BalanceSource` adapter (S3.7) | `intR01_readsInKeyOrder` (i05-multi-category) | Host CCSID (G01) and the order of mixed letter/digit type codes (G06); the fixtures hold digits only: **confirm on the mainframe** - mainframe data owner |
| i | REDEFINES | PRESENT in the program, ABSENT in the copybooks | 126, 151; search `REDEFINES` in the five copybooks: nothing | A 2-byte binary is read as two characters to print a status; the 26-byte timestamp is filled through a redefinition | S2.2: no byte overlay. The timestamp is one formatter (u), the status line one function (q) | `intT05_timestampFormat`, `intT08_statusLineFormat` | - |
| j | FILLER and fixed record length across a round trip | PRESENT | CVTRA01Y:10, CVACT03Y:8, CVTRA02Y:10, CVACT01Y:17, CVTRA05Y:18; 356, 373; `app/jcl/INTCALC.jcl:39` (LRECL=350) | The 178-byte account FILLER is read and rewritten untouched; the transaction FILLER is never set | S3.6: rule E4 - FILLER is `BYTEA`, written back byte for byte; the calculation updates three columns only; every output record has its full length | `e4_accountFillerRoundTrip` (i01-baseline: ACCTDATA compared field by field) | See g (G05) |
| k | Tables: OCCURS, subscripts from 1, OCCURS DEPENDING ON | ABSENT | Search `OCCURS` in CBACT04C.cbl and the five copybooks: nothing | - | - | - | - |
| l | WORKING-STORAGE kept from record to record; fields used before set | PRESENT | 167-173 (`WS-LAST-ACCT-NUM`, `WS-TOTAL-INT`, `WS-FIRST-TIME`, `WS-TRANID-SUFFIX`); 352 uses the `ACCOUNT-RECORD` of the previous account; `TRAN-RECORD` FILLER and `TRAN-DESC` tail are never set | The account total, the account and card read at the break, and the counter survive between balances | S2.3: the item is one account with all its balances, so nothing is carried between items except the transaction counter, which lives in the step's restart state | `intR07_totalIsPerAccount` (i05-multi-category), `intQ02_counterRunsAcrossAccounts` (i01-baseline) | Unset bytes on the host: see g (G05) |
| m | Control break and end of file | PRESENT | 188-222, 339-340 | Account change triggers the update of the previous account; end of file ends the loop before the last update (INT-Q04). An empty file writes nothing (INT-T09) | S2.3, S5.1: the reader groups by account; the processor returns the result; "is this the account that ends the input" is decided once and passed to `interest.policy.updateLastAccount` | `intR07_updatesAccountAtBreak` (i01, i02, i07), `intQ04_lastAccountNotUpdated` (all seven), `intT09_emptyInputWritesNothing` (no fixture) | - |
| n | GO TO, PERFORM THRU, fall-through | ABSENT | Search `GO TO`, `THRU`, `THROUGH`: nothing. Every paragraph is PERFORMed singly; 232 `GOBACK` ends the mainline before the first paragraph | - | S2.1 still applies: no method per paragraph | - | - |
| o | Level-88 condition names | PRESENT | 134-135 (`APPL-AOK`, `APPL-EOF`) on `APPL-RESULT` | Every I/O sets 0, 8, 12 or 16 and tests the 88 | S2.4: end of input is the reader returning no item; a missing rate is an empty `Optional`; a failure is an exception. No result-code field | Covered by `intR03_*` and `intR08_*` tests | - |
| p | Indexed files: sequential and random access, alternate index, START, duplicate keys, REWRITE after READ | PRESENT, except START: ABSENT (search `START `: only the DISPLAY at 181) | 28-51 (SELECTs); 394-395 (read by alternate key); 356, 373 (REWRITE after random READ); `app/jcl/XREFFILE.jcl:72-92` (NONUNIQUEKEY) | TCATBALF sequential by key; ACCTDATA and DISCGRP random by primary key; CARDXREF random by the non-unique account key; the REWRITE at the break writes the previous account | S2.3: the ports of FI-3 - `BalanceSource` (ordered), `AccountStore`, `RateLookup`, `CardLookup` (by `card_xref_acct_ix`). Two cards for one account go to `interest.policy.multiCard`, default STOP (INT-H03) | `intR02_rateByGroupTypeCategory` (i01), `intR06_cardNumberOfAccount` (i01), `intH03_twoCardsStops` (no fixture) | Status 02, 00 or an open error for an account with several cards (INT-H03, G08, G12): **confirm on the mainframe** - mainframe application owner |
| q | File status codes and abends | PRESENT | 234-323, 327-347, 357-369, 378-390, 400-412, 422-439, 446-459, 501-514, 522-611, 628-648 | Status 00 continues; 10 on TCATBALF ends the input; 23 on DISCGRP falls back to DEFAULT; anything else prints a message and the status line and abends with user code 999 | S6.1-S6.3: the table in section 6 | `intR08_*` one per stop condition (no fixture has an abend) | U0999 on the host against RC 12 from `tools/stubs/CEE3ABD.cbl`: **confirm on the mainframe** - mainframe operations owner (REL-10) |
| r | JCL: PARM, GDG generation, DISP, step return code | PRESENT | `app/jcl/INTCALC.jcl:22` (PARM), `:37-41` (`DISP=(NEW,CATLG,DELETE)`, `SYSTRAN(+1)`, LRECL 350), `:27-36` (DISP=SHR) | PARM text becomes the first 10 bytes of `TRAN-ID`; a normal end catalogs the new generation and returns 0; an abend deletes it but keeps the accounts already rewritten | S6.4: `parmDate` is a job parameter; the export writes `SYSTRAN.G0001V00` and `rc.json` in the fixture format; a failed run leaves no export | `intQ02_idIsParmPlusCounter` (i01-baseline), `rcJson_matchesRecorded` (all seven) | `tools/runjcl/runjcl.py:598-599` reads only the first DISP subparameter, so the abnormal-end disposition is not modelled; generation numbering restarts per case: **confirm on the mainframe** - mainframe operations owner |
| s | DISPLAY output compared by parity | PRESENT | 181, 193, 230, 418-419; `tools/parity/compare.py:93-95` | Start line, each balance record as read (50 bytes, overpunch and trailing spaces included), the two fallback lines, end line | S6.5: `RunLog` writes these lines byte for byte to `sysout/INTCALC.STEP15.txt`; the record echo is the codec's image of the record, not a formatted amount | `sysout_matchesRecorded` (all seven; i04 for the fallback lines) | Where DISPLAY goes on the host (stream ask A10): **confirm on the mainframe** - mainframe operations owner |
| t | Business date from PARM against the system clock | PRESENT | 176-178, 476-480 (PARM); 496-498, 614 (clock) | The PARM text is used only in `TRAN-ID`; both timestamps come from the clock. No date arithmetic | S3.8: `parmDate` is a 10-character string, never parsed as a date; time comes from the injected `Clock` | `intQ02_idIsParmPlusCounter`, `intQ03_timestampsFromRunClock` (i01-baseline: PARM 2022071800, clock 2022-07-05) | - |
| u | CURRENT-DATE, hundredths, timestamp format | PRESENT | 140-165, 613-624 | `YYYY-MM-DD-HH.MM.SS.hh0000`: hundredths from CURRENT-DATE, then the literal `0000`; read again for every transaction | S3.8: one formatter; hundredths cut from the clock, not rounded; clock read once per written transaction | `intT05_timestampFormat` (i01-baseline: `2022-07-05-10.00.00.000000`) | The harness freezes the clock, so differing timestamps inside one run and the time zone of the host clock are not exercised: **confirm on the mainframe** - mainframe operations owner |
| v | Two-digit years and Julian dates | ABSENT | Search `ACCEPT`, `DAY-OF`, `JULIAN`: nothing; the PARM (`INTCALC.jcl:22`) and CURRENT-DATE carry four-digit years | - | - | - | - |
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

### 2.5 Quirks and open decisions

Rules of type QUIRK (INT-Q01 to INT-Q06) and HOLE (INT-H01 to INT-H06) are OPEN in
`modernization/rules/interest/decisions.md`. The legacy behaviour is what is built.

| Id | Standard as it reads for interest | Cites | Do | Do not |
|---|---|---|---|---|
| S5.1 | Each QUIRK or OPEN behaviour is reproduced in exactly one named place, behind its switch of `architecture.md` section 8 (FI-5), read only through `interest-policy`, with the legacy value as default. The rule id is in the name of that code and of its tests. | INT-Q04; `CBACT04C:188-222`; switch `interest.policy.updateLastAccount` | `policy.updatesAccountEndingInput()` (Javadoc: INT-Q04) called once in the processor; test `intQ04_lastAccountNotUpdated`. | `if (isLast) { /* legacy bug */ }` in the writer and again in the export; a second property name for the same decision. |
| S5.2 | *as shared, word for word* | INT-Q06, INT-H01, INT-H04, INT-T07; `CBACT04C:214-217, 219-220, 518-520` | Write the 0.00 transaction (INT-Q06); charge closed accounts (INT-H04); write no fee (INT-H01). | Skip zero amounts "because they are noise", add an active-status check, or add a retry: none has a rule id. |

### 2.6 Errors and operations

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
| Open fails (five files) | 234-323 | abend 999 before any work | `InterestRunStoppedException(OPEN_INPUT)`, one reason for every file, carrying the file as data: the table or location of that file cannot be reached | `FAILED`, exit `STOPPED` | the legacy text chosen from the file the exception carries, including `ERROR OPENING DALY REJECTS FILE` for the rates (INT-T10), then `ABENDING PROGRAM` |
| Close fails (five files) | 522-611 | abend 999 after all work | `InterestRunStoppedException(CLOSE_<file>)` from the export step | `FAILED`, exit `STOPPED` | `ERROR CLOSING ...` of that file, `ABENDING PROGRAM` |
| Amount or counter does not fit | 464-467, 474; INT-H05 | not determined by the source | `InterestRunStoppedException(OVERFLOW)` when `overflow=STOP` | `FAILED`, exit `STOPPED` | a line naming INT-H05 (no legacy text exists) |
| `CALL 'CEE3ABD'`, code 999 | 628-632 | U0999 on the host; off-host stub RC 12 | the exception above ends the step | exit `STOPPED` (value: REL-10) | nothing after `ABENDING PROGRAM`; the stub's line is not reproduced |

| Id | Standard as it reads for interest | Cites | Do | Do not |
|---|---|---|---|---|
| S6.1 | Every row above is implemented as written; a file status is never a field passed around. The status line keeps the legacy format `FILE STATUS IS: NNNN` + four digits and is written only where the condition determines the legacy status (a missing record is 23). A database or file-system failure writes the legacy message of the operation it replaces to the run log and its own cause to the application log. | INT-R08, INT-T08; `CBACT04C:635-648` | `runLog.line("FILE STATUS IS: NNNN0023")` for a missing account. | Invent a status such as `0099` for a `SQLException`, or copy the 9x binary-byte branch. |
| S6.2 | One exception type stops the run; it carries the reason and the rule id. It is never caught to continue: no skip, no retry (`architecture.md` section 6). The exit code is one constant. | INT-R08, INT-T03; `CBACT04C:628-632` | `throw new InterestRunStoppedException(StopReason.ACCOUNT_MISSING)`; the step ends `FAILED`. | `catch (Exception e) { log.warn(...); continue; }`, `.faultTolerant().skip(...)`, `System.exit(12)` in several places. |
| S6.3 | Restart follows `architecture.md` section 6 (REL-11, proposed): `loadExtractsStep` and `exportDatasetsStep` repeat; `calcInterestStep` continues after the last committed account with the saved counter and the run log cut back. A second run with the same PARM is not a restart and is not blocked while `rerunGuard=OFF` (INT-H02). **With file adapters:** there is no restart state and no restart; a stopped run is launched again from the start with a new `runId`. | INT-H02; `CBACT04C:352-356, 474` | Restart test: stop after a chunk, restart, compare with an uninterrupted run. | Restart from the first account; reset the counter to 000001 on restart. |
| S6.4 | A normal run writes the outputs in the recorded format (FI-2): `datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt` (350-byte records), the ACCTDATA image, `rc.json`. A failed run leaves no SYSTRAN generation in place (legacy `DISP=(NEW,CATLG,DELETE)`) but keeps committed account updates. **With file adapters:** nothing is committed, so a stopped run keeps nothing - no output directory and no account update. | INT-T09; `app/jcl/INTCALC.jcl:22, 37-41`; `CVTRA05Y/TRAN-ID` | Export to a temporary directory, move on success. | Write SYSTRAN while calculating, or delete committed rows on failure. |
| S6.5 | Two logs, kept apart. The **run log** (`RunLog` port) is the legacy SYSOUT, byte for byte, and is a parity output; it contains what the legacy printed, including the balance record echo and `ACCOUNT NOT FOUND: <id>`. The **application log** (SLF4J) carries run id, step, rule id, counts, sums, exit status and stop reason - never an account id, card number, balance, amount or record image. | INT-T02; `CBACT04C:181, 193, 230, 375, 418-419`; `CVTRA01Y/FILLER` | `log.info("run {} step {} accounts={} transactions={}", ...)`; `runLog.echo(recordImage)`. | `log.info("account {} interest {}", accountId, amount)`; formatting the echo as `12.35` instead of the record image. |

### 2.7 Tests

| Id | Standard as it reads for interest | Cites | Do | Do not |
|---|---|---|---|---|
| S7.1 | At least one test per rule id a ticket implements (INT-R01 to R08, INT-Q01 to Q06, INT-H01 to H05), named `<ruleId>_<whatItProves>` in camelCase. INT-H06 is outside the job (scheduler order) and has no test; say so in the checklist. A switch has a test for the default and for each other value. | INT-Q01; `CBACT04C:464-465` | `intQ01_truncatesHalfCent`, `intQ01_halfUpWhenSwitched`. | `testCalculate1`, or one test "covering everything". |
| S7.2 | Each rule's tests cover the fixture cases and edge cases listed for that rule in `rules.yaml` and in the ticket's Verification block; behaviours with no fixture (section 4: b, e, f, m empty input, p two cards, q, w) get synthetic data under `java/interest/src/test`. | INT-R03, INT-Q05; `fixtures/interest/i04-default-group-fallback`, `i06-negative-balance` | `intQ05_negativeBalanceGivesNegativeInterest` reads the i06 input. | Test only i01-baseline. |
| S7.3 | Integration tests (steps, JDBC adapters, restart) run against PostgreSQL 15 in a container - the `db` service of `docker-compose.yml` (`postgres:15`) or the same image - with the schema of FI-4. | `CVTRA01Y/TRAN-CAT-KEY` (`COLLATE "C"`), `CVACT01Y/FILLER` (`BYTEA`); E6 | `intR01_readsInKeyOrder` against `postgres:15`. | H2 or another in-memory database, or mocks of `JdbcTemplate` in place of an integration test. |
| S7.4 | Expected values come from the recorded legacy output (`fixtures/interest/<case>/expected`, compared with `tools/parity/compare.py`) or are written by hand in the test with the arithmetic shown. Never from the code under test. | INT-Q01; `fixtures/interest/i02-tie-half-cent`; FI-2 | `assertEquals(new BigDecimal("12.35"), ...)` with the comment `1235.50 * 12.00 / 1200 = 12.355`. | Run the job, save its output as `expected.txt`, assert against it; or compute the expectation with the same formula class. |

### 2.8 Guardrails

| Id | Standard as it reads for interest | Cites | Do | Do not |
|---|---|---|---|---|
| S8.1 | *as shared, word for word* | FI-1, FI-2; `CVTRA05Y/TRAN-AMT` | A parity difference is fixed in `java/interest`. | Change a copybook PIC or an expected file so the Java output passes. |
| S8.2 | *as shared, word for word* | INT-Q01; `fixtures/interest/i02-tie-half-cent` | Report a suspected wrong golden file in the pull request. | `make record CHAIN=interest`. |
| S8.3 | *as shared, word for word* | E2 (the codec is built here, from the copybooks) | "Adds `org.testcontainers:postgresql` (test scope) for S7.3." | Pull in a COBOL-record or EBCDIC library unannounced. |
| S8.4 | Frozen shared interfaces FI-1 to FI-6 are not changed by a ticket other than the one that owns them; a needed change is reported. | `architecture.md` section 9; INT-H03 (`CardLookup`) | Propose a second `CardLookup` method in the pull request. | Add a parameter to a port, rename a switch, or alter `db/postgres/02-interest.sql` from another ticket. |
| S8.5 | *as shared, word for word* | `CVACT03Y/XREF-CARD-NUM`, `CVACT01Y/ACCT-ID` | Account 00000002003 and the card numbers of the fixtures. | A real card number, name or customer reference. |

### 2.9 Review checklist of the interest stream

The ticket session copies this list into its pull request and marks each line `PASS`, `FAIL` (with the reason) or
`N/A` (with the reason: outside this ticket's scope). The reviewer checks it against the diff.

Standards:

- [ ] S1.1 Sources used in order; recorded output decides - PASS / FAIL / N/A
- [ ] S1.2 Rule/code disagreements reproduced as coded and reported - PASS / FAIL / N/A
- [ ] S2.1 Designed from rules; no method per paragraph - PASS / FAIL / N/A
- [ ] S2.2 Immutable business records; copybook names only at the mapping - PASS / FAIL / N/A
- [ ] S2.3 Spring Batch shape, transaction and restart boundaries of the architecture - PASS / FAIL / N/A
- [ ] S2.4 Types and return values, no flags or result codes - PASS / FAIL / N/A
- [ ] S2.5 Constructor injection; no mutable static state - PASS / FAIL / N/A
- [ ] S3.1 `BigDecimal` with data-map precision and scale; no `double`/`float` - PASS / FAIL / N/A
- [ ] S3.2 Formula: exact product, one division, mode from the policy - PASS / FAIL / N/A
- [ ] S3.3 Rounding mode named on every other arithmetic step - PASS / FAIL / N/A
- [ ] S3.4 Range check before store; overflow goes to the policy - PASS / FAIL / N/A
- [ ] S3.5 Encodings only in `record-codec` - PASS / FAIL / N/A
- [ ] S3.6 Width, padding and FILLER only in codec and schema - PASS / FAIL / N/A
- [ ] S3.7 Code page and key order each in one place - PASS / FAIL / N/A
- [ ] S3.8 Clock and run parameters injected; `parmDate` is text - PASS / FAIL / N/A
- [ ] S4.1 Every PRESENT behaviour touched has its test; new ones reported - PASS / FAIL / N/A
- [ ] S5.1 Each quirk/open decision in one named place behind its switch, rule id in names - PASS / FAIL / N/A
- [ ] S5.2 Nothing fixed, tidied or added without a rule id - PASS / FAIL / N/A
- [ ] S6.1 Status and message mapping as in section 6 - PASS / FAIL / N/A
- [ ] S6.2 One stop exception, one exit-code constant, no skip or retry - PASS / FAIL / N/A
- [ ] S6.3 Restart as in the architecture - PASS / FAIL / N/A
- [ ] S6.4 Outputs in the recorded format; failed run leaves no SYSTRAN - PASS / FAIL / N/A
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

Behaviours PRESENT in section 4 (k, n and v are ABSENT and have no line):

- [ ] a Truncation without ROUNDED - PASS / FAIL / N/A
- [ ] b High-order digit loss (range check, STOP) - PASS / FAIL / N/A
- [ ] c Intermediate precision of the COMPUTE - PASS / FAIL / N/A
- [ ] d Zoned overpunch signs - PASS / FAIL / N/A
- [ ] e Binary items stay out of data and domain - PASS / FAIL / N/A
- [ ] f Non-numeric data rejected at the boundary - PASS / FAIL / N/A
- [ ] g Fixed width, padding, STRING - PASS / FAIL / N/A
- [ ] h Code page and collating order - PASS / FAIL / N/A
- [ ] i REDEFINES replaced by formatter and function - PASS / FAIL / N/A
- [ ] j FILLER and record length round trip - PASS / FAIL / N/A
- [ ] l No state carried between accounts except the counter - PASS / FAIL / N/A
- [ ] m Control break and end of file (INT-Q04, INT-T09) - PASS / FAIL / N/A
- [ ] o Level-88 result codes replaced by types - PASS / FAIL / N/A
- [ ] p Indexed access through the ports; several cards to the policy - PASS / FAIL / N/A
- [ ] q File statuses and abend - PASS / FAIL / N/A
- [ ] r PARM, generation, disposition, return code - PASS / FAIL / N/A
- [ ] s DISPLAY output in the run log - PASS / FAIL / N/A
- [ ] t PARM text against the clock - PASS / FAIL / N/A
- [ ] u Timestamp format and hundredths - PASS / FAIL / N/A
- [ ] w PARM length checked - PASS / FAIL / N/A
- [ ] x No stale rate after a failed lookup - PASS / FAIL / N/A
- [ ] y Typed fields where COBOL mixed text and numbers - PASS / FAIL / N/A
- [ ] z Abend ends the step; stub output not reproduced - PASS / FAIL / N/A
- [ ] aa Dead and empty code not ported - PASS / FAIL / N/A

## 3. Build: what `carddemo-ticket-build` left out

Follow `carddemo-ticket-build` with `<stream>` = `interest`. Steps 3 and 10 are SHARED and have nothing here;
step 1 is SHARED too and is here only for the versions the pilot was proven with.
The steps below are the interest text of the MIXED steps, with the same numbers: the paths, the layout the pilot
created and the figures of the pilot. Where a step quotes output, it is the pilot's (step 5a) unless it says
otherwise; the output of step 5c is quoted in the shared skill.

Commands and layout only, each run on the pilot ticket (01) in step 5a and quoted with the output that
shows success; text marked with a ticket number was reported by that ticket's session in step 5b. What the code must be - naming, types, numbers, encodings, quirks, rule ids, tests - is
`carddemo-conversion-standards` with section 2 of this annex; read it before writing code and fill in its
section 9. Failures and their fixes: `carddemo-pitfalls`. Proving the ticket: `carddemo-ticket-verify`.

1. **Check the tools.** Proven with: JDK 21.0.13, Maven 3.6.3, Docker Engine 29.9.2, Docker Compose
   v5.6.1, Python 3.10.12, GNU Make 4.3.

   ```sh
   docker --version && docker compose version && python3 --version && make --version | head -1
   ```
   ```
   Docker version 29.9.2, build a82e528
   Docker Compose version v5.6.1
   Python 3.10.12
   GNU Make 4.3
   ```

2. **Put Maven on a JDK 21.** `mvn -v` names the JDK Maven uses; it must say 21. On Debian/Ubuntu with
   several JDKs installed, this selects the 21 one for the current shell:

   ```sh
   export JAVA_HOME="$(dirname "$(dirname "$(update-alternatives --list java | grep -m1 -- '-21-')")")"
   mvn -v
   ```
   ```
   Apache Maven 3.6.3
   Java version: 21.0.13.0.3, vendor: Ubuntu, runtime: .../java-21-openjdk-amd64
   ```
   Every `mvn` and every `make parity-java` below needs this shell setting (pitfall P1).

4. **Read, in this order** (standard S1.1): the ticket file, the rules it names in
   `modernization/rules/interest/rules.yaml`, `modernization/design/architecture.md`,
   `modernization/design/data-map-interest.md`, the `app/cbl/CBACT04C.cbl` lines each rule cites, and
   `fixtures/interest/<case>/` of the cases the ticket names. Use `cat -A` on a fixture file to see
   trailing spaces and the missing final newline of `rc.json`.

5. **Know the layout the pilot created.** Everything of the stream is in one Maven module.

   | Path | What is there |
   |---|---|
   | `java/interest/pom.xml` | one module `com.carddemo:interest:0.1.0`; parent Spring Boot 3.5.6 (Spring Batch 5.2.3, PostgreSQL driver 42.7.7); `java.version` 21; Testcontainers pinned to 1.21.4; surefire runs `*Test`, failsafe runs `*IT`; jar `target/interest.jar` |
   | `java/interest/src/main/java/com/carddemo/interest/domain` | the calculation, no framework |
   | `.../policy` | `InterestPolicy`, the only reader of `interest.policy.*` |
   | `.../port` | FI-3: `BalanceSource`, `AccountStore`, `CardLookup`, `RateLookup`, `TransactionSink`, `RunLog` |
   | `.../file` | file adapters of the ports (`FileRunWorkspace`), recorded output format |
   | `.../codec` | `Field` (encoding rules) and one `*Record` class per copybook layout |
   | `.../batch` | `InterestJobConfig` (job `interestCalcJob`, step `calcInterestStep`), `CalcInterestRun`, `InterestJobParameters`, `InterestExitCode(s)` |
   | `.../domain/RunStop`, `StopReason`, `LegacyFile` | the stop classes (ticket 08): every stop goes through `RunStop`, which writes the legacy lines; never throw `InterestRunStoppedException` directly from the calculation |
   | `.../jdbc` | JDBC adapters of the ports (ticket 06); `batch/JdbcCalcStepIT` runs the step on the tables with `interest.calc.store=jdbc`; `src/test/.../jdbc/InterestTables` loads a case's input into the replica tables |
   | `db/postgres/02-interest.sql` | the six tables of the stream (ticket 02, FI-4); `db/postgres/tests/run.sh` checks them with psql. The Java tests do not load the script until tickets 05 and 06 |
   | `java/interest/src/main/resources/application.yml` | datasource (`INTEREST_DB_URL`, `INTEREST_DB_USER`, `INTEREST_DB_PASSWORD`, default `localhost:5432/carddemo`), job repository in schema `batch`, the `interest.policy.*` defaults, `interest.calc.commitInterval`, `interest.clock.frozen` |
   | `java/interest/src/test/java/com/carddemo/interest` | same packages; `Fixtures.java` reads `fixtures/interest` (Maven runs in `java/interest`, so the path is `../../fixtures/interest`); `batch/InterestCalcJobIT.java` is the integration test on `postgres:15` |
   | `java/interest/evidence/` | the pilot's three evidence files; each later ticket writes its own under `java/interest/evidence/<ticket file name>/` (see `carddemo-ticket-verify` step 8) |
   | `Makefile`, target `parity-java` | FI-6, the parity entry point |
   | `docker-compose.yml`, service `db` | `postgres:15`, database, user and password `carddemo`, init scripts from `db/postgres/` |

   This table is the pilot's layout plus the rows later tickets reported. Packages `load`, `export` and
   `recon` of `architecture.md` section 2 exist once tickets 05, 07 and 10 are in your base branch;
   `ls java/interest/src/main/java/com/carddemo/interest` shows what your base has.

6. **Start PostgreSQL 15.** Only the `db` service is needed; the `estate` image is not.

   ```sh
   docker compose up -d --wait db
   docker compose exec -T db psql -U carddemo -d carddemo -c "show server_version"
   ```
   ```
    Container ts-cobol-carddemo-db-1 Healthy
    15.20 (Debian 15.20-1.pgdg13+1)
   ```
   The service publishes no port to the host (pitfall P3). The integration tests do not use it: they
   start their own `postgres:15` through Testcontainers, so `mvn verify` needs Docker, not this service.

7. **Build.**

   ```sh
   mvn -q -f java/interest/pom.xml -DskipTests package && ls java/interest/target/interest.jar
   ```
   ```
   java/interest/target/interest.jar
   ```
   `-q` prints nothing on success. A command other than the job (a second main class, as the data-move
   kit of ticket 10) is started from the same jar with
   `java -cp interest.jar -Dloader.main=<class> org.springframework.boot.loader.launch.PropertiesLauncher`;
   start its context with `spring.batch.job.enabled=false`.

8. **Run the tests.** Unit and integration tests together; this is the Build line of every ticket.

   ```sh
   mvn -B -f java/interest/pom.xml verify 2>&1 | grep -E '^\[(INFO|ERROR|WARNING)\] (Tests run:|BUILD)'
   ```
   On the pilot's code this ends with:
   ```
   [INFO] Tests run: 60, Failures: 0, Errors: 0, Skipped: 0
   [INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 3.558 s -- in com.carddemo.interest.batch.InterestCalcJobIT
   [INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
   [INFO] BUILD SUCCESS
   ```
   The first total is surefire (unit), the second failsafe (integration). These figures are the
   pilot's: compare with the totals in the newest `java/interest/evidence/*/test-output.txt` of your
   base branch, not with the figures quoted here. Your ticket adds to both numbers; neither may go down. One unit test class while you work:
   ```sh
   mvn -B -f java/interest/pom.xml test -Dtest=InterestFormulaTest 2>&1 | grep -E '^\[(INFO|ERROR)\] (Tests run:|BUILD)'
   ```
   ```
   [INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
   [INFO] BUILD SUCCESS
   ```

9. **Before every commit,** undo what the comparator changed and check you stayed in scope.

   ```sh
   git checkout -- tools/parity/__pycache__
   git diff --stat origin/<base-branch> -- app fixtures tools modernization .agents db docker-compose.yml
   git status --short
   ```
   The `git diff --stat` line must print nothing for a ticket whose Allowed files are `java/interest/**`
   and the `Makefile`; take out of that list only a path your ticket's Scope allows (pitfall P6).

   Two cases the Allowed files do not settle:
   * A new step is a `@Bean Step` in the ticket's own package; test it by launching a job built in the
     test. Adding it to `InterestJobConfig` belongs to the ticket that owns `batch/**`; report it in
     section 2 of your report.
   * When a Deliverable names a file outside the Allowed files, put the text in the ticket's
     package-info and in `report.md` section 2, and name the ticket that may edit the file.

## 4. Verify: what `carddemo-ticket-verify` left out

Follow `carddemo-ticket-verify` with `<stream>` = `interest`, `<parity-target>` = `parity-java` and `<out>` =
`work/parity-java`. Step 10 is SHARED and has nothing here. Steps 4 ("Run the job by hand") and 6 ("Error path")
are STREAM: they exist only here. The helper `.agents/skills/interest-ticket-verify/parity-report.sh` is
interest's and stays where it is.

Commands only, each run on the pilot ticket (01) in step 5a and quoted with the output that shows
success. What the code must be is `carddemo-conversion-standards` with section 2 of this annex; this skill

1. **Run the job on a case the ticket names and compare (FI-6).**

   ```sh
   make parity-java CASE=i01-baseline
   ```
   ```
   docker compose up -d --wait db
   mvn -q -f java/interest/pom.xml -DskipTests package
   interest/i01-baseline [java]: PASS (4 records, 50 fields, 0 differences)
   report: work/parity-java/report.md
   ```
   The target starts the `db` service, builds `java/interest/target/interest.jar`, removes
   `work/parity-java/<case>` and `work/parity-java/report.md`, runs the jar once per case in an
   `eclipse-temurin:21-jre` container on the network of the `db` container with the PARM `2022071800`
   (`app/jcl/INTCALC.jcl:22`) and the clock frozen at `2022-07-05 10:00:00.00`, and compares. Exit code 0
   only if every case it ran passed. Without `CASE` it runs every case under `fixtures/interest`.

2. **Know the files the comparator reads.** The job writes them itself; nothing has to be converted.

   ```sh
   find work/parity-java/i01-baseline -type f | sort && wc -l work/parity-java/i01-baseline/actual/datasets/*
   ```
   ```
   work/parity-java/i01-baseline/actual/datasets/AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS.txt
   work/parity-java/i01-baseline/actual/datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt
   work/parity-java/i01-baseline/actual/rc.json
   work/parity-java/i01-baseline/actual/sysout/INTCALC.STEP15.txt
     2 work/parity-java/i01-baseline/actual/datasets/AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS.txt
     2 work/parity-java/i01-baseline/actual/datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt
   ```
   A run that stops leaves no `actual` directory at all. `work/` is ignored by git.

3. **Compare each case with the comparator.** One case:

   ```sh
   python3 tools/parity/compare.py interest i01-baseline --actual work/parity-java/i01-baseline/actual --label java
   ```
   ```
   interest/i01-baseline [java]: PASS (4 records, 50 fields, 0 differences)
   ```
   Exit code 0 is PASS, 1 is FAIL. All cases, into one fresh Markdown report (pitfall P7: the report
   file is appended to, so remove it first):

   ```sh
   make parity-java            # runs all seven; non-zero while any case fails
   rm -f work/parity-java/compare.md
   for c in $(ls fixtures/interest); do
     python3 tools/parity/compare.py interest $c --actual work/parity-java/$c/actual --label java --report work/parity-java/compare.md
     echo "  exit $?"
   done
   ```
   Result on the pilot's code (ticket 01 claims i01-baseline only):
   ```
   interest/i01-baseline [java]: PASS (4 records, 50 fields, 0 differences)
     exit 0
   interest/i02-tie-half-cent [java]: PASS (4 records, 50 fields, 0 differences)
     exit 0
   interest/i03-zero-rate-skipped [java]: FAIL (2 records, 25 fields, 4 differences)
     exit 1
   interest/i04-default-group-fallback [java]: FAIL (2 records, 25 fields, 4 differences)
     exit 1
   interest/i05-multi-category [java]: PASS (3 records, 38 fields, 0 differences)
     exit 0
   interest/i06-negative-balance [java]: PASS (2 records, 25 fields, 0 differences)
     exit 0
   interest/i07-nontie-rounding [java]: PASS (4 records, 50 fields, 0 differences)
     exit 0
   ```
   `make parity-java` printed for i03 and i04 `java job ended with exit code 12` and ended with
   `make: *** [Makefile:96: parity-java] Error 1`. A case your ticket does not name may pass or fail;
   report it, do not claim it. That result is the pilot's: from ticket 04 on, all seven cases PASS and
   `make parity-java` exits 0; any FAIL is a regression.

   Run Python of your own that imports the comparator with `PYTHONDONTWRITEBYTECODE=1`, and delete an
   untracked `tools/parity/__pycache__/compare.*.pyc`: importing `tools.parity.compare` leaves one
   there, which `git checkout -- tools/parity/__pycache__` does not remove.

4. **Run the job by hand when you need another setting** (a policy switch, another input directory).
   This is the command `make parity-java` runs, with one option added:

   ```sh
   db=$(docker compose ps -q db)
   docker run --rm --network container:$db --user $(id -u):$(id -g) -v "$PWD":/estate -w /estate eclipse-temurin:21-jre \
     java -jar java/interest/target/interest.jar --interest.clock.frozen="2022-07-05 10:00:00.00" --logging.level.root=WARN \
       --interest.policy.rounding=HALF_UP \
       parmDate=2022071800 runId=manual-$(date +%s) format=fixture \
       inputDir=fixtures/interest/i02-tie-half-cent/input outputDir=work/manual/i02-halfup/actual
   echo "job exit=$?"
   python3 tools/parity/compare.py interest i02-tie-half-cent --actual work/manual/i02-halfup/actual --label java-halfup --report work/manual/diff.md
   ```
   ```
   job exit=0
   interest/i02-tie-half-cent [java-halfup]: FAIL (4 records, 50 fields, 3 differences)
   ```
   `runId` must be new for every launch. Exit code 0 is a COMPLETED job, 12 a stopped one.

5. **Read a difference.** The Markdown report names the dataset, the record with its key, the field,
   the legacy value and the candidate value, already decoded. Each row below was produced in step 5a;
   the fix is always in `java/interest`, never in `fixtures/` or the comparator (standards S8.1, S8.2).

   | What the report shows | How it was produced | Where the rule is |
   |---|---|---|
   | `TRAN-AMT` 12.35 against 12.36 and `ACCT-CURR-BAL` 1247.85 against 1247.86 on i02-tie-half-cent | step 4 with `--interest.policy.rounding=HALF_UP` | standards S3.2, section 4 row a |
   | `ACCT-CURR-BAL` 2400.00 against 2437.00 and `ACCT-CURR-CYC-CREDIT` 2400.00 against 0.00 on the last account (i01-baseline, ACCT-ID=2002) | step 4 with `--interest.policy.updateLastAccount=true` | standards S5.1, section 4 row m |
   | `TRAN-ORIG-TS` and `TRAN-PROC-TS` 2022-07-05-10.00.00.000000 against today's time | step 4 without `--interest.clock.frozen` | standards S3.8, section 4 rows t and u |
   | `(file)` `1 records` against `missing` for both datasets, and the whole SYSOUT and rc.json as removed lines | the job stopped (exit 12) and left no output: i03, i04 on the pilot's code. Read the job's own `InterestRunStoppedException: <REASON> (<rule id>)` line in the `make parity-java` output | standards section 6 |
   | `rc.json` `(text)` with `STEP05` ... `STEP30` removed | an `rc.json` holding STEP15 only | pitfall P4 |

6. **Check the error path of the ticket** from the command line. Missing input file (ticket 01):

   ```sh
   mkdir -p work/manual/miss/input && cp fixtures/interest/i01-baseline/input/* work/manual/miss/input/ \
     && rm work/manual/miss/input/AWS.M2.CARDDEMO.CARDXREF.PS.txt
   db=$(docker compose ps -q db)
   docker run --rm --network container:$db --user $(id -u):$(id -g) -v "$PWD":/estate -w /estate eclipse-temurin:21-jre \
     java -jar java/interest/target/interest.jar --logging.level.root=WARN parmDate=2022071800 runId=miss-$(date +%s) format=fixture \
       inputDir=work/manual/miss/input outputDir=work/manual/miss/actual 2>&1 | grep -E 'InterestRunStoppedException|CalcInterestRun  '
   echo "job exit=${PIPESTATUS[0]}"; ls work/manual/miss
   ```
   ```
   com.carddemo.interest.domain.InterestRunStoppedException: OPEN_INPUT (INT-R08): cannot read input file AWS.M2.CARDDEMO.CARDXREF.PS.txt
   ... c.c.interest.batch.CalcInterestRun       : run miss-1794733240 step calcInterestStep stopped before any work
   job exit=12
   input
   ```
   Only `input` is listed: no output directory and no staging directory were left.

   From ticket 08 the stop conditions are in `StopConditionsTest` and `ErrorPathsJdbcIT`; a new stop
   needs a row in both and the run-log lines cited from CBACT04C.

7. **Check the database assertions of the ticket** in the `db` service.

   ```sh
   docker compose exec -T db psql -U carddemo -d carddemo \
     -c "select i.job_name, e.status, (select string_agg(s.step_name || ':' || s.status || ' read=' || s.read_count || ' write=' || s.write_count, ', ' order by s.step_execution_id) from batch.batch_step_execution s where s.job_execution_id = e.job_execution_id) as steps, p.parameter_value as run_id from batch.batch_job_execution e join batch.batch_job_instance i using (job_instance_id) join batch.batch_job_execution_params p on p.job_execution_id = e.job_execution_id and p.parameter_name = 'runId' where p.parameter_value like '%i01-baseline' order by e.job_execution_id desc limit 1" \
     -c "select table_schema, table_name from information_schema.tables where table_schema in ('carddemo', 'public') order by 1, 2"
   ```
   Output, without the rule and count lines:
   ```
       job_name     |  status   |                   steps                   |            run_id
    interestCalcJob | COMPLETED | calcInterestStep:COMPLETED read=2 write=2 | 1794733207-33341-i01-baseline
    carddemo     | transaction_type
    carddemo     | transaction_type_categor
   ```
   The job repository is in schema `batch`. The same assertions run in `InterestCalcJobIT` on a
   Testcontainers `postgres:15`; a ticket that adds tables changes the expected list there.

   That output is the pilot's. Since ticket 02 the table list holds the eight tables of
   `db/postgres/01-transaction-type.sql` and `02-interest.sql`: `account`, `card_xref`,
   `disclosure_group`, `interest_run`, `interest_transaction`, `tran_cat_balance`, `transaction_type`,
   `transaction_type_category`. The steps of a job are listed in the order they ran
   (`order by s.step_execution_id`). From ticket 06 the run's row and its rows are in schema `carddemo`:

   ```sh
   docker compose exec -T db psql -U carddemo -d carddemo \
     -c "select run_id, status, written_transaction_count, written_tran_amt_sum, written_account_count from carddemo.interest_run order by run_id desc limit 1"
   ```

8. **Write the evidence, from the committed revision, in the ticket's own directory.** Commit the code
   first, then produce the files, then commit them alone, so the evidence names the revision it came
   from. Every ticket writes under `java/interest/evidence/<ticket file name>/`, where the name is the
   ticket's file under `modernization/design/tickets` without `.md` (for example
   `java/interest/evidence/04-calculation-rules/`), so tickets built in parallel never write the same
   file. Write nothing directly in `java/interest/evidence/`: the three files there
   (`test-output.txt`, `parity-report.md`, `PILOT-REPORT.md`) are the pilot's, written before this
   rule, and stay as they are.

   | File in `java/interest/evidence/<ticket file name>/` | How it is produced |
   |---|---|
   | `test-output.txt` | the command below |
   | `parity-report.md` | `.agents/skills/interest-ticket-verify/parity-report.sh <ticket file name> <case the ticket names> [...]` |
   | `report.md` (the ticket's report) | by hand, step 9 |
   | other evidence the ticket's Verification names | files or a sub-folder in the ticket's evidence folder, listed at the top of `report.md` |

   The commands were run in step 5a with ticket 01, the only ticket built, to prove them; that
   directory was removed again and is not in the repository.

   ```sh
   ticket=01-pilot-thin-slice          # your ticket's file name without .md
   mkdir -p java/interest/evidence/$ticket
   { echo "# mvn -B -f java/interest/pom.xml clean verify"
     echo "# revision $(git rev-parse HEAD), $(date -u +%Y-%m-%dT%H:%M:%SZ), $("$JAVA_HOME"/bin/java -version 2>&1 | head -1), Docker $(docker version --format '{{.Server.Version}}')"
     mvn -B -f java/interest/pom.xml clean verify 2>&1 | sed 's/\x1b\[[0-9;]*m//g'
     echo "# exit code ${PIPESTATUS[0]}"; } > java/interest/evidence/$ticket/test-output.txt
   tail -1 java/interest/evidence/$ticket/test-output.txt; grep -E 'Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+$|BUILD' java/interest/evidence/$ticket/test-output.txt
   ```
   ```
   # exit code 0
   [INFO] Tests run: 60, Failures: 0, Errors: 0, Skipped: 0
   [INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
   [INFO] BUILD SUCCESS
   ```
   ```sh
   .agents/skills/interest-ticket-verify/parity-report.sh $ticket i01-baseline
   ```
   ```
   i01-baseline	0	0
   i02-tie-half-cent	0	0
   i03-zero-rate-skipped	12	1
   i04-default-group-fallback	12	1
   i05-multi-category	0	0
   i06-negative-balance	0	0
   i07-nontie-rounding	0	0
   report: java/interest/evidence/01-pilot-thin-slice/parity-report.md
   ```
   The three columns are case, exit code of the Java job, exit code of the comparator. The script runs
   steps 1 and 3 again and writes the sections of the pilot's parity report: result per case, acceptance
   run with the job's output files, all cases, the comparator's report, job repository and schema. It
   exits 0 when every case named on the command line passes. In the result table it leaves
   `_fill in_` in the columns "In the ticket's scope" and "Why" of the cases the ticket does not name:
   complete them by hand. A ticket name with no file under `modernization/design/tickets`, or a case
   with no directory under `fixtures/interest`, ends the script with exit code 2
   (`no ticket file modernization/design/tickets/99-nothing.md`). `REPORT=<file>` writes somewhere else.

   The script needs at least one case and marks each case on its command line "yes - acceptance case
   of the ticket". Where that does not fit the ticket:

   | The ticket | What to do |
   |---|---|
   | names no case of its own, but asks that the seven cases still pass | pass every case: `parity-report.sh <ticket> $(ls fixtures/interest)` |
   | names no parity case because its code is not part of `interestCalcJob`, or does not change the job (02, 05, 10) | pass `i01-baseline` and change its scope cell by hand to `regression case of the job`; the ticket's own figures come from its tests in `test-output.txt` |
   | is proven through the tables (06) | say in `report.md` section 1 which integration test compares the tables with `fixtures/interest/<case>/expected`; the parity report then shows no regression |
   | has as evidence a directory the job does not write (07: the exported directories) | run `compare.py interest <case> --actual <dir> --label <name>` on it yourself and append the command and its output as a last section of `parity-report.md` |

   Files the tests leave under `java/interest/target/` are those of the last `clean verify`; use them
   before running Maven again. Paste the command and output of a step-4 run the ticket asks for (a
   policy switch) into section 6 of `report.md`.

9. **Write the ticket's report** as `java/interest/evidence/<ticket file name>/report.md`, with the
   seven sections of the pilot's `java/interest/evidence/PILOT-REPORT.md`, in this order:

   1. Result - one row per Acceptance line of the ticket: met or not, and the test or report section that shows it.
   2. What the ticket does not cover - rules and work left, with the owning ticket.
   3. For the decision register - findings with evidence and a suggested owner.
   4. Standards review checklist - section 9 of `carddemo-conversion-standards` (for interest: section 2.9 of this annex), every line PASS, FAIL or N/A with the file or test that shows it.
   5. What is wrong or missing in the standards skill - proposals only; do not edit that skill.
   6. Method - the commands run and the failures hit (symptom, cause, fix).
   7. Dependencies - every new one, why, and what was considered instead (standard S8.3).

### The combined check, as recorded for interest

`modernization/design/build-run.md`, section 3, on `demo/05b-integration` at `079b641`:

| Command | Result |
|---|---|
| `mvn -B -f java/interest/pom.xml clean verify` | 396 unit, 105 integration, 0 failures, 0 errors, BUILD SUCCESS |
| `make up && make build` | exit 0 |
| `make parity-java` | 7 of 7 cases PASS, exit 0 |
| `make parity` (legacy chains) | all 24 cases PASS, exit 0 |

Step 5c repeated it from a clean checkout following the shared skills and this annex; the result is in
`modernization/design/evidence/05c-shared-skills-check.txt` and in `modernization/design/skills-shared.md`,
section 4.

## 5. Pitfalls: what `carddemo-pitfalls` left out

P1 to P7 are the failures the pilot (ticket 01) hit; each was reproduced in step 5a with the command
shown, and the symptom is the output of that command. P8 to P12 were reported by the sessions that
built tickets 04, 05, 07 and 10 in step 5b; each names the ticket and the evidence that shows it. Where the fix is a conversion standard, the entry names the
section of `carddemo-conversion-standards` with section 2 of this annex and stops there.

P5, P8, P10 and the `search_path` entry under "Reported, not reproduced" are STREAM: they exist only here. For
the others the shared skill has symptom, cause and fix without interest's names; the entry below is the interest
text, with the command that reproduced it or the ticket that reported it.

## P1. Maven runs on the wrong JDK

* **Symptom.** `mvn -q -f java/interest/pom.xml -DskipTests package` prints
  `Fatal error compiling: error: release version 21 not supported`.
* **Cause.** Without `JAVA_HOME`, Maven took another installed JDK (`mvn -v` said
  `Java version: 17.0.21.0.3`); the module needs 21.
* **Fix.** `carddemo-ticket-build` step 2, in every shell, also before `make parity-java`.
* **Reproduced by:** `env -u JAVA_HOME mvn -q -f java/interest/pom.xml -DskipTests package`.

## P2. The integration test cannot reach Docker

* **Symptom.** `mvn verify` fails in `InterestCalcJobIT` with
  `ContainerFetch Can't get Docker image: RemoteDockerImage(imageName=postgres:15, ...)`; further up the log:
  `client version 1.32 is too old. Minimum supported API version is 1.40`.
* **Cause.** Testcontainers 1.21.3, the version Spring Boot 3.5.6 manages, cannot talk to Docker Engine 29.
* **Fix.** Already in `java/interest/pom.xml`: `<testcontainers.version>1.21.4</testcontainers.version>`.
  Keep that property when you touch the pom or the Spring Boot version.
* **Reproduced by:** `mvn -B -f java/interest/pom.xml verify -Dtestcontainers.version=1.21.3` (BUILD FAILURE);
  without the override: BUILD SUCCESS.

## P3. The job cannot reach the `db` service from the host or from the `estate` container

* **Symptom.** `java -jar java/interest/target/interest.jar ...` on the host does not connect to the
  compose database. On the machine of step 5a another PostgreSQL answered on `localhost:5432` and the job
  ended with `FATAL: password authentication failed for user "carddemo"`, exit code 1.
* **Cause.** The `db` service of `docker-compose.yml` publishes no port (`docker compose port db 5432`
  prints `:0`), and the `estate` image has no JDK. The ticket scope does not allow a compose change for this.
* **Fix.** Run the jar in a JRE container that shares the network of the `db` container, where
  `localhost:5432` is the `db` service: `make parity-java`, or by hand as in `carddemo-ticket-verify` step 4.
* **Reproduced by:** the host `java -jar` call above, and `docker compose port db 5432`.

## P4. rc.json differs although the job ended normally

* **Symptom.** The comparator reports `rc.json | (text)` with
  `-   "STEP05": 0,` ... `-   "STEP30": 0` and `+   "STEP15": 0`.
* **Cause.** The recorded `rc.json` of every interest case lists six steps under `INTCALC`; only STEP15
  is INTCALC's, the others come from the harness's setup jobs (`tools/parity/record.py:131`). The file has
  no final newline.
* **Fix.** Standard S6.4 (recorded format, FI-2). The pilot's writer is `file/RecordedReturnCodes.java`;
  what the target should write outside the harness is an open finding in `modernization/design/skills.md`.
* **Reproduced by:** comparing a copy of the i01-baseline output whose `rc.json` held STEP15 only.

## P5. `StringIndexOutOfBoundsException` from `codec/Field`

* **Symptom.** `java.lang.StringIndexOutOfBoundsException: Range [0, 10) out of bounds for length 9`
  where a record rejection was expected.
* **Cause.** `Field` was given a record image shorter than the record length. In the pilot it was test
  data one character short.
* **Fix.** Give `Field` a full-length image: every line read from a file goes through
  `RecordImage.fromLine(line, <LENGTH of the layout>)` first. Standard S3.6.
* **Reproduced by:** `new Field("TRAN-SOURCE", 0, 10).text("123456789")` in `jshell --class-path java/interest/target/classes`;
  with `RecordImage.fromLine("123456789", 10)` it returns `123456789`.

## P6. `git status` shows a changed file under `tools/parity` you never touched

* **Symptom.** ` M tools/parity/__pycache__/copybook.cpython-310.pyc` after any `compare.py` or `make parity-java`.
* **Cause.** The `.pyc` files are tracked and Python rewrites them.
* **Fix.** `git checkout -- tools/parity/__pycache__` before every commit (standard S8.1).
* **Reproduced by:** `python3 tools/parity/compare.py interest i01-baseline --actual work/parity-java/i01-baseline/actual --label java; git status --short`.

## P7. The comparator report holds old results

* **Symptom.** The Markdown report shows a case twice, or sections of an earlier run.
* **Cause.** `compare.py` appends to its report file and never replaces it: the default
  `work/parity/report.md`, and any `--report <file>` too.
* **Fix.** Remove the report file before a series of comparisons (`rm -f work/parity-java/compare.md`).
  `make parity-java` does this for `work/parity-java/report.md`.
* **Reproduced by:** running the same comparison twice; `grep -c '^## ' work/parity/report.md` printed `2`.

## P8. A test that reads JSON cannot use Jackson

* **Symptom.** `package com.fasterxml.jackson.databind does not exist` when a test is compiled.
* **Cause.** The module has no Jackson.
* **Fix.** `org.json.JSONObject` from `spring-boot-starter-test`; do not add a dependency (standard S8.3).
* **Reported by:** ticket 04, `java/interest/evidence/04-calculation-rules/report.md` section 6.

## P9. An integration test finds no schema `carddemo` for the interest tables

* **Symptom.** `db/postgres/02-interest.sql` alone cannot create its tables in a Testcontainers `postgres:15`.
* **Cause.** `CREATE SCHEMA carddemo` is only in `db/postgres/01-transaction-type.sql`.
* **Fix.** Copy `db/postgres/01-transaction-type.sql` and `02-interest.sql` into
  `/docker-entrypoint-initdb.d/` of the container, as `ExtractLoaderIT` does.
* **Reported by:** ticket 05, `java/interest/evidence/05-extract-loader/report.md` section 5.

## P10. rc.json has to be written outside package `file`

* **Symptom.** Another package cannot reuse the rc.json writer of the pilot.
* **Cause.** `file/RecordedReturnCodes` is package-private, and `file/**` belongs to another ticket.
* **Fix.** A writer of your own in your package (`export/ExportedReturnCodes`); test it against the
  recorded `rc.json` of every case, so the two writers cannot drift apart (see P4).
* **Reported by:** ticket 07, `java/interest/evidence/07-dataset-export/report.md` section 5 (seen while
  reading the code, not reproduced as a failure).

## P11. `compare.py` cannot compare two directories

* **Symptom.** The comparator has no option for the legacy side: it always reads
  `fixtures/<chain>/<case>/expected`.
* **Cause.** `tools/parity/compare.py` takes `--actual` only.
* **Fix.** Lay the legacy directory out as `<root>/interest/<name>/expected` with a `manifest.json` and
  point `compare.FIXTURES` at `<root>` from your own Python call, as the `sidebyside-java` target of the
  `Makefile` does; never edit the comparator. Run that Python with `PYTHONDONTWRITEBYTECODE=1` (P6).
* **Reported by:** ticket 10, `java/interest/evidence/10-data-move-kit/report.md` finding F1.

## P12. A class a later ticket needs is package-private in a package it may not edit

* **Symptom.** A later ticket writes a second copy of something that exists
  (`recon/TrialLoadProfile` beside `load/ExtractProfile`).
* **Cause.** The factory it needs (`ExtractProfile.of`, `RoundTrip.compare`, `ExtractDecoder`) is
  package-private, and the later ticket's Allowed files do not include that package.
* **Fix.** Make what a later ticket of the design reads public when you write it; a later ticket cannot
  widen it.
* **Reported by:** ticket 10, `java/interest/evidence/10-data-move-kit/report.md` finding F7.

## Reported, not reproduced

The pilot: none. All seven failures in section 6 of `java/interest/evidence/PILOT-REPORT.md` are above.

Ticket 05: the datasource sets `search_path` to `batch` (`connection-init-sql` in `application.yml`), so
an unqualified table name in application SQL is looked up in the job repository schema. Qualify every
application table as `carddemo.<table>`. Not reproduced: the loader was written with qualified names
from the start.
