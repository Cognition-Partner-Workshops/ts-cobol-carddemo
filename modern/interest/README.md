# INTCALC interest candidate

Java 21 / Spring Batch on PostgreSQL 15. The isolated `interest` schema stores
imported indexed datasets and generated transactions; Spring Batch stores job
metadata there too. The loader and exporter use the shared record codec.
Only synthetic fixture data is supported. Runs are serialized by the harness.

No expected files are read by the application.

## Cutover decision and follow-up

The owner approved retaining both interest truncation and the unchanged final
account exactly as COBOL does them for cutover. **After-cutover follow-up:**
KAN-249 tracks the missing final-account rewrite as a separate business change,
with explicit tests and revised acceptance criteria; do not change it in this
parity migration.

KSDS fixture loading keeps the first record for each primary key, like the
runner's generated loader. `setup/<JOB>.STEP15.txt` records processed/skipped
counts and `IDC3302I DUPLICATE KEYS SKIPPED`; `setup/<JOB>.rc.json` records
raw loader RC 4 on duplicates (0 otherwise). These are supplementary loader
logs, not the runner's utility-step return code: `_idc_repro` normalizes loader
RC 4 to utility RC 0. The recorder selects only INTCALC SYSOUT; INTCALC's
STEP15 overwrites the setup STEP15 entry in the recorded `rc.json`.

As in `record.py`, omitted input files are materialized as empty datasets.
Missing account/xref/disclosure files therefore reach the job's legacy
missing-record abends; missing category balances give a normal empty run.

`src/main/resources/schema.sql` is initialized by Spring. Typed `account`,
`card_xref`, `disclosure_group`, `tran_cat_balance` and `"transaction"` tables
mirror the named copybook fields with exact NUMERIC precision/scale. Primary
keys match the indexed source keys; card account-id is a nonunique alternate
index. Category balances are read in compound-key order. FILLER stays in the
codec, not the relational schema. Tests audit database numeric types too.

The third command-line argument becomes the `parmDate` Batch job parameter;
the fourth is the execution `clock`, including hundredths, used for both
transaction timestamps. `INTEREST_CLOCK` defaults from chains.json.
The harness defaults `INTEREST_PARM_DATE` from INTCALC.jcl's `PARM` value;
override it explicitly when needed. Java has no literal transaction prefix.

Missing account, cross-reference or default disclosure records write the
legacy diagnostics and file status 23, `ABENDING PROGRAM`, and the local
CEE3ABD stub's `USER ABEND U+0000000999` line. `rc.json` records STEP15/maxrc
12, the Batch job fails, and prior account updates/transactions are exported.
The successful end-of-program line is omitted. These paths have unit tests
but are **unproven by fixture parity**: no current interest fixture abends.

## Intentional legacy defects

- **Monthly interest truncation** (`i02-tie-half-cent`): CBACT04C's
  `COMPUTE WS-MONTHLY-INT` has no `ROUNDED`. Its `PIC S9(09)V99` receiver
  truncates toward zero to cents before adding to `WS-TOTAL-INT` and writing
  `TRAN-AMT`. BigDecimal uses `RoundingMode.DOWN` at that same point; the
  transaction is 12.35, not 12.36. `i06-negative-balance` still writes -3.00.

- **Final account not rewritten** (`i01-baseline`, and every interest case):
  `1050-UPDATE-ACCOUNT` runs on account-id breaks, not after EOF. The EOF read
  sets `END-OF-FILE` to `Y`, skipping the body; the next loop test exits before
  the outer `ELSE` can perform the update. The Java break accumulator likewise
  has no final flush. The final account's balance and cycle credit/debit stay
  unchanged, but all its interest transactions are written. `i05-multi-category`
  proves this also applies to an account with multiple categories. SYSOUT still
  displays each successfully read category record, including the final one;
  EOF itself displays no record.

```
make up
docker compose exec -T java mvn -Dmaven.repo.local=/estate/work/maven -f modern/pom.xml test
docker compose exec -T java mvn -Dmaven.repo.local=/estate/work/maven -f modern/pom.xml package -DskipTests
parm=$(sed -n "s/.*PARM='\([^']*\)'.*/\1/p" app/jcl/INTCALC.jcl)
docker compose exec -T java java -jar modern/interest/target/interest-0.1.0.jar fixtures/interest/i01-baseline/input work/run/interest/i01-baseline "$parm" "2022-07-05 10:00:00.00"
make parity CHAIN=interest
make parity CHAIN=interest TARGET=legacy
```

The harness defaults to Java for a chain with a directory under `modern/` and
legacy otherwise. CI's existing `make parity` therefore builds and runs this
candidate in the Compose Java service. `parity-naive` remains the Python reference.
Maven tests use the Compose PostgreSQL service and a separate `interest_test`
schema. No host Java or Maven installation is required.
