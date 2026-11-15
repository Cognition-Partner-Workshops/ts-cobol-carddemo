---
name: interest-ticket-build
description: Procedure to build one ticket of the interest stream (modernization/design/tickets) in java/interest - where things are, which Java and tool versions, and the exact commands to start PostgreSQL 15, build with Maven and run the unit and integration tests. Use when starting or continuing an interest ticket (INT-T02 to INT-T10), or when the build or the tests of java/interest do not run.
---

# Interest ticket: build

Commands and layout only, each run on the pilot ticket (01) in step 5a and quoted with the output that
shows success; text marked with a ticket number was reported by that ticket's session in step 5b. What the code must be - naming, types, numbers, encodings, quirks, rule ids, tests - is
`.agents/skills/interest-conversion-standards/SKILL.md`; read it before writing code and fill in its
section 9. Failures and their fixes: `interest-pitfalls`. Proving the ticket: `interest-ticket-verify`.

Run every command from the repository root.

## Workflow

1. **Check the tools.** Proven with: JDK 21.0.13, Maven 3.6.3, Docker Engine 29.9.2, Docker Compose
   v5.6.1, Python 3.10.12, GNU Make 4.3.

   ```sh
   docker --version && docker compose version && python3 --version && make --version | head -1
   ```
   ```
   Docker version 29.9.2, build a82e528
   Docker Compose version v5.6.1
   Python 3.10.12
   GNU Make 4.3
   ```

2. **Put Maven on a JDK 21.** `mvn -v` names the JDK Maven uses; it must say 21. On Debian/Ubuntu with
   several JDKs installed, this selects the 21 one for the current shell:

   ```sh
   export JAVA_HOME="$(dirname "$(dirname "$(update-alternatives --list java | grep -m1 -- '-21-')")")"
   mvn -v
   ```
   ```
   Apache Maven 3.6.3
   Java version: 21.0.13.0.3, vendor: Ubuntu, runtime: .../java-21-openjdk-amd64
   ```
   Every `mvn` and every `make parity-java` below needs this shell setting (pitfall P1).

3. **Check that nobody has the ticket already,** then branch from the base your prompt names.

   ```sh
   git fetch origin
   git ls-remote --heads origin 'demo/*'          # is there a branch for this ticket?
   gh pr list --state open --json number,title,headRefName,baseRefName
   git checkout -b <your-branch> origin/<base-branch>
   ```
   Stop and report if a branch or an open PR already covers the ticket.

4. **Read, in this order** (standard S1.1): the ticket file, the rules it names in
   `modernization/rules/interest/rules.yaml`, `modernization/design/architecture.md`,
   `modernization/design/data-map-interest.md`, the `app/cbl/CBACT04C.cbl` lines each rule cites, and
   `fixtures/interest/<case>/` of the cases the ticket names. Use `cat -A` on a fixture file to see
   trailing spaces and the missing final newline of `rc.json`.

5. **Know the layout the pilot created.** Everything of the stream is in one Maven module.

   | Path | What is there |
   |---|---|
   | `java/interest/pom.xml` | one module `com.carddemo:interest:0.1.0`; parent Spring Boot 3.5.6 (Spring Batch 5.2.3, PostgreSQL driver 42.7.7); `java.version` 21; Testcontainers pinned to 1.21.4; surefire runs `*Test`, failsafe runs `*IT`; jar `target/interest.jar` |
   | `java/interest/src/main/java/com/carddemo/interest/domain` | the calculation, no framework |
   | `.../policy` | `InterestPolicy`, the only reader of `interest.policy.*` |
   | `.../port` | FI-3: `BalanceSource`, `AccountStore`, `CardLookup`, `RateLookup`, `TransactionSink`, `RunLog` |
   | `.../file` | file adapters of the ports (`FileRunWorkspace`), recorded output format |
   | `.../codec` | `Field` (encoding rules) and one `*Record` class per copybook layout |
   | `.../batch` | `InterestJobConfig` (job `interestCalcJob`, step `calcInterestStep`), `CalcInterestRun`, `InterestJobParameters`, `InterestExitCode(s)` |
   | `.../domain/RunStop`, `StopReason`, `LegacyFile` | the stop classes (ticket 08): every stop goes through `RunStop`, which writes the legacy lines; never throw `InterestRunStoppedException` directly from the calculation |
   | `.../jdbc` | JDBC adapters of the ports (ticket 06); `batch/JdbcCalcStepIT` runs the step on the tables with `interest.calc.store=jdbc`; `src/test/.../jdbc/InterestTables` loads a case's input into the replica tables |
   | `db/postgres/02-interest.sql` | the six tables of the stream (ticket 02, FI-4); `db/postgres/tests/run.sh` checks them with psql. The Java tests do not load the script until tickets 05 and 06 |
   | `java/interest/src/main/resources/application.yml` | datasource (`INTEREST_DB_URL`, `INTEREST_DB_USER`, `INTEREST_DB_PASSWORD`, default `localhost:5432/carddemo`), job repository in schema `batch`, the `interest.policy.*` defaults, `interest.calc.commitInterval`, `interest.clock.frozen` |
   | `java/interest/src/test/java/com/carddemo/interest` | same packages; `Fixtures.java` reads `fixtures/interest` (Maven runs in `java/interest`, so the path is `../../fixtures/interest`); `batch/InterestCalcJobIT.java` is the integration test on `postgres:15` |
   | `java/interest/evidence/` | the pilot's three evidence files; each later ticket writes its own under `java/interest/evidence/<ticket file name>/` (see `interest-ticket-verify` step 8) |
   | `Makefile`, target `parity-java` | FI-6, the parity entry point |
   | `docker-compose.yml`, service `db` | `postgres:15`, database, user and password `carddemo`, init scripts from `db/postgres/` |

   This table is the pilot's layout plus the rows later tickets reported. Packages `load`, `export` and
   `recon` of `architecture.md` section 2 exist once tickets 05, 07 and 10 are in your base branch;
   `ls java/interest/src/main/java/com/carddemo/interest` shows what your base has.

6. **Start PostgreSQL 15.** Only the `db` service is needed; the `estate` image is not.

   ```sh
   docker compose up -d --wait db
   docker compose exec -T db psql -U carddemo -d carddemo -c "show server_version"
   ```
   ```
    Container ts-cobol-carddemo-db-1 Healthy
    15.20 (Debian 15.20-1.pgdg13+1)
   ```
   The service publishes no port to the host (pitfall P3). The integration tests do not use it: they
   start their own `postgres:15` through Testcontainers, so `mvn verify` needs Docker, not this service.

7. **Build.**

   ```sh
   mvn -q -f java/interest/pom.xml -DskipTests package && ls java/interest/target/interest.jar
   ```
   ```
   java/interest/target/interest.jar
   ```
   `-q` prints nothing on success. A command other than the job (a second main class, as the data-move
   kit of ticket 10) is started from the same jar with
   `java -cp interest.jar -Dloader.main=<class> org.springframework.boot.loader.launch.PropertiesLauncher`;
   start its context with `spring.batch.job.enabled=false`.

8. **Run the tests.** Unit and integration tests together; this is the Build line of every ticket.

   ```sh
   mvn -B -f java/interest/pom.xml verify 2>&1 | grep -E '^\[(INFO|ERROR|WARNING)\] (Tests run:|BUILD)'
   ```
   On the pilot's code this ends with:
   ```
   [INFO] Tests run: 60, Failures: 0, Errors: 0, Skipped: 0
   [INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 3.558 s -- in com.carddemo.interest.batch.InterestCalcJobIT
   [INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
   [INFO] BUILD SUCCESS
   ```
   The first total is surefire (unit), the second failsafe (integration). These figures are the
   pilot's: compare with the totals in the newest `java/interest/evidence/*/test-output.txt` of your
   base branch, not with the figures quoted here. Your ticket adds to both numbers; neither may go down. One unit test class while you work:
   ```sh
   mvn -B -f java/interest/pom.xml test -Dtest=InterestFormulaTest 2>&1 | grep -E '^\[(INFO|ERROR)\] (Tests run:|BUILD)'
   ```
   ```
   [INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
   [INFO] BUILD SUCCESS
   ```

9. **Before every commit,** undo what the comparator changed and check you stayed in scope.

   ```sh
   git checkout -- tools/parity/__pycache__
   git diff --stat origin/<base-branch> -- app fixtures tools modernization .agents db docker-compose.yml
   git status --short
   ```
   The `git diff --stat` line must print nothing for a ticket whose Allowed files are `java/interest/**`
   and the `Makefile`; take out of that list only a path your ticket's Scope allows (pitfall P6).

   Two cases the Allowed files do not settle:
   * A new step is a `@Bean Step` in the ticket's own package; test it by launching a job built in the
     test. Adding it to `InterestJobConfig` belongs to the ticket that owns `batch/**`; report it in
     section 2 of your report.
   * When a Deliverable names a file outside the Allowed files, put the text in the ticket's
     package-info and in `report.md` section 2, and name the ticket that may edit the file.

10. **Go on with `interest-ticket-verify`:** run the fixture cases, compare, write the evidence, open the
    pull request. Do not merge it.
