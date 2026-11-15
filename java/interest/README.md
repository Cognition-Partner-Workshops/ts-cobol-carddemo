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
empty `interest_run` column, a run that is only `LOADED`) is `NOT AVAILABLE`, never equal. Exit code 0 when every
line of RC1-RC5 is `EQUAL`, 1 otherwise, 2 when the command could not run.

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

**Staging is a stand-in.** The job has no calculation step on the tables yet (ticket 06). `STAGE=yes` (the default)
therefore loads the case with the loader (ticket 05), runs `interestCalcJob` on its file adapters, posts that result
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
| decision INT-Q04 | one of the three account-break amounts on the last ACCTDATA record |
| gap G05 | `TRAN-DESC` where the legacy value continues after the target's text |
| defect | everything else |

Only the INT-Q03 class under `TS=format` is accepted; every other difference is open. Exit code 0 with no open
difference, 1 otherwise or when nothing could be compared (`NOT AVAILABLE`), 2 when the command could not run.
