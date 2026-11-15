---
name: interest-pitfalls
description: The failures the pilot ticket of the interest stream actually hit while building, testing and comparing java/interest, each as symptom, cause and fix. Use when a Maven build, an integration test, `make parity-java` or tools/parity/compare.py fails or behaves oddly on an interest ticket, before changing any code.
---

# Interest stream: pitfalls

Only failures the pilot (ticket 01) hit. Each was reproduced in step 5a with the command shown; the
symptom is the output of that command. Where the fix is a conversion standard, the entry names the
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

## P8. Catalog text is unqualified for the role `carddemo`

* **Symptom.** SQL assertions that compare with `carddemo.account` fail with `(nothing)` or with
  `FOREIGN KEY (run_id) REFERENCES interest_run(run_id)`; first run of `db/postgres/tests/run.sh`:
  `43 assertions, 9 failed`, e.g. `FAIL  keys: primary key of account is (acct_id) / (nothing)`.
* **Cause.** The role has schema `carddemo` on its search_path (`"$user", public`), so `regclass` text and
  `pg_get_constraintdef()` leave the schema out.
* **Fix.** `SET LOCAL search_path = pg_catalog;` in the test transaction. After it: `43 assertions, 0 failed`.
* **Reported by:** ticket 02-schema (step 5b, wave 1).

## P9. A test cannot read `modernization/rules/interest/expected/<case>.json`

* **Symptom.** `NullPointerException: Cannot invoke "java.util.List.iterator()" because the return value of "java.util.Map.get(Object)" is null`.
* **Cause.** Spring Boot's `BasicJsonParser` does not read the nested, indented files.
* **Fix.** `com.jayway.jsonpath.JsonPath.parse(text).read("$")`; `json-path` is on the test classpath through
  `spring-boot-starter-test`, no new dependency.
* **Reported by:** ticket 04-calculation-rules (`PolicySwitchFixtureTest`; 63 of 63 pass with JsonPath).

## P10. The expectation files give the SYSOUT record echo without trailing spaces

* **Symptom.** `run log ==> expected "000000020010100010000010000{" but was "000000020010100010000010000{                      "`.
* **Cause.** `run.sysout.value` in `modernization/rules/interest/expected/<case>.json` is trimmed; the recorded
  SYSOUT has the 50-byte record.
* **Fix.** Compare with the expectation file after `stripTrailing`; compare byte for byte only with
  `fixtures/interest/<case>/expected/sysout`.
* **Reported by:** ticket 04-calculation-rules (`PolicySwitchFixtureTest`).

## P11. `mvn -o` fails on a machine that never built the module

* **Symptom.** `mvn -q -o -DskipTests compile` ends with `maven-failsafe-plugin:jar:3.5.4 has not been downloaded`.
* **Cause.** Offline mode with an empty local repository.
* **Fix.** Run once without `-o`.
* **Reported by:** ticket 04-calculation-rules.

## P12. `TransactionRecord.decode` takes a `RecordOrigin`

* **Symptom.** `method decode in class com.carddemo.interest.codec.TransactionRecord cannot be applied to given types`
  when a test decodes a transaction line.
* **Cause.** Since ticket 03 the method has a second parameter.
* **Fix.** `TransactionRecord.decode(image, RecordOrigin.IN_MEMORY)`.
* **Reported by:** ticket 04-calculation-rules (second session, after merging the wave 1 combination).

## P13. The job's connections have `search_path = batch`; `InterestCalcJobIT` mounts only the first init script

* **Symptom.** An integration test on the tables fails with `relation "carddemo.account" does not exist`, or a
  statement without a schema name does not find a replica table.
* **Cause.** `application.yml`: `connection-init-sql: CREATE SCHEMA IF NOT EXISTS batch; SET search_path TO batch`.
  The container of `InterestCalcJobIT` mounts `db/postgres/01-transaction-type.sql` only.
* **Fix.** Name the schema in every statement (`carddemo.account`) and mount `db/postgres/02-interest.sql` as a
  second init script (`/docker-entrypoint-initdb.d/02-interest.sql`), as `ReplicaTables.postgres()` does.
* **Reported by:** tickets 05-extract-loader and 06-calc-step-jdbc. Neither reproduced the failure; both wrote
  their tests with the fix from the start.

## Reported by the pilot, not reproduced

None. All seven failures in section 6 of `java/interest/evidence/PILOT-REPORT.md` are above.
