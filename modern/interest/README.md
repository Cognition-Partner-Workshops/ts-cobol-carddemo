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

The final-account rewrite behaviour is not yet corrected against parity.

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
