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
