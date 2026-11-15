# java/interest - interest stream (INTCALC / CBACT04C) on Java 21, Spring Batch 5, PostgreSQL 15

`interestCalcJob` replaces `INTCALC.STEP15` (`app/jcl/INTCALC.jcl` -> `app/cbl/CBACT04C.cbl`). With the profile
`tables` one launch runs three steps through PostgreSQL:

| Step | What it does | Tables (schema `carddemo`, `db/postgres/02-interest.sql`) |
|---|---|---|
| `loadExtractsStep` | decodes the four unloaded files of `inputDir` and replaces the replica tables; opens the `interest_run` row with the figures of RC1 and RC2 | `account`, `card_xref`, `tran_cat_balance`, `disclosure_group`, `interest_run` |
| `calcInterestStep` | one item is one account with its balances in key order; inserts the interest transactions, updates three account columns, writes the run log (legacy SYSOUT) | `interest_transaction`, `account`, `interest_run` (RC5 figures, `CALCULATED`) |
| `exportDatasetsStep` | writes the run to `outputDir` in the recorded format (FI-2): SYSTRAN generation, ACCTDATA image, `rc.json`; keeps the run log | `interest_run` (`EXPORTED`) |

Without the profile the job is `calcInterestStep` alone on the file adapters, as in the pilot; PostgreSQL then
holds the Spring Batch job repository only (schema `batch`).

| Setting | Values | Meaning |
|---|---|---|
| `interest.job.steps` | `calc` (default), `load-calc-export` | the steps of one launch; `load-calc-export` needs `interest.calc.store=jdbc` and is refused otherwise |
| `interest.calc.store` | `file` (default), `jdbc` | the adapters of the ports of the calculation (FI-3) |
| `--spring.profiles.active=tables` | | `load-calc-export` on `jdbc` (`src/main/resources/application-tables.yml`) |
| `interest.policy.*` | see the table of behaviours below | the policy switches (FI-5); the legacy behaviour is every default |

| Package | What it holds |
|---|---|
| `domain` | the calculation, no framework |
| `policy` | `InterestPolicy`, the only reader of `interest.policy.*` |
| `port` | FI-3: `BalanceSource`, `AccountStore`, `CardLookup`, `RateLookup`, `TransactionSink`, `RunLog` |
| `codec` | the encoding rules and one record class per copybook layout |
| `file` | file adapters of the ports in the recorded fixture format |
| `load`, `jdbc/load` | `loadExtractsStep` |
| `jdbc/calc` | JDBC adapters of the ports |
| `export`, `jdbc/export` | `exportDatasetsStep` |
| `batch` | the job and its assembly (`InterestJobConfig`, `JobSteps`, `InterestRunHandover`), `calcInterestStep`, job parameters, exit codes |

Job parameters (architecture.md section 4): `parmDate` (exactly 10 characters), `runId` (new per launch),
`inputDir`, `outputDir`, `format=fixture`. Exit code 0 for a COMPLETED job, 12 otherwise (REL-10 is open).

## Build, test, parity gate

```sh
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64      # any JDK 21
mvn -B -f java/interest/pom.xml verify                    # unit tests + integration tests (PostgreSQL 15 via Testcontainers, needs Docker)
make parity-java                                          # the gate: seven cases through PostgreSQL -> work/parity-java/report.md
make parity-java CASE=i02-tie-half-cent                   # one case
make parity-java INTEREST_OUT=work/parity-java-half-even INTEREST_OPTS=--interest.policy.rounding=HALF_EVEN   # negative control
make parity-java INTEREST_PROFILE=default                 # calcInterestStep alone on the file adapters
```

`make parity-java` (FI-6) starts the `db` service, builds the jar and, for every case under `fixtures/interest`,
launches the job once in a JRE container on the network of the `db` container with PARM `2022071800` and the clock
frozen at `2022-07-05 10:00:00.00`. For each case it

1. compares `work/parity-java/<case>/actual` with the recorded legacy run, field by field
   (`tools/parity/compare.py`: both datasets, SYSOUT, `rc.json`);
2. checks RC1, RC2 and RC5 of `modernization/design/strategy-interest.md` in the `interest_run` row of that launch:
   the loaded counts and sums against the input files, the written counts and sums against the exported files, both
   decoded with the estate's copybook decoder (`tools/parity/copybook.py`), and the sum of TRAN-AMT against the
   balance change plus the interest of the last account.

A case fails when the job does not end with exit code 0, when the comparator finds a difference or when an
assertion does not hold; a failed case is listed in the report, never left out, and the target exits non-zero.
A db volume created before `db/postgres/02-interest.sql` existed gets that script applied by the target.

The negative control is the same gate with `interest.policy.rounding=HALF_EVEN`: it fails on i02-tie-half-cent only
(TRAN-AMT 12.36 against 12.35 and the balance of account 00000002003), as `make parity-naive` does. CI
(`.github/workflows/parity.yml`) runs the Maven suite, the gate and the negative control on every pull request and
uploads both reports as the artifact `parity-java-report`.

## Legacy behaviours reproduced on purpose

Each is OPEN in `modernization/rules/interest/decisions.md` under the same id; the legacy behaviour is the default
of its switch, and nothing else is fixed or tidied (standards S5.1, S5.2).

| Decision | Legacy behaviour reproduced | Switch and default | `CBACT04C` lines | Proven by the gate |
|---|---|---|---|---|
| INT-Q01 | interest is cut off at the cent, not rounded: 12.355 is charged as 12.35 | `interest.policy.rounding` = TRUNCATE | 464-465 | i02-tie-half-cent, i07-nontie-rounding, the negative control |
| INT-Q02 | the transaction id is the PARM text plus a six-digit counter that starts at 000001 in every run | `interest.policy.idPrefix` = PARM | 176-178, 474-480 | all seven |
| INT-Q03 | both timestamps are the clock at the moment the record is written | `interest.policy.timestampSource` = RUN_CLOCK | 496-498, 613-624 | all seven (frozen clock) |
| INT-Q04 | the account that ends the input gets its transactions but is never updated | `interest.policy.updateLastAccount` = false | 188-222 | all seven; RC5 |
| INT-Q05 | a credit balance earns negative interest | `interest.policy.negativeBalance` = CHARGE | 464-467 | i06-negative-balance |
| INT-Q06 | an interest of 0.00 is still posted | `interest.policy.zeroInterest` = WRITE | 214-217 | not proven: no fixture |
| INT-H01 | no fee is calculated: the fee paragraph is empty | none (empty fee hook) | 518-520 | all seven (no fee record) |
| INT-H02 | nothing prevents a second run with the same PARM | none in the code: a launch with a new `runId` is never refused (architecture.md section 8 names `interest.policy.rerunGuard` = OFF) | 352-356, 474 | not proven: no fixture |
| INT-H04 | closed and expired accounts are charged like any other | `interest.policy.inactiveAccounts` = CHARGE | 373-390 | not proven: every fixture account is active |
| INT-R03 | a missing disclosure group falls back to DEFAULT, with the two SYSOUT lines | - (a rule, listed because the fallback writes to SYSOUT) | 416-459 | i04-default-group-fallback |
| INT-T02 | the SYSOUT is the start line, every balance record as read and the end line | - | 181, 193, 230 | all seven |
| INT-T04 | the tail of TRAN-DESC and the transaction FILLER are written as spaces (the legacy leaves them unset, G05) | - | 485-489 | all seven |
| INT-T05 | timestamp format `YYYY-MM-DD-HH.MM.SS.hh0000`, hundredths cut | - | 140-165, 613-624 | all seven |
| P4 (pitfalls) | `rc.json` lists STEP05 to STEP30 of the harness's setup jobs under INTCALC, all 0 | - | `tools/parity/record.py` | all seven |

Decided by a proposal, because the source does not determine the behaviour (not a legacy behaviour):
INT-H03 (`interest.policy.multiCard` = STOP) and INT-H05 (`interest.policy.overflow` = STOP).

## Not proven by this gate

The gate proves the as-coded candidate on the seven recorded cases and nothing else.

| Id | What | Why not | Where it is tested instead |
|---|---|---|---|
| INT-Q06 | interest of 0.00 is posted | no fixture | unit and integration tests on synthetic data |
| INT-R08 | every stop condition | no fixture has an abend | `ErrorPathsIT`, `InterestJobTablesIT.intR08_*` |
| INT-H02 | second run, restart | no fixture | `CalcInterestTablesIT`, `CalcInterestRestartIT`, `InterestJobRestartIT` |
| INT-H03 | account with several cards | no fixture | `ErrorPathsIT`, `MultiCardFirstByCardNumIT` |
| INT-H04 | inactive accounts | every fixture account is active | `InactiveAccountsSkipJobIT` |
| INT-H05 | amounts and counters larger than their fields | no fixture | `ErrorPathsIT` |
| INT-H06 | order against posting and the reloads | outside the job (scheduler) | none |
| host format | EBCDIC input and output (`format=host`, REL-08) | the job parameters accept `fixture` only | codec and export tests |
| RC3, RC4, RC6 | round trip, keys, write-back | not in the ticket; RC6 needs the mainframe | RC3 and RC4: `LoadExtractsStepIT` |

## Open items

* **Restart of the three steps (REL-11).** architecture.md section 6 repeats `loadExtractsStep` on a restart and
  lets `calcInterestStep` continue. The repeated load replaces the account table and opens a new `interest_run`
  row, so the committed updates of the first execution are lost and the export refuses the new row: the restarted
  job ends FAILED and exports nothing (`InterestJobRestartIT.rel11_restartOfTheThreeStepsExportsNothing`). Until
  REL-11 is decided, launch again from the start with a new `runId`
  (`rel11_newLaunchAfterAStopGivesTheRowsOfAnUninterruptedRun`).
* **No `rc.json` for a stopped run.** A stopped run leaves the run log only (with the tables) or nothing (file
  adapters); the comparator reports the case as failed.
* **`JdbcAccountStore` raises `WRITE_OUTPUT`** for a failed account update; `batch/DatabaseFailureStops`
  translates it to the legacy `ERROR RE-WRITING ACCOUNT FILE`.
* **`interest_run.finished_at`** is empty on a run that reached EXPORTED, and `started_at` is the run clock (frozen in parity runs).
* **The default of `interest.job.steps` is `calc` on the file adapters**, because the tests of tickets 01 to 08
  launch the job that way; production needs the profile `tables`.
* **Required check.** The workflow runs the gate on every pull request; marking the check as required is a branch
  protection setting of the repository, not a file.
* The replica tables are replaced by every launch and `interest_transaction` keeps every run (REL-12 is open).

Evidence per ticket: [evidence/](evidence/).

## Data-move kit: reconciliation report and side-by-side comparison (ticket 10)

Two commands for the trial load and the side-by-side period of
[strategy-interest.md](../../modernization/design/strategy-interest.md), section "Moving the data". The code is the
package `com.carddemo.interest.recon`; it is not part of `interestCalcJob`. The kit reports. It decides nothing:
the number of cycles and the bar for "equal" are REL-04 and REL-05, and both are open.

### `make recon-java [CASE=<case>]`

Loads and calculates the case (`make parity-java CASE=<case> INTEREST_OUT=work/recon-java/run`), then writes
`work/recon-java/<case>.md` and all cases together as `work/recon-java/report.md`. Without `CASE` it runs the seven
cases. Exit code 0 only if RC1 to RC5 are EQUAL in every report.

Each figure is computed on two sides and both are printed: the unloaded files (`inputDir`) and the exported files
(`outputDir`) on one side, `carddemo.interest_run` and the tables on the other. A line is `EQUAL`, `DIFFERENT` or
`NOT AVAILABLE`; a figure that cannot be computed (a missing file, a record on the reject list, no run row, an
empty column of `interest_run`, a database that cannot be reached) is `NOT AVAILABLE` and never equal.

| Check | What the report compares |
|---|---|
| RC1 | records of each unloaded file, rows of its table, and the count `interest_run` recorded at the load |
| RC2 | the five account sums, the sum of TRAN-CAT-BAL, the sum of DIS-INT-RATE and the three hash totals of the account ids: file against `interest_run`, and file against the table now |
| RC3 | every row, encoded again, against its source record byte for byte; G02 and G03 sign bytes are counted, any other difference fails |
| RC4 | distinct keys against rows; lowest and highest key of the table against the first and last record of the file |
| RC5 | transactions written against the records of the exported SYSTRAN; sum of TRAN-AMT against the exported file; sum of TRAN-AMT against the balance change plus the interest of the last account, which is not added (INT-Q04) |
| RC6 | `NOT AVAILABLE`: needs ACCTDATA unloaded again on the mainframe after the write-back. It cannot be shown off-host and is not part of the result |

After the calculation the table no longer holds the unloaded ACCT-CURR-BAL, ACCT-CURR-CYC-CREDIT and
ACCT-CURR-CYC-DEBIT (INT-R07). For these three RC2 compares the file with `interest_run` only, RC3 leaves them
out and says so, and RC5 covers ACCT-CURR-BAL. The report also holds the columns of the reconciliation table of
strategy-interest.md (for i01-baseline: 2 transactions, 47.00, a balance change of 10.00, 37.00 not added to the
last account), the trial-load profile of TL4 (several cards, type codes, minus zero, F signs, fillers that are not
blank, inactive accounts, balances without a rate) and the reject list of TL3.

The report reads in one read-only transaction and writes nothing to the database.

| Setting | Meaning |
|---|---|
| `RECON_LOAD=no` | do not run the job: report on what the tables of the `db` service hold now. They hold the last case that ran |
| `RECON_INPUT=<dir>` `RECON_ACTUAL=<dir>` | the unloaded files and the exported files of that run (default: `fixtures/interest/<case>/input` and `work/parity-java/<case>/actual`) |
| `RECON_RUN=<run_id>` | the row of `interest_run` to report on (default: the highest `run_id`) |
| `RECON_OUT=<dir>` | where the reports go (default `work/recon-java`) |

### `make sidebyside-java [CASE=<case>] [TS=format|exact]`

Compares a legacy run's files with a target run's files field by field through `tools/parity/compare.py`, which is
used unchanged, and writes `work/sidebyside-java/<name>/report.md` (the comparator's own report is `compare.md`
next to it). Two output directories in the recorded format (FI-2, with `manifest.json`):

```sh
make sidebyside-java LEGACY=<legacy run directory> TARGET=<target run directory> NAME=<label> TS=format
```

Without `LEGACY` and `TARGET` it compares `fixtures/interest/<case>/expected` with `work/parity-java/<case>/actual`
for one case or for all seven.

`TS` is the timestamp rule of SS3, and the report states the one that was used. `format` (the default) compares
TRAN-ORIG-TS and TRAN-PROC-TS for the format `YYYY-MM-DD-HH.MM.SS.hh0000` only: two values of that format that
differ are listed and accepted. `exact` expects the target run to have had the legacy run's clock
(`--interest.clock.frozen`), and a value that differs is a difference. Which of the two is used is not decided here.

Every difference line carries a class (SS4):

| Class | Given when |
|---|---|
| `INT-Q03` | TRAN-ORIG-TS or TRAN-PROC-TS differ and both values have the format |
| `INT-Q01` | TRAN-AMT differs by exactly one cent (truncated against rounded) |
| `INT-Q02` | TRAN-ID differs in its first ten characters (the PARM) and the counter is the same |
| `G05` | TRAN-DESC has the same text and differs only in the bytes after it |
| `defect` | everything else, including a timestamp of another format, a missing file, a record count, SYSOUT and `rc.json` |

A class other than `defect` is a proposal read from the two values; the test lead confirms it. Exit code 0 when no
difference is left after the timestamp rule, 1 when one is, 2 when nothing could be compared (the report then says
`NOT AVAILABLE`, never equal).

### Gaps of the kit

* **RC6** needs a mainframe unload after the write-back and cannot be shown off-host.
* **Host format.** The kit reads the fixture format only, as the job does (`format=host`, REL-08, is open).
* **The tables hold one case.** Each load replaces the replica tables, so a report with `RECON_LOAD=no` is about the
  last case that ran; the kit cannot tell from the database which input directory a run was loaded from.
* **`interest_run.finished_at` is empty** on an EXPORTED run and `started_at` is the run clock, so the report prints
  the status and the PARM of the run and no time.
* **A stopped run** leaves no `rc.json` and a restart of the three steps exports nothing (REL-11): the report of
  such a run shows RC5 as `NOT AVAILABLE` or `DIFFERENT`; it does not repair the run.
* **Classes are rules on single lines.** A difference that follows from another one (the balance of an account
  after a one-cent difference of its interest) is a `defect` until a person classifies it; there is no file of
  agreed classifications.
* **The comparator** checks the files the legacy directory names; a file that exists only in the target directory
  is not reported.
* With `interest.policy.updateLastAccount=true` the interest of the last account is added, so the RC5 identity as
  written in the strategy shows `DIFFERENT`; the kit does not read the policy.
