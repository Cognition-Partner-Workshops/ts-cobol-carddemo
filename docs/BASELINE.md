# Baseline: what compiles and runs off-host

Captured by `tools/build.sh` inside the `estate` container (Ubuntu 22.04,
GnuCOBOL 3.1.2, Open COBOL ESQL 1.4, PostgreSQL 15 client).  Every program is
compiled from a scratch copy of the copybooks (`expand -t8 app/cpy/*.cpy ->
work/cpy`); **no source under `app/` was edited to make anything compile**.

Flags: `cobc -m -std=ibm -fsign=EBCDIC -I work/cpy` (`-std=ibm` for IBM
Enterprise COBOL syntax, `-fsign=EBCDIC` so zoned decimal overpunch signs match
the mainframe `{ A-I } J-R` convention).

## Batch programs (`app/cbl`)

| Program | Compiles | Runs in a chain | Notes |
|---|---|---|---|
| CBACT01C | yes | no (not in a demo chain) | account file print |
| CBACT02C | yes | no | card file print |
| CBACT03C | yes | no | cross-reference file print |
| CBACT04C | yes | **interest** (`INTCALC.jcl`) | truncating interest arithmetic, see below |
| CBCUS01C | yes | no | customer file print |
| CBSTM03A / CBSTM03B | yes | no | statement generation; needs `COBSWAIT` |
| CBTRN01C | yes | no | daily transaction pre-validation |
| CBTRN02C | yes | **posting** (`POSTTRAN.jcl`) | rejects -> RC 4 |
| CBTRN03C | yes | **posting** (`TRANREPT.jcl`) | 133-byte report after SORT |
| COBSWAIT | yes | no | calls `MVSWAIT` (HLASM, see docs/LEGACY-LANGUAGES.md) |
| CSUTLDTC | yes | no | date utility; `CALL 'CEEDAYS'` is an LE service, resolved at run time only |
| CBFEEDRV (new, lab) | yes | no | COBOL caller of the PL/I `CBFEECAL`; `FEECALC.jcl` documented only |
| CBEXPORT | **no** | - | `'EXPORT-SEQUENCE-NUM' is not defined`: the program references a field that no shipped copybook declares (upstream defect; fails on z/OS too without the missing copybook) |
| CBIMPORT | **no** | - | same missing `EXPORT-SEQUENCE-NUM` |

## CICS programs (`app/cbl/CO*`)

All 19 online programs fail with `DFHBMSCA: No such file or directory` or
`<mapname>: No such file or directory`: they `COPY` the CICS-supplied `DFHAID`
/ `DFHBMSCA` members and the symbolic maps that the BMS assembler generates
from `app/bms/*.bms`.  Neither a CICS region nor the BMS macro assembler is
available off-host, so these are **analysis-only** in this estate.  They are
still fully readable input for discovery (`make chain-graph` lists their
copybooks and files).

| Program | Reason |
|---|---|
| COACTUPC, COACTVWC, COCRDLIC, COCRDSLC, COCRDUPC | `DFHBMSCA` missing |
| COADM01C, COMEN01C | symbolic map `COADM01` missing |
| COBIL00C | `COBIL00` map missing |
| CORPT00C | `CORPT00` map missing |
| COSGN00C | `COSGN00` map missing |
| COTRN00C, COTRN01C, COTRN02C | `COTRN00/01/02` maps missing |
| COUSR00C, COUSR01C, COUSR02C, COUSR03C | `COUSR00/01/02/03` maps missing |

## Db2 sub-application (`app/app-transaction-type-db2`)

| Program | ocesql precompile | Compiles | Runs | Notes |
|---|---|---|---|---|
| COBTUPDT | yes | yes | **trantype-update** (`MNTTRDB2.jcl`) | batch insert/update/delete against `TRANSACTION_TYPE` |
| CBTRNDYN (new, lab) | yes | yes | **trantype-dynamic** (`MNTTRDYN.jcl`) | dynamic SQL `PREPARE/EXECUTE` + dynamic `CALL WS-PGM-NAME` |
| COTRTLIC, COTRTUPC | not attempted | - | - | CICS + Db2 online programs (BMS maps missing, see above) |

### Open COBOL ESQL gaps (handled in the scratch copy only, `tools/esql_prep.py`)

`ocesql` 1.4 is a much smaller precompiler than the Db2 one.  Three constructs
in the shipped source are not accepted and are normalised **in `work/esql`
only** - `COBTUPDT.cbl` is untouched:

1. **Multi-line `EXEC SQL INCLUDE SQLCA END-EXEC`** - ocesql only parses it on
   one line.  Joined.
2. **`EXEC SQL DECLARE <table> TABLE (...) END-EXEC` inside DCLGEN members**
   (`DCLTRTYP`, `DCLTRTCT`) - ocesql's grammar has no `DECLARE TABLE`.  The
   block is stripped; the COBOL host structure that follows it is kept.
3. **Sequence numbers in columns 1-6 / 73-80** - stripped so ocesql's free
   scanner is not confused.

Other things to know when the Db2 chain is ported:

* `ocesql` ignores the DCLGEN 49-level `VARCHAR` length and sends the full
  50-byte, space-padded host variable, so `TR_DESCRIPTION` arrives padded in
  PostgreSQL.  The golden `db/*.csv` snapshots record that behaviour.
* COBTUPDT only tests `SQLCODE < 0`; `DELETE` of a missing key (SQLCODE +100)
  prints `RECORD DELETED SUCCESSFULLY`.  Case `t05-delete-missing` keeps it.
* Duplicate insert returns SQLCODE -403 through ocesql (Db2 would say -803);
  case `t06-duplicate-add` records the ocesql value.
* `IKJEFT01 / DSN RUN PROGRAM(COBTUPDT) PLAN(CARDDEMO)` is emulated by
  `tools/stubs/DSNRUN.cbl`: it connects (`OCESQLConnect`), calls the program
  and commits at end of task through `DSNEXIT` (`CBL_EXIT_PROC`) because the
  application ends with `STOP RUN`, exactly like the DSN command processor.

## Dynamic transaction-type chain (`trantype-dynamic`)

* `d01-update-then-apply`: DYNSQL skips only lines beginning with `--`,
  concatenates columns 1-72 plus a space, and counts non-comment lines in
  a two-digit counter. The statement buffer is 800 bytes. Column truncation
  is also covered by Java's `column72AndCommentQuirk` unit test.
* `d01-update-then-apply`: dynamic SQL and the JCL-selected dynamic CALL
  share one connection and one end-of-task commit; COBTUPDT sends the full
  padded 50-character description. Its `STOP RUN` ends the run unit, so
  `CBTRNDYN: END` is absent in all three fixtures.
* `d02-delete-statement`: an empty INPFILE still opens successfully and
  commits the dynamic DELETE.
* `d03-bad-statement`: ocesql PREPARE stores the SQL locally; the invalid
  column fails at EXECUTE with SQLCODE -400. PostgreSQL rejects the called
  program's UPDATE because the same transaction is aborted. End-of-task
  COMMIT rolls back that transaction and the snapshot is unchanged.
  No savepoint or per-statement recovery is introduced.
* `d03-bad-statement`: CBTRNDYN sets RC 8, but COBTUPDT's
  `9999-ABEND` overwrites it with RC 4 and continues. The golden RC is 4,
  despite the fixture README describing the driver's intermediate RC 8.

These are retained under the **COBOL batch parity standard**. Business question:
should statement-level recovery replace PostgreSQL's whole-transaction failure
after cutover? Default: **keep legacy behaviour**.

## z/OS services emulated by the local runner (`tools/runjcl`)

| Facility | Local stand-in | Fidelity |
|---|---|---|
| VSAM KSDS / AIX / PATH | GnuCOBOL indexed files (Berkeley DB), AIX kept as a second indexed file | key semantics, duplicates, `START`/`READ NEXT`; no CI/CA tuning |
| GDG `(+1)` / `(0)` / `(-1)` | catalogue in `datasets/.catalog.json`, committed at job end | generation numbers start at `G0001V00` per fresh case |
| IDCAMS `DEFINE/DELETE/REPRO/BLDINDEX/DEFINE GDG` | Python + generated loader programs | the subset the CardDemo JCL uses |
| DFSORT (`SORT FIELDS`, `INCLUDE/OMIT COND`) | Python stable sort | the subset `TRANREPT.jcl` uses |
| `IEBGENER`, `IEFBR14` | copy / no-op | - |
| `PARM='...'` | `tools/stubs/PARMDRV.cbl` builds the halfword-length parameter area | - |
| `CEE3ABD` | `tools/stubs/CEE3ABD.cbl` displays the user abend and returns RC 12 | - |
| `SDSF` steps (`/F CICS...` commands) | no-op | no CICS region |
| `STEPLIB/JOBLIB/DBRMLIB/SYSUDUMP` | ignored | - |
| `IKJEFT01` + `DSN SYSTEM(...) RUN PROGRAM(...)` | `DSNRUN` | commit-at-end-of-task only |
| `COB_CURRENT_DATE` | frozen clock `2022-07-05 10:00:00.00` per chain | needed for deterministic `PROC-TS` |

## Legacy behaviours worth knowing (all preserved in the golden files)

* **Truncation, not rounding**: `CBACT04C` computes
  `WS-MONTHLY-INT = TRAN-CAT-BAL * DIS-INT-RATE / 1200` without `ROUNDED`,
  so 12.355 becomes 12.35.  `fixtures/interest/i02-tie-half-cent` isolates this.
* **Last account never updated**: `CBACT04C` sets `END-OF-FILE` inside the
  `PERFORM UNTIL` body, so the `ELSE PERFORM 1050-UPDATE-ACCOUNT` branch is
  never reached for the final account on `TCATBALF`; its interest transactions
  are written but `ACCT-CURR-BAL` is not rewritten.  Every interest fixture
  shows it (the highest account id keeps its old balance).
* `CBTRN02C` tests the credit limit against the *cycle* amounts
  (`CYC-CREDIT - CYC-DEBIT + AMT`), not `ACCT-CURR-BAL`.
* Reject records are 430 bytes: the 350-byte daily transaction plus an 80-byte
  trailer (`tools/parity/layouts/REJTRAN.cpy`).
