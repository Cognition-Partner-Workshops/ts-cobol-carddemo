# Hand-off: trantype-update stream (Track B, first)

> **PROPOSED - not approved.** Written by an agent in step 7 from the source on `demo/06-lab` at `d39ebac`. No
> rule, design, annex, Java module or parity target exists for this stream. Nothing was run for it.
> **Owner who must confirm: the target architect.** Plan and owners: `modernization/design/tracks.md`.

| | |
|---|---|
| Chain | `trantype-update` in `tools/runjcl/chains.json` |
| Job and program | `app/app-transaction-type-db2/jcl/MNTTRDB2.jcl` STEP1 (`IKJEFT01`, `DSN SYSTEM(DAZ1)`, `RUN PROGRAM(COBTUPDT) PLAN(CARDDEMO)`, `:21-30`) -> `app/app-transaction-type-db2/cbl/COBTUPDT.cbl` (237 lines) |
| Input | `INPFILE`, 53 bytes; the tables seeded by `fixtures/trantype-update/<case>/input/db/seed.sql` |
| Output | `sysout/MNTTRDB2.STEP1.txt`, `rc.json`, `db/carddemo.transaction_type.csv`, `db/carddemo.transaction_type_categor.csv`. No dataset |
| Cases | seven, `t01` to `t07` (section 5) |
| Shared and frozen | `modernization/design/shared-layouts.md`, sections 2, 3 and 5: the two tables, and `COBTUPDT`'s input record and what its caller relies on |

This stream **owns the conversion of `COBTUPDT`**. `trantype-dynamic` calls the result and does not change it.

## 1. Reused unchanged from interest

| What | Where | How this stream uses it |
|---|---|---|
| Schema | `db/postgres/01-transaction-type.sql` | the two tables as they are; frozen. Nothing of `02-interest.sql` is used |
| Codec | `java/interest`, `com.carddemo.interest.codec` | **not needed.** `INPFILE` is 53 bytes of text (`PIC X` only); there is no zoned, packed or binary field and no copybook |
| Mapping | `data-map-interest.md` | only rule E3 (text, padded with spaces) applies. No row of the data map covers `INPFILE` or the two tables |
| Comparator | `tools/parity/compare.py trantype-update <case> --actual <dir> --label java` | unchanged: SYSOUT as text, `rc.json`, and the two table snapshots as `db/<table>.csv` |
| Legacy side | `make up`, `make build`, `make run CHAIN=trantype-update`, `make parity CHAIN=trantype-update` | unchanged |
| Lab | `lab/` | runs interest only; used unchanged as the regression check of interest |
| Shared skills | `carddemo-conversion-standards`, `carddemo-ticket-build`, `carddemo-ticket-verify`, `carddemo-pitfalls`, `carddemo-parity` | read-only. They were written from a file-based stream; section 3 lists what they do not cover |
| Its annex | `.agents/skills/carddemo-stream-trantype-update` | **not present.** The pilot writes it |
| Evidence layout | `java/<stream>/evidence/<ticket>/` | same form |

## 2. What is new

* **A program whose only business effect is in a database.** No output dataset; parity is the two table
  snapshots, the SYSOUT lines and the return code.
* **The job step is a Db2 launcher.** `IKJEFT01` reads `SYSTSIN`; the program name and plan are in the control
  cards, not on the `EXEC`. Off-host `tools/stubs/DSNRUN.cbl` connects, calls the program and commits at end of
  task; its two `DSNRUN:` lines are in the golden SYSOUT.
* **A program that is also a callee** (`shared-layouts.md`, section 3.3). The eight things its caller relies on
  are frozen before the pilot starts.
* **An error that does not stop the run** and a return code 4 at a normal end (`COBTUPDT.cbl:230-233`).
* A rulebook, a design, a Java module `java/trantype-update` and a parity entry point - none exists.

## 3. Gaps the Track B pilot must close

No standard and no skill covers these. `carddemo-conversion-standards` has no id for SQL; its 27 behaviour rows
(a to aa) ask nothing about a database. **No standard is written here.** For each gap the pilot reproduces the
recorded behaviour on `t01-add`, and proposes the standard text and the behaviour row to the target architect in
its pull request. What `t01-add` cannot exercise is marked: the pilot then writes the proposal from the source
and names the case that will prove it.

### 3.1 Embedded SQL

| Gap | What the source shows | Proven by the pilot on `t01-add`? |
|---|---|---|
| Static statements with host variables | INSERT `:137-148`, UPDATE `:171-175`, DELETE `:201-204`; host variables `:INPUT-REC-NUMBER`, `:INPUT-REC-DESC` are fields of the record just read | INSERT yes; UPDATE and DELETE by `t02`, `t03` |
| Fixed-length host variable into a `VARCHAR` column | the description is stored with its trailing spaces; the snapshot holds them (`docs/BASELINE.md`). The DCLGEN declares a length-prefixed `VARCHAR` (`dcl/DCLTRTYP.dcl`), which the program does not use for the statement | yes |
| `INCLUDE SQLCA`, `INCLUDE DCLTRTYP` | `:50-54`. `tools/esql_prep.py` rewrites both in a scratch copy for `ocesql`; the original is not changed | yes - where the declarations live in the target |
| Qualified table name | `CARDDEMO.TRANSACTION_TYPE` in all three statements | yes |
| Unit of work | no COMMIT, no ROLLBACK; one commit at end of task, by the launcher | yes for the commit; a failure in the middle by `t06` |
| Plan and package | `PLAN(CARDDEMO)`, `DBRMLIB` (`MNTTRDB2.jcl:25, :30`); `CREADB21.jcl` sets the database up. Nothing off-host stands for a plan | no - nothing to run; states what the target has in its place |
| The launcher's own lines in SYSOUT | `DSNRUN: DSN ATTACHED, RUNNING COBTUPDT`, `DSNRUN: COMMIT AT END OF TASK` come from the harness, and the host would print neither | yes - they are compared, so they are written |

### 3.2 SQLCODE handling

| Gap | What the source shows | Proven by the pilot on `t01-add`? |
|---|---|---|
| SQLCODE zero | one success line per statement (`:152-153, :178-179, :208-209`) | INSERT yes |
| SQLCODE negative | message with the code edited `----9`, return code 4, **the run goes on** (`:154-162`, `:230-233`) | no - `t06-duplicate-add` |
| The code is the precompiler's, not Db2's | `t06` records `-403` where Db2 gives `-803` (`docs/BASELINE.md`); the number is in the compared SYSOUT line | no - `t06`. The proposal says which number the target prints and where the mapping lives |
| SQLCODE +100 | UPDATE and DELETE have a `No records found.` branch with return code 4 (`:180-184, :210-214`); INSERT has none. The golden `t05` shows success and return code 0 for a DELETE that finds nothing. `AGENTS.md` and `docs/BASELINE.md` say +100 is ignored | no - `t05`. Register row SHR-05; reproduce the golden file |
| Return code 4 at a normal end | `:232`; `t06` records `"STEP1": 4` | no - `t06` |
| An unknown record type | `ERROR: TYPE NOT VALID`, return code 4, the run goes on (`:122-128`) | no - no case has one |
| A failed open | prints `OPEN FILE NOT OK` and goes on to read (`:83-88`) | no - no case has one |
| `EXIT` that does not leave | every paragraph ends in `EXIT.`; `9999-ABEND` returns to its caller, and `STOP RUN` is reached only at `:99` | yes - the control flow of the pilot depends on it |

### 3.3 Dynamic SQL and the dynamic call

These belong to `CBTRNDYN`, which this stream does not convert. The pilot still closes them **as proposals**,
because the conversion of `COBTUPDT` decides whether they can be met. They are listed with their evidence in
`modernization/streams/trantype-dynamic/handoff.md`, section 3. The pilot must leave `COBTUPDT`:

* reachable by its name, with no parameters, reading `INPFILE` by itself;
* running in a connection and transaction it is given, without committing;
* ending the whole run when it ends, with its return code as the step's.

## 4. Open asks

| Id | Ask | Why | Evidence | Owner | Blocks |
|---|---|---|---|---|---|
| TU-1 | Answer SHR-05: what a DELETE or UPDATE of a missing type reports | the source and the golden file disagree | `COBTUPDT.cbl:180-184, :210-214`; `t05-delete-missing` | business owner; mainframe application owner | nothing: the golden file is reproduced; the switch default depends on it |
| TU-2 | What does Db2 return for the cases recorded with `ocesql` codes (`-403`, `-400`), and which text must operators see? | the codes are in compared SYSOUT lines | `t06`, `d03`, `docs/BASELINE.md` | mainframe application owner | host parity only |
| TU-3 | Who decides the standards for SQL (section 3)? The pilot proposes; it does not edit the shared skills | no standard exists | `carddemo-conversion-standards` | target architect | the rest of the stream, not the pilot |
| TU-4 | Rule-id prefix and place of the design documents; one prefix for both trantype streams or two? | none chosen | - | target architect | the rest of the stream |
| TU-5 | Does anything else write `TRANSACTION_TYPE` while the job runs? | the CICS programs `COTRTLIC` and `COTRTUPC` use the table; no `CLOSEFIL` guards a Db2 table | `app/app-transaction-type-db2/cbl/COTRTUPC.cbl`; `CardDemo.controlm:26-31` | mainframe application owner | coexistence, not the seven cases |
| TU-6 | Is the table name `transaction_type_categor` kept (SHR-04)? | cut short off-host | `01-transaction-type.sql:11` | data architect | nothing: kept until answered |
| TU-7 | What must stay true for TRANEXTR, which runs after this job? | `CardDemo.controlm:57-59`: TRANEXTR waits on the condition MNTTRDB2 sets | `TRANEXTR.jcl:65-115` | scheduler owner | the job's place in the schedule |

## 5. What this stream must pass

For each case: `python3 tools/parity/compare.py trantype-update <case> --actual <dir> --label java` exits 0 on
the SYSOUT file, `rc.json` and both table snapshots. Before and after, `make parity CHAIN=trantype-update` stays
green.

| Case | Proves (its README) | `STEP1` |
|---|---|---|
| `t01-add` | two new types are inserted | 0 |
| `t02-update` | the description of type 02 is updated | 0 |
| `t03-delete` | type 03 is deleted | 0 |
| `t04-comments` | comment lines (`*`) are ignored; one add is applied | 0 |
| `t05-delete-missing` | deleting a type that does not exist still prints success; table unchanged | 0 |
| `t06-duplicate-add` | a duplicate insert reports the error (`-403`); table unchanged | 4 |
| `t07-mixed-batch` | add, update, delete and a comment in one file | 0 |

After every change the seven interest cases pass unchanged: `make parity CHAIN=interest` and `make parity-java`.
Once `trantype-dynamic` has started, every change to the converted `COBTUPDT` also reruns the three `d` cases.

## 6. Not to touch

`app/`, `fixtures/`, `tools/`, `db/postgres/01-transaction-type.sql` and `02-interest.sql`, `java/interest`,
`lab/`, the shared skills, the `Makefile` and the CI workflow (proposed to their owner, `tracks.md` section 7),
`CBTRNDYN` and anything of Track A.
