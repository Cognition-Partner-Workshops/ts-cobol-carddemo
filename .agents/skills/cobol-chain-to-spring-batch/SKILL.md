---
name: cobol-chain-to-spring-batch
description: Convert one CardDemo COBOL/JCL batch chain to Java 21 Spring Batch on PostgreSQL under modern/, proving field-level equivalence against every golden fixture and documenting preserved legacy defects.
---

# Convert one COBOL chain to Spring Batch

## Scope and baseline

1. Work on exactly one requested chain: `posting`, `interest`, `trantype-update`,
   or `trantype-dynamic`. Leave the others alone. Read `AGENTS.md`, `REVIEW.md`,
   `.agents/skills/carddemo-parity/SKILL.md`, and the org playbook
   **COBOL batch parity standard** before starting.
2. Read the chain entry in `tools/runjcl/chains.json`, its JCL, programs,
   copybooks, fixture READMEs, and `tools/parity/record.py` and `compare.py`.
   Trace every executed step, dynamic CALL/SQL dispatch, input, output, return
   code and DB snapshot; do not infer reachability from a program's existence.
3. Run `make up && make build`, then
   `make parity CHAIN=<chain> TARGET=legacy`. Require every legacy case green
   before implementing. Coordinate shared containers and parity runs: they
   reuse datasets, databases and `work/parity/report.md`; do not run concurrently.

## First candidate: straight port

4. Add `modern/<chain>/pom.xml` as a module in `modern/pom.xml`, following
   `modern/interest`: Java 21, Spring Batch, PostgreSQL 15 via Compose `db`.
   Use a chain-specific schema for candidate data and Batch metadata, separate
   from legacy DB tables. Put explicit typed DDL in the module's
   `src/main/resources/schema.sql`, initialized by Spring: one column per named
   copybook field, NUMERIC precision/scale matching its PIC, and primary and
   alternate indexes matching the VSAM keys (including nonunique alternate
   keys). Do not store opaque fixed-record blobs as the relational model.
   Load decoded fields into rows; read key-ordered rows in the Batch step;
   update typed rows and encode them through the codec at export.
   Use the existing `java` Compose service for Maven
   and execution; the host needs neither Java 21 nor Maven. Do not change the
   estate Dockerfile's COBOL toolchain.
5. Import `fixtures/<chain>/<case>/input/*.txt` and any DB fixture inputs used
   by `record.py`. Read and write every fixed-length record through
   `modern/record-codec`. Audit **every called layout**, including existing
   layouts, against `app/cpy/*.cpy` or `tools/parity/layouts/*.cpy` per
   `REVIEW.md`: field name, offset, length, PIC, usage and sign. Named fields
   must not become FILLER. Add codec tests and job tests for normal, empty and
   error paths. Preserve overpunch signs and fixed-length padding.
6. Export the recorder's contract into a case-specific candidate directory:
   `datasets/<resolved-DSN>.txt` (one record per line, trailing spaces stripped),
   `sysout/<JOB>.<STEP>.txt`, `rc.json`, and `db/<table>.csv` for DB-chain
   snapshots. Follow `chains.json` outputs, `record.py` generation resolution,
   SYSOUT selection, return-code structure, and DB CSV order/format. The
   comparator reads the **expected** manifest; the candidate need not generate
   one. Never copy golden outputs into candidate output or read them at runtime.
7. Port the business intent directly first; do not pre-reproduce documented
   quirks. Use plain BigDecimal arithmetic (HALF_EVEN for the initial interest
   port). Freeze the clock from `chains.json`, including hundredths, and follow
   JCL parameters: pass them as Batch job parameters, defaulted from the JCL by
   the harness/run command, not literals in Java. Commit this straight version
   alone before parity fixes.
8. The Makefile defaults to `TARGET=java` when `modern/<chain>` exists, legacy
   otherwise; `TARGET=legacy` explicitly selects COBOL. Java dispatch builds
   `modern/pom.xml -pl <chain> -am package`, then runs
   `modern/<chain>/target/<chain>-0.1.0.jar <input-directory> <output-directory>`.
   Interest additionally takes `<parm-date>` as its third argument; the
   Makefile defaults `INTEREST_PARM_DATE` from `app/jcl/INTCALC.jcl`.
   Keep this entry-point/artifact contract, or update dispatch for the new
   chain without breaking others. The existing `java` service supplies Java 21.
   Optional `MAVEN_ARGS` is passed to the harness Maven build.

## Parity loop and findings

9. Run container Maven tests:
   `docker compose exec -T java mvn -Dmaven.repo.local=/estate/work/maven -f modern/pom.xml test`.
   Build and run the candidate alone on a fixture; inspect its outputs. Then
   run `make parity CHAIN=<chain>` across every case. It invokes
   `tools/parity/compare.py --label java`, appends field-level results to
   `work/parity/report.md`, and fails on differences. If screen recording is
   requested, start before the first candidate parity run and keep it through
   the all-green result; do not run an unrecorded trial first.
10. For each differing field, trace the cause to the executed COBOL statement,
    receiver PIC/usage, control flow or SQL behaviour. Check SYSOUT, return
    codes and DB CSVs too. Make **one fix per cause per commit**, with a unit
    test proving it; rerun Maven tests and all chain fixtures. Do not mask a
    layout error as a business-rule difference. Stop only when every case is
    green; record cases, findings and their causes in the PR.
    Trace abends through `9999-ABEND-PROGRAM`, `tools/stubs/CEE3ABD.cbl` and the
    runner too: local CEE3ABD returns RC 12. Emit actual failure diagnostics,
    failed STEP/maxrc and a failed Batch job, not hard-coded success. Test
    uncovered failures and label them unproven by fixture parity in the README.
11. Reproduce legacy defects rather than quietly repairing them. Name each
    defect in a code-site comment, `modern/<chain>/README.md`, and the PR,
    citing a proving fixture and the COBOL cause. Ask the owner whether the
    business wants it retained for cutover; parity is not cutover approval.
    Follow the requested base branch, review and open-PR policy.

## Worked example: interest

- The first `modern/interest` port used HALF_EVEN and updated every account.
  Recorded parity was 0/7; two causes explained all differences.
- `i02-tie-half-cent`: `COMPUTE WS-MONTHLY-INT` has no `ROUNDED` and its
  receiver is `PIC S9(09)V99`. Truncate toward zero to cents with
  `RoundingMode.DOWN` **before** adding to `WS-TOTAL-INT` or writing `TRAN-AMT`:
  12.35, not 12.36. Negative amounts also truncate toward zero;
  `i06-negative-balance` still produces -3.00.
- Every case: `1050-UPDATE-ACCOUNT` runs at an account-id break, not after EOF.
  The EOF read sets the flag, skips the body and exits the loop before the
  apparent final-update ELSE can execute. Model the account-break accumulator
  with no final flush: the final account's balance and cycle credit/debit stay
  unchanged, while its interest transactions and displayed category records
  remain. `i01-baseline` proves the final-account defect;
  `i05-multi-category` proves it with multiple categories.
- Separate fixes and tests produced a recorded rerun of 7/7 PASS, zero
  differences. Keep both defects documented and raise the cutover decision.

## Environment friction

- Docker Hub 429: pull the needed image as `mirror.gcr.io/library/<name>` and
  retag to the normal name (for example `postgres:15`, `ubuntu:22.04`, or
  `maven:3.9-eclipse-temurin-21`). If necessary use a local-only no-pull
  override; do not commit registry/pull-policy workarounds.
- Maven Central 429: create a `settings.xml` **outside the repo**, with a
  `mirrorOf` of `central` and URL
  `https://maven-central.storage-download.googleapis.com/maven2`. Copy it into
  the Java container, e.g.
  `docker compose cp ~/maven-settings.xml java:/tmp/maven-settings.xml`.
  Pass `-s /tmp/maven-settings.xml` to local Maven commands and run parity as
  `make parity CHAIN=<chain> MAVEN_ARGS='-s /tmp/maven-settings.xml'`.
  Recopy after container recreation. Do not commit settings or make CI depend
  on the mirror; CI uses Central by default.

## Never

- Edit `fixtures/*/expected`, `app/**`, copybooks, or the comparator.
- Run `make record` or rewrite golden files to make the candidate pass.
- Substitute raw byte diff or a successful build for field-level parity.
- Stage generated `.pyc` files, local mirror settings, or unrelated changes.
