---
name: interest-ticket-build
description: Procedure to build one ticket of the interest stream (modernization/design/tickets) in java/interest - where things are, which Java and tool versions, and the exact commands to start PostgreSQL 15, build with Maven and run the unit and integration tests. Use when starting or continuing an interest ticket (INT-T02 to INT-T10), or when the build or the tests of java/interest do not run.
---

# Interest ticket: build

Commands and layout only, each run on the pilot ticket (01) in step 5a and quoted with the output that
shows success. Corrected in step 5b from what the sessions of tickets 02 to 10 reported; the quoted
outputs are still the pilot's unless a line says otherwise. What the code must be - naming, types, numbers, encodings, quirks, rule ids, tests - is
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
   Every `mvn` and every `make parity-java` below needs this setting (pitfall P1). The `export` lasts
   for one shell only: where every command starts in a new shell, set `JAVA_HOME` in the environment of
   each command.

3. **Check that nobody has the ticket already,** then branch from the base your prompt names.

   ```sh
   git fetch origin
   git ls-remote --heads origin 'demo/*'          # is there a branch for this ticket?
   gh pr list --state open --json number,title,headRefName,baseRefName
   git checkout -b <your-branch> origin/<base-branch>
   ```
   Stop and report if a branch or an open PR already covers the ticket. The exception is a repeat
   attempt of the same ticket, when your prompt says so: go on with the existing branch and the existing
   pull request, and open no second one.

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
   | `.../codec` | `RecordLayout`, `RecordCodec`, `FieldSpec`, `FieldKind`, `CodePage`, `RecordImage` (encoding rules, on `byte[]`; ticket 03 replaced the pilot's `Field`) and one `*Record` class per copybook layout |
   | `.../jdbc`, `.../load`, `.../export`, `.../recon` | table adapters of the ports (ticket 06), the extract loader (05), the dataset export (07), the reconciliation and side-by-side commands (10) |
   | `.../batch` | `InterestJobConfig` (job `interestCalcJob`, step `calcInterestStep`), `CalcInterestRun`, `InterestJobParameters`, `InterestExitCode(s)` |
   | `java/interest/src/main/resources/application.yml` | datasource (`INTEREST_DB_URL`, `INTEREST_DB_USER`, `INTEREST_DB_PASSWORD`, default `localhost:5432/carddemo`), job repository in schema `batch`, the `interest.policy.*` defaults, `interest.calc.commitInterval`, `interest.clock.frozen` |
   | `java/interest/src/test/java/com/carddemo/interest` | same packages; `Fixtures.java` reads `fixtures/interest` (Maven runs in `java/interest`, so the path is `../../fixtures/interest`); `batch/InterestCalcJobIT.java` is the integration test on `postgres:15` |
   | `java/interest/evidence/` | the pilot's three evidence files; each later ticket writes its own under `java/interest/evidence/<ticket file name>/` (see `interest-ticket-verify` step 8) |
   | `Makefile`, target `parity-java` | FI-6, the parity entry point |
   | `docker-compose.yml`, service `db` | `postgres:15`, database, user and password `carddemo`, init scripts from `db/postgres/` |
   | `db/postgres/02-interest.sql`, `db/postgres/tests/` | the tables of the interest stream in schema `carddemo` (ticket 02) and their SQL assertions (`db/postgres/tests/run.sh`) |

   The compose `db` service applies `db/postgres/02-interest.sql` only on a new volume. The
   Testcontainers `postgres:15` of an integration test does not apply `db/postgres` at all: a test that
   needs the tables copies `db/postgres/02-interest.sql` into its container as a further init script.

   The module has no JSON library at compile scope. A test that reads
   `modernization/rules/interest/expected/*.json` can use `org.json` from `spring-boot-starter-test`;
   no new dependency is needed.

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
   `-q` prints nothing on success.

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
   The first total is surefire (unit), the second failsafe (integration). Your ticket adds to both
   numbers; neither may go down. The numbers of your base are higher than the pilot's 60 and 12 (385
   and 119 with tickets 02 to 10 on `demo/05b-integration`): run the command on your base first and
   compare with that. Do not use `mvn -o`: the failsafe plugin is not in the local repository
   (`maven-failsafe-plugin:jar:3.5.4 has not been downloaded`). One unit test class while you work:
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
   and the `Makefile`; take out of that list only a path your ticket's Scope allows (pitfall P6). Most
   tickets have a narrower list. The check that decides is every changed path against the ticket's
   Allowed files (and what your prompt grants):
   ```sh
   git diff --name-only origin/<base-branch>
   ```

   `make` ends with exit code 2 for any failing recipe; the recipe's own exit code shows only in the
   line `make: *** [...] Error 1`. Evidence of a make target quotes that line.

10. **Go on with `interest-ticket-verify`:** run the fixture cases, compare, write the evidence, open the
    pull request. Do not merge it.
