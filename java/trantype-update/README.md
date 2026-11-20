# COBTUPDT on Java 21 / Spring Boot 3.5 / Spring Batch / JDBC

The trantype-update chain still runs MNTTRDB2 unchanged. Its IKJEFT01
DSN RUN PROGRAM(COBTUPDT) dispatch selects this module only when requested.
tools/runjcl/impl/trantype-update.json defaults to **legacy**. Cut over by
changing that default to java; roll back with legacy.

    make up && make build
    make java-trantype-update-build
    make run CHAIN=trantype-update IMPL=java CASE=t01-add
    make parity-java-trantype-update
    make parity

Alternatively: CARDDEMO_IMPL=trantype-update=java make run CHAIN=trantype-update.
Selection is chain-scoped; interest and trantype-dynamic remain unaffected.

TrantypeApplication launches Spring Batch job MNTTRDB2 / tasklet STEP1.
UpdateProgram reads fixed 53-byte DD_INPFILE records and executes parameterized
JDBC statements directly against PostgreSQL 15's existing
carddemo.transaction_type. The category table is unchanged. No VSAM adapter
or replacement business schema is needed. Batch metadata lives in a separate
trantype_batch schema, rebuilt on each invocation (like interest's scratch
metadata); this coexistence adapter is not restartable or safe for concurrent
invocations of the same chain.

The business connection has its own transaction, committed at end of task,
independent of Batch metadata. STDOUT is exclusively the legacy SYSOUT,
including the DSNRUN attach and commit envelope bypassed by candidate dispatch.
The process exits with the program RC. Infrastructure failures fail the job;
ordinary legacy validation/SQL errors set RC 4 and continue reading.

## Quirks deliberately reproduced

| Behaviour | Implementation / coverage |
|---|---|
| Description is the entire 50-byte space-padded field, even though the column is VARCHAR. No trim. | JDBC binding; t01, t02, t07 and unit test |
| Missing DELETE reports RECORD DELETED SUCCESSFULLY, RC 0, not “No records found.” | Ignore affected-row count; t05. The estate ocesql command path reports SQLCODE 0 for PostgreSQL COMMAND_OK even for zero rows (the documented +100/deleted quirk). |
| Duplicate INSERT is ocesql SQLCODE **-403**, not Db2 -803 or PostgreSQL 23505. | SQLSTATE mapping; t06 and unit test |
| Error message is an 80-character working-storage field, including trailing spaces; SQLCODE is five characters. | error, sqlcode; t06 |
| Comments print the full PROCESSING record, then IGNORING COMMENTED LINE. Each record DISPLAY retains trailing spaces. | t04, t07 |
| A/U/D are case-sensitive; invalid types display ERROR: TYPE NOT VALID, set RC 4, and continue (9999-ABEND is not a real abend). | Source semantics; unit test |
| DSNRUN attach/open/commit messages and end-of-task commit are retained. | All t01–t07; empty-file unit test |

## Decisions on behaviour outside t01–t07

* A zero-row UPDATE also reports success: use the same PostgreSQL COMMAND_OK
  semantics as zero-row DELETE, rather than manufacturing SQLCODE +100.
* Keep one business transaction, without per-record commits or savepoints.
  A database error leaves PostgreSQL's transaction aborted; subsequent SQL
  fails and the final COMMIT rolls back the transaction, as with the attach
  driver's connection. Validation errors do not abort the transaction.
* Preserve sticky RC 4 after any error and continue processing. The 80-byte
  return message is overwritten from its start without clearing the old
  suffix, matching COBOL STRING into working storage.
* Map PostgreSQL SQLSTATEs using Open COBOL ESQL v1.4's
  OCDB_PGSetResultStatus; unknown database errors become -400.
* Input is fixed-record Latin-1, not newline-delimited text. A short final
  record is space-padded. Missing/unreadable DDs and connection/startup failures
  are infrastructure errors, not a promise of byte-identical GnuCOBOL runtime
  diagnostics; these paths are outside fixture parity.

## Tests

make parity-java-trantype-update packages with Maven/Temurin 21 in Docker
and compares all seven fixtures' SYSOUT, RC and database snapshots.
Run mvn -B verify from java/ to verify both modules; the stacked base has no
Maven wrapper, so use installed Maven with JDK 21. Unit tests cover fixed
records, padding, empty input, zero-row operations, comments, invalid types,
SQL errors, continuation and return codes. CI runs Java parity after legacy
and interest parity. No fixture or golden is regenerated.
