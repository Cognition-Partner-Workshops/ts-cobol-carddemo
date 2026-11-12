# Transaction-type maintenance: straight port

Java 21 Spring Batch job MNTTRDB2 / STEP1 ports the executed IKJEFT01
dispatch to COBTUPDT. No other program or dynamic CALL is executed by this
chain. JCL has no PARM value. The harness passes the frozen chains.json clock
as a job parameter; this job performs no clock-dependent business operation.
There is no arithmetic requiring BigDecimal rounding.

Run with input directory, output directory and quoted clock as three arguments.
The Spring-initialized schema and Batch metadata use PostgreSQL schema
trantype_update, separate from carddemo and interest. Records are decoded into
typed maintenance_input rows and read in record_number order (file order matters).
The two domain tables mirror the Db2 DDL, primary keys and foreign key; there
are no VSAM alternate indexes in this chain.

The only called codec is TrantypeInput.LAYOUT. Audited against COBTUPDT's
inline WS-INPUT-REC and FD WS-INPUT-VARS: INPUT-REC-TYPE offset 0 length 1
PIC X(1), INPUT-REC-NUMBER offset 1 length 2 PIC X(2), INPUT-REC-DESC
offset 3 length 50 PIC X(50). All are unsigned DISPLAY text, with no filler.
The DCLTRTYP/DCLTRCAT SQL host declarations are not serialized datasets:
their VARCHAR length prefixes are not domain columns.

Input INPFILE.txt uses the record codec; optional input/db/seed.sql is loaded
only into the candidate schema. Outputs follow record.py: empty datasets/,
sysout/MNTTRDB2.STEP1.txt, rc.json with STEP1/maxrc, and both
db/carddemo.*.csv snapshots, exported by PostgreSQL COPY in recorder key order.

This is deliberately a business-intent straight port, not parity-approved.
Descriptions retain the full PIC X(50) host-variable padding in VARCHAR(50)
storage, matching COBTUPDT 10031/10032 and ocesql v1.4
OCESQLPutSQLVar / OCDB_TYPE_ALPHANUMERIC's fixed-length memcpy. Both the
legacy and translated DDL specify VARCHAR(50), not CHAR(50): seed descriptions
remain unpadded; only maintenance writes carry host padding. Fixtures t01,
t02, t04 and t07 prove this distinction. Missing update/delete targets
retain success (RC 0), and SQL errors report actual PostgreSQL diagnostics. No legacy
SQLCODE mapping or padding defects have been emulated.
COBTUPDT source explicitly tests +100 on UPDATE/DELETE, despite the estate
notes saying it ignores +100; the t05 README/golden description reports success.
The executed ocesql v1.4 path resolves the discrepancy: generated COBTUPDT
calls OCESQLExecParams, which calls OCDBSetResultStatus, ultimately
OCDB_PGSetResultStatus (dblib/ocpgsql.c). PGRES_COMMAND_OK always sets SQLCODE
0, even when PQcmdTuples reports zero; +100 applies to empty SELECT results,
not zero-row UPDATE/DELETE. COBTUPDT 10032/10033 consequently display success.
This preserved legacy defect is proven for DELETE by t05-delete-missing.
The same missing-UPDATE path is covered by a unit test but is **unproven by
fixture parity** (no corresponding fixture). The owner should decide whether
to retain this false-success behavior at cutover; parity retains it by default.

Fixtures: t01 adds two types; t02 updates 02; t03 deletes 03; t04 ignores
comments and adds one type; t05 deletes missing 09; t06 inserts duplicate 01;
t07 mixes add/update/delete/comment.

Tests cover ordered maintenance, empty input, duplicate insert with continuation,
missing targets, invalid operations, missing input and oversized records.
Fatal input errors export RC 12 and fail Batch; recoverable record errors
export RC 4. Uncovered fatal database outages are not proven by fixture parity.
The COBOL batch parity standard requires recorded all-case comparator results
before equivalence can be claimed; that is intentionally deferred to the next step.
