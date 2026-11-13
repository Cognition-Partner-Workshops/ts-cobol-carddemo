# Java batch container

All four chain jars are packaged together from this Maven reactor. The Java
runtime uses the same PostgreSQL 15 Compose service, immutable fixture inputs,
JCL parameters and field-level comparator as the COBOL harness. Candidate data
and Spring Batch metadata remain in each chain's own schema.

## Unit tests (no candidate image build or parity)

```sh
docker compose up -d --wait db java
docker compose exec -T java mvn -Dmaven.repo.local=/estate/work/maven -f modern/pom.xml test
python3 -m unittest discover -s modern -p 'test_*.py'
```

## Image and parity (after review approval)

```sh
make modern-image
make parity-container
# optional selection, without rebuilding the image:
make parity-container CHAIN=posting CASE=p02-invalid-card
# COBOL baseline, through the existing harness:
make build
make parity TARGET=legacy
```

The candidate service is profile-gated and is not built by `make up`.
`make parity-container` uses `--no-build`: it cannot silently rebuild an image
before review. It streams Java process output and every unchanged
`tools/parity/compare.py` verdict. Outputs are isolated at
`work/parity-container/<chain>/<case>/actual`; the Markdown report appends to
`work/parity-container/report.md`. Use a fresh report for a new evidence run.
The runner executes all fixtures by default and returns nonzero if a Java
process or comparison fails. A process success is **not** a parity claim.

The image copies only Java sources/jars and the runner. The Compose mounts for
`fixtures/`, `app/` and `tools/` are read-only; only `work/` is writable.
Java jobs read fixture inputs, not expected outputs; only the comparator reads
goldens. Python bytecode writes are disabled.

Optional local Maven mirror settings can be provided as a BuildKit
`maven_settings` secret via a local Compose override; never commit those settings.

The interest last-account defect remains preserved for cutover
([KAN-249](https://partner-workshops-2.atlassian.net/browse/KAN-249)).
No changes to that job are required by this container.
