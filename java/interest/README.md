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
