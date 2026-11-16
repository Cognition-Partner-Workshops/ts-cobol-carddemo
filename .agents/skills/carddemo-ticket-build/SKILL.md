---
name: carddemo-ticket-build
description: Procedure to build one ticket of any converted batch stream of the CardDemo estate in java/<stream> - which tool versions, and the exact commands to put Maven on JDK 21, start PostgreSQL 15, build, run the unit and integration tests and check the scope before a commit. Paths and commands take the stream as <stream>; the module layout and the figures of one stream are in its annex (carddemo-stream-<stream>). Use when starting or continuing a ticket of any stream, or when the build or the tests of java/<stream> do not run.
---

# CardDemo ticket: build (shared)

> **Proven on one stream only: interest.** Every command below was run in step 5c from a clean checkout with
> `<stream>` = `interest`, following this skill and `.agents/skills/carddemo-stream-interest/SKILL.md` only; the
> output quoted is that run's (`modernization/design/evidence/05c-shared-skills-check.txt`). For another stream
> the commands are unproven until its annex says otherwise.

Commands and layout only. What the code must be - naming, types, numbers, encodings, quirks, rule ids, tests - is
`.agents/skills/carddemo-conversion-standards/SKILL.md` with the annex of the stream; read both before writing
code and fill in the review checklist. Failures and their fixes: `carddemo-pitfalls`. Proving the ticket:
`carddemo-ticket-verify`.

**Placeholders.** `<stream>` is the stream's name: its chain in `tools/runjcl/chains.json`, its directory under
`fixtures/` and `modernization/rules/`, and its Maven module `java/<stream>` with the jar
`java/<stream>/target/<stream>.jar`. `<case>` is a directory under `fixtures/<stream>`. `<ticket>` is the ticket's
file name without `.md`. The annex of the stream gives every other value (section 1 of the annex).

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
   Java version: 21.0.13.0.3, vendor: Ubuntu, runtime: /usr/lib/jvm/java-21-openjdk-amd64
   ```
   Every `mvn` and the stream's Java parity target (annex) need this shell setting (pitfall P1).

3. **Check that nobody has the ticket already,** then branch from the base your prompt names.

   ```sh
   git fetch origin
   git ls-remote --heads origin 'demo/*'          # is there a branch for this ticket?
   gh pr list --state open --json number,title,headRefName,baseRefName
   git checkout -b <your-branch> origin/<base-branch>
   ```
   Stop and report if a branch or an open PR already covers the ticket.

4. **Read, in this order** (standard S1.1): the ticket file, the rules it names in
   `modernization/rules/<stream>/rules.yaml`, the architecture and the data map of the stream (annex), the
   `app/cbl` lines each rule cites, and `fixtures/<stream>/<case>/` of the cases the ticket names. Use `cat -A`
   on a fixture file to see trailing spaces and the missing final newline of `rc.json`.

5. **Know the layout.** What is the same for every stream:

   | Path | What is there |
   |---|---|
   | `java/<stream>/pom.xml` | the Maven module of the stream; its packages and versions are in the annex |
   | `java/<stream>/evidence/<ticket>/` | the evidence of one ticket (see `carddemo-ticket-verify` step 8) |
   | `fixtures/<stream>/<case>/` | `input/`, `expected/` (recorded legacy output) and a `README.md` saying what the case proves |
   | `modernization/rules/<stream>/` | `rules.yaml`, `decisions.md` |
   | `db/postgres/` | init scripts of the `db` service, run in file-name order on an empty volume |
   | `docker-compose.yml`, service `db` | `postgres:15`, database, user and password `carddemo`, init scripts from `db/postgres/` |
   | `tools/parity/compare.py` | the comparator; never edited (standard S8.1) |

6. **Start PostgreSQL 15.** Only the `db` service is needed; the `estate` image is not.

   ```sh
   docker compose up -d --wait db
   docker compose exec -T db psql -U carddemo -d carddemo -c "show server_version"
   ```
   ```
    Container carddemo-5c-db-1 Healthy
    15.20 (Debian 15.20-1.pgdg13+1)
   ```
   The service publishes no port to the host (pitfall P3).

7. **Build.**

   ```sh
   mvn -q -f java/<stream>/pom.xml -DskipTests package && ls java/<stream>/target/<stream>.jar
   ```
   Example output (interest):
   ```
   java/interest/target/interest.jar
   ```
   `-q` prints nothing on success.

8. **Run the tests.** Unit and integration tests together; this is the Build line of every ticket.

   ```sh
   mvn -B -f java/<stream>/pom.xml verify 2>&1 | grep -E '^\[(INFO|ERROR|WARNING)\] (Tests run:|BUILD)'
   ```
   The output ends with two totals and the build result. Example (interest, step 5c):
   ```
   [INFO] Tests run: 396, Failures: 0, Errors: 0, Skipped: 0
   ...
   [INFO] Tests run: 105, Failures: 0, Errors: 0, Skipped: 0
   [INFO] BUILD SUCCESS
   ```
   The first total is surefire (unit), the second failsafe (integration). Compare with the totals in the
   newest `java/<stream>/evidence/*/test-output.txt` of your base branch, not with the figures quoted here.
   Your ticket adds to both numbers; neither may go down. One unit test class while you work:
   ```sh
   mvn -B -f java/<stream>/pom.xml test -Dtest=<TestClass> 2>&1 | grep -E '^\[(INFO|ERROR)\] (Tests run:|BUILD)'
   ```
   Example (interest, the test class named in the annex):
   ```
   [INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.052 s -- in com.carddemo.interest.domain.InterestFormulaTest
   [INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
   [INFO] BUILD SUCCESS
   ```

9. **Before every commit,** undo what the comparator changed and check you stayed in scope.

   ```sh
   git checkout -- tools/parity/__pycache__
   git diff --stat origin/<base-branch> -- app fixtures tools modernization .agents db docker-compose.yml
   git status --short
   ```
   The `git diff --stat` line must print nothing for a ticket whose Allowed files are `java/<stream>/**`
   and the `Makefile`; take out of that list only a path your ticket's Scope allows (pitfall P6).

   Two cases the Allowed files do not settle:
   * A new step is a `@Bean Step` in the ticket's own package; test it by launching a job built in the
     test. Adding it to the job configuration belongs to the ticket that owns that package; report it in
     section 2 of your report.
   * When a Deliverable names a file outside the Allowed files, put the text in the ticket's
     package-info and in `report.md` section 2, and name the ticket that may edit the file.

10. **Go on with `carddemo-ticket-verify`:** run the fixture cases, compare, write the evidence, open the
    pull request. Do not merge it.
