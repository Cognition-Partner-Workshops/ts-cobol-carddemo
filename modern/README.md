# Four-chain Spring Batch integration

Java 21 jobs run in `modern/Dockerfile` against the Compose PostgreSQL 15
service, using the same read-only fixture inputs as the COBOL harness. The
runtime image contains all four executable jars and `/opt/carddemo/run-job`.
It contains no fixtures or expected outputs. Jobs use isolated schemas:
`posting`, `interest`, `trantype_update`, and `trantype_dynamic` (see each
module's application properties). Runs are serialized by the harness.

```sh
make up
make build
make parity TARGET=legacy
docker compose exec -T java mvn -Dmaven.repo.local=/estate/work/maven -f modern/pom.xml test
make parity TARGET=java
```

Java parity automatically builds the runtime image and starts `candidate`
and `estate`; PostgreSQL health is checked first. No host Maven or Java is
needed. `CHAIN` and `CASE` select a subset. With no `TARGET`, converted chains
default to Java. All outputs still go through `tools/parity/compare.py` in
`estate`, including SYSOUT, return codes, DB snapshots and decoded datasets.
CI's existing `make parity` exercises this container path.

`MODERN_BUILD_ARGS` passes optional arguments to `docker build`. For a restricted
network, a local Maven settings file can be supplied as a BuildKit secret:
`MODERN_BUILD_ARGS='--secret id=maven_settings,src=/absolute/local/settings.xml'`.
Settings and credentials must remain outside the checkout and image.

Parameters are derived from the existing JCL/chain metadata by Make:
`POSTING_CLOCK`, `POSTING_SORT_START`, `POSTING_SORT_END`, `INTEREST_PARM_DATE`, `INTEREST_CLOCK`,
`TRANTYPE_CLOCK`, and `TRANTYPE_DYNAMIC_PARMS` can be overridden explicitly.

The integration combines the still-open interest (#158), trantype-update
(#159) and trantype-dynamic (#160) branches and adds posting. See each module's
README for preserved behavior. Interest retains CBACT04C's last-account defect;
this integration's cutover decision has been raised with the owner. Parity
does not constitute approval to retain business defects at cutover.

The first recorded Java container comparison was **18/24 PASS**: all seven
interest, seven trantype-update, three trantype-dynamic and posting's empty-day
case passed. Posting p01–p06 exposed timestamp separators, a dropped named
reject reason field, edited amount widths/signs, and CBTRN03C's stale EOF
record. Those causes and their proving cases are described in `posting/README.md`.

Only synthetic data is supported. The database host and development credentials
match the local harness; this image is a migration/parity runner, not a
production deployment configuration.
