---
name: interest-pitfalls
description: The failures the pilot ticket of the interest stream actually hit while building, testing and comparing java/interest, each as symptom, cause and fix. Use when a Maven build, an integration test, `make parity-java` or tools/parity/compare.py fails or behaves oddly on an interest ticket, before changing any code.
---

# Interest stream: pitfalls

P1 to P7 are the failures the pilot (ticket 01) hit. Each was reproduced in step 5a with the command
shown; the symptom is the output of that command. P8 to P12 were reported by tickets 02 to 10 and are
marked as not reproduced. Where the fix is a conversion standard, the entry names the
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

Pilot code only: ticket 03 replaced `codec/Field` with `RecordLayout` / `RecordCodec` / `FieldSpec` on
`byte[]`, so the reproduction below no longer compiles. The rule stays: give the codec a full-length image.

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

## Reported by tickets 02 to 10, not reproduced

Added in step 5b from what the build sessions of the fan-out reported
(`modernization/design/build-run.md` section 3.3, and the "Failures hit" of
`java/interest/evidence/<ticket>/report.md`). The symptom is the session's wording; none was run again
in step 5b.

### P8. Other tracked and untracked `.pyc` files after `make parity` (tickets 09, 10)

* **Symptom.** After `make parity`, `git status` also shows ` M tools/runjcl/__pycache__/runjcl.cpython-310.pyc`.
  After importing `tools/parity`, a new untracked `tools/parity/__pycache__/compare.cpython-310.pyc`.
* **Cause.** As P6: `runjcl.cpython-310.pyc` is tracked too; `compare.cpython-310.pyc` is not tracked,
  so `git checkout` does not remove it.
* **Fix.** `git checkout -- tools/parity/__pycache__ tools/runjcl/__pycache__` before every commit, and
  delete an untracked `.pyc` under `tools/parity/__pycache__`. Never commit either.

### P9. `make parity-naive` ends with `service "estate" is not running` (ticket 09)

* **Cause.** Only `docker compose up -d --wait db` was run; the legacy targets (`make parity`,
  `make parity-naive`) need the `estate` service.
* **Fix.** `make up` first (and `make build` on a fresh checkout).

### P10. A test compares the run-log record echo with the fixture input line (tickets 04, 08)

* **Symptom.** Expected `000000020010100010000010000{`, was the same followed by 22 spaces; in ticket 08
  an echo expected with 28 characters was 50.
* **Cause.** The legacy `DISPLAY` writes the 50-byte record with its trailing spaces; the fixture input
  lines are stored without them.
* **Fix.** The test states the 50-byte image, or takes the line from the recorded run log
  (`fixtures/interest/<case>/expected/sysout/INTCALC.STEP15.txt`). The code was right both times.

### P11. A SQL assertion on catalog text does not find the schema name (ticket 02)

* **Symptom.** An assertion on `pg_get_constraintdef` (or other catalog text) that expects
  `carddemo.<table>` fails.
* **Cause.** The catalog qualifies a table only when its schema is not on the `search_path`; user
  `carddemo` has schema `carddemo` on it.
* **Fix.** Set the `search_path` in the assertion script before reading catalog text.

### P12. `mvn -o` cannot resolve the failsafe plugin (tickets 04, 08, 10)

* **Symptom.** `Plugin org.apache.maven.plugins:maven-failsafe-plugin:3.5.4 ... could not be resolved ... in offline mode`.
* **Fix.** Run Maven without `-o`.

All seven failures in section 6 of `java/interest/evidence/PILOT-REPORT.md` are in P1 to P7.
