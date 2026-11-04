# Legacy languages and dynamic code in the estate

Everything a discovery tool must find beyond straight COBOL: HLASM, PL/I,
dynamic `CALL`, dynamic SQL, and the z/OS services they lean on.  "Run status"
says whether the artefact executes in the Docker estate (`make parity`) or is
analysis-only.

| Artefact | Language | Callers | Run status |
|---|---|---|---|
| `app/asm/COBDATFT.asm` | HLASM | `CBACT01C` (`CALL 'COBDATFT' USING CODATECN-REC`) | analysis-only (no assembler); `CBACT01C` is not in a chain |
| `app/asm/MVSWAIT.asm` | HLASM (uses `ASMWAIT`/STIMER macro) | `COBSWAIT` (`CALL 'MVSWAIT' USING MVSWAIT-TIME`), itself run from `CBSTM03A/B` JCL | analysis-only |
| `app/pli/CBFEECAL.pli` | PL/I | `CBFEEDRV` (`app/cbl/CBFEEDRV.cbl`), job `app/jcl/FEECALC.jcl` | analysis-only (no license-clean PL/I compiler) |
| `app/cbl/CBFEEDRV.cbl` | COBOL | `FEECALC.jcl` STEP05 | compiles; cannot run (CBFEECAL unresolved) |
| `app/app-transaction-type-db2/cbl/CBTRNDYN.cbl` | COBOL + dynamic SQL + dynamic CALL | `MNTTRDYN.jcl` (`IKJEFT01` → `DSN RUN PROGRAM(CBTRNDYN) PARMS('COBTUPDT')`) | **runs**: chain `trantype-dynamic`, 3 cases |
| `tools/stubs/PARMDRV.cbl`, `DSNRUN.cbl` | COBOL, dynamic CALL | local runner only | runs (test harness, not estate code) |

## HLASM

### COBDATFT - date reformatting service

* **Purpose**: converts a date between `YYYYMMDD` (type `1`) and
  `YYYY-MM-DD` (type `2`) in a parameter record; sets `INVALID INPUT` in the
  message field when the direction/format pair does not match.
* **Linkage**: standard OS linkage.  `STM R14,R12,12(R13)`, own save area
  `SAVE` chained via `SAVE+4` / `8(,R13)`, `R13` switched to it, `R12` as base
  (`USING COBDATFT,R12`), `LM R14,R12,12(R13)` and `BR R14` on exit.
  `R1` → parameter list, `R2` → the single parameter (`USING COREC,R2`).
* **Parameter record** (`COREC`, COBOL copy `CODATECN-REC` in `CBACT01C`):
  `COINTYPE` X(1) input format, `COINPDT` X(10) input date, `COOUTYPE` X(1)
  output format, `COOUTDT` X(10) output date, `COERMSG` message.
* **Registers used**: R1 (parm list), R2 (record base), R12 (code base), R13
  (save area), R14/R15 (return / entry).  R15 is not set to a return code; the
  caller inspects `COERMSG`.
* **Local stand-in**: none needed for the chains.  If `CBACT01C` is ported, the
  routine is a 10-line date formatter; a GnuCOBOL `COBDATFT` with `INSPECT`/
  `STRING` or a Java `DateTimeFormatter` is equivalent.  Note the `VALIDIN2`
  branch has its hyphen check commented out, so `2 → 1` accepts any separator.

### MVSWAIT - timed wait

* **Purpose**: suspends the task for *n* hundredths of a second via the
  `ASMWAIT` macro (STIMER WAIT) so `COBSWAIT`/`CBSTM03A` can throttle.
* **Linkage**: `USING *,15`, `STM 14,12,12(13)` into the caller's save area,
  `L 5,0(1)` → address of the fullword delay, `L 1,0(5)` its value stored in
  `BINLBL`, `LM 14,12,12(13)`, `MVI 12(13),X'FF'`, `SR 15,15` (RC 0), `BR 14`.
  No own save area: it never calls anything, so it keeps using the caller's.
* **Parameter**: one fullword binary (`MVSWAIT-TIME PIC 9(8) COMP`).
* **Local stand-in**: `CALL 'C$SLEEP'` (GnuCOBOL) or `Thread.sleep`.  The
  off-host stand-in should be a no-op in batch tests - a wait has no
  functional output, and the statement chain is not part of parity.

## PL/I

### CBFEECAL - late-fee calculation

Synthetic but representative: `DECIMAL FIXED` arithmetic with PL/I's
truncating assignment, `ON ZERODIVIDE` / `ON SIZE` / `ON ERROR` units that set
a return code and leave through a label, and a `BASED` structure (`FEE_REQ
BASED (REQ_PTR)`) overlaying the caller's storage.  The COBOL mirror of the
structure is `FEE-REQ` in `CBFEEDRV` (`COMP-3` for `DECIMAL FIXED`, `COMP`
halfwords for `FIXED BINARY (15)`).

Rules: `fee = cycle balance * 2.500 / 100` assigned to `DECIMAL FIXED (9,2)`
(**truncates**), floor 5.00, cap 35.00; `GOLD` with cycle balance < 100.00 is
waived (RC 4); average daily balance = cycle balance / cycle days, and 0 days
raises `ZERODIVIDE` (RC 8, fee 0).  `FEE_LATE_DAYS <= 0` returns fee 0, RC 0.

Worked examples (inputs as `CBFEEDRV` builds them: `LATE_DAYS` 10 and
`CYCLE_DAYS` 30 for any positive cycle balance):

| # | GROUP_ID | CYCLE_BAL | LATE_DAYS | CYCLE_DAYS | FEE_AMT | FEE_AVG_DAILY | FEE_RC | Why |
|---|---|---|---|---|---|---|---|---|
| 1 | STANDARD | 1234.56 | 10 | 30 | **30.86** | 41.15 | 0 | 1234.56 × 2.5 % = 30.864 → truncated, not 30.87 |
| 2 | STANDARD | 100.00 | 10 | 30 | 5.00 | 3.33 | 0 | 2.50 lifted to the 5.00 floor |
| 3 | STANDARD | 5000.00 | 10 | 30 | 35.00 | 166.66 | 0 | 125.00 capped at 35.00 |
| 4 | GOLD | 80.00 | 10 | 30 | 0.00 | 2.66 | 4 | waived: GOLD below 100.00 |
| 5 | STANDARD | 1000.00 | 10 | 0 | 0.00 | 0.00 | 8 | `ZERODIVIDE` in the average-daily division |
| 6 | GOLD | 0.00 | 0 | 30 | 0.00 | 0.00 | 0 | nothing overdue |

(`FEE_AVG_DAILY` is also a truncating `DECIMAL FIXED (12,2)` assignment:
100/30 = 3.33, 5000/30 = 166.66, 80/30 = 2.66.)

No PL/I compiler is shipped in the estate image: IBM Enterprise PL/I is
licensed and the open-source alternatives (Iron Spring PL/I, free but
binary-only and 32-bit; PL/I front-ends for GCC are unfinished) are not
license-clean for a shared container.  A converter therefore has to be
verified against the table above, not against an execution.

## Dynamic CALL

* `CBTRNDYN`: `CALL WS-PGM-NAME` where `WS-PGM-NAME` comes from the JCL
  `PARMS('COBTUPDT')`.  Static analysis cannot resolve the target; the JCL can.
  Runs in chain `trantype-dynamic`.
* Harness stubs `PARMDRV` (`CALL WS-PGM-NAME USING WS-PARM-AREA`) and
  `DSNRUN` (`CALL WS-PGM-NAME USING WS-PARM-AREA`) do the same for every chain
  step with `PARM=` and for `DSN RUN PROGRAM(...)`.
* `DSNRUN` also installs an end-of-task exit with `CBL_EXIT_PROC` and a
  `PROGRAM-POINTER` set from `ENTRY 'DSNEXIT'`.

## Dynamic SQL

* `CBTRNDYN` 2000-RUN-DYNAMIC-SQL: `EXEC SQL PREPARE DYNSTMT FROM :WS-SQL-STMT
  END-EXEC` followed by `EXEC SQL EXECUTE DYNSTMT END-EXEC`.  The statement text
  is read at run time from the `DYNSQL` DD (80-byte cards, `--` comments).
  Open COBOL ESQL accepts it provided the host variable is a VARCHAR group whose
  49-levels are named `<group>-LEN` and `<group>-ARR`.
* Behaviour difference captured in `fixtures/trantype-dynamic/d03-bad-statement`:
  after a failed dynamic statement PostgreSQL aborts the whole transaction
  (`SQLCODE -400` on every later statement until commit/rollback), whereas Db2
  rolls back only the failing statement.  COBTUPDT's subsequent update is
  therefore lost under PostgreSQL.  The golden files record the PostgreSQL
  behaviour; the migration team has to decide between a savepoint per
  statement and failing the job.

## Not present in this estate

* **NatStar / Natural**: none.  The application has no Software AG artefacts
  and none were invented.
* **CICS/BMS**: present (`app/bms`, `app/cbl/CO*`) but analysis-only, see
  `docs/BASELINE.md`.
