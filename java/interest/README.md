# java/interest - interest stream (INTCALC / CBACT04C) on Java 21, Spring Batch 5, PostgreSQL 15

`interestCalcJob` replaces INTCALC.STEP15 (`app/jcl/INTCALC.jcl` -> `app/cbl/CBACT04C.cbl`). Under profile `tables`
it runs the three steps of architecture.md section 4 through PostgreSQL: `loadExtractsStep` (the unloaded files into
the replica tables) -> `calcInterestStep` (on the tables of schema `carddemo`) -> `exportDatasetsStep` (SYSTRAN,
ACCTDATA, the run log and `rc.json` in the recorded format). A step starts only when the one before it completed.
Without the profile the job is the pilot of ticket 01: `calcInterestStep` alone on file adapters, PostgreSQL holding
the Spring Batch job repository only (schema `batch`).

| Package | What it holds |
|---|---|
| `domain` | the calculation, no framework: `InterestCalculator`, `InterestFormula`, `RunTimestamp`, `TransactionCounter` |
| `policy` | `InterestPolicy`: the switches `interest.policy.*` (FI-5) |
| `port` | FI-3: `BalanceSource`, `AccountStore`, `CardLookup`, `RateLookup`, `TransactionSink`, `RunLog` |
| `file`, `jdbc` | adapters of the ports: the recorded fixture format (FI-2) and the tables (FI-4) |
| `codec` | encoding rules E1-E4 for the fixture code page and the five record layouts |
| `load`, `export` | the loader of the unloaded files and the exporter of the recorded format |
| `recon` | the data-move kit (below) |
| `batch` | job, steps, job parameters, exit codes |

```sh
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64      # any JDK 21
mvn -B -f java/interest/pom.xml verify                    # unit tests + integration tests (PostgreSQL 15 via Testcontainers, needs Docker)
make parity-java                                          # FI-6, the gate: all seven cases through the database
make parity-java CASE=i01-baseline                        # one case
make parity-java ROUNDING=HALF_EVEN PARITY_JAVA_OUT=work/parity-java-half-even   # negative control: fails on i02-tie-half-cent only
```

Job parameters (architecture.md section 4): `parmDate` (exactly 10 characters), `runId` (new per launch),
`inputDir`, `outputDir`, `format=fixture`. Exit code 0 for a COMPLETED job, 12 otherwise (REL-10 is open).
Evidence per ticket: [evidence/](evidence/).

## The parity gate (ticket 09)

`make parity-java` builds the jar and, for each case under `fixtures/interest`, runs the job with
`--spring.profiles.active=tables` in a JRE container on the network of the `db` service, with the clock frozen as
the legacy harness freezes it. A case is PASS only when all three hold:

| Check | How | Where to look |
|---|---|---|
| the job ended with exit code 0 | the exit code of the container | console, `work/parity-java/report.md` |
| the exported files equal the recorded legacy run, field by field | `tools/parity/compare.py`, unchanged | `work/parity-java/report.md` |
| RC1 to RC5 hold in `interest_run` for the run the job left | the reconciliation report of the data-move kit, read only | `work/parity-java/<case>/recon.md` |

A case whose job fails is still compared and is listed as FAIL with its exit code; no case is skipped, and `make`
ends non-zero when any case is FAIL. The table at the end of `work/parity-java/report.md` has one row per case.

CI (`.github/workflows/parity.yml`) runs the gate and the negative control on every pull request and uploads both
reports as the artifact `parity-java-report`. The negative control must end non-zero with exactly one FAIL row,
i02-tie-half-cent, as `make parity-naive` does.

### Legacy behaviour reproduced on purpose

The gate proves the candidate **as coded**: every switch below is at its default, which is what CBACT04C does.
None of these is a decision taken; each id is a row of `modernization/rules/interest/decisions.md` (all OPEN).

| Decision | Legacy behaviour the job reproduces | Switch and default | COBOL | Cases that show it |
|---|---|---|---|---|
| INT-Q01 | interest is cut off at the cent, not rounded (12.355 is charged as 12.35) | `interest.policy.rounding=TRUNCATE` | CBACT04C:464-465 | i02-tie-half-cent, i07-nontie-rounding |
| INT-Q02 | the transaction id is the 10 characters of the job PARM and a six-digit counter that starts at 000001 in every run | `interest.policy.idPrefix=PARM` | CBACT04C:474-480 | all seven |
| INT-Q03 | both timestamps are the system clock when the record is written, not the cycle date | `interest.policy.timestampSource=RUN_CLOCK` | CBACT04C:496-498, 613-624 | all seven |
| INT-Q04 | the last account of the category-balance file gets its transactions but its balance is not increased and its cycle amounts are not reset | `interest.policy.updateLastAccount=false` | CBACT04C:188-222, 339-340 | all seven |
| INT-Q05 | a credit (negative) balance with a non-zero rate gets a negative interest transaction | `interest.policy.negativeBalance=CHARGE` | CBACT04C:214-217, 464-467 | i06-negative-balance |
| INT-Q06 | interest below one cent is still written as a transaction of 0.00 | `interest.policy.zeroInterest=WRITE` | CBACT04C:214-217, 464-468 | none (unit tests only) |
| INT-H01 | no fee is calculated: the fee paragraph is empty | none (empty fee hook) | CBACT04C:518-520 | all seven |
| INT-H02 | nothing prevents a second run from charging the same interest again | none in the job (`rerunGuard` is not built) | CBACT04C:350-356 | none |
| INT-H04 | closed and expired accounts are charged like any other | `interest.policy.inactiveAccounts=CHARGE` | CBACT04C:372-376 | none (unit tests only) |

Also reproduced, with the gap of `modernization/design/data-map-interest.md` they belong to: `TRAN-DESC` after its
text and the 20-byte filler of an interest transaction are spaces, as off-host (G05, INT-T04).

Not legacy behaviour: INT-H03 (`interest.policy.multiCard=STOP`) and INT-H05 (`interest.policy.overflow=STOP`). The
source does not determine either; the job stops instead of choosing a card number or cutting digits off. Both stay
OPEN.

### Not proven by this gate

No fixture case exists for these, so `make parity-java` says nothing about them. Where a test of the Maven suite
covers the behaviour it is named; that is a test against the code's own reading of the rule, not against a recorded
legacy run.

| Id | What | Covered elsewhere |
|---|---|---|
| INT-Q06 | interest of 0.00 is still posted | unit tests of the domain |
| INT-R08 | a missing record or a file error stops the run | `ErrorPathsTablesIT`, `LoadCalcExportJobIT` (no recorded abend to compare with) |
| INT-H02 | a second run charges again | no |
| INT-H03 | account with more than one card | unit tests of the proposed STOP |
| INT-H04 | closed or expired accounts | unit tests of the domain |
| INT-H05 | amounts and counters larger than their fields | unit tests of the proposed STOP |
| INT-H06 | order against posting and the reloads | no; outside the job |

## Data-move kit (ticket 10): reconciliation report and side-by-side comparison

Package `recon`. Both commands report; they do not decide the exit of the side-by-side period (REL-04 and REL-05 are open).

```sh
make recon-java CASE=i01-baseline             # stage the case (load, calculate, export), then report
make recon-java CASE=i01-baseline STAGE=no    # report on what the tables hold now (RUN=<run_id>, default the latest run)
make sidebyside-java LEGACY=<dir> TARGET=<dir> [TS=format|exact]
```

**`recon-java`** writes `work/recon-java/<case>/recon.md` (strategy-interest.md, RC1-RC6 and the profile of TL4).
Every line has two figures, where each comes from, and `EQUAL`, `DIFFERENT` or `NOT AVAILABLE`. One figure is counted
by the report from the unloaded files or from the tables as they are now; the other is what the loader or the
exporter stored in `interest_run`. A figure that cannot be computed (a file that is missing or does not decode, an
empty `interest_run` column, a run that is only `LOADED`) is `NOT AVAILABLE`, never equal. `make` ends with 0 when every
line of RC1-RC5 is `EQUAL`; otherwise it ends non-zero, printing `Error 1` for a line that is `DIFFERENT` or
`NOT AVAILABLE` and `Error 2` when the command could not run.

| Check | Left figure | Right figure |
|---|---|---|
| RC1 | records of each unloaded file | rows of its table |
| RC2 | sums and hash totals of the unloaded files | `interest_run.loaded_*_sum` |
| RC3 | 0 allowed | file records that have no table row which encodes to the same record; G02 and G03 counted on their own lines against `interest_run` |
| RC4 | distinct keys; first and last record of the file | rows; lowest and highest key of the table |
| RC5 | rows and sum of `TRAN-AMT` in `interest_transaction` | `written_systran_count`, `written_transaction_count`; balance change + `written_interest_not_added_sum` (INT-Q04) |
| RC6 | the mainframe unload of ACCTDATA after the write-back | always `NOT AVAILABLE` off-host; not part of the result |

The report runs in one read-only transaction and writes nothing to the database. After a calculation RC3 leaves
out the three amounts the account break writes (`ACCT-CURR-BAL`, `ACCT-CURR-CYC-CREDIT`, `ACCT-CURR-CYC-DEBIT`,
INT-R07); RC5 accounts for them.

**Staging is a stand-in** that predates the joined job (ticket 09; `make parity-java` now runs the job on the tables
and then this report). `STAGE=yes` (the default) loads the case with the loader (ticket 05), runs `interestCalcJob` on its file adapters, posts that result
to `interest_transaction`, `account` and `interest_run` (`CalculatedRunStaging`), and exports with the exporter
(ticket 07). When ticket 06 is merged the staging goes and `STAGE=no` after a job run is the normal use.

**`sidebyside-java`** takes two output directories in the recorded format (FI-2: `datasets/`, `sysout/`, `rc.json`;
the legacy one with its `manifest.json`), compares them with `tools/parity/compare.py` unchanged, and writes
`work/sidebyside-java/report.md` (the comparator's own report is `compare.md` next to it). The report names the
timestamp rule used and gives every difference line a class:

| Class | When |
|---|---|
| decision INT-Q03 (accepted) | `TS=format`: `TRAN-ORIG-TS` / `TRAN-PROC-TS` differ and both are `YYYY-MM-DD-HH.MM.SS.hh0000` |
| defect | `TS=exact`: the timestamps differ at all; any malformed timestamp in either mode |
| decision INT-Q01 | `TRAN-AMT` exactly one cent apart |
| decision INT-Q02 | `TRAN-ID` with the same six-digit counter and another prefix |
| decision INT-Q04 | on the last ACCTDATA record: another `ACCT-CURR-BAL`, or a cycle amount the target has as 0.00 |
| gap G05 | `TRAN-DESC` where the legacy value continues after the target's text |
| defect | everything else |

Only the INT-Q03 class under `TS=format` is accepted; every other difference is open. `make` ends with 0 with no open
difference; otherwise non-zero, printing `Error 1` for an open difference or when nothing could be compared
(`NOT AVAILABLE`) and `Error 2` when the command could not run.
