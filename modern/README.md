# Java nightly batch container

The four converted chains share `record-codec` for fixed-length dataset records.
The container builds all modules and runs their unit tests with Java 21.

```sh
make modern-image
make up
make build
make parity TARGET=java
# Or one case:
make parity TARGET=java CHAIN=interest CASE=i02-tie-half-cent
```

Java jobs run in the `modern` Compose service; the copybook-driven comparator
still runs in `estate`. Both use the same `db` PostgreSQL service and fixtures
mounted at `/estate`. Candidate outputs land in
`work/parity/<chain>/<case>/java`; the report is `work/parity/report.md`.
Cases execute sequentially because the chains reset shared database tables.

`make modern-build` retains the estate-hosted Maven build for development.
`make parity TARGET=legacy` runs only the legacy harness.
Set `JAVA_RUNNER=estate` to use locally built jars instead of the Java container.
Image registries can be overridden with `MODERN_MAVEN_IMAGE` and
`MODERN_JAVA_IMAGE` when building through Compose.
