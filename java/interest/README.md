# java/interest - interest stream (INTCALC / CBACT04C) on Java 21, Spring Batch 5, PostgreSQL 15

Ticket 01 (pilot): `interestCalcJob` with one step, `calcInterestStep`, on file adapters. PostgreSQL holds the
Spring Batch job repository only (schema `batch`); no business table exists yet (tickets 02, 06).

| Package | What it holds |
|---|---|
| `domain` | the calculation, no framework: `InterestCalculator`, `InterestFormula`, `RunTimestamp`, `TransactionCounter` |
| `policy` | `InterestPolicy`: the switches `interest.policy.*` (INT-Q01 to INT-Q04 in the pilot) |
| `port` | FI-3: `BalanceSource`, `AccountStore`, `CardLookup`, `RateLookup`, `TransactionSink`, `RunLog` |
| `file` | file adapters of the ports in the recorded fixture format (FI-2) |
| `codec` | encoding rules E1-E4 for the fixture code page and the five record layouts |
| `batch` | job, step, job parameters, exit codes |

```sh
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64      # any JDK 21
mvn -q -f java/interest/pom.xml verify                    # unit tests + integration tests (PostgreSQL 15 via Testcontainers, needs Docker)
make parity-java CASE=i01-baseline                        # FI-6: run the job on a fixture case and compare
python3 tools/parity/compare.py interest i01-baseline --actual work/parity-java/i01-baseline/actual --label java
```

`make parity-java` without `CASE` runs all seven cases. In the pilot i03-zero-rate-skipped (INT-R04) and
i04-default-group-fallback (INT-R03) stop with exit code 12 and leave no output; they belong to ticket 04.

Job parameters (architecture.md section 4): `parmDate` (exactly 10 characters), `runId` (new per launch),
`inputDir`, `outputDir`, `format=fixture`. Exit code 0 for a COMPLETED job, 12 otherwise (REL-10 is open).
Evidence of the last revision: [evidence/](evidence/).

## Data flow and the data move kit (tickets 05, 07, 10)

| Package | What it holds |
|---|---|
| `jdbc` | the four replica tables and `interest_transaction` as records of their copybooks: `ReplicaTable`, `ReplicaTableWriter`, `ReplicaTableReader`, `InterestTransactionReader` |
| `load` | ticket 05, step `loadExtractsStep`: `ExtractLoader` replaces `account`, `card_xref`, `tran_cat_balance`, `disclosure_group` from the four unloaded files and records RC1/RC2 in `interest_run` (status LOADED or FAILED). A record that does not decode is listed in `<outputDir>.load-rejects.txt` and no table is touched |
| `export` | ticket 07, step `exportDatasetsStep`: `DatasetExporter` writes a CALCULATED run as the SYSTRAN generation, the ACCTDATA image and `rc.json`, replaces the output directory as a whole and sets the run EXPORTED |
| `recon` | ticket 10, the data move kit: `ReconReporter` (reconciliation report), `SideBySide` (comparison of two output directories), `DataMoveKit` (command line behind the two make targets) |

Both steps take `format=fixture` (the recorded text format, FI-2) or `format=host` (fixed-length records, cp037, zoned signs
C/D, no separator).

Not done yet, and not in these tickets' files: `interestCalcJob` still starts at `calcInterestStep` on the file adapters.
`loadExtractsStep` and `exportDatasetsStep` are beans, tested in jobs of their own; putting them into the job flow, the
calculation on the tables and `format=host` as a job parameter belong to the tickets that own `batch/` (06, 08).

Open decisions, implemented as proposed and not decided here: REL-01 (files at the dataset boundary), REL-02 (the tables are
replaced on every run), REL-03 (the files are the hand-over; the mainframe catalogs SYSTRAN(+1) and writes the accounts back),
REL-08 (host files in cp037), REL-04 and REL-05 (how many side-by-side cycles, and the bar for "equal": the kit reports, it
does not decide).

`rc.json` mirrors the recorded shape (FI-2): six steps under `INTCALC`. Only `STEP15` is a step of INTCALC; `STEP05`, `STEP10`,
`STEP20`, `STEP25` and `STEP30` are steps of the harness's setup jobs that the recorder carries into the entry. The export
writes them as 0; the Java job does not run them.

### Reconciliation report

```sh
make recon-java CASE=i01-baseline            # report on the newest interest_run -> work/recon-java/i01-baseline.md
make recon-java CASE=i01-baseline LOAD=1     # first load the four tables from the case's input (trial load, TL3)
make recon-java CASE=i01-baseline RUN=42 OUT=some/output/dir FORMAT=host
```

The report compares the unloaded files of `fixtures/interest/<case>/input`, `interest_run` and the tables, and the exported
files of `OUT` (default `work/parity-java/<case>/actual`). It reads the database in one read-only transaction and writes
nothing to it. Every line is EQUAL, DIFFERENT or NOT AVAILABLE with both figures; exit code 1 when a line is DIFFERENT.

| Check | Compared |
|---|---|
| RC1 | records of each unloaded file = rows the load recorded in `interest_run` = rows in the table |
| RC2 | the five account sums, the sums of TRAN-CAT-BAL and DIS-INT-RATE and the three account-id hash totals: file = `interest_run` |
| RC3 | every row, encoded again, equals its record; differences counted per gap (G02 F sign, G03 minus zero), any other is DIFFERENT. Once the run is calculated, the three account columns the calculation updates are left out |
| RC4 | distinct keys = rows; lowest and highest key = first and last record of the file (keys shown by their last four characters) |
| RC5 | rows in `interest_transaction` = records of the exported SYSTRAN; sum of TRAN-AMT = balance change + interest of the last account, which is not added (INT-Q04); what the calculation recorded in `interest_run` = the exported files |
| RC6 | NOT AVAILABLE: needs ACCTDATA unloaded again after the write-back on the mainframe; cannot be shown off-host |
| TL4 | profile of the unloaded files: several cards (G08), mixed type codes (G06), minus zero (G03), F signs (G02), non-blank fillers, inactive accounts (INT-H04), balances without a rate (INT-R08) |

A figure that cannot be computed - no run, a FAILED load, a run that is not calculated on the tables, a missing or unreadable
file - is NOT AVAILABLE with the reason, never EQUAL. On this branch the job does not calculate on the tables, so after
`LOAD=1` the RC5 lines that need `interest_transaction` are NOT AVAILABLE; the RC5 equation is still computed from the files.

### Side by side

```sh
make sidebyside-java CASE=i01-baseline                                   # golden files of the case against work/parity-java/<case>/actual
make sidebyside-java CASE=cycle-1 LEGACY=<legacy dir> TARGET=<target dir> TS=exact
```

Compares two output directories in the recorded fixture format field by field with `tools/parity/compare.py` (unchanged; the
target reads the legacy directory in place of `fixtures/`) and writes `work/sidebyside-java/<case>/report.md`. `TS` is the
timestamp rule of SS3 and is written into the report: `format` (default) accepts a TRAN-ORIG-TS / TRAN-PROC-TS of the target
when it has the form `YYYY-MM-DD-HH.MM.SS.hh0000`, whatever its value; `exact` requires equal values. Every difference line
carries a class (SS4); exit code 1 when one is a defect.

| Class | When |
|---|---|
| `INT-Q03` | both timestamps well formed, values differ (rule `exact` only) |
| `INT-Q02` | TRAN-ID with the same six-digit counter and another ten-character prefix |
| `INT-Q01` | TRAN-AMT one cent apart |
| `G05` | TRAN-DESC with the same text and other bytes after it |
| `defect` | everything else, including a malformed timestamp, a missing file, a record count, SYSOUT and `rc.json` |

The kit cannot see the policy switches of the target run, so a difference on the last account's balance (INT-Q04) is listed
as a defect for a person to reclassify.
