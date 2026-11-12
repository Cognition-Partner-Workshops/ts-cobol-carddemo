# INTCALC interest candidate

Java 21 / Spring Batch on PostgreSQL 15. The isolated `interest` schema stores
imported indexed datasets and generated transactions; Spring Batch stores job
metadata there too. The loader and exporter use the shared record codec.
Only synthetic fixture data is supported. Runs are serialized by the harness.

No expected files are read by the application.

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
docker compose exec -T java java -jar modern/interest/target/interest-0.1.0.jar fixtures/interest/i01-baseline/input work/run/interest/i01-baseline
make parity CHAIN=interest
make parity CHAIN=interest TARGET=legacy
```

The harness defaults to Java for a chain with a directory under `modern/` and
legacy otherwise. CI's existing `make parity` therefore builds and runs this
candidate in the Compose Java service. `parity-naive` remains the Python reference.
Maven tests use the Compose PostgreSQL service and a separate `interest_test`
schema. No host Java or Maven installation is required.
