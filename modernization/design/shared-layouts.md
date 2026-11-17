# Shared layouts, tables and programs - the freeze (version 1.0.0)

> **PROPOSED - not approved.** Written by an agent in step 7 from the source on `demo/06-lab` at `d39ebac`.
> Nothing was built or run for this document except `make design-check`. Line numbers are those of the file named.
> **Owners who must confirm: the target architect (layouts, program contract) and the data architect (tables).**

Three streams take over from interest: `posting`, `trantype-update` and `trantype-dynamic`
(`tools/runjcl/chains.json`). This document freezes what more than one stream uses, before any of them starts.

**Rule of the freeze.** Every item below is frozen from the approval of this document. A stream reads it and
builds on it; it does not change it. A change is a row in the register of section 6, answered by the owner named
there - not a ticket, and not an edit inside a stream's pull request. An approved change raises the version of
this document, and of `architecture.md` when a table owner changes.

Each list was given in the step prompt and checked against the source. "Added" marks what the given list missed.

## 1. Files shared by interest and posting

### 1.1 Who reads and who updates

| Layout | Dataset | Length | Interest (`CBACT04C`) | Posting (`CBTRN02C`, `CBTRN03C`) | In the given list |
|---|---|---|---|---|---|
| `CVACT01Y` account | `ACCTDATA.VSAM.KSDS` | 300 | **reads and updates**: read by key `:373`; adds the interest to `ACCT-CURR-BAL`, sets `ACCT-CURR-CYC-CREDIT` and `ACCT-CURR-CYC-DEBIT` to zero, rewrites the whole record `:352-356` | **reads and updates** (`CBTRN02C`): read by key `:395`; tests `ACCT-CREDIT-LIMIT` against cycle credit minus cycle debit plus the amount `:403-405` and `ACCT-EXPIRAION-DATE` `:414`; adds the amount to `ACCT-CURR-BAL` and to cycle credit or cycle debit `:547-551`, rewrites the whole record `:554`. `CBTRN03C` does not open it | yes |
| `CVTRA01Y` category balance | `TCATBALF.VSAM.KSDS` | 50 | **reads only**: opened INPUT `:236`, read in key order `:326` | **creates and updates** (`CBTRN02C`): opened I-O `:329`; read by key `:474`; not found creates the record `:504-510`, found adds the amount and rewrites `:527-528` | yes |
| `CVACT03Y` card cross-reference | `CARDXREF.VSAM.KSDS` | 50 | **reads only**: opened INPUT `:254`, read by the alternate key `XREF-ACCT-ID` `:394` | **reads only**: `CBTRN02C` by card number `:383`; `CBTRN03C` by card number `:485` | yes |
| `CVTRA05Y` transaction | interest: `SYSTRAN(+1)`; posting: `TRANSACT.VSAM.KSDS`, `TRANSACT.BKUP(+1)`, `TRANSACT.DALY(+1)` | 350 | **writes** new records to a sequential generation `:309, :500`. Never reads one | **writes** (`CBTRN02C`: opened OUTPUT `:256`, keyed WRITE `:564`) and **reads** (`CBTRN03C`: `:249`, after the REPRO and SORT of `TRANREPT.jcl:23-55`) | yes |

The same layout, but **not the same dataset**, for `CVTRA05Y`: interest writes `SYSTRAN`, posting writes
`TRANSACT.VSAM.KSDS`. The two meet only in `COMBTRAN` (`app/jcl/COMBTRAN.jcl`), which is not handed off.

Both streams **update the same three account fields**. Posting adds to the cycle amounts and tests the limit
against them; interest sets them to zero. Which stream runs first decides what the other sees
(`modernization/streams/posting/handoff.md`, section 5).

### 1.2 The mapping built for interest - frozen as it is

| Frozen item | Where | What it fixes |
|---|---|---|
| Field-to-column rows of the four layouts | `modernization/design/data-map-interest.md`, sections `CVACT01Y`, `CVACT03Y`, `CVTRA01Y`, `CVTRA05Y` | column names, types, precision, scale, NOT NULL, CHECKs, collation |
| Encoding rules E1-E6 | same file, section "Encoding rules" | zoned digits, trailing overpunch sign, text, FILLER as raw bytes, no COMP-3 on a dataset, key order |
| Tables | `db/postgres/02-interest.sql` (FI-4): `carddemo.account`, `carddemo.card_xref` with index `card_xref_acct_ix`, `carddemo.tran_cat_balance` | posting reuses these three tables unchanged |
| Record codec | `java/interest`, package `com.carddemo.interest.codec` (`CardDemoLayouts.ACCOUNT`, `CARD_XREF`, `CATEGORY_BALANCE`, `TransactionRecord`) | decoding and encoding of the four layouts in the fixture code page |
| Recorded output format and comparator (FI-2) | `fixtures/<chain>/<case>/expected`, `tools/parity/compare.py`, `tools/parity/copybook.py` | `datasets/<DSN>.txt`, `sysout/<JOB>.<STEP>.txt`, `rc.json`, `db/<table>.csv`, `manifest.json` |
| The twelve mapping gaps G01-G12 | `data-map-interest.md`, last section | they stay open for posting too; G02, G03, G07 and G09 touch posting directly |

What is **not** shared, although it has the same layout: `carddemo.interest_transaction` and
`carddemo.interest_run` are owned by interest (`architecture.md`, section 3). Posting does not write to them.
Where posting keeps the transactions it writes is new work of posting, not a change of this freeze.

### 1.3 Added - what the given list missed

| Item | Why it is shared | Who reads, who updates |
|---|---|---|
| Table ownership of `carddemo.account` | `architecture.md` section 3 names one writer per column: interest-loader (load) and interest-calc (three columns). Posting writes the same three columns | the first shared-change row, SHR-01. Until it is answered posting's pilot uses file adapters and writes no shared table |
| The datasets themselves | `ACCTDATA`, `CARDXREF`, `TCATBALF` are loaded for both chains from files of the same name (`chains.json`, `inputs` of `posting` and `interest`); each chain has its own fixture content | both streams read the unloaded `.PS` image; both write `ACCTDATA.VSAM.KSDS` as an output |
| The timestamp format and the frozen clock | `CBTRN02C:692-705` and the same paragraph in `CBACT04C` (`:613-624`) build `YYYY-MM-DD-HH.MM.SS.hh0000`; the harness freezes the clock at `2022-07-05 10:00:00.00` for every chain | both write it to `TRAN-PROC-TS`; interest also to `TRAN-ORIG-TS`, posting copies `TRAN-ORIG-TS` from the input (`CBTRN02C` `2000-POST-TRANSACTION`) |
| The status-line and abend paragraphs | same code in the three programs; abend through `CALL 'CEE3ABD'`, return code 12 off-host (`tools/stubs/CEE3ABD.cbl`) | both streams write the same SYSOUT lines on a failed open, read or write |
| Category code typed two ways | `TRANCAT-CD`, `TRAN-CAT-CD` are `PIC 9(04)` -> `NUMERIC(4,0)`; `trc_type_category` is `CHAR(4)` (gap G09). `CBTRN03C:505` reads `TRANCATG` by this code | joins the file side to the table side; see section 4 |
| `CVTRA02Y` disclosure group | **not shared.** The posting chain loads `DISCGRP` in its setup, but neither `CBTRN02C` nor `CBTRN03C` opens it | interest only |
| `tools/parity/layouts/REJTRAN.cpy` | **not shared.** Comparator layout of posting's reject file | posting only |

## 2. Tables shared by the trantype streams

Source of the tables: `app/app-transaction-type-db2/ddl/TRNTYPE.ddl`, `TRNTYCAT.ddl`; off-host
`db/postgres/01-transaction-type.sql`. Seeded per case by `fixtures/<chain>/<case>/input/db/seed.sql`.

| Table | Columns | Key | `trantype-update` (`COBTUPDT`) | `trantype-dynamic` (`CBTRNDYN`) |
|---|---|---|---|---|
| `carddemo.transaction_type` | `tr_type CHAR(2)`, `tr_description VARCHAR(50)`, both NOT NULL | `tr_type` | **updates**: INSERT `:137-148`, UPDATE `:171-175`, DELETE `:201-204`. Never selects | **updates**, twice: by the statement read from `DYNSQL` (`:88-97`), and by `COBTUPDT`, which it calls (`:62`) |
| `carddemo.transaction_type_categor` | `trc_type_code CHAR(2)`, `trc_type_category CHAR(4)`, `trc_cat_data VARCHAR(50)`, all NOT NULL | `(trc_type_code, trc_type_category)`; foreign key to `transaction_type (tr_type)`, `ON DELETE RESTRICT` | **neither reads nor updates** in any statement. Reached only through the foreign key: a DELETE of a type that has categories is refused | the same; the dynamic statement is free text, so the source cannot rule a statement on this table out. The three cases name `TRANSACTION_TYPE` only |

Both tables are in the recorded snapshot of every case of both chains (`chains.json`, `database.tables`), ordered
by the key columns above, so both are compared even where the program never names one. In every one of the ten
cases the category table is empty before and after.

**Frozen:** table names, column names, types, lengths, NOT NULL, the two primary keys, the foreign key with
`ON DELETE RESTRICT`, the schema name `carddemo`, and the snapshot format `db/<schema>.<table>.csv` with its
`ORDER BY`.

### 2.1 Added - what the given list missed

| Item | What the source shows | Who reads, who updates |
|---|---|---|
| The table name is cut short off-host | Db2 name `TRANSACTION_TYPE_CATEGORY` (`TRNTYCAT.ddl`, `dcl/DCLTRCAT.dcl`, `TRANEXTR.jcl:112`); PostgreSQL name `transaction_type_categor` (`01-transaction-type.sql:11`, `chains.json`). No file says why | frozen as the off-host name, because the snapshots are named by it. Register row SHR-04 |
| Readers outside the two streams | `TRANEXTR.jcl` STEP40 and STEP50 select both tables; the CICS programs `COTRTLIC` and `COTRTUPC` use `TRANSACTION_TYPE`; interest's schema test names both tables (`db/postgres/tests/02-interest-assertions.sql:99, :128, :181`) | none of them is handed off; all depend on the frozen names |
| The description arrives padded | `ocesql` sends the full 50-byte host variable, so `tr_description` holds trailing spaces after an INSERT or UPDATE by `COBTUPDT` (`docs/BASELINE.md`, "Other things to know"); the golden `db/*.csv` hold them. A row written by the dynamic statement is not padded (`d01`: `Payment - SEPA`) | both streams must write what the golden snapshot holds |
| One unit of work | Neither program has a COMMIT or ROLLBACK (search: nothing). The step commits once at end of task; off-host `tools/stubs/DSNRUN.cbl` and `DSNEXIT.cbl` do it and print `DSNRUN: DSN ATTACHED, RUNNING <program>` and `DSNRUN: COMMIT AT END OF TASK`, which are in the golden SYSOUT | both streams; the two `DSNRUN:` lines are output of the harness, not of the estate, and are still compared |
| The file image of the two tables | `CVTRA03Y` (60 bytes) and `CVTRA04Y` (60 bytes), read by `CBTRN03C` of posting | section 4 |

## 3. Program `COBTUPDT` - run by one stream, called by the other

`app/app-transaction-type-db2/cbl/COBTUPDT.cbl`, 237 lines. `trantype-update` runs it as the main program
(`MNTTRDB2.jcl:30`). `trantype-dynamic` calls it from `CBTRNDYN` by the name held in `WS-PGM-NAME`, taken from the
PARM (`CBTRNDYN.cbl:59-62`, `MNTTRDYN.jcl:33`). **One conversion, owned by `trantype-update`; `trantype-dynamic`
reads it and does not change it.**

### 3.1 Its input record - frozen

DD `INPFILE` (`:31`), fixed length 53 (`chains.json`: `"INPFILE": 53`), layout in the program, no copybook (`:39-45`):

| Field | PIC | Offset | Length | Meaning |
|---|---|---|---|---|
| `INPUT-REC-TYPE` | X(1) | 0 | 1 | `A` add, `U` update, `D` delete, `*` comment line; anything else is an error (`:110-128`) |
| `INPUT-REC-NUMBER` | X(2) | 1 | 2 | `tr_type` |
| `INPUT-REC-DESC` | X(50) | 3 | 50 | `tr_description`, sent padded to 50 |

The record is echoed to SYSOUT as `PROCESSING   ` plus the 53 bytes (`:105`).

### 3.2 What it does, as far as a caller can see

| Behaviour | Line | Recorded in |
|---|---|---|
| Prints `OPEN FILE OK` or `OPEN FILE NOT OK`; a failed open does not stop it | `:83-88` | every case |
| One SQL statement per record, in file order; one message line per record | `:109-128` | every case |
| An error does **not** stop the run: `9999-ABEND` prints the message, sets return code 4 and returns; the next record is processed | `:230-233` | `t06-duplicate-add`, `d03-bad-statement` |
| Negative SQLCODE: `Error accessing: TRANSACTION_TYPE table. SQLCODE:` and the code, edited `----9` | `:154-162, :185-193, :216-224` | `t06` (`-403`), `d03` (`-400`) - values of `ocesql`, not of Db2 |
| SQLCODE +100 on UPDATE and DELETE: the source prints `No records found.` and sets return code 4. INSERT has no +100 branch | `:180-184, :210-214` | **not recorded.** `t05-delete-missing` deletes a missing type and the golden SYSOUT says `RECORD DELETED SUCCESSFULLY`, return code 0: off-host the SQLCODE was 0. Register row SHR-05 |
| No COMMIT, no ROLLBACK | search: nothing | all changes of the step commit together or not at all |
| Ends with `STOP RUN`, not `GOBACK` | `:99` | `d01`, `d02`, `d03`: see 3.3 |
| No `LINKAGE SECTION`, no `USING` | `:80` | it takes nothing from a caller |

### 3.3 What the caller relies on - frozen

Each line is something `CBTRNDYN` or `MNTTRDYN.jcl` depends on and the conversion of `COBTUPDT` must keep.

1. **The name.** The caller knows the program only as the eight characters of its PARM (`CBTRNDYN.cbl:46, :60`).
   The converted program stays reachable under the name `COBTUPDT`.
2. **No parameters.** `CALL WS-PGM-NAME` has no `USING` (`:62`). The callee finds its input by itself.
3. **`INPFILE` belongs to the caller's step.** `MNTTRDYN.jcl:30` allocates it; the callee opens it by DD name.
4. **The caller's connection and unit of work.** The callee's statements run in the transaction the caller's
   dynamic statement ran in; neither commits. `d01`: both changes are in the snapshot. `d03`: the caller's
   statement fails, and the callee's UPDATE then fails with `-400` and the table is unchanged - under
   PostgreSQL the failed statement has ended the whole transaction; the case README says Db2 would have rolled
   back the one statement and applied the update. Register row SHR-06.
5. **Control does not come back.** `STOP RUN` in the callee ends the run unit. `CBTRNDYN.cbl:64` would print
   `CBTRNDYN: END`; that line is in none of the three golden SYSOUT files. The last lines are the callee's and
   the harness's commit line.
6. **The return code is the callee's when it sets one.** `d03`: the caller sets 8 (`:100`), the callee sets 4
   (`:232`), the recorded step code is 4. When the callee sets none, the source alone does not say whether the
   caller's 8 survives; no case records it.
7. **SYSOUT is one stream.** The caller's lines, then `CBTRNDYN: CALLING COBTUPDT`, then the callee's lines, in
   that order, in `sysout/MNTTRDYN.STEP1.txt`.
8. **The call is made even after a failed statement** (`:59-63` test the PARM length only) - `d03`.

## 4. The file image of the two tables - shared across the tracks

| Layout | Dataset | Table it is unloaded from | Read by |
|---|---|---|---|
| `CVTRA03Y` transaction type, 60 bytes: `TRAN-TYPE` X(02), `TRAN-TYPE-DESC` X(50), FILLER X(08) | `TRANTYPE.PS` -> `TRANTYPE.VSAM.KSDS` | `CARDDEMO.TRANSACTION_TYPE` (`TRANEXTR.jcl:65-84`) | `CBTRN03C:495`, by type |
| `CVTRA04Y` transaction category, 60 bytes: `TRAN-TYPE-CD` X(02), `TRAN-CAT-CD` 9(04), `TRAN-CAT-TYPE-DESC` X(50), FILLER X(04) | `TRANCATG.PS` -> `TRANCATG.VSAM.KSDS` | `CARDDEMO.TRANSACTION_TYPE_CATEGORY` (`TRANEXTR.jcl:95-115`) | `CBTRN03C:505`, by type and category |

Frozen: the two 60-byte layouts and the dataset names. Posting reads them; no stream of this hand-off writes
them. What the source proves about the path from the tables to these files, and what it does not, is in
`modernization/streams/posting/handoff.md`, section 6.

## 5. Frozen items that are not layouts

| Item | Frozen as | Owner |
|---|---|---|
| `app/**`, `fixtures/**`, `tools/parity/**`, `tools/runjcl/**`, `tools/stubs/**` | read-only for every stream (`AGENTS.md`, Rules) | the estate |
| The seven interest cases | pass unchanged after every change of any stream: `make parity CHAIN=interest` and `make parity-java` | interest |
| `java/interest/**`, `db/postgres/02-interest.sql`, `lab/**` | not edited by another stream | interest |
| `.agents/skills/carddemo-conversion-standards`, `carddemo-ticket-build`, `carddemo-ticket-verify`, `carddemo-pitfalls`, `carddemo-parity` | read-only for a stream; a stream proposes, the owner edits | target architect |

Owners of the files every track touches (`Makefile`, CI workflow, shared skills, shared schema):
`modernization/design/tracks.md`, section 7.

## 6. Register of shared changes

Every row is OPEN. Nothing here is decided; the "until answered" column is what a stream does meanwhile, and it
never changes a frozen item. A new row is added by the owner of this document on a stream's request.

| Id | Question | Raised by | Evidence | Until answered | Owner |
|---|---|---|---|---|---|
| SHR-01 | Posting updates `acct_curr_bal`, `acct_curr_cyc_credit` and `acct_curr_cyc_debit`, which `architecture.md` section 3 gives to interest alone. Who may write these columns, and is the table still a copy refreshed for each run? | posting | `CBTRN02C:547-554`; `CBACT04C:352-356`; `architecture.md` section 3 | posting works on file adapters; it writes no table of `02-interest.sql` | data architect |
| SHR-02 | How does a second Java module use the record codec that lives in `com.carddemo.interest.codec`: depend on the interest module, or move the codec to a common module in a change owned by interest? | posting | `java/interest/src/main/java/com/carddemo/interest/codec`; `skills-shared.md` section 5, "Assumed, not decided" | **blocks the Track A pilot.** No copy of the codec is made and `java/interest` is not edited by posting | target architect |
| SHR-03 | One type for the category code: `NUMERIC(4,0)` on the file side, `CHAR(4)` in `transaction_type_categor` (G09) | interest, posting | `data-map-interest.md` G09; `CVTRA04Y`; `01-transaction-type.sql` | each side keeps its type; no foreign key between them | data architect |
| SHR-04 | Is the cut-off table name `transaction_type_categor` kept in the target, or renamed to the Db2 name? | trantype | `01-transaction-type.sql:11`; `TRNTYCAT.ddl` | kept; the snapshots are named by it | data architect |
| SHR-05 | SQLCODE +100. The source has a `No records found.` branch with return code 4 for UPDATE and DELETE; the golden case `t05` shows success and return code 0; `AGENTS.md` and `docs/BASELINE.md` say the program ignores +100, which is true of INSERT only. Which behaviour is the target's? | trantype | `COBTUPDT.cbl:180-184, :210-214`; `fixtures/trantype-update/t05-delete-missing` | reproduce the golden file (success, return code 0); keep the branch behind a named switch | business owner of transaction types; mainframe application owner for what Db2 returns |
| SHR-06 | A failed statement: PostgreSQL ends the whole transaction, Db2 rolls back the statement only. Savepoint per statement, or fail the job? | trantype | `fixtures/trantype-dynamic/d03-bad-statement/README.md` | reproduce the golden file (the later UPDATE fails, table unchanged, return code 4) | target architect |
| SHR-07 | Which stream updates the account first in a cycle, posting or interest? | interest (INT-H06, ask A6), posting | `app/scheduler/CardDemo.ca7:70`; `app/scheduler/CardDemo.controlm:69` | each stream is proven on its own fixtures; no combined run is built | scheduler owner |

## 7. Not in this document

No Java, no DDL, no change to `app/`, `fixtures/`, a skill or a generated design file. `architecture.md`,
`data-map-interest.md` and `decisions-release.md` are unchanged; SHR-01 would change `architecture.md` section 3
once answered.
