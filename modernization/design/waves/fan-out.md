# Step 5b - fan-out of the interest tickets: wave plan and results

Coordinator session: https://partner-workshops.devinenterprise.com/sessions/03a775612d734af78ea527e88133b769
Integration branch: `demo/05b-integration`, created from `demo/05a-skills` at `6f6c1a6`. Nothing is merged; every
ticket pull request has base `demo/05b-integration` and stays open.

## 1. Wave plan (written before any wave started)

Pre-flight on `demo/05a-skills`: the four skills `interest-conversion-standards`, `interest-ticket-build`,
`interest-ticket-verify`, `interest-pitfalls` are present under `.agents/skills/`. Ticket 01 (pilot) is built
(`demo/05-pilot`, PR #174) and is not run again. No `demo/05-<NN-slug>` branch and no open PR covered tickets 02-10.

Dependencies are those of `modernization/design/tickets/README.md`. A wave is the set of tickets whose dependencies
are complete; **if two tickets of one wave are allowed to touch the same file, one moves to the next wave.** Taken
literally (the reviewer chose this over narrowing the tickets' scopes) the Allowed files overlap much more than the
dependency graph:

* 04, 06 and 08 allow `java/interest/src/test/**`, which contains `src/test/**/codec/**` (03), `src/test/**/load/**` (05)
  and `src/test/**/export/**` (07);
* 05, 06 and 07 allow `java/interest/src/main/java/**/jdbc/**`; 06 and 08 allow `**/batch/**`; 04 and 08 allow `**/domain/**`;
* 09 and 10 allow the `Makefile` and `java/interest/README.md`.

So only 02 and 03 can share a wave. Order after that follows the longest dependency chain (04 -> 06 -> 08) first.

| Wave | Ticket | Branch | Depends on | Allowed files (from the ticket; plus `java/interest/evidence/<ticket>/**`) | Why it is in this wave |
|---|---|---|---|---|---|
| 1 | 02-schema | `demo/05-02-schema` | - | `db/postgres/02-interest.sql`, `db/postgres/tests/**`, `docker-compose.yml` (mount only) | no dependency |
| 1 | 03-record-codec | `demo/05-03-record-codec` | 01 | `java/interest/src/main/java/**/codec/**`, `src/test/**/codec/**`, `src/main/java/**/file/**` | no file in common with 02 |
| 2 | 04-calculation-rules | `demo/05-04-calculation-rules` | 01 | `src/main/java/**/domain/**`, `**/policy/**`, `src/test/**` | moved out of wave 1: `src/test/**` contains 03's `src/test/**/codec/**` |
| 3 | 06-calc-step-jdbc | `demo/05-06-calc-step-jdbc` | 02, 04 | `src/main/java/**/jdbc/**`, `**/batch/**`, `src/test/**` | waits on the schema and 04; overlaps 05 and 07 (`jdbc/**`, tests) |
| 4 | 08-error-paths | `demo/05-08-error-paths` | 06 | `src/main/java/**/domain/**`, `**/batch/**`, `src/test/**` | waits on 06; overlaps 05 and 07 (tests) |
| 5 | 05-extract-loader | `demo/05-05-extract-loader` | 02, 03 | `src/main/java/**/load/**`, `**/jdbc/**` (replica writes), `src/test/**/load/**` | ready after wave 1, held back by the overlaps with 04, 06, 08; overlaps 07 (`jdbc/**`) |
| 6 | 07-dataset-export | `demo/05-07-dataset-export` | 02, 03 | `src/main/java/**/export/**`, `**/jdbc/**` (reads), `src/test/**/export/**` | as 05 |
| 7 | 09-parity-gate | `demo/05-09-parity-gate` | 05, 06, 07, 08 | `Makefile` (parity-java), `.github/workflows/*.yml` (new steps), `java/interest/README.md`, `src/main/resources/**` | final gate, alone |
| 8 | 10-data-move-kit | `demo/05-10-data-move-kit` | 05, 07 | `src/main/java/**/recon/**`, `src/test/**/recon/**`, `java/interest/README.md`, `Makefile` (recon-java, sidebyside-java) | final gate, alone (shares `Makefile` and README with 09) |

Frozen for every child, for the whole wave: `app/**`, `fixtures/**`, `tools/**` (comparator), `.agents/skills/**`,
`modernization/**`, `java/interest/pom.xml` (build file), `java/interest/src/main/java/**/port/**` (FI-3), the
`interest.policy.*` switch names (FI-5), `db/postgres/01-transaction-type.sql`, the pilot's evidence files, other
tickets' evidence directories. Frozen for every ticket but its owner: the schema `db/postgres/02-interest.sql` and
`db/postgres/tests/**` (owner 02, wave 1 only), the record classes `codec/*Record.java` (owner 03, wave 1 only), the
`Makefile` (owners 09 and 10, their targets only), `docker-compose.yml` (02), `.github/**` (09). A child that needs
one changed stops and reports; the coordinator decides between waves.

How a wave runs:

* Script `modernization/design/waves/wave-<N>.py`, started with `run_workflow`: one child session per ticket, at most
  four at a time, a failed agent is retried once, a stopped one (frozen file needed) is not retried.
* **Combine:** each ticket branch is brought onto `demo/05b-integration` with `git merge --squash` (one commit per
  ticket naming the branch and its head). A plain merge or fast-forward would make the ticket's head reachable from
  its PR base, and GitHub would then close the ticket PR as merged; the squash keeps every ticket PR open and unmerged.
* **Combined check** (`modernization/design/waves/combined-check.sh <N>`, output in `combined-check-wave-<N>.txt`):
  `make reset`, `make up`, `make build`; `mvn -B -f java/interest/pom.xml clean verify`; every case under
  `fixtures/interest` through `make parity-java` and `tools/parity/compare.py`; `make parity` (legacy lane).
  A wave is complete when: build and all tests pass; `make parity` is all PASS; no case that passed on the previous
  integration revision fails; every case named by the wave's tickets passes. (Baseline on `6f6c1a6`: 60 + 12 tests,
  parity-java 5 of 7 - i03 and i04 stop with exit 12 until ticket 04 - and `make parity` all PASS.)
* Skill corrections reported by children are applied between waves to the three procedure skills, one commit each.
  `interest-conversion-standards` is not changed; proposals against it are collected in section 5.

## 2. Results

Filled in wave by wave.
