# Conversion standards - interest stream (INTCALC -> CBACT04C)

> **PROPOSED - not confirmed. Nothing here has been run.** No Java exists yet and no standard has been applied to
> code. The pilot ticket (`modernization/design/tickets/01-pilot-thin-slice.md`) is the first test of these
> standards. **Owner who must confirm: the target architect.**

This is the readable summary. The binding text, with a do / do-not example for every standard and the review
checklist ticket sessions fill in, is the skill `.agents/skills/interest-conversion-standards/SKILL.md`. If the two
differ, the skill is right. Standards are written only there; later steps refer to it and propose changes, they do
not edit it.

Written from: `modernization/rules/interest/rules.yaml` and `decisions.md`, `modernization/design/architecture.md`
(0.1.0), `modernization/design/data-map-interest.md`, the tickets, `app/cbl/CBACT04C.cbl`, the five copybooks,
`app/jcl/INTCALC.jcl` and `tools/build.sh`.

## 1. The standards, one line each

**What to convert from**

- S1.1 Work from the ticket and rules, then the architecture and data map, then the cited COBOL lines; the recorded legacy output decides what is correct.
- S1.2 Where rule text and code disagree, reproduce the code and report the disagreement for the decision register.

**Idiomatic Java, not translated COBOL**

- S2.1 Design from the rules: no method per paragraph, no emulated GO TO or PERFORM THRU.
- S2.2 Copybook layouts become immutable records named for their business meaning; copybook field names appear once, at the mapping; no WORKING-STORAGE class.
- S2.3 Read, process and write as the Spring Batch steps of the architecture: one item is one account with its balances, chunks are whole accounts, restart state is saved with the commit.
- S2.4 A type or a return value replaces every flag, switch and result code.
- S2.5 Constructor injection; no mutable static state.

**Numbers and data**

- S3.1 Money and rates are `BigDecimal` with the precision and scale of the data map; no `double` or `float`.
- S3.2 Interest is one exact multiplication and one division to two decimals, with the rounding mode taken from the policy (default: truncate towards zero).
- S3.3 Every other arithmetic step names its rounding mode (`UNNECESSARY` for sums of cents).
- S3.4 A value is checked against its field size before it is stored; one that does not fit stops the run (INT-H05) - digits are never cut off.
- S3.5 EBCDIC, overpunch signs, packed and binary are handled only in the record codec at the boundary.
- S3.6 Field width, padding and FILLER belong to the codec and the schema; FILLER bytes go back unchanged.
- S3.7 Code page and key order are each fixed in one place.
- S3.8 The clock and the run's date parameters are injected; the PARM date is text, never parsed, never the system clock.

**COBOL behaviours with no Java equivalent**

- S4.1 Every behaviour marked PRESENT in section 2 is reproduced as its row says and proven by the test named there.

**Quirks and open decisions**

- S5.1 Each quirk or open decision is reproduced in one named place behind its policy switch, with the rule id in code and test names.
- S5.2 Nothing that changes output is fixed or tidied, and no behaviour is added without a rule id.

**Errors and operations**

- S6.1 Every file status, abend and return code maps to the exception, exit status and log line listed in the skill.
- S6.2 One exception type stops the run; one exit-code constant; no skip, no retry.
- S6.3 Loading and export repeat on restart; the calculation continues after the last committed account.
- S6.4 Outputs are written in the recorded format; a failed run leaves no SYSTRAN generation.
- S6.5 The run log is the legacy SYSOUT byte for byte; the application log holds no account data.

**Tests**

- S7.1 At least one test per rule id, named for it.
- S7.2 The fixture cases and edge cases listed for each rule are covered.
- S7.3 Integration tests run against PostgreSQL 15 in a container.
- S7.4 No test asserts against a copy of the code's own output.

**Guardrails**

- S8.1 No edit under `app/`, `fixtures/` or the comparator.
- S8.2 No golden file is re-recorded.
- S8.3 No new dependency without the reason in the pull request.
- S8.4 Frozen shared interfaces FI-1 to FI-6 are not changed.
- S8.5 Synthetic data only.

## 2. COBOL behaviours with no Java equivalent

Checked against `app/cbl/CBACT04C.cbl`, the five copybooks, `app/jcl/INTCALC.jcl` and `tools/build.sh:16`
(`cobc -m -std=ibm -fsign=EBCDIC`). Nothing was executed for this table; the fixture cases are recorded legacy runs.
S-numbers refer to section 1.

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
| q | File status codes and abends | PRESENT | 234-323, 327-347, 357-369, 378-390, 400-412, 422-439, 446-459, 501-514, 522-611, 628-648 | Status 00 continues; 10 on TCATBALF ends the input; 23 on DISCGRP falls back to DEFAULT; anything else prints a message and the status line and abends with user code 999 | S6.1-S6.3: the mapping table in section 6 of the skill | `intR08_*` one per stop condition (no fixture has an abend) | U0999 on the host against RC 12 from `tools/stubs/CEE3ABD.cbl`: **confirm on the mainframe** - mainframe operations owner (REL-10) |
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

## 3. Three before / after examples from CBACT04C

The Java is illustrative: it shows the shape the standards ask for and has not been compiled.

### 3.1 A calculation - monthly interest (INT-R05, INT-Q01; S3.1, S3.2)

Before, `app/cbl/CBACT04C.cbl:464-467`:

```cobol
           COMPUTE WS-MONTHLY-INT
            = ( TRAN-CAT-BAL * DIS-INT-RATE) / 1200

           ADD WS-MONTHLY-INT  TO WS-TOTAL-INT
```

`WS-MONTHLY-INT` is `S9(09)V99` and there is no `ROUNDED`, so 1235.50 * 12.00 / 1200 = 12.355 is stored as 12.35.

After:

```java
/** INT-R05 formula; INT-Q01 the mode comes from interest.policy.rounding (default TRUNCATE = RoundingMode.DOWN). */
BigDecimal monthlyInterest(BigDecimal balance, BigDecimal yearlyRatePercent) {
    return balance.multiply(yearlyRatePercent)
                  .divide(TWELVE_HUNDRED, 2, policy.roundingMode());
}
```

Not this - it gives 12.36 on fixture i02-tie-half-cent and loses cents on large balances:

```java
double monthlyInt = tranCatBal * disIntRate / 1200;          // double, no rounding rule
```

### 3.2 A file loop - reading the balances and breaking on the account (INT-R01, INT-R07; S2.3, S2.4)

Before, `app/cbl/CBACT04C.cbl:188-203` (shortened):

```cobol
           PERFORM UNTIL END-OF-FILE = 'Y'
               IF  END-OF-FILE = 'N'
                   PERFORM 1000-TCATBALF-GET-NEXT
                   IF  END-OF-FILE = 'N'
                     IF TRANCAT-ACCT-ID NOT= WS-LAST-ACCT-NUM
                       IF WS-FIRST-TIME NOT = 'Y'
                          PERFORM 1050-UPDATE-ACCOUNT
                       ELSE
                          MOVE 'N' TO WS-FIRST-TIME
                       END-IF
                       MOVE 0 TO WS-TOTAL-INT
                       MOVE TRANCAT-ACCT-ID TO WS-LAST-ACCT-NUM
```

Two flags, a saved key and a running total carry the state from record to record.

After - the reader hands over one account with all its balances, so there is no flag and nothing to carry:

```java
record AccountBalances(long accountId, List<CategoryBalance> balances, boolean endsInput) {}

/** INT-R01, INT-R07: one item is one account; the total is a local sum. */
AccountInterest process(AccountBalances item) {
    Account account = accounts.find(item.accountId()).orElseThrow(() -> stopped(ACCOUNT_MISSING));
    List<InterestTransaction> transactions = item.balances().stream()
            .flatMap(balance -> interestFor(account, balance).stream())      // INT-R02..R06
            .toList();
    return new AccountInterest(account, transactions, policy.updatesAccount(item.endsInput()));  // INT-Q04
}
```

Not this - the COBOL loop in Java syntax:

```java
while (!endOfFile.equals("Y")) { p1000TcatbalfGetNext(); if (!acctId.equals(wsLastAcctNum)) { ... } }
```

### 3.3 A quirk - the last account is never updated (INT-Q04; S5.1, S5.2)

Before, `app/cbl/CBACT04C.cbl:188-190` and `219-222`:

```cobol
           PERFORM UNTIL END-OF-FILE = 'Y'
               IF  END-OF-FILE = 'N'
                   PERFORM 1000-TCATBALF-GET-NEXT
      ...
               ELSE
                    PERFORM 1050-UPDATE-ACCOUNT
               END-IF
           END-PERFORM.
```

The read sets `END-OF-FILE` to `Y` inside the `IF`; the loop condition then ends the loop, so the `ELSE` branch
never runs. The account with the highest number gets its interest transactions but no balance update. Every interest
fixture shows it.

After - reproduced in one named place, behind the switch of the architecture, with the rule id in the name:

```java
/** INT-Q04: as coded, the account that ends the input is not updated. interest.policy.updateLastAccount=false. */
boolean updatesAccount(boolean endsInput) {
    return !endsInput || updateLastAccount;
}
```

```java
@Test
void intQ04_lastAccountNotUpdated() { /* i01-baseline: the highest account keeps its recorded balance */ }
```

Not this - either silently "fixing" it (every account updated: all seven fixture cases fail on ACCTDATA) or
scattering `if (isLast)` through writer and export with no rule id.

## 4. What remains to be confirmed

Everything in this document is PROPOSED until the **target architect** confirms it. Beyond that:

**Only the mainframe can settle** (each is marked "confirm on the mainframe" in section 2):

| Point | Behaviour rows | Owner |
|---|---|---|
| Compiler options of the production load module (ARITH, TRUNC, NUMPROC, ZONEDATA); the off-host build is GnuCOBOL 3.1.2 `-std=ibm -fsign=EBCDIC` and no host listing is in the estate | b, c, e, f | mainframe application owner |
| What an overflowing amount or the millionth transaction really does (INT-H05) | b | mainframe application owner |
| What a non-numeric balance or rate does: S0C7 or a computed value | f | mainframe application owner |
| Bytes of `TRAN-DESC` 25-100 and the transaction FILLER on the host (G05) | g, j, l | mainframe application owner |
| What a PARM shorter than 10 characters puts into `TRAN-ID` | w | mainframe application owner |
| An account with several cards: status 02, 00 or an error, and which DD serves the read (INT-H03, G08, G12) | p | mainframe application owner |
| Host code page (G01); F-signed positives and minus zero in host data (G02, G03); order of mixed letter/digit type codes (G06) | d, h | mainframe data owner |
| Abend U0999 on the host against RC 12 from the off-host stub; what the scheduler must see (REL-10) | q, z | mainframe operations owner |
| Abnormal-end disposition and generation numbering of `SYSTRAN(+1)`; the runner models neither | r | mainframe operations owner |
| Where DISPLAY output goes on the host (stream ask A10) | s | mainframe operations owner |
| Timestamps that differ inside one run and the time zone of the host clock; the harness freezes the clock | u | mainframe operations owner |

**Open in the registers and reproduced as coded until decided:** INT-Q01 to INT-Q06 and INT-H01 to INT-H06
(`modernization/rules/interest/decisions.md`), REL-10 (exit code) and REL-11 (restart)
(`modernization/design/decisions-release.md`), G11 (volumes, hence the chunk size).

**Proposals made by this document that no source dictates** - for the target architect:

- Test names (`intQ01_truncatesHalfCent` ...), the exception name `InterestRunStoppedException` and its reasons.
- Refusing a `parmDate` that is not exactly 10 characters (row w) and rejecting non-numeric data at the loader (row f) instead of imitating an undefined legacy result.
- Writing a status line only where the condition determines the legacy file status (S6.1).
- The split between the run log (legacy SYSOUT, holds account ids because the legacy printed them and parity compares them) and the application log (no account data).
- The reasoning in row c that intermediate precision cannot change a truncated result.

**Not tested by anything yet:** the standards themselves. If the pilot ticket shows a standard is wrong, unclear or
missing, the ticket session reports it in its pull request; the skill is changed only by its owner.
