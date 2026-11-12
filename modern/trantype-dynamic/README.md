# MNTTRDYN trantype-dynamic candidate

Java 21 / Spring Batch on PostgreSQL 15 for `MNTTRDYN -> CBTRNDYN` (dynamic
SQL `PREPARE/EXECUTE`, then dynamic `CALL` of the program named in `PARMS`,
which is `COBTUPDT`). Job `MNTTRDYN`, step `STEP1`, one tasklet.

```
trantype-dynamic <input-dir> <output-dir> [<parms>]
```

The harness takes `<parms>` from `MNTTRDYN.jcl` (`TRANTYPE_DYNAMIC_PARMS`).
Inputs `DYNSQL.txt` (LRECL 80) and `INPFILE.txt` (LRECL 53) are decoded with
`Layouts.DYNSQL` and `Layouts.TRANTYPE_UPDATE` from `modern/record-codec`; both
layouts are declared in the programs, not in copybooks, and are tested against
the COBOL source. Output is the recorder contract: `sysout/MNTTRDYN.STEP1.txt`,
`rc.json` and `db/carddemo.transaction_type*.csv`. No expected files are read.

Tables live in the isolated `trantype_dynamic` schema (`schema.sql`, a copy of
`db/postgres/01-transaction-type.sql`) and are seeded from the fixture's
`db/seed.sql`. DYNSQL statements address unqualified table names, so they
resolve there. Spring Batch metadata lives in the same schema.

`DSNRUN`/`DSNEXIT` are modelled by `MnttrdynRun`: one connection, autocommit
off, commit at end of task. Dynamic `CALL` resolves through a name -> program
map; an unknown name fails the step.

## Behaviour reproduced for parity

- `DISPLAY SQLCODE` prints a sign and ten digits: the ocesql SQLCA declares
  `SQLCODE PIC S9(9) COMP-5` and GnuCOBOL displays a 4-byte binary item at its
  full capacity (`+0000000000`).
- SQLCODEs are the Open COBOL ESQL 1.4 values (`dblib/ocpgsql.c`): `-403`
  duplicate key, a few connection/transaction codes, `-400` for every other
  PostgreSQL error. Db2 would report `-206`, `-803` and so on.
- `:INPUT-REC-DESC` is `PIC X(50)` and is bound with its trailing spaces, so
  `TR_DESCRIPTION` rows written by COBTUPDT are space-padded to 50.
- No statement savepoints (ocesql runs without `OCDB_PG_IGNORE_ERROR`). A failed
  statement aborts the PostgreSQL transaction, every later statement fails with
  `-400`, and the end-of-task `COMMIT` silently rolls back while still printing
  `DSNRUN: COMMIT AT END OF TASK` (`d03-bad-statement`). Db2 would roll back only
  the failing statement. This is the estate's semantic gap, recorded in the fixture.

## Intentional legacy defects

- **COBTUPDT ends the run unit with `STOP RUN`** (every case that CALLs it):
  `1001-READ-NEXT-RECORDS` ends with `STOP RUN`, although CBTRNDYN CALLs it as
  a subprogram. Control never returns, so `CBTRNDYN: END` is never displayed and
  the step RC is COBTUPDT's `RETURN-CODE`. CBTRNDYN's `MOVE 8 TO RETURN-CODE` for
  a failed dynamic statement is lost: `d03` ends RC 4 (from COBTUPDT's own
  failure), and a failed statement followed by an INPFILE with no SQL ends
  **RC 0**, which was confirmed against the legacy run on a scratch input (unit
  test `failedDynamicStatementRc8IsLostWhenCobtupdtRunsNoSql`). Java throws
  `StopRun` from `Cobtupdt` to reproduce it.

- **COBTUPDT ignores SQLCODE +100** (no fixture of this chain; confirmed against
  the legacy run on a scratch input, and `trantype-update/t05-delete-missing`
  shows it for the same program): ocesql reports SQLCODE 0 for an `UPDATE` or
  `DELETE` that matches no row, so the `No records found.` branches never fire;
  a missing key prints `RECORD UPDATED/DELETED SUCCESSFULLY` with RC 0. The same
  holds for a zero-row dynamic DML statement in CBTRNDYN; only a query with no
  rows gives `+0000000100`.

## Unproven by fixture parity

Unit-tested only: `PREPARE FAILED` (ocesql prepares client-side, so it does not
occur for SQL errors), empty `PARMS` (no CALL, `CBTRNDYN: END`, RC 8 kept), the
800-byte statement cut, more than 99 DYNSQL lines (`PIC 9(02)` wraps), invalid
INPFILE record type, an unknown CALL target, and the scratch-input cases above
(failed statement then no SQL, missing keys, zero-row dynamic query), which were
checked once against the legacy run but are not golden fixtures.

```
make up
docker compose exec -T java mvn -Dmaven.repo.local=/estate/work/maven -f modern/pom.xml test
make parity CHAIN=trantype-dynamic
make parity CHAIN=trantype-dynamic TARGET=legacy
```

Maven tests use the Compose PostgreSQL service and a `trantype_dynamic_test` schema.
