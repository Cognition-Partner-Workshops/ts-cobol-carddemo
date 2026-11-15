# java/interest - interest stream (INTCALC / CBACT04C) on Java 21, Spring Batch 5, PostgreSQL 15

`interestCalcJob` replaces `INTCALC.STEP15` (`CBACT04C`). With the profile `database` it is the job of
`modernization/design/architecture.md`: `loadExtractsStep` -> `calcInterestStep` -> `exportDatasetsStep`, all through
schema `carddemo` of PostgreSQL 15 (`db/postgres/02-interest.sql`); the Spring Batch job repository is in schema `batch`.
Without the profile the job is `calcInterestStep` alone on the file adapters, as the pilot (ticket 01) built it.

| Package | What it holds |
|---|---|
| `domain` | the calculation, no framework: `InterestCalculator`, `InterestFormula`, `RunTimestamp`, `TransactionCounter` |
| `policy` | `InterestPolicy`: the switches `interest.policy.*` (FI-5) |
| `port` | FI-3: `BalanceSource`, `AccountStore`, `CardLookup`, `RateLookup`, `TransactionSink`, `RunLog` |
| `file` | file adapters of the ports in the recorded fixture format (FI-2) |
| `codec` | encoding rules E1-E4 and the five record layouts |
| `batch` | job, steps of the job, job parameters, exit codes |

```sh
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64      # any JDK 21
mvn -B -f java/interest/pom.xml verify                    # unit tests + integration tests (PostgreSQL 15 via Testcontainers, needs Docker)
make parity-java                                          # the gate (FI-6, INT-T09): all seven cases through PostgreSQL
make parity-java CASE=i01-baseline                        # one case
python3 tools/parity/compare.py interest i01-baseline --actual work/parity-java/i01-baseline/actual --label java
```

Job parameters (architecture.md section 4): `parmDate` (exactly 10 characters), `runId` (new per launch),
`inputDir`, `outputDir`, `format=fixture`. Exit code 0 for a COMPLETED job, 12 otherwise (REL-10 is open).
Evidence: [evidence/](evidence/), one directory per ticket.

## The parity gate (ticket 09)

`make parity-java` runs, for every case under `fixtures/interest` (or the one named by `CASE`):

1. the job in a `eclipse-temurin:21-jre` container on the network of the `db` service, with
   `--spring.profiles.active=database`, the PARM `2022071800` and the clock frozen at `2022-07-05 10:00:00.00`:
   the four unloaded files of `fixtures/interest/<case>/input` are loaded into the replica tables, the calculation
   reads and writes the tables, the export writes `work/parity-java/<case>/actual`;
2. the database assertions: the reconciliation report of the data move kit on the run's row of `interest_run`
   (`work/parity-java/<case>/recon.md`). Every line of RC1, RC2 and RC5 must be EQUAL; NOT AVAILABLE is not equal;
3. the comparator, `tools/parity/compare.py`, against `fixtures/interest/<case>/expected`, into
   `work/parity-java/report.md`.

A case passes when the job ends with exit code 0, the assertions hold and the comparator reports PASS. A case whose run
fails is reported as failed (`java job ended with exit code 12`, then the comparator's FAIL); it is never skipped. The
last line is `parity-java: <n> of <m> cases PASS`; the exit code is 0 only when every case passed. When the volume of the
`db` service was created before `db/postgres/02-interest.sql` existed, the target applies that script first.

| Setting | Default | What it does |
|---|---|---|
| `CASE` | every case | one case |
| `INTEREST_ARGS` | empty | extra options of the job, for example another policy value |
| `PARITY_JAVA_OUT` | `work/parity-java` | where the output and the report go |
| `INTEREST_PARM`, `INTEREST_CLOCK` | `2022071800`, `2022-07-05 10:00:00.00` | PARM and frozen clock of the run |

Negative control: the gate must notice a wrong amount. With half-even rounding it fails on i02-tie-half-cent only
(`TRAN-AMT` 12.36 against 12.35 and `ACCT-CURR-BAL` 1247.86 against 1247.85), as `make parity-naive` does:

```sh
make parity-java INTEREST_ARGS="--interest.policy.rounding=HALF_EVEN" PARITY_JAVA_OUT=work/parity-java-half-even
```

CI (`.github/workflows/parity.yml`, on every pull request): the Maven suite, the gate, the negative control (6 of 7,
the tie case failing), and the artifact `parity-java-report` with `report.md`, the `recon.md` of every case and the
negative control's report.

### Settings of the job

| Property | Values | Meaning |
|---|---|---|
| `interest.job.steps` | `calc` (default in the code), `all` (profile `database`) | `calc`: `calcInterestStep` alone. `all`: load, calculation, export; needs `interest.calc.store=jdbc` |
| `interest.calc.store` | `file` (default in the code), `jdbc` (profile `database`) | adapters of the calculation's ports |
| `interest.calc.commitInterval` | 100 | accounts per chunk (G11: a guess until the volumes are known) |
| `interest.clock.frozen` | empty | `yyyy-MM-dd HH:mm:ss.SS` freezes the run clock |
| `interest.policy.*` | see below | one switch per open decision (FI-5) |

In a job with all three steps the load creates the row of the run in `interest_run`; the calculation and the export work
on that row (`LoadedRunHandOver`), which ends EXPORTED.

### Legacy behaviour reproduced on purpose

Each is the default of its switch or has no switch because nothing else is built; none is decided here. The decision ids
are those of `modernization/rules/interest/decisions.md` (all OPEN) and `modernization/design/decisions-release.md`.

| Decision | Legacy behaviour the Java job reproduces | Switch and default | Shown by the gate |
|---|---|---|---|
| INT-Q01 | interest is cut off at the cent, towards zero: 12.355 is charged as 12.35 (no `ROUNDED`) | `interest.policy.rounding=TRUNCATE` | i02-tie-half-cent, and the negative control |
| INT-Q02 | the transaction id is the ten characters of the PARM followed by a six-digit counter that starts at 000001 in every run; the PARM is never read as a date | `interest.policy.idPrefix=PARM` | all seven cases (`TRAN-ID`) |
| INT-Q03 | both timestamps of a transaction are the clock of the run at the moment of the write, hundredths cut, followed by `0000`; not the cycle date | `interest.policy.timestampSource=RUN_CLOCK` | all seven cases, with the frozen clock |
| INT-Q04 | the account that ends the input gets its transactions, but its balance is not increased and its cycle amounts are not reset | `interest.policy.updateLastAccount=false` | all seven cases; RC5 (`interest of the last account, not added`) |
| INT-Q05 | a credit balance with a rate gets negative interest, which lowers the balance | `interest.policy.negativeBalance=CHARGE` | i06-negative-balance |
| INT-Q06 | an interest of less than one cent is still written as a 0.00 transaction | `interest.policy.zeroInterest=WRITE` | no fixture: not proven by this gate |
| INT-H01 | no fee is charged: the fee paragraph of the program is empty | none | all seven cases (no fee transaction) |
| INT-H02 | a second run with the same PARM is not refused and charges again | `rerunGuard` is not built; the behaviour is OFF | no fixture: not proven by this gate |
| INT-H04 | inactive and expired accounts are charged like any other | `interest.policy.inactiveAccounts=CHARGE` | no fixture: not proven by this gate |
| INT-R03 | a balance whose group has no rate is charged at the rate of group `DEFAULT`, with the two SYSOUT lines | none (a rule, not a decision) | i04-default-group-fallback |
| INT-R04 | a balance with rate 0 gets no transaction | none (a rule, not a decision) | i03-zero-rate-skipped |
| INT-R08, REL-10 | one failure path: the legacy message, `ABENDING PROGRAM`, exit code 12 (the value of the off-host stub; U0999 on the host) | one constant, `InterestExitCode.STOPPED` | no fixture: not proven by this gate |
| FI-2 (finding) | `rc.json` lists six steps under `INTCALC`; only STEP15 is the job's, the other five are written as 0 | none | all seven cases |
| S6.5 | the run log (`sysout/INTCALC.STEP15.txt`) is the legacy SYSOUT byte for byte, including the echo of every balance record | none | all seven cases |

Not legacy behaviour, but the target's own proposed answer where the source does not determine one: INT-H03
(`multiCard=STOP`) and INT-H05 (`overflow=STOP`).

### Not proven by this gate

No fixture case exists for: INT-Q06 (zero-amount transaction), INT-R08 (the failure path), INT-H02 (second run, restart),
INT-H03 (several cards), INT-H04 (inactive accounts), INT-H05 (overflow), INT-H06 (order of posting, reloads and the
interest run; outside the job). They are covered by unit and integration tests on synthetic data where the code exists
(see the reports under `evidence/`), not by a recorded legacy run. Also outside the gate: `format=host` (the job
parameter accepts `fixture` only), RC6 (needs a mainframe unload after the write-back), volumes beyond the fixtures
(the chunk size G11, the 1,000,000th transaction), and host compiler options (standards section 4).

## Data flow and the data move kit (tickets 05, 07, 10)

| Package | What it holds |
|---|---|
| `jdbc` | the four replica tables and `interest_transaction` as records of their copybooks: `ReplicaTable`, `ReplicaTableWriter`, `ReplicaTableReader`, `InterestTransactionReader` |
| `load` | ticket 05, step `loadExtractsStep`: `ExtractLoader` replaces `account`, `card_xref`, `tran_cat_balance`, `disclosure_group` from the four unloaded files and records RC1/RC2 in `interest_run` (status LOADED or FAILED). A record that does not decode is listed in `<outputDir>.load-rejects.txt` and no table is touched |
| `export` | ticket 07, step `exportDatasetsStep`: `DatasetExporter` writes a CALCULATED run as the SYSTRAN generation, the ACCTDATA image and `rc.json`, replaces the output directory as a whole and sets the run EXPORTED |
| `recon` | ticket 10, the data move kit: `ReconReporter` (reconciliation report), `SideBySide` (comparison of two output directories), `DataMoveKit` (command line behind the two make targets) |

Both steps take `format=fixture` (the recorded text format, FI-2) or `format=host` (fixed-length records, cp037, zoned signs
C/D, no separator).

Both steps are in the job flow with the profile `database` (ticket 09). `format=host` is handled by the loader and the
exporter but still refused as a job parameter (`InterestJobParameters`).

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
file - is NOT AVAILABLE with the reason, never EQUAL. After `make parity-java` the newest run is the one of the last case
the gate ran; after `LOAD=1` alone there is no calculated run and the RC5 lines that need `interest_transaction` are
NOT AVAILABLE.

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
