# CBTRNDYN on Java 21 / Spring Boot 3.5 / Spring Batch

This independent Maven module mirrors `java/interest` on the base branch:
there is no multi-module parent or common/codec/batch/online structure there,
and packages use `com.carddemo`. No application COBOL, JCL, fixtures or goldens
are changed.

## Run

```sh
make up && make build
make parity-java-trantype-dynamic
# or one case:
make run CHAIN=trantype-dynamic CASE=d03-bad-statement IMPL=java
cd java/trantype-dynamic && mvn -B verify
```

`java-trantype-dynamic-build` packages the executable jar using the same Maven
Java 21 container/cache pattern as interest. The chain-specific
`tools/runjcl/impl/trantype-dynamic.json` defaults to **legacy**. Set
`CARDDEMO_IMPL=trantype-dynamic=java` to opt in; set it to `legacy` to roll back.
The existing IKJEFT01 dispatcher resolves the inner DSN RUN PROGRAM(CBTRNDYN)
and passes its PARM as the first argument. STDOUT is SYSOUT and the process
exit status is the step return code.

## Design

`DynamicApplication` launches job MNTTRDYN / tasklet STEP1. It consumes the
runner's fixed-record DD_DYNSQL (80 bytes) and DD_INPFILE (53 bytes) images
as Latin-1, without newline splitting. `DynamicMaintenance` assembles SQL,
executes it and dispatches the COBTUPDT dynamic CALL to Java maintenance
logic. Both use **one JDBC connection and one transaction** against the
existing PostgreSQL 15 carddemo tables. No new business schema is needed.
PGHOST/PGPORT/PGDATABASE/PGUSER/PGPASSWORD follow the estate environment.

The job uses Spring Batch's resourceless repository and transaction manager:
the JCL step is the restart boundary, not individual statements. JDBC owns
the business unit of work explicitly, avoiding chunk commits, savepoints or
Spring exception-triggered rollback before the called program runs. There
is no durable Batch metadata or restart-in-place.

## Legacy quirks preserved

* Only `--` in columns 1-2 marks a SQL comment. Other records contribute
  columns 1-72 plus one space, including blank lines; the SQL buffer is 800
  characters and the displayed line counter wraps at 100.
* The DSN attach and end-of-task commit lines are part of SYSOUT.
* The local ocesql PREPARE defers server validation until EXECUTE: a bad
  column produces `EXECUTE SQLCODE -0000000400`, not a PREPARE error.
* **d03 reproduces PostgreSQL whole-unit abort, not Db2 single-statement
  rollback.** There is no rollback/savepoint after the failed dynamic SQL.
  The subsequent COBTUPDT UPDATE fails in that same aborted transaction,
  displays SQLCODE -400, and the final COMMIT rolls the entire unit back.
* COBTUPDT's STOP RUN prevents `CBTRNDYN: END` from appearing when called.
  Its return code replaces the outer program's (d03 is 4, not 8).
* Maintenance descriptions are sent as all 50 space-padded bytes, matching
  ocesql's VARCHAR host-variable handling. PROCESSING and error lines retain
  their trailing spaces (53-byte records and 80-byte error buffers).
* SQL errors do not stop the input loop. Duplicate keys map to ocesql -403;
  other statement errors map to -400. A missing update/delete is reported
  successful, matching the off-host affected-row handling.

## Decisions outside d01-d03 coverage

* Only COBTUPDT is a supported dynamic CALL target. An unknown program fails
  explicitly instead of loading arbitrary Java classes or external commands.
* No PARM skips the call, displays CBTRNDYN: END and retains dynamic SQL's
  return code (0 or 8). An empty called input returns 0.
* Malformed partial fixed-width DD records fail explicitly. Missing DDs and
  infrastructure failures fail the job rather than masquerading as SQLCODEs.
* No SQL filtering or rewriting is imposed: DYNSQL remains operator-supplied
  SQL executed with the estate connection's privileges.

Unit tests cover normal/empty/error SQL input, fixed records, padding,
maintenance operations, unknown calls, SQLCODE mapping and continued
processing without rollback. The three golden parity cases validate real
PostgreSQL state, SYSOUT and return codes through the existing runner.
