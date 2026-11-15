---
name: interest-pitfalls
description: The failures the pilot ticket of the interest stream actually hit while building, testing and comparing java/interest, each as symptom, cause and fix. Use when a Maven build, an integration test, `make parity-java` or tools/parity/compare.py fails or behaves oddly on an interest ticket, before changing any code.
---

# Interest stream: pitfalls

P1 to P7 are the failures the pilot (ticket 01) hit; each was reproduced in step 5a with the command
shown, and the symptom is the output of that command. P8 to P12 were reported by the sessions that
built tickets 04, 05, 07 and 10 in step 5b; each names the ticket and the evidence that shows it. Where the fix is a conversion standard, the entry names the
section of `.agents/skills/interest-conversion-standards/SKILL.md` and stops there.

## How to use

1. Find your symptom in the Symptom lines below.
2. Apply the fix as written. Do not edit `app/`, `fixtures/` or `tools/parity` to make a symptom go away.
3. A failure that is not listed here goes into your pull request under "failures hit", so it can be added.

## P1. Maven runs on the wrong JDK

* **Symptom.** `mvn -q -f java/interest/pom.xml -DskipTests package` prints
  `Fatal error compiling: error: release version 21 not supported`.
* **Cause.** Without `JAVA_HOME`, Maven took another installed JDK (`mvn -v` said
  `Java version: 17.0.21.0.3`); the module needs 21.
* **Fix.** `interest-ticket-build` step 2, in every shell, also before `make parity-java`.
* **Reproduced by:** `env -u JAVA_HOME mvn -q -f java/interest/pom.xml -DskipTests package`.

## P2. The integration test cannot reach Docker

* **Symptom.** `mvn verify` fails in `InterestCalcJobIT` with
  `ContainerFetch Can't get Docker image: RemoteDockerImage(imageName=postgres:15, ...)`; further up the log:
  `client version 1.32 is too old. Minimum supported API version is 1.40`.
* **Cause.** Testcontainers 1.21.3, the version Spring Boot 3.5.6 manages, cannot talk to Docker Engine 29.
* **Fix.** Already in `java/interest/pom.xml`: `<testcontainers.version>1.21.4</testcontainers.version>`.
  Keep that property when you touch the pom or the Spring Boot version.
* **Reproduced by:** `mvn -B -f java/interest/pom.xml verify -Dtestcontainers.version=1.21.3` (BUILD FAILURE);
  without the override: BUILD SUCCESS.

## P3. The job cannot reach the `db` service from the host or from the `estate` container

* **Symptom.** `java -jar java/interest/target/interest.jar ...` on the host does not connect to the
  compose database. On the machine of step 5a another PostgreSQL answered on `localhost:5432` and the job
  ended with `FATAL: password authentication failed for user "carddemo"`, exit code 1.
* **Cause.** The `db` service of `docker-compose.yml` publishes no port (`docker compose port db 5432`
  prints `:0`), and the `estate` image has no JDK. The ticket scope does not allow a compose change for this.
* **Fix.** Run the jar in a JRE container that shares the network of the `db` container, where
  `localhost:5432` is the `db` service: `make parity-java`, or by hand as in `interest-ticket-verify` step 4.
* **Reproduced by:** the host `java -jar` call above, and `docker compose port db 5432`.

## P4. rc.json differs although the job ended normally

* **Symptom.** The comparator reports `rc.json | (text)` with
  `-   "STEP05": 0,` ... `-   "STEP30": 0` and `+   "STEP15": 0`.
* **Cause.** The recorded `rc.json` of every interest case lists six steps under `INTCALC`; only STEP15
  is INTCALC's, the others come from the harness's setup jobs (`tools/parity/record.py:131`). The file has
  no final newline.
* **Fix.** Standard S6.4 (recorded format, FI-2). The pilot's writer is `file/RecordedReturnCodes.java`;
  what the target should write outside the harness is an open finding in `modernization/design/skills.md`.
* **Reproduced by:** comparing a copy of the i01-baseline output whose `rc.json` held STEP15 only.

## P5. `StringIndexOutOfBoundsException` from `codec/Field`

* **Symptom.** `java.lang.StringIndexOutOfBoundsException: Range [0, 10) out of bounds for length 9`
  where a record rejection was expected.
* **Cause.** `Field` was given a record image shorter than the record length. In the pilot it was test
  data one character short.
* **Fix.** Give `Field` a full-length image: every line read from a file goes through
  `RecordImage.fromLine(line, <LENGTH of the layout>)` first. Standard S3.6.
* **Reproduced by:** `new Field("TRAN-SOURCE", 0, 10).text("123456789")` in `jshell --class-path java/interest/target/classes`;
  with `RecordImage.fromLine("123456789", 10)` it returns `123456789`.

## P6. `git status` shows a changed file under `tools/parity` you never touched

* **Symptom.** ` M tools/parity/__pycache__/copybook.cpython-310.pyc` after any `compare.py` or `make parity-java`.
* **Cause.** The `.pyc` files are tracked and Python rewrites them.
* **Fix.** `git checkout -- tools/parity/__pycache__` before every commit (standard S8.1).
* **Reproduced by:** `python3 tools/parity/compare.py interest i01-baseline --actual work/parity-java/i01-baseline/actual --label java; git status --short`.

## P7. The comparator report holds old results

* **Symptom.** The Markdown report shows a case twice, or sections of an earlier run.
* **Cause.** `compare.py` appends to its report file and never replaces it: the default
  `work/parity/report.md`, and any `--report <file>` too.
* **Fix.** Remove the report file before a series of comparisons (`rm -f work/parity-java/compare.md`).
  `make parity-java` does this for `work/parity-java/report.md`.
* **Reproduced by:** running the same comparison twice; `grep -c '^## ' work/parity/report.md` printed `2`.

## P8. A test that reads JSON cannot use Jackson

* **Symptom.** `package com.fasterxml.jackson.databind does not exist` when a test is compiled.
* **Cause.** The module has no Jackson.
* **Fix.** `org.json.JSONObject` from `spring-boot-starter-test`; do not add a dependency (standard S8.3).
* **Reported by:** ticket 04, `java/interest/evidence/04-calculation-rules/report.md` section 6.

## P9. An integration test finds no schema `carddemo` for the interest tables

* **Symptom.** `db/postgres/02-interest.sql` alone cannot create its tables in a Testcontainers `postgres:15`.
* **Cause.** `CREATE SCHEMA carddemo` is only in `db/postgres/01-transaction-type.sql`.
* **Fix.** Copy `db/postgres/01-transaction-type.sql` and `02-interest.sql` into
  `/docker-entrypoint-initdb.d/` of the container, as `ExtractLoaderIT` does.
* **Reported by:** ticket 05, `java/interest/evidence/05-extract-loader/report.md` section 5.

## P10. rc.json has to be written outside package `file`

* **Symptom.** Another package cannot reuse the rc.json writer of the pilot.
* **Cause.** `file/RecordedReturnCodes` is package-private, and `file/**` belongs to another ticket.
* **Fix.** A writer of your own in your package (`export/ExportedReturnCodes`); test it against the
  recorded `rc.json` of every case, so the two writers cannot drift apart (see P4).
* **Reported by:** ticket 07, `java/interest/evidence/07-dataset-export/report.md` section 5 (seen while
  reading the code, not reproduced as a failure).

## P11. `compare.py` cannot compare two directories

* **Symptom.** The comparator has no option for the legacy side: it always reads
  `fixtures/<chain>/<case>/expected`.
* **Cause.** `tools/parity/compare.py` takes `--actual` only.
* **Fix.** Lay the legacy directory out as `<root>/interest/<name>/expected` with a `manifest.json` and
  point `compare.FIXTURES` at `<root>` from your own Python call, as the `sidebyside-java` target of the
  `Makefile` does; never edit the comparator. Run that Python with `PYTHONDONTWRITEBYTECODE=1` (P6).
* **Reported by:** ticket 10, `java/interest/evidence/10-data-move-kit/report.md` finding F1.

## P12. A class a later ticket needs is package-private in a package it may not edit

* **Symptom.** A later ticket writes a second copy of something that exists
  (`recon/TrialLoadProfile` beside `load/ExtractProfile`).
* **Cause.** The factory it needs (`ExtractProfile.of`, `RoundTrip.compare`, `ExtractDecoder`) is
  package-private, and the later ticket's Allowed files do not include that package.
* **Fix.** Make what a later ticket of the design reads public when you write it; a later ticket cannot
  widen it.
* **Reported by:** ticket 10, `java/interest/evidence/10-data-move-kit/report.md` finding F7.

## Reported, not reproduced

The pilot: none. All seven failures in section 6 of `java/interest/evidence/PILOT-REPORT.md` are above.

Ticket 05: the datasource sets `search_path` to `batch` (`connection-init-sql` in `application.yml`), so
an unqualified table name in application SQL is looked up in the job repository schema. Qualify every
application table as `carddemo.<table>`. Not reproduced: the loader was written with qualified names
from the start.
