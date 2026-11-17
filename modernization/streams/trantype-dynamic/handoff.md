# Hand-off: trantype-dynamic stream (Track B, last)

> **PROPOSED - not approved.** Written by an agent in step 7 from the source on `demo/06-lab` at `d39ebac`. No
> rule, design, annex, Java module or parity target exists for this stream. Nothing was run for it.
> **Owner who must confirm: the target architect.** Plan and owners: `modernization/design/tracks.md`.

| | |
|---|---|
| Chain | `trantype-dynamic` in `tools/runjcl/chains.json` |
| Job and programs | `app/app-transaction-type-db2/jcl/MNTTRDYN.jcl` STEP1 (`IKJEFT01`, `RUN PROGRAM(CBTRNDYN) PLAN(CARDDEMO) PARMS('COBTUPDT')`, `:23-33`) -> `app/app-transaction-type-db2/cbl/CBTRNDYN.cbl` (103 lines), which calls `COBTUPDT` by a name held in `WS-PGM-NAME` (`:62`) |
| Input | `DYNSQL`, 80 bytes (one SQL statement); `INPFILE`, 53 bytes (read by `COBTUPDT`); the tables seeded by `input/db/seed.sql` |
| Output | `sysout/MNTTRDYN.STEP1.txt`, `rc.json`, the two table snapshots. No dataset |
| Cases | three, `d01` to `d03` (section 5) |
| Shared and frozen | `modernization/design/shared-layouts.md`, sections 2, 3 and 5 |

**This stream starts only once `COBTUPDT` is converted** and its seven cases pass: every case here runs
`COBTUPDT` after the dynamic statement, and `d01` and `d03` need its add, update and error paths.

## 1. Reused unchanged

| What | Where | How this stream uses it |
|---|---|---|
| The converted `COBTUPDT` | `java/trantype-update`, from the `trantype-update` stream | called, not changed. A change it needs is asked of that stream |
| Schema | `db/postgres/01-transaction-type.sql` | the two tables as they are; frozen |
| Codec, mapping | `java/interest` codec, `data-map-interest.md` | not needed: `DYNSQL` and `INPFILE` are text |
| Comparator | `tools/parity/compare.py trantype-dynamic <case> --actual <dir> --label java` | unchanged |
| Legacy side | `make run CHAIN=trantype-dynamic`, `make parity CHAIN=trantype-dynamic` | unchanged |
| Lab | `lab/` | runs interest only; used unchanged as the regression check of interest |
| Shared skills | the five `carddemo-*` skills | read-only |
| The annex and standards of `trantype-update` | written by the Track B pilot and the session after it | reused for embedded SQL, SQLCODE and the unit of work |
| Its own annex | `.agents/skills/carddemo-stream-trantype-dynamic` | **not present.** Written by this stream's session |

## 2. What is new

Everything `CBTRNDYN` does beyond `COBTUPDT`: a statement that is not in the program, and a call whose target is
not in the program. It was added to the estate for the lab (`docs/BASELINE.md`: "CBTRNDYN (new, lab)"), and
`MNTTRDYN` is in neither scheduler export.

## 3. Gaps - dynamic SQL and the dynamic call

No standard and no skill covers these, and none is written here. The Track B pilot proposes the text
(`modernization/streams/trantype-update/handoff.md`, section 3.3); this stream is where the three cases prove or
correct it.

### 3.1 Dynamic SQL

| Gap | What the source shows | Case |
|---|---|---|
| The statement is data | read from `DYNSQL` at run time, prepared and executed (`:88-97`). Any statement the connection may run can be in the file | all |
| How the text is built | lines starting `--` are dropped (`:73`); of every other line columns 1 to 72 are appended with one space (`:74-77`); 800 bytes at most (`:50`), with no overflow test on the STRING | `d01` (three lines, one of them a comment) |
| The line count is printed | `CBTRNDYN: nn SQL LINES READ` with a two-digit counter (`:45, :84`) | all |
| Unqualified table name | the three cases say `TRANSACTION_TYPE`, not `CARDDEMO.TRANSACTION_TYPE`; which schema that means is set outside the program | all |
| PREPARE fails | prints `CBTRNDYN: PREPARE FAILED SQLCODE`, return code 8, and does not execute (`:91-93`) | **none** |
| EXECUTE result | prints `CBTRNDYN: EXECUTE SQLCODE` and the code as a signed ten-digit number (`+0000000000`, `-0000000400`); negative sets return code 8 (`:98-101`) | `d01`, `d02` (zero), `d03` (`-400`) |
| A statement that changes no row | +100 is not tested; only a negative code is an error (`:99`) | none |
| No parameter markers, no cursor | one PREPARE, one EXECUTE; a SELECT in the file cannot be executed this way | none |
| A failed statement and the transaction | PostgreSQL ends the whole transaction; Db2 rolls back the statement only (`d03` README). Register row SHR-06 | `d03` |
| What may be run | nothing in the program limits the statement to the two tables or to data changes | none - an ask (TD-2) |

### 3.2 The dynamic call

| Gap | What the source shows | Case |
|---|---|---|
| The target is a value | `PARM-VALUE(1:PARM-LENGTH)` moved to `WS-PGM-NAME` X(08), then `CALL WS-PGM-NAME` (`:59-62`). The name is in the job's control cards (`MNTTRDYN.jcl:33`), not in the program | all (`COBTUPDT`) |
| Which names are allowed | none is checked. The only name in evidence is `COBTUPDT`; a PARM longer than eight bytes is not guarded | none - an ask (TD-1) |
| No PARM, no call | `IF PARM-LENGTH > 0` (`:59`) | none |
| The call is made after a failed statement | the test is on the PARM only | `d03` |
| No arguments | no `USING`; the callee reads `INPFILE` of the caller's step (`MNTTRDYN.jcl:30`) | all |
| The callee ends the run | `COBTUPDT` ends with `STOP RUN`, so `CBTRNDYN: END` (`:64`) is never printed and `GOBACK` (`:65`) is never reached | all three golden SYSOUT files |
| Whose return code | caller 8, then callee 4: the step records 4 | `d03` |
| A callee that is not found | no `ON EXCEPTION` | none |
| One unit of work across the call | neither program commits; the launcher commits once | `d01` (both changes), `d03` (neither) |

## 4. Open asks

| Id | Ask | Why | Evidence | Owner | Blocks |
|---|---|---|---|---|---|
| TD-1 | Which program names may the PARM hold in production? | the target of the call is a value; only `COBTUPDT` is in evidence | `MNTTRDYN.jcl:33`; `CBTRNDYN.cbl:59-62` | mainframe application owner | how the target resolves a name; the three cases need `COBTUPDT` only |
| TD-2 | Which statements may `DYNSQL` hold, and who may submit the job? | the program runs any statement | `CBTRNDYN.cbl:88-97` | business owner; security owner | what the target accepts; the three cases need UPDATE and DELETE on one table |
| TD-3 | Answer SHR-06: savepoint per statement, or fail the job, after a failed statement | `d03` records the PostgreSQL behaviour, which differs from Db2's | `d03-bad-statement/README.md` | target architect | nothing: the golden file is reproduced |
| TD-4 | Is MNTTRDYN scheduled, or submitted by hand? | it is in neither export | search `MNTTRDYN` in `app/scheduler`: nothing | scheduler owner | the job's launch |
| TD-5 | Does the caller's return code 8 survive when the callee sets none? | no case records it; off-host and host may differ | `CBTRNDYN.cbl:100`; `COBTUPDT.cbl:99` | mainframe application owner | an error path with no case |
| TD-6 | What is the default schema of the job's connection on the host? | the statements are unqualified | the three `DYNSQL.txt` files | mainframe application owner | host parity only |
| TD-7 | A changed type has no proven path to `TRANTYPE.PS` | TRANEXTR waits on MNTTRDB2's condition only | `CardDemo.controlm:57-59` | scheduler owner | the link to posting's report |

## 5. What this stream must pass

For each case: `python3 tools/parity/compare.py trantype-dynamic <case> --actual <dir> --label java` exits 0 on
the SYSOUT file, `rc.json` and both table snapshots. Before and after, `make parity CHAIN=trantype-dynamic` stays
green.

| Case | Proves (its README) | `STEP1` |
|---|---|---|
| `d01-update-then-apply` | the dynamic UPDATE runs, then `COBTUPDT` applies one add; both committed together | 0 |
| `d02-delete-statement` | a dynamic DELETE of type 03; the update file is empty | 0 |
| `d03-bad-statement` | the statement fails (`-400`), return code 8, `COBTUPDT` is still called, its UPDATE fails too and the table is unchanged | 4 |

After every change: the seven `trantype-update` cases (the callee is theirs) and the seven interest cases pass
unchanged (`make parity CHAIN=interest`, `make parity-java`).

## 6. Not to touch

`app/`, `fixtures/`, `tools/`, `db/postgres/`, `java/interest`, `java/trantype-update` (ask its owner), `lab/`,
the shared skills, the `Makefile` and the CI workflow (proposed to their owner, `tracks.md` section 7), and
anything of Track A.
