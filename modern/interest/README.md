# INTCALC interest candidate

Java 21 / Spring Batch on PostgreSQL 15. The isolated `interest` schema stores
imported indexed datasets and generated transactions; Spring Batch stores job
metadata there too. The loader and exporter use the shared record codec.
Only synthetic fixture data is supported. Runs are serialized by the harness.

This first port deliberately uses BigDecimal HALF_EVEN monthly interest and
updates every account, including the last one. It has not yet been corrected
against parity findings. No expected files are read by the application.

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
