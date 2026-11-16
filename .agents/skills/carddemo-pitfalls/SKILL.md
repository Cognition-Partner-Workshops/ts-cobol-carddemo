---
name: carddemo-pitfalls
description: The failures that building, testing and comparing a converted batch stream of the CardDemo estate can hit whatever the stream - wrong JDK, Testcontainers against Docker 29, the db service without a port, rc.json with the harness's steps, tracked .pyc files, the appended comparator report, the missing schema, the comparator's fixed legacy side, package-private classes - each as symptom, cause and fix. The pitfalls of one stream's own code are in its annex (carddemo-stream-<stream>). Use when a Maven build, an integration test, the stream's parity target or tools/parity/compare.py fails or behaves oddly, before changing any code.
---

# CardDemo pitfalls (shared)

> **Seen on one stream only: interest.** Every pitfall here was hit while building the interest stream
> (tickets 01 to 10). It is listed here because its cause is in the machine, the repository or the harness and
> not in interest's code. None has been seen on a second stream yet.

The numbers are those of `interest-pitfalls` and do not change. P5, P8 and P10 are pitfalls of interest's own
code: they are in `.agents/skills/carddemo-stream-interest/SKILL.md` and their numbers stay free here. For each
pitfall below, the annex of interest keeps the command that reproduced it and the names the text here leaves
out. Where the fix is a conversion standard, the entry names it and stops there.

## How to use

1. Find your symptom in the Symptom lines below, then in the pitfalls of your stream's annex.
2. Apply the fix as written. Do not edit `app/`, `fixtures/` or `tools/parity` to make a symptom go away.
3. A failure that is not listed here goes into your pull request under "failures hit", so it can be added.

## P1. Maven runs on the wrong JDK

* **Symptom.** `mvn -q -f java/<stream>/pom.xml -DskipTests package` prints
  `Fatal error compiling: error: release version 21 not supported`.
* **Cause.** Without `JAVA_HOME`, Maven took another installed JDK (`mvn -v` names it); the module needs 21.
* **Fix.** `carddemo-ticket-build` step 2, in every shell, also before the stream's Java parity target.

## P2. The integration test cannot reach Docker

* **Symptom.** `mvn verify` fails in an integration test with
  `ContainerFetch Can't get Docker image: RemoteDockerImage(imageName=postgres:15, ...)`; further up the log:
  `client version 1.32 is too old. Minimum supported API version is 1.40`.
* **Cause.** Testcontainers 1.21.3, the version Spring Boot 3.5.6 manages, cannot talk to Docker Engine 29.
* **Fix.** In the module's pom: `<testcontainers.version>1.21.4</testcontainers.version>`.
  Keep that property when you touch the pom or the Spring Boot version.

## P3. The job cannot reach the `db` service from the host or from the `estate` container

* **Symptom.** `java -jar java/<stream>/target/<stream>.jar ...` on the host does not connect to the
  compose database.
* **Cause.** The `db` service of `docker-compose.yml` publishes no port (`docker compose port db 5432`
  prints `:0`), and the `estate` image has no JDK. A ticket's scope does not allow a compose change for this.
* **Fix.** Run the jar in a JRE container that shares the network of the `db` container, where
  `localhost:5432` is the `db` service: the stream's Java parity target, or by hand as its annex shows.

## P4. rc.json differs although the job ended normally

* **Symptom.** The comparator reports `rc.json | (text)` with step lines removed and only the step of the
  converted program added.
* **Cause.** The recorded `rc.json` lists, under the first job of the chain, the steps of the harness's setup
  jobs as well as the job's own (`tools/parity/record.py:131`). The file has no final newline.
* **Fix.** Standard S6.4 (recorded format). What the target should write outside the harness is an open
  finding in `modernization/design/skills.md`.

## P6. `git status` shows a changed file under `tools/parity` you never touched

* **Symptom.** ` M tools/parity/__pycache__/copybook.cpython-310.pyc` after any `compare.py` or parity target.
* **Cause.** The `.pyc` files are tracked and Python rewrites them. `make parity` does the same under
  `tools/runjcl/__pycache__`.
* **Fix.** `git checkout -- tools/parity/__pycache__` before every commit (standard S8.1); after `make parity`
  also `git checkout -- tools/runjcl/__pycache__`.

## P7. The comparator report holds old results

* **Symptom.** The Markdown report shows a case twice, or sections of an earlier run.
* **Cause.** `compare.py` appends to its report file and never replaces it: the default
  `work/parity/report.md`, and any `--report <file>` too.
* **Fix.** Remove the report file before a series of comparisons (`rm -f <out>/compare.md`).

## P9. An integration test finds no schema `carddemo` for the stream's tables

* **Symptom.** The stream's own script under `db/postgres/` alone cannot create its tables in a
  Testcontainers `postgres:15`.
* **Cause.** `CREATE SCHEMA carddemo` is only in `db/postgres/01-transaction-type.sql`.
* **Fix.** Copy `db/postgres/01-transaction-type.sql` and the stream's script into
  `/docker-entrypoint-initdb.d/` of the container.

## P11. `compare.py` cannot compare two directories

* **Symptom.** The comparator has no option for the legacy side: it always reads
  `fixtures/<chain>/<case>/expected`.
* **Cause.** `tools/parity/compare.py` takes `--actual` only.
* **Fix.** Lay the legacy directory out as `<root>/<stream>/<name>/expected` with a `manifest.json` and
  point `compare.FIXTURES` at `<root>` from your own Python call; never edit the comparator. Run that
  Python with `PYTHONDONTWRITEBYTECODE=1` (P6).

## P12. A class a later ticket needs is package-private in a package it may not edit

* **Symptom.** A later ticket writes a second copy of something that exists.
* **Cause.** The factory it needs is package-private, and the later ticket's Allowed files do not include
  that package.
* **Fix.** Make what a later ticket of the design reads public when you write it; a later ticket cannot
  widen it.

## Pitfalls of one stream

They are in the annex of the stream, numbered there. A stream pitfall whose cause turns out to be in the
machine, the repository or the harness is proposed for this skill in the pull request that found it.
