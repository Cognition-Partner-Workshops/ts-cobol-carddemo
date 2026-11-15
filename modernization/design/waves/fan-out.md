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

Taken literally that gives eight near-serial waves (02+03, 04, 06, 08, 05, 07, 09, 10). That plan was chosen first and
wave 1 was started with 02 and 03 (`wave-1.py`). Ten minutes later the reviewer changed the choice to **narrowed
scopes**: for the length of its wave a ticket's Allowed files are cut down to a subset that no other ticket of the
wave may touch (nothing is widened). 04 was then added to the running wave 1 with its own script (`wave-1b.py`);
02 and 03 were not disturbed. The plan in force:

| Wave | Ticket | Branch | Depends on | Allowed files in this wave (plus `java/interest/evidence/<ticket>/**`) | Narrowed from the ticket file |
|---|---|---|---|---|---|
| 1 | 02-schema | `demo/05-02-schema` | - | `db/postgres/02-interest.sql`, `db/postgres/tests/**`, `docker-compose.yml` (mount only) | - |
| 1 | 03-record-codec | `demo/05-03-record-codec` | 01 | `java/interest/src/main/java/**/codec/**`, `src/test/**/codec/**`, `src/main/java/**/file/**` | - |
| 1 | 04-calculation-rules | `demo/05-04-calculation-rules` | 01 | `src/main/java/**/domain/**`, `**/policy/**`, `src/test/**` except `src/test/**/codec/**` | tests: minus 03's codec tests |
| 2 | 05-extract-loader | `demo/05-05-extract-loader` | 02, 03 | `src/main/java/**/load/**`, `**/jdbc/load/**`, `src/test/**/load/**` | `jdbc/**` -> `jdbc/load/**` |
| 2 | 06-calc-step-jdbc | `demo/05-06-calc-step-jdbc` | 02, 04 | `src/main/java/**/jdbc/**` except `jdbc/load/**`, `jdbc/export/**`; `**/batch/**`; `src/test/**` except `**/load/**`, `**/export/**`, `**/codec/**` | `jdbc/**` and tests minus 05's and 07's parts |
| 2 | 07-dataset-export | `demo/05-07-dataset-export` | 02, 03 | `src/main/java/**/export/**`, `**/jdbc/export/**`, `src/test/**/export/**` | `jdbc/**` -> `jdbc/export/**` |
| 3 | 08-error-paths | `demo/05-08-error-paths` | 06 | `src/main/java/**/domain/**`, `**/batch/**`, `src/test/**` | - (alone) |
| 4 | 09-parity-gate | `demo/05-09-parity-gate` | 05, 06, 07, 08 | `Makefile` (parity-java), `.github/workflows/*.yml` (new steps), `java/interest/README.md`, `src/main/resources/**` | - (final gate, alone) |
| 5 | 10-data-move-kit | `demo/05-10-data-move-kit` | 05, 07 | `src/main/java/**/recon/**`, `src/test/**/recon/**`, `java/interest/README.md`, `Makefile` (recon-java, sidebyside-java) | - (final gate, alone; shares `Makefile` and README with 09) |

Wave 2 starts only after the schema ticket (02) has passed the wave 1 combined check. A consequence of the narrowing:
inside wave 2, 05 and 07 cannot use 06's JDBC classes; a child that needs one stops and reports.

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
  `interest-conversion-standards` is not changed; proposals against it are collected in section 6.

## 2. Results

Open this section first. One row per child session; a ticket with several rows was continued after a decision of
the coordinator between runs (section 4). "Parity per case" is the line `tools/parity/compare.py` printed for the
case on the child's branch (`[java]`), i01 to i07 = `i01-baseline`, `i02-tie-half-cent`, `i03-zero-rate-skipped`,
`i04-default-group-fallback`, `i05-multi-category`, `i06-negative-balance`, `i07-nontie-rounding`.
Skills: standards = `interest-conversion-standards`, ticket-build, ticket-verify, pitfalls = the three procedure
skills; all four were read by every child. The full answer of every child (checklist, notes, changed files) is in
its pull request and in `java/interest/evidence/<ticket>/report.md`.

| Wave | Run (script, run id) | Ticket | Session | PR | Status | Skills used | Build | Tests | Parity per case |
|---|---|---|---|---|---|---|---|---|---|
| 1 | `wave-1` wfr-dd66ecfdec544bcca220539cf3ac97a9 | 02-schema | [8d04c540](https://partner-workshops.devinenterprise.com/sessions/8d04c540675d42f68586b34ba3643b76) | [#176](https://github.com/Cognition-Partner-Workshops/ts-cobol-carddemo/pull/176) | done | conversion-standards, ticket-build, ticket-verify, pitfalls, pr-comment-information | PASS | PASS 60 unit, 12 integration | i01 PASS i02 PASS i03 FAIL i04 FAIL i05 PASS i06 PASS i07 PASS |
| 1 | `wave-1` wfr-dd66ecfdec544bcca220539cf3ac97a9 | 03-record-codec | [85daec63](https://partner-workshops.devinenterprise.com/sessions/85daec63788740d3b0044902fecf80c6) | [#178](https://github.com/Cognition-Partner-Workshops/ts-cobol-carddemo/pull/178) | done | conversion-standards, ticket-build, ticket-verify, pitfalls, pr-comment-information, communication-style-plain-english-concise-with-context | PASS | PASS 81 unit, 12 integration | i01 PASS i02 PASS i03 FAIL i04 FAIL i05 PASS i06 PASS i07 PASS |
| 1 | `wave-1b` wfr-915bb2f4ca734bf1b26a6c9b0f8126b8 | 04-calculation-rules | [5315df93](https://partner-workshops.devinenterprise.com/sessions/5315df93383246e487e92c4f3ac038d1) | [#177](https://github.com/Cognition-Partner-Workshops/ts-cobol-carddemo/pull/177) | stopped | conversion-standards, ticket-build, ticket-verify, pitfalls, carddemo-parity, pr-comment-information | PASS | PASS 142 unit, 20 integration | 7/7 PASS |
| 1 | `wave-1c` wfr-c40cc021bac94090a03832ca8c300364 | 04-calculation-rules | [0e2333e8](https://partner-workshops.devinenterprise.com/sessions/0e2333e89892475884069bb25d7ad7e6) | [#177](https://github.com/Cognition-Partner-Workshops/ts-cobol-carddemo/pull/177) | stopped | conversion-standards, ticket-build, ticket-verify, pitfalls, pr-comment-information | FAIL | FAIL 176 unit, 1 failure (ticket 03 `RecordLayoutTest`); integration not reached | 7/7 PASS |
| 1 | `wave-1d` wfr-39daba50baea4f3aa74b7c9346037b5f | 04-calculation-rules | [f9e06519](https://partner-workshops.devinenterprise.com/sessions/f9e06519d7864ab2ac5e7e6104c6d20f) | [#177](https://github.com/Cognition-Partner-Workshops/ts-cobol-carddemo/pull/177) | done | conversion-standards, ticket-build, ticket-verify, pitfalls | PASS | PASS 176 unit, 28 integration | 7/7 PASS |
| 2 | `wave-2` wfr-d29b09c8dc6b425aa25e110dc3709e28 | 05-extract-loader | [7249d2e0](https://partner-workshops.devinenterprise.com/sessions/7249d2e0a82c48359fcf9c1db4a30e63) | [#181](https://github.com/Cognition-Partner-Workshops/ts-cobol-carddemo/pull/181) | done | conversion-standards, ticket-build, ticket-verify, pitfalls, pr-comment-information | PASS | PASS 195 unit, 40 integration | 7/7 PASS |
| 2 | `wave-2` wfr-d29b09c8dc6b425aa25e110dc3709e28 | 06-calc-step-jdbc | [901b6b61](https://partner-workshops.devinenterprise.com/sessions/901b6b6153364c648d1ba593a8cdff56) | [#180](https://github.com/Cognition-Partner-Workshops/ts-cobol-carddemo/pull/180) | done | conversion-standards, ticket-build, ticket-verify, pitfalls, pr-comment-information | PASS | PASS 182 unit, 57 integration | 7/7 PASS |
| 2 | `wave-2` wfr-d29b09c8dc6b425aa25e110dc3709e28 | 07-dataset-export | [4b401c3d](https://partner-workshops.devinenterprise.com/sessions/4b401c3d99294a18ab4eb44c3eb229c0) | [#179](https://github.com/Cognition-Partner-Workshops/ts-cobol-carddemo/pull/179) | done | conversion-standards, ticket-build, ticket-verify, pitfalls, pr-comment-information | PASS | PASS 188 unit, 39 integration | 7/7 PASS |
| 3 | `wave-3` wfr-23e47c433fa34499ae0fe11693e4daf3 | 08-error-paths | [968575c7](https://partner-workshops.devinenterprise.com/sessions/968575c778694682b17adcbeed3c5c20) | [#182](https://github.com/Cognition-Partner-Workshops/ts-cobol-carddemo/pull/182) | done | conversion-standards, ticket-build, ticket-verify, pitfalls, pr-comment-information | PASS | PASS 224 unit, 90 integration | 7/7 PASS |
| 3 | `wave-3b` wfr-e8ed691de0aa438494b1ad4fd33ed65c | 08-error-paths | [8b8919d7](https://partner-workshops.devinenterprise.com/sessions/8b8919d76eab4817b8ab70eca39aedae) | [#182](https://github.com/Cognition-Partner-Workshops/ts-cobol-carddemo/pull/182) | done | conversion-standards, ticket-build, ticket-verify, pitfalls, pr-comment-information | PASS | PASS 228 unit, 90 integration | 7/7 PASS |
| 4 | `wave-4` wfr-5f3d39769ef64b7f9c2e63909aa57d3e | 09-parity-gate | [acf7f74e](https://partner-workshops.devinenterprise.com/sessions/acf7f74ec8f54012a043836a81445ace) | [#183](https://github.com/Cognition-Partner-Workshops/ts-cobol-carddemo/pull/183) | done | conversion-standards, ticket-build, ticket-verify, pitfalls, pr-comment-information | PASS | PASS 234 unit, 101 integration | 7/7 PASS |
| 5 | `wave-5` wfr-52e3a9f65b8c4e8ab246b8570b3ef870 | 10-data-move-kit | [1a72fca3](https://partner-workshops.devinenterprise.com/sessions/1a72fca3ad7e42eeaf06085c8245809e) | [#184](https://github.com/Cognition-Partner-Workshops/ts-cobol-carddemo/pull/184) | done | conversion-standards, ticket-build, ticket-verify, pitfalls, pr-comment-information | PASS | PASS 257 unit, 123 integration | 7/7 PASS |

Notes to the table:

* Tickets 02 and 03 ran before ticket 04 was combined: i03 and i04 stopped with exit code 12 on their branches, as on
  the baseline (they need the rules of ticket 04). Neither case is named by ticket 02 or 03. Both pass since wave 1
  was combined.
* Ticket 04, runs `wave-1b` and `wave-1c`: status `stopped` is the child stopping at a frozen file and reporting, as
  the contract asks; it is not a retry after a failure. No child failed twice, so the retry rule stopped no ticket.
* Ticket 08, run `wave-3`: `done` with one acceptance line met in part; `wave-3b` finished it.
* All nine ticket pull requests are open and unmerged. Their branches were combined on `demo/05b-integration` with
  `git merge --squash`, one commit per ticket (and one per continuation), so the pull requests show as open.

### 2.1 Combined check after each wave

`modernization/design/waves/combined-check.sh <wave>`: `make reset`, `make up`, `make build`, `mvn clean verify`,
the schema assertions, `make parity-java` for every case, each case again through `tools/parity/compare.py`,
`make parity`. Output: `modernization/design/waves/combined-check-wave-<N>.txt`.

| Wave | Integration revision checked | Unit tests | Integration tests | Schema assertions | Cases through the comparator | `make parity` (legacy) | Result |
|---|---|---|---|---|---|---|---|
| 1 | `fa19764` | 176 | 28 | 43, 0 failed | 7/7 PASS (file adapters) | 24 PASS, 0 FAIL | **PASS** |
| 2 | `c31fc1d` | 213 | 80 | 43, 0 failed | 7/7 PASS (file adapters) | 24 PASS, 0 FAIL | **PASS** |
| 3 | `ef063eb` | 228 | 90 | 43, 0 failed | 7/7 PASS (file adapters) | 24 PASS, 0 FAIL | **PASS** |
| 4 | `9cd2e63` | 234 | 101 | 43, 0 failed | 7/7 PASS through PostgreSQL (load, calculation, export); RC1, RC2, RC5 20 of 20 per case | 24 PASS, 0 FAIL | **PASS** |
| 5 | `413c0b6` | 257 | 123 | 43, 0 failed | 7/7 PASS through PostgreSQL; RC1, RC2, RC5 20 of 20 per case | 24 PASS, 0 FAIL | **PASS** |

### 2.2 Combined result on the integration branch

**PASS.** The last combined check ran on `413c0b6` (all nine tickets combined): 257 unit and 123 integration tests,
0 failures; 43 schema assertions; all seven cases of `fixtures/interest` equal field by field through
`tools/parity/compare.py`, run through PostgreSQL; `make parity` 24 PASS, 0 FAIL (legacy lane unchanged).
The commits after `413c0b6` change only the three procedure skills (section 5) and files under
`modernization/design/waves/` (this document, the check output); `git diff --stat 413c0b6 HEAD` outside those two
paths prints nothing. The check was not run again on the last revision for that reason.

What the result does not show: no case exists for a stop condition, a restart, host (EBCDIC) files or a policy
other than the default; those are proved by unit and integration tests only (section 7).

## 3. Tickets not done

None. Tickets 02 to 10 are built, each with an open pull request, and all are combined. Ticket 01 is the pilot.

Open inside finished tickets (reported by the children, not fixed, because no ticket's files allow it):

* **Restart of the three-step job** (ticket 09): a restart loads again, loses the committed account updates and
  fails in the export. A new launch with a new run id gives the right result. REL-11, `architecture.md` section 6.
* **No load-only launch** (ticket 10): RC1 to RC4 cannot be checked between load and calculation.
* **`JdbcAccountStore` stop reasons** and **`rc.json` for a stopped run** (ticket 08): asked for, not granted.
* **`interest.policy.overflow=WIDEN`** is refused at start (ticket 08): INT-H05 option B needs a schema change.
* **The CI check is not marked required** (ticket 09): a branch protection setting, not a file.
* **RC6** needs a mainframe unload and is reported NOT AVAILABLE (ticket 10); host format not accepted (REL-08).

## 4. Decisions of the coordinator between runs

| When | Ticket | Decision | Why |
|---|---|---|---|
| before wave 1 | all | Option B of the plan: allowed files narrowed to disjoint sets, five waves | chosen by the user |
| after `wave-1b` | 04 | granted: `codec/AccountRecord.decode` passes ACCT-ACTIVE-STATUS; three `interest.policy` lines in `application.yml` | acceptance line `inactiveAccounts=SKIP` could not run through the job |
| after `wave-1c` | 04 | granted: one assertion of ticket 03's `RecordLayoutTest` | the granted change broke it; conflict goes to the later ticket |
| wave 1 | 04 | accepted: new file `test/**/codec/AccountRecordActiveStatusTest` outside the narrowed scope | touches no file of ticket 03 |
| after `wave-3` | 08 | granted: `multiCard` and `overflow` in `policy/InterestPolicy.java`, two lines in `application.yml` | the two switches could not be set by property |
| after `wave-3` | 08 | not granted: `JdbcAccountStore` stop reasons, `rc.json` for a stopped run | not needed for acceptance; worked around or not asked by the ticket |
| before wave 4 | 09 | granted: job definition in `batch/InterestJobConfig.java`, new `batch/` classes that only join the steps, tests of the joined job | no ticket owned joining load, calculation and export; the gate could not pass otherwise |

### 4.1 Frozen-change requests of the children, as reported

| Run | Ticket | Request | Decision |
|---|---|---|---|
| `wave-1` | 03 | Not blocking the ticket's acceptance, but needed before a host run: java/interest/src/main/java/com/carddemo/interest/batch/InterestJobParameters.java accepts only format=fixture and FileRunWorkspace.open(Path, Path, String) has no format argument. To start the job on EBCDIC files the batch package (tickets 06/08) must pass the format (and code page) to the workspace; RecordFormat.host() and CodePage.host(Charset) are ready for it. | open (REL-08): not needed for acceptance |
| `wave-1b` | 04 | java/interest/src/main/java/com/carddemo/interest/codec/AccountRecord.java (frozen record class): decode must pass the active flag to the domain - new Account(id, balance, group, Optional.of("Y".equals(<ACCT-ACTIVE-STATUS, offset 11, length 1>))). Without it interest.policy.inactiveAccounts=SKIP (INT-H04 option B) cannot run through the job: it stops with ACTIVE_FLAG_NOT_SUPPLIED (INT-H04), exit 12. The default CHARGE is not affected. | granted |
| `wave-1b` | 04 | java/interest/src/main/resources/application.yml (outside the Allowed files): add under interest.policy the lines 'negativeBalance: CHARGE  # INT-Q05', 'zeroInterest: WRITE  # INT-Q06', 'inactiveAccounts: CHARGE  # INT-H04'. Not needed for behaviour (defaults are in InterestPolicy), only so the file lists every switch. | granted |
| `wave-1c` | 04 | java/interest/src/test/java/com/carddemo/interest/codec/RecordLayoutTest.java (ticket 03's test), line 65: `assertEquals(new Account(2001L, new BigDecimal("1000.00"), "GOLD"), AccountRecord.decode(image));` must become `assertEquals(new Account(2001L, new BigDecimal("1000.00"), "GOLD", Optional.of(true)), AccountRecord.decode(image));` plus `import java.util.Optional;`. Why: the granted AccountRecord.decode change makes the decoded account carry active=Optional[true], so this assertion fails and with it the build. It is the only failing test. | granted |
| `wave-2` | 06 | None blocks the ticket. Would make the code plainer: (1) java/interest/src/main/resources/application.yml - add `interest.calc.store: jdbc` (ticket 09 owns the file; default `file` is in InterestJobConfig until then). (2) domain/TransactionCounter - a constructor taking the number already issued, instead of CalcInterestTableRun.counterAt issuing n ids on restart. (3) domain/StopReason - UPDATE_ACCOUNT (and OVERFLOW, ticket 08); WRITE_OUTPUT and RECORD_REJECTED are used meanwhile. (4) codec/RecordValues - a `with` for a FILLER. (5) FI-4 schema - a column on interest_run for the job's runId (text), so a run can be found without the job execution context. | open: none blocks the ticket; (1) done by ticket 09 in `application-tables.yml` |
| `wave-3` | 08 | java/interest/src/main/java/com/carddemo/interest/policy/InterestPolicy.java (outside the Allowed files; FI-5 switch names unchanged): add components `multiCard` (STOP default, FIRST_BY_CARD_NUM) and `overflow` (STOP), so that interest.policy.multiCard and interest.policy.overflow are read by the one policy class; then build InterestJobConfig.holeDecisions() from them (or move HoleDecisions' methods into the policy). Until then the acceptance line 'with multiCard=FIRST_BY_CARD_NUM the lower card number is used' is met in code and tests but cannot be switched on by the property. | granted |
| `wave-3` | 08 | java/interest/src/main/resources/application.yml (outside the Allowed files): add `multiCard: STOP  # INT-H03` and `overflow: STOP  # INT-H05` under interest.policy. | granted |
| `wave-3` | 08 | java/interest/src/main/java/com/carddemo/interest/jdbc/calc/JdbcAccountStore.java (outside the Allowed files): raises WRITE_OUTPUT for a refused account update and RECORD_REJECTED for a balance beyond its picture; should raise UPDATE_ACCOUNT and OVERFLOW. Worked around inside the Allowed files: batch/DatabaseFailureStops translates WRITE_OUTPUT to UPDATE_ACCOUNT, and the domain's INT-H05 check runs before the adapter's. | not granted, open |
| `wave-3` | 08 | java/interest/src/main/java/com/carddemo/interest/export/** (outside the Allowed files): no rc.json with STEP15 = 12 is written for a stopped run (left by ticket 07 to ticket 08; the ticket file does not ask for it). | not granted, open |
| `wave-4` | 09 | Not needed for the gate (the gate passes without it), needed for a working restart of the three-step job: `load/LoadExtractsStepConfig.loadExtractsStep` (`allowStartIfComplete(true)`, opens a new interest_run row and truncates `carddemo.account` on every execution) together with `modernization/design/architecture.md` section 6. Case: accounts of i01-baseline + i05-multi-category + i07-nontie-rounding, commit interval 2, forced failure at account 2009. Difference: after the restart accounts 2001 and 2002 are 1000.00 and 2400.00 instead of 1010.00 and 2437.00, `interest_run` has a row 1 CALCULATED and a row 2 LOADED, and `export/DatasetExport` refuses row 2 (`export refused: run 2 has status LOADED`), so the job ends FAILED and exports nothing. | open (REL-11) |
| `wave-4` | 09 | Optional, to remove `batch/InterestRunHandover`: one job execution context key for the interest_run row instead of `interestRunId` (`load/LoadExtractsStepConfig.RUN_ID_KEY`, `batch/CalcInterestTableRun.INTEREST_RUN_ID`) and `interest.run.id` (`export/ExportDatasetsTasklet.RUN_ID_KEY`). | open, optional |

## 5. Skill corrections made and why

64 commits on `demo/05b-integration`, one per correction, each naming the child that reported it. Only the
three procedure skills were changed; `interest-conversion-standards` has no commit
(`git log 6f6c1a6..HEAD -- .agents/skills/interest-conversion-standards` prints nothing). Corrections were applied
between waves (and between the runs of wave 1), never while a child of that wave was running.

| Commit | Skill and place | Reported by | Why |
|---|---|---|---|
| `058cb5a` | ticket-verify: step 8, a ticket without a fixture case | ticket 02-schema child, session 8d04c540 | parity-report.sh 02-schema ended with the usage line and exit 2; with i01-baseline it labelled that case the ticket's acceptance case. |
| `51ea95d` | ticket-verify: step 8, evidence file for psql output | ticket 02-schema child, session 8d04c540 | Ticket 02's Verification asks for the psql output of the apply and of the assertion script; the table named no file for it. |
| `6d061c4` | ticket-verify: step 7, table list after ticket 02 and old db volumes | ticket 02-schema child, session 8d04c540 | Its parity report shows 8 tables in carddemo; the docker-entrypoint log shows 02-interest.sql applied only on an empty volume. |
| `59f8172` | ticket-build: step 9, scope check of a ticket under db/ | ticket 02-schema child, session 8d04c540 | The listed command necessarily prints lines for db/postgres/02-interest.sql and db/postgres/tests/**; the replacement printed nothing on its branch. |
| `fc45ac2` | pitfalls: P8, catalog text unqualified for role carddemo | ticket 02-schema child, session 8d04c540 | New failure hit: 9 false failures of the schema assertions until search_path was set. |
| `2c26390` | ticket-verify: step 7, skip when the ticket has no database assertions | ticket 03-record-codec child, session 85daec63 (also reported by ticket 04-calculation-rules child, session 5315df93) | Tickets 03 and 04 have the Acceptance line 'Database assertions: none'. |
| `1c03756` | ticket-verify: step 8, source for the Why column of cases outside the ticket | ticket 03-record-codec child, session 85daec63 | parity-report.sh left six '_fill in_' cells; i03 and i04 failed exactly as on the base, which only the kept base output shows. |
| `5ba9b53` | ticket-verify: step 6, example of a record error | ticket 03-record-codec child, session 85daec63 | The step showed only the missing-file error of ticket 01; the codec error path is proved by one changed input byte. |
| `e8dc9ae` | ticket-build: step 5, new switch defaults when application.yml is not an allowed file | ticket 04-calculation-rules child, session 5315df93 | Its Allowed files are domain/** and policy/** only; InterestPolicyBindingTest.fi5_noPropertyGivesTheLegacyBehaviourOfEverySwitch passes with no property set. |
| `1489112` | ticket-build: step 9, scope check of a ticket with narrower Allowed files | ticket 04-calculation-rules child, session 5315df93 | The listed command does not cover the frozen pom, Makefile, .github and frozen packages; the longer command printed nothing on its branch. |
| `20f08a1` | ticket-verify: step 8, evidence file for switch tests | ticket 04-calculation-rules child, session 5315df93 | Ticket 04's Verification asks for 'the switch-test output'; step 4 shows one run and step 8 named no file. |
| `7a0ea51` | ticket-verify: step 5, HALF_EVEN result beside HALF_UP | ticket 04-calculation-rules child, session 5315df93 | switch-test-output.txt: 'interest/i02-tie-half-cent [java-halfeven]: FAIL (4 records, 50 fields, 2 differences)' and i07 PASS. |
| `c630091` | ticket-verify: step 6, skip when the ticket has no command-line error line | ticket 04-calculation-rules child, session 5315df93 | Ticket 04's error line is the empty input, shown by InterestCalcJobIT.intT09_emptyInputWritesNothing. (The same for step 7 is the correction reported by ticket 03.) |
| `1e05ad8` | ticket-verify: step 3, sample result after ticket 04 | ticket 04-calculation-rules child, session 5315df93 | make parity-java on its branch and the wave 1 combined check on demo/05b-integration: 7 of 7 PASS. |
| `835be9e` | pitfalls: P9, reading the expectation files in a test | ticket 04-calculation-rules child, session 5315df93 | New failure hit in PolicySwitchFixtureTest.assertMatchesExpectation. |
| `b6bd030` | pitfalls: P10, trimmed SYSOUT in the expectation files | ticket 04-calculation-rules child, session 5315df93 | New failure hit: the assertion message above, before the fix. |
| `a0089b2` | pitfalls: P11, mvn -o on a fresh machine | ticket 04-calculation-rules child, session 5315df93 | New failure hit on the child's fresh machine. |
| `daa72f8` | ticket-verify: parity-report.sh no longer reports its own evidence files as uncommitted changes | ticket 04-calculation-rules child, second session 0e2333e8 and ticket 04-calculation-rules child, third session f9e06519 | The flag was computed after the report file under java/ was truncated and counted test-output.txt; at 5ac6473 with a clean tree the report said 'with uncommitted changes', with REPORT=work/... it did not. The flag is now computed before the block and leaves out java/interest/evidence. |
| `b0adf7c` | ticket-verify: step 8, what to record when the unit phase fails | ticket 04-calculation-rules child, second session 0e2333e8 | Required run: 'Tests run: 176, Failures: 1', BUILD FAILURE, no failsafe summary; second run 203 (surefire) and 28 (failsafe), BUILD SUCCESS. |
| `7d1a902` | ticket-verify: step 4, a switch that no fixture exercises | ticket 04-calculation-rules child, second session 0e2333e8 | Seven runs with --interest.policy.inactiveAccounts=SKIP: each job exit=0 and PASS with 0 differences; the skip is shown only by InactiveAccountsSkipJobIT.intH04_inactiveAccountSkippedThroughTheJob. |
| `e028555` | pitfalls: P12, TransactionRecord.decode takes a RecordOrigin | ticket 04-calculation-rules child, second session 0e2333e8 | New compile failure hit after ticket 03's codec was combined. |
| `3d40983` | ticket-build: step 3, a run that continues a ticket | ticket 04-calculation-rules child, third session f9e06519 | demo/05-04-calculation-rules and PR #177 existed and the prompt said to reuse both. |
| `f7490b1` | ticket-build: step 8, test totals after wave 1 | ticket 04-calculation-rules child, third session f9e06519 | Its test-output.txt and the wave 1 combined check: 'Tests run: 176' and 'Tests run: 28', BUILD SUCCESS. (The child's sentence named 163/20 for the integration branch before its own continuation was combined; the combined figure is used.) |
| `1609623` | ticket-build: step 5, packages and tables that now exist | ticket 06-calc-step-jdbc child, session 901b6b61 | ls db/postgres lists 02-interest.sql; its branch adds jdbc/calc. (Coordinator added the packages of 05 and 07, combined in the same wave.) |
| `35f9327` | ticket-build: step 9, scope check also covers domain and policy | ticket 06-calc-step-jdbc child, session 901b6b61 | With the two packages added and batch taken out (allowed for ticket 06) the command printed nothing on its branch. |
| `bd7fe6e` | ticket-verify: step 6, error line of a step that the jar does not run | ticket 05-extract-loader child, session 7249d2e0, ticket 06-calc-step-jdbc child, session 901b6b61 and ticket 07-dataset-export child, session 4b401c3d | InterestJobConfig built the job with calcInterestStep only; the error lines are shown by LoadExtractsStepIT, CalcInterestRestartIT.intR07_forcedFailureBetweenInsertsAndUpdateLeavesNeither and DatasetExportIT. |
| `92c1eab` | ticket-verify: step 7, database assertions of a run that exists only in Testcontainers | ticket 05-extract-loader child, session 7249d2e0, ticket 06-calc-step-jdbc child, session 901b6b61 and ticket 07-dataset-export child, session 4b401c3d | LoadExtractsStepIT (12), CalcInterestTablesIT (10), JdbcCalcAdaptersIT (14) and DatasetExportIT.export_changesNoRowExceptTheStatusOfTheRun_andTheExportedFiguresEqualRc5 pass on such a container. |
| `236f7b9` | ticket-verify: step 8, what to write for passing cases outside the ticket | ticket 07-dataset-export child, session 4b401c3d | The generated parity-report.md had _fill in_ for i02, i03, i04, i05 and i07. |
| `9f4b74d` | ticket-verify: step 8, cases proved through code that make parity-java does not run | ticket 05-extract-loader child, session 7249d2e0, ticket 06-calc-step-jdbc child, session 901b6b61 and ticket 07-dataset-export child, session 4b401c3d | 'interest/i05-multi-category [java-tables]: PASS (3 records, 38 fields, 0 differences)' (06, seven cases) and 'interest/i01-baseline [java-export]: PASS (4 records, 50 fields, 0 differences)' (07) came from java/interest/target, not from work/parity-java. |
| `8b6777d` | pitfalls: P13, search_path batch and the second init script | ticket 05-extract-loader child, session 7249d2e0 and ticket 06-calc-step-jdbc child, session 901b6b61 | LoadExtractsStepIT and JdbcCalcAdaptersIT pass with both init scripts and schema-qualified names; InterestCalcJobIT.java lines 53-60 mount only the first script. |
| `6661966` | pitfalls: P14, GitHub secondary rate limit when opening the pull request | ticket 05-extract-loader child, session 7249d2e0 and ticket 06-calc-step-jdbc child, session 901b6b61 | git_create_pr failed at 11:37:09, 11:38:47 and 11:40:54 UTC with that message and succeeded after the wait. |
| `3232171` | pitfalls: P15, hand-written amounts a factor of ten too small | ticket 07-dataset-export child, session 4b401c3d | New failure hit; the recorded files caught the wrong test data. |
| `04b4f43` | ticket-build: step 8, test totals after waves 2 and 3 | ticket 08-error-paths child, session 968575c7 | test-output.txt of ticket 08: 224 and 90 in its first session, 228 and 90 after the continuation (coordinator used the later figure, confirmed by the wave 3 combined check). |
| `59b1683` | ticket-build: step 5, classes of ticket 08 and the sixth constructor argument | ticket 08-error-paths child, session 968575c7 | grep -rn 'new InterestCalculator' java/interest/src: the two test call sites had to change with the constructor. |
| `1c844dc` | ticket-verify: step 8, tickets whose fixture cases are regression cases only | ticket 08-error-paths child, session 968575c7 and ticket 08-error-paths child (continuation), session 8b8919d7 | parity-report.sh line 68 sets the scope to 'yes - acceptance case of the ticket' and line 75 the heading 'Acceptance run' for every case given. |
| `328afc2` | ticket-verify: step 4, exit code 1 for a switch value that does not exist | ticket 08-error-paths child (continuation), session 8b8919d7 | switch-test-output.txt of ticket 08: --interest.policy.overflow=WIDEN gives 'job exit=1' and 'FAIL (4 records, 50 fields, 4 differences)'. |
| `cba2ceb` | pitfalls: P16, hand-written balance record image a digit too long | ticket 08-error-paths child, session 968575c7 | New failure hit in ErrorPathsIT; the continuation session confirmed it as the P15 family. |
| `6c7ca6f` | pitfalls: P17, extra run log lines of a stop in earlier tests | ticket 08-error-paths child, session 968575c7 | The three tests had to be changed with ticket 08. |
| `9390e59` | pitfalls: P18, constructor binding of InterestPolicy with two constructors | ticket 08-error-paths child (continuation), session 8b8919d7 | New failure hit and fixed in the continuation session. |
| `bfa179f` | ticket-verify: step 1, make parity-java runs the three steps through PostgreSQL | ticket 09-parity-gate child, session acf7f74e | make parity-java prints both lines per case; job repository row 'loadExtractsStep:COMPLETED read=0 write=21, calcInterestStep:COMPLETED read=2 write=2, exportDatasetsStep:COMPLETED read=0 write=2'. |
| `f483063` | ticket-verify: step 2, what a stopped run leaves with the tables | ticket 09-parity-gate child, session acf7f74e | InterestJobTablesIT.intR08_missingAccountStopsTheJobInTheCalculation asserts the run log lines and no datasets, no rc.json. |
| `76acf04` | ticket-verify: step 4, running by hand what the gate runs | ticket 09-parity-gate child, session acf7f74e | With the profile the manual run of i01-baseline printed 'job exit=0' and left interest_run EXPORTED; INTEREST_OPTS=--interest.policy.rounding=HALF_EVEN failed i02-tie-half-cent only. |
| `075f7be` | ticket-verify: step 7 and parity-report.sh, steps listed in execution order | ticket 09-parity-gate child, session acf7f74e | string_agg had no ORDER BY: parity-report.md of ticket 09 listed exportDatasetsStep before loadExtractsStep. |
| `ca2b10d` | ticket-verify: step 7, make parity-java applies the schema to an old db volume | ticket 09-parity-gate child, session acf7f74e | Makefile line with to_regclass('carddemo.interest_run'); not triggered in the child's session (new volume). |
| `3a0e221` | ticket-verify: step 8, the gate runs the tables since ticket 09 | ticket 09-parity-gate child, session acf7f74e | java/interest/evidence/09-parity-gate/switch-test-output.txt. |
| `5ae821c` | ticket-verify: step 6, the error line of the gate | ticket 09-parity-gate child, session acf7f74e | java/interest/evidence/09-parity-gate/switch-test-output.txt. |
| `253623a` | ticket-build: step 5, job assembly classes and the profile tables | ticket 09-parity-gate child, session acf7f74e | Files of PR 183. |
| `9a67c48` | ticket-build: step 8, test totals after ticket 09 | ticket 09-parity-gate child, session acf7f74e | 'Tests run: 234, Failures: 0, Errors: 0, Skipped: 0' and 'Tests run: 101, ...', BUILD SUCCESS. |
| `ff6f75d` | ticket-build: step 9, make parity also rewrites tools/runjcl/__pycache__ | ticket 09-parity-gate child, session acf7f74e | git status --short after make parity: ' M tools/runjcl/__pycache__/runjcl.cpython-310.pyc'. |
| `0fcac08` | pitfalls: P6, tools/runjcl/__pycache__ as well | ticket 09-parity-gate child, session acf7f74e | Same observation as for interest-ticket-build step 9. |
| `2e66812` | pitfalls: P19, the execution context is serialized Java | ticket 09-parity-gate child, session acf7f74e | Hit while writing the RC assertions of the gate. |
| `be6c057` | pitfalls: P20, JobRepository must be set | ticket 09-parity-gate child, session acf7f74e | New failure hit and fixed in the test. |
| `c770787` | ticket-build: step 5, package recon exists since ticket 10 | ticket 10-data-move-kit child, session 1a72fca3 | The sentence was out of date. The jar's manifest has Start-Class InterestApplication (frozen) and holds PropertiesLauncher; make recon-java CASE=i01-baseline printed EQUAL for RC1 to RC5. |
| `2bdef2f` | ticket-build: step 8, test totals after ticket 10 | ticket 10-data-move-kit child, session 1a72fca3 | test-output.txt of the ticket: Tests run: 257 and Tests run: 123, BUILD SUCCESS; the wave 5 combined check shows the same totals. |
| `c0608b9` | ticket-build: step 9, scope check of a ticket allowed one package, the README and Makefile targets | ticket 10-data-move-kit child, session 1a72fca3 | The step had no command for such a ticket; both commands printed nothing and 0 on its branch. |
| `1ec3193` | ticket-verify: step 6, error path commands of ticket 10 | ticket 10-data-move-kit child, session 1a72fca3 | The step named no command for the kit; recon-report.md and sidebyside-report.md, section 'Error path', show both lines and make exit code 2. |
| `d2329e1` | ticket-verify: step 7, database assertion 'writes nothing' | ticket 10-data-move-kit child, session 1a72fca3 | The query of the step shows the job repository only and does not fit such a ticket; ReconReportIT, 22 tests, 0 failures. |
| `f2aea48` | ticket-verify: step 8, evidence files written by a ticket's own targets | ticket 10-data-move-kit child, session 1a72fca3 | The table had no row for them; both files are in java/interest/evidence/10-data-move-kit/. |
| `7c61c89` | ticket-verify: step 8, parity report of a tool beside the job | ticket 10-data-move-kit child, session 1a72fca3 | The sentence spoke only of a step missing from the job; parity-report.sh works as written for seven cases. |
| `118c1d1` | ticket-verify: step 8, own evidence file instead of an own parity section | ticket 10-data-move-kit child, session 1a72fca3 | Second half of the same report: parity-report.md of ticket 10, note under the result table. |
| `30e3b67` | ticket-verify: step 1, the two targets of ticket 10 and their order | ticket 10-data-move-kit child, session 1a72fca3 | make sidebyside-java right after make recon-java printed NOT AVAILABLE for all seven cases and make exit 1; after a parity run seven EQUAL lines. |
| `345be27` | pitfalls: P21, account table differs from the unloaded file after the calculation | ticket 10-data-move-kit child, session 1a72fca3 | New failure hit and fixed during the build of the kit. |
| `810d6c6` | pitfalls: P22, compare.py compares only with the fixture directory | ticket 10-data-move-kit child, session 1a72fca3 | sidebyside-report.md: 'NOT AVAILABLE (the legacy directory has no manifest.json ...)'. |
| `96c289b` | pitfalls: P6, which of the ticket 10 targets rewrite the tracked .pyc | ticket 10-data-move-kit child, session 1a72fca3 | git status after make recon-java showed the .pyc changed; after make sidebyside-java alone no file under tools/. |

## 6. Proposed changes to the conversion standards

`interest-conversion-standards` was not changed. The 57 proposals below are the children's words; a proposal
repeated word for word by a later session of the same ticket is listed once. They are not checked against each
other: some overlap, and a person decides.


**Ticket 02-schema**

* (`wave-1`) Section 9: most lines concern Java; a schema-only ticket marks them N/A or 'PASS for the schema half'. Proposed: add 'A ticket with no Java marks S2.x, S6.x and behaviours a, c, l, m, o, q, s, w, x, z, aa N/A in one line.'
* (`wave-1`) S3.6: CHAR(n) does not enforce width (PostgreSQL pads shorter text). Proposed addition: 'The schema enforces exact length only for FILLER (BYTEA + octet_length); the width of text fields is enforced by the codec before the insert.'
* (`wave-1`) S7.3: names Testcontainers only. Proposed addition: 'SQL assertions under db/postgres/tests, run by db/postgres/tests/run.sh on a scratch database of the db service (postgres:15), count as integration tests of the schema.'
* (`wave-1`) Missing standard on schema scripts. Proposed: 'Schema scripts are one transaction, never DROP or ALTER an existing object, and fail on re-application; a change to FI-4 is a new numbered script.' The code shows 02-interest.sql doing exactly this.

**Ticket 03-record-codec**

* (`wave-1`) S3.5: the standard does not say what a record image is in Java. The ports (BalanceRead.recordImage, RunLog.echo) carry a String while the codec works on byte[]. Proposed: state that a record image String is one character per byte (ISO-8859-1) and that only record-codec converts it.
* (`wave-1`) Section 4 row f / G07: every byte value is a character in ISO-8859-1 and in cp037, so 'a byte that is not a character of the code page' cannot be tested by the charset. The code rejects control characters after translation (C0 and C1). Proposed: write that rule into the standard, or name the bytes to reject.
* (`wave-1`) Section 9: no line for 'errors name file, record number and field', which the tickets require. Proposed: add one.

**Ticket 04-calculation-rules**

* (`wave-1b`) S3.8: the code (InterestPolicy.intQ03CycleDate) reads the first eight characters of the PARM as the cycle date under timestampSource=CYCLE_DATE, because rules.yaml INT-Q03 option B and every expected/<case>.json define it as the cycle date of the PARM at 00.00.00 (2022-07-18-00.00.00.000000) and no cycle-date job parameter exists. Proposed: S3.8 allows this for CYCLE_DATE only, or architecture.md section 4 gains a cycleDate job parameter and the expectation files say where the date comes from.
* (`wave-1b`) Section 4 (S4.1): no row for the break of an account whose balances all have rate zero; the code reproduces CBACT04C:194-201, 350-356 (total 0.00 added, cycle amounts reset), test intR04_accountWithOnlyZeroRatesIsStillUpdatedAtTheBreak. Proposed: add the row.
* (`wave-1b`) S5.1: does not say what a SKIP option does to lookups, run-log lines and the transaction counter. The code: INT-Q05 SKIP still looks up the rate (fallback lines appear), INT-H04 SKIP echoes the records, does no card or rate lookup and no account rewrite, a skipped balance or account uses no transaction number. Proposed: one sentence per switch.
* (`wave-1b`) Section 4 row aa / S5.2 against architecture.md section 8 (INT-H01 'empty fee hook'): S5.2 says empty code is not ported, the architecture names a hook. Built as one private method InterestCalculator.intH01Fees returning nothing. Proposed: say whether a hook is wanted.
* (`wave-1b`) Section 6, row 'DEFAULT rate missing': named a proposal for ticket 08, but ticket 04 needs the stop once the fallback exists; StopReason.DEFAULT_RATE_MISSING was added without the legacy lines. Proposed: say that ticket 04 adds the reason and ticket 08 the messages.
* (`wave-1b`) S6.2: an unreadable PARM under CYCLE_DATE raises IllegalArgumentException from the policy (policy does not depend on domain); the step still fails with exit 12. Proposed: say whether InterestRunStoppedException may live where policy can use it.
* (`wave-1c`) S3.8: INT-Q03 option B and every expected/<case>.json define the cycle date from the PARM; S3.8 forbids reading the PARM as a date. Proposed: allow the policy to read the first eight characters of the PARM under timestampSource=CYCLE_DATE only, or add a cycleDate job parameter to architecture.md section 4.
* (`wave-1c`) Section 4: no row for the break of an account whose balances all have rate zero (rewritten with total 0.00 and cycle amounts reset, CBACT04C:194-201, 350-356). Proposed row with test intR04_accountWithOnlyZeroRatesIsStillUpdatedAtTheBreak.
* (`wave-1c`) S5.1: does not say what a SKIP option does to lookups, run-log lines and the transaction counter. Proposed: one sentence per switch.
* (`wave-1c`) Section 4 row aa / S5.2: INT-H01 is called an empty fee hook in the architecture while S5.2 says empty code is not ported. Proposed: say whether a hook is wanted.
* (`wave-1c`) Section 6: DEFAULT_RATE_MISSING is named as a proposal for ticket 08 but ticket 04 needs the stop once the fallback exists. Proposed: say so in the row.
* (`wave-1c`) S8.4 / S7: a granted change to a record class can contradict a frozen test of the ticket that owns the class (here RecordLayoutTest line 65). Proposed: a grant on a frozen file names the tests of that file that may be updated with it.
* (`wave-1c`) Data map row CVACT01Y/ACCT-ACTIVE-STATUS says 'carried, never read'; it is now read under INT-H04 option B. Proposed: S3.5/S3.6 or the data map note the exception.
* (`wave-1d`) S3.8: forbids reading the PARM as a date and asks for a separate cycle-date job parameter; INT-Q03 option B and every expected/<case>.json define the timestamp from the PARM (2022-07-18-00.00.00.000000 for 2022071800), and architecture.md section 4 lists no such parameter. InterestPolicy.intQ03CycleDate reads the first eight characters of the PARM under CYCLE_DATE only. Proposed: S3.8 allows that under CYCLE_DATE, or architecture gains a cycleDate job parameter.
* (`wave-1d`) Section 4: no row for the break of an account whose balances all have rate zero (CBACT04C:194-201, 350-356: total 0.00 added, cycle amounts reset). Proposed row with test intR04_accountWithOnlyZeroRatesIsStillUpdatedAtTheBreak.
* (`wave-1d`) S5.1: does not say what a SKIP option does to lookups, run-log lines and the transaction counter. Proposed: one sentence per switch (built: Q05 SKIP still looks up the rate and logs the fallback lines; Q06 SKIP adds nothing; H04 SKIP echoes the records, no lookup, no transaction, no rewrite; a skipped item uses no number).
* (`wave-1d`) Section 4 row aa / S5.2 against architecture section 8: INT-H01 is called 'an empty fee hook' while S5.2 says empty code is not ported. Built as one private method returning no transaction. Proposed: say whether a hook is wanted.
* (`wave-1d`) Section 6: DEFAULT_RATE_MISSING is named as a proposal for ticket 08, but ticket 04 needs the stop once the fallback exists (added without the legacy lines). Proposed: say so in the row.
* (`wave-1d`) S8.4 / section 9: no line for a frozen file changed under an explicit grant. Proposed: 'PASS with granted changes, each listed with who granted it' as an allowed mark (this ticket: AccountRecord.from, three application.yml lines, one assertion of RecordLayoutTest).

**Ticket 05-extract-loader**

* (`wave-2`) S2.2: the code shows a loader needs copybook field names beyond the codec mapping - key fields, the hash-total field (ExtractFile) and the fields the TL4 profile reads (ExtractProfile). Proposed change: add 'and in the table mapping of the loader, which derives the column name from the field name'.
* (`wave-2`) Section 6 (status and message mapping): no row for loadExtractsStep. Proposed change: add RECORD_REJECTED (reject list written, interest_run FAILED, no table changed), WRITE_OUTPUT (a table could not be replaced, that table rolled back, interest_run FAILED) and OPEN_INPUT (extract file unreadable, interest_run FAILED).
* (`wave-2`) S7.1: a ticket with 'Rules: none' has no rule id to name tests for. Proposed change: allow '<strategyItemOrGapId>_<whatItProves>' (tl3_, rc1Rc2_, rc4_, g02_, g03_ as used in ExtractDecoderTest).

**Ticket 06-calc-step-jdbc**

* (`wave-2`) S6.5 (and ticket 06 Acceptance 'a failure ends the step FAILED with the account id in the message'): the code keeps the id out of the application log and the exception message and puts it into the step's exit description (batch.batch_step_execution.exit_message). Proposed addition to S6.5: 'The account id of a failure may be stored in the step's exit description in the job repository; it is not written to the application log.'
* (`wave-2`) S6.4 / S6.3: nothing says where the run log of a restartable run lives. The code writes <outputDir>/sysout/INTCALC.STEP15.txt in place and a failed run keeps it for the restart. Proposed addition to S6.4: 'With table adapters the run log is written in place under the output directory while the step runs; a failed run keeps it for the restart and the export does not remove it.'
* (`wave-2`) S2.3: the id of the run's interest_run row has no agreed place. The code reads the job execution context key `interestRunId` and inserts the row itself when it is absent. Proposed addition: 'The id of the run's interest_run row travels in the job execution context under `interestRunId`; the step that opens the row puts it there.'
* (`wave-2`) S7.1: restart has no rule id (REL-11 is a release decision), so its tests are named rel11_*. Proposed addition: 'Tests of a release decision are named rel<nn>_<whatItProves>.'
* (`wave-2`) S6.1 / S6.2: a DataAccessException of the driver fails the step without being an InterestRunStoppedException, and Spring Batch logs its text. Proposed: say that ticket 08 wraps database failures of the JDBC adapters and that the wrapped message must not carry the driver's row detail.

**Ticket 07-dataset-export**

* (`wave-2`) S3.6 / data-map E4: the codec decodes a FILLER (RecordValues.filler(int)) but offers no way to set one, so ExportRecords.withFiller places the raw bytes itself (offset and length taken from the layout). Proposed: add RecordValues.withFiller(int index, byte[] raw) to the codec and name it in S3.6 as the only way.
* (`wave-2`) S6.3: 'restart repeats the step' disagrees with ticket 07 (a run that is not CALCULATED is refused) once the export has completed. Proposed: S6.3 says a repeat is possible until the run is EXPORTED.
* (`wave-2`) Section 2 (no standard): nothing says how the interest_run.run_id travels between steps. The code uses job execution context key 'interest.run.id' (Long). Proposed: name that key in section 2.
* (`wave-2`) Section 9 checklist: no line for 'the evidence goes through the ticket's own code'. make parity-java passes for a ticket whose step is not in the job. Proposed line: 'The parity run goes through the code of the ticket, or the report says what does.'

**Ticket 08-error-paths**

* (`wave-3`) Section 6 (table): no row for a failed read of ACCTFILE or XREFFILE other than record missing; CBACT04C:378-390 and 400-412 abend on every status but 00. Proposed: add rows READ_ACCOUNT (`ERROR READING ACCOUNT FILE`) and READ_CARD (`ERROR READING XREF FILE`), no status line. Built that way.
* (`wave-3`) S3.4 and S5.1: say the overflow and several-cards decisions go to the policy (`policy.checkFits`), but ticket 08's Allowed files exclude policy/. The code has them in domain/HoleDecisions. Proposed: add policy/** and application.yml to ticket 08's Allowed files, or state in S5.1 that a ticket without policy/ keeps the decision in one domain type and reports the binding.
* (`wave-3`) Section 6, rows INT-H03 and INT-H05: 'a line naming INT-H03 (no legacy text exists)' gives no text. Built: `INT-H03 ACCOUNT HAS SEVERAL CARDS: <11-digit id>` and `INT-H05 VALUE DOES NOT FIT FIELD: <field>`, each followed by `ABENDING PROGRAM`. Proposed: fix the wording in the standard and say that `ABENDING PROGRAM` follows.
* (`wave-3`) Section 6, row 'DEFAULT rate missing or unreadable': one reason for two conditions, but `FILE STATUS IS: NNNN0023` is right for the missing record only. Proposed: split - missing: DEFAULT_RATE_MISSING with status 23; unreadable: READ_RATE with `ERROR READING DEFAULT DISCLOSURE GROUP` and no status line (the code writes `ERROR READING DISCLOSURE GROUP FILE` for an unreadable DEFAULT read today).
* (`wave-3`) Section 4 row b / S3.4: the range check of ACCT-CURR-BAL (S9(10)V99, CBACT04C:352) is named by row b but not by rule INT-H05. The code stops with OVERFLOW (INT-H05). Proposed: cite line 352 in S3.4 and ask the rules owner to add it to INT-H05.
* (`wave-3b`) 1. **Section 6 has no row for a failed read of ACCTFILE or XREFFILE other than "record missing".** `CBACT04C:378-390` and `400-412` abend on every status but 00. Built: `READ_ACCOUNT` and `READ_CARD` with `ERROR READING ACCOUNT FILE` / `ERROR READING XREF FILE` and no status line. Proposed: add the two rows.
* (`wave-3b`) 2. **S3.4 and S5.1 say the overflow and the several-cards decision go to the policy,** but ticket 08's Allowed files are `domain/**`, `batch/**` and tests; the binding needed a grant of the coordinator (this run). Proposed: add `policy/InterestPolicy.java` and `application.yml` (the two switches only) to the Allowed files of ticket 08, and say in S3.4 that the check may live in a domain type built from the policy (`HoleDecisions.of(policy)`), not only as `policy.checkFits`.
* (`wave-3b`) 3. **Section 6 says "a line naming INT-H03 / INT-H05 (no legacy text exists)"** without the text. Proposed: fix the wording (finding 2 above) so that operations can search for it.
* (`wave-3b`) 4. **Section 6, row "DEFAULT rate missing or unreadable"** gives one reason for a missing record and a read failure, but the status line `NNNN0023` is right for the first only. Proposed: split the row (missing: with status 23; unreadable: `READ_RATE`, no status line).
* (`wave-3b`) 5. **S6.1 does not say whether `ABENDING PROGRAM` follows the line of a hole.** Built: it does, because the target has one failure path. Proposed: say so.

**Ticket 09-parity-gate**

* (`wave-4`) S6.3: the code shows that `loadExtractsStep repeats` and `calcInterestStep continues after the last committed account` cannot both hold in one job (InterestJobRestartIT.rel11_restartOfTheThreeStepsExportsNothing: the restarted job ends FAILED in the export, the committed account updates are replaced by the reload). Proposed: after REL-11 is decided, state which applies once the calculation has committed a chunk (the load is not repeated, or the run is restored and launched again with a new runId).
* (`wave-4`) S6.4: the sentence on a stopped run covers the file adapters only. Proposed addition: "With the tables: a stopped run leaves the run log (`sysout/INTCALC.STEP15.txt`) under the output directory and no `datasets` directory and no `rc.json`."
* (`wave-4`) S7.1: no naming rule for a test of code that implements no rule (job assembly, the gate). Proposed: "A test of code with no rule id is named `<whatIsTested>_<whatItProves>` (for example `jobSteps_loadCalcExportInThatOrderOnTheTables`)."
* (`wave-4`) Section 9: no checklist line for the reconciliation figures. Proposed line: "RC1, RC2, RC5 hold in interest_run after every fixture case - PASS / FAIL / N/A".
* (`wave-4`) Section 2 / S2.3: `interest.job.steps` and `interest.calc.store` are not named in the standards or in architecture.md section 8. Proposed: one sentence saying which settings choose the adapters and the steps, and which value is the production default.

**Ticket 10-data-move-kit**

* (`wave-5`) **S2.2** says a copybook field name appears once, in the codec or row mapping. A reconciliation report has to name the fields it sums and the fields the comparator reports (`ACCT-CURR-BAL`, `TRAN-AMT`, `TRAN-ORIG-TS`), and `load.ExtractProfile` (ticket 05) already does. Proposed addition: "Reporting code (`recon`, the profile of the load) may name a copybook field to say which figure it reports; it reads the value through the codec's `RecordValues` and never from the record image."
* (`wave-5`) **S6.5** covers the run log and the application log. Proposed addition: "A reconciliation or comparison report is a third output. It may hold keys (account id, card number) where a check is about keys (RC4, the last account of RC5) and holds no other field of an account; where reports are kept is a decision of the data owner."
* (`wave-5`) **S7.1** names tests `<ruleId>_<whatItProves>` for INT-R, INT-Q and INT-H rules. The checks of the data move have ids too (RC1-RC6, TL3, TL4, SS3, SS4). Proposed addition: "A test of a data-move item is named `<itemId>_<whatItProves>` (`rc3_brokenFillerByte...`)."
* (`wave-5`) **Section 6** has no row for a tool that is not a step of the job. Proposed sentence under S6.2: "A command beside the job (the reconciliation report, the side-by-side comparison) has its own exit codes as named constants: 0 equal, 1 different or not available, 2 nothing could be compared; it never uses the job's `STOPPED` code."

## 7. New rows for the decision register

75 findings, in the children's words, repeated ones listed once. Nothing was written to a ticket system or
to the register itself; the rows are proposals for its owner. Later tickets report some findings of earlier ones
again in other words; these are kept, because they add evidence.


**Ticket 02-schema**

* (`wave-1`) interest_run reconciliation columns are not enumerated in the data map (data-map-interest.md:126 gives only 'loaded_* / written_* counts and sums: BIGINT / NUMERIC(18,2)'). This ticket named 18 nullable columns from RC1, RC2, RC5; they become FI-4. Hash totals of account ids are NUMERIC(18,2) although they have no decimals; RC3, RC4, RC6 have no column. Owner: architect; consumers tickets 05-07 must use these names or request a change before FI-4 is merged.
* (`wave-1`) interest_run.status has no CHECK on its five values and started_at/finished_at are nullable, because the data map's type cells say only VARCHAR(12) NOT NULL and TIMESTAMPTZ. Owner: architect.
* (`wave-1`) CHAR(n) keys compare ignoring trailing blanks: ('DEFAULT') duplicates ('DEFAULT   ') in disclosure_group - matches the fixed-width legacy key; non-blank padding (low-values) would be a different key (with G07). Owner: data owner.
* (`wave-1`) CHAR(n) pads shorter text silently; text width is not enforced by the schema, only by the codec (S3.6). Owner: architect.
* (`wave-1`) Second apply of 02-interest.sql fails (psql exit 3, nothing changed) rather than being a no-op; re-running or changing the schema later needs a migration-tool decision not in the architecture. Owner: release manager.
* (`wave-1`) Init-script mount applies 02-interest.sql only on an empty db volume; existing volumes need make reset or a manual psql -f. docker-compose.yml needed no change. Owner: tickets 05/09.
* (`wave-1`) InterestCalcJobIT copies only 01-transaction-type.sql into its Testcontainers database and asserts the carddemo table list; it does not see the new tables. The first ticket that makes the job use the schema must change that test (frozen for this ticket). Owner: ticket 05/06.

**Ticket 03-record-codec**

* (`wave-1`) REL-08/G01 still open: CodePage.host() uses IBM037, CodePage.host(Charset) takes another page; only PIC X text depends on the page. Owner: mainframe data owner.
* (`wave-1`) G07 needs a precise rule: the codec rejects control characters (X'00'-X'1F', X'7F'-X'9F' in the fixture page; the cp037 bytes that translate to them); the pilot rejected X'00'-X'1F' only. Owner: data owner.
* (`wave-1`) The codec checks every field of a record, so a non-digit in a field CBACT04C never reads (e.g. ACCT-CREDIT-LIMIT) now stops the run with RECORD_REJECTED; the legacy program would not notice. Asked for by the ticket's acceptance. Owner: application owner.
* (`wave-1`) A fixture line longer than the record length is rejected; the pilot and tools/parity/record.py cut it. No fixture has such a line. Owner: coordinator.
* (`wave-1`) G02/G03: decode then encode changes an F-signed or minus-zero byte, so AccountRecord.withInterestPosted rewrites only the bytes of the three INT-R07 fields. No fixture record holds such a byte.

**Ticket 04-calculation-rules**

* (`wave-1b`) **INT-Q03 option B and standard S3.8 disagree on where the cycle date comes from.** `rules.yaml` INT-Q03 option B is "the cycle date of the PARM at 00.00.00" and every `expected/<case>.json` gives `2022-07-18-00.00.00.000000` for PARM `2022071800`. S3.8 says the PARM is never read as a date and the business date is its own job parameter, which `architecture.md` section 4 does not list. Built as the rule says (`InterestPolicy.intQ03CycleDate`), non-default only. Suggested owner: target architect.
* (`wave-1b`) **INT-H04 option B cannot run through the job.** `inactiveAccounts=SKIP` needs `ACCT-ACTIVE-STATUS`, which `codec/AccountRecord.decode` does not map (data map: "carried, never read"). The domain is built and tested; through the job the option stops with `ACTIVE_FLAG_NOT_SUPPLIED (INT-H04)`, exit 12 (`switch-test-output.txt`). Also open for the business: the rule says "active flag is not Y"; the expiry date is named in the rule's statement but not in option B, so it is not looked at. Suggested owner: target architect (codec), business owner (expiry).
* (`wave-1b`) **Behaviour not in section 4 of the standards: an account whose balances all have a zero rate is still rewritten at the break.** `CBACT04C:194-201, 350-356` add a total of 0.00 and set cycle credit and cycle debit to zero. Reproduced (`intR04_accountWithOnlyZeroRatesIsStillUpdatedAtTheBreak`); no fixture shows it (in i03 the account is the last one). Suggested owner: mainframe application owner.
* (`wave-1b`) **What SKIP means for the lookups is not stated by the rules.** Built as: INT-Q05 SKIP still looks up the rate (so the two fallback lines still appear) and then writes nothing; INT-Q06 SKIP adds nothing to the account; INT-H04 SKIP echoes the balance records, does no card or rate lookup, writes no transaction and does not rewrite the account. A skipped balance or account uses no transaction number. Suggested owner: business owner with the decision on each rule.
* (`wave-1b`) **INT-Q06 and INT-H04 have no fixture; INT-T09 has none either.** Tested with synthetic data only (section 2). A recorded legacy run of a sub-cent balance, an inactive account and an empty TCATBALF would settle them. Suggested owner: mainframe application owner.
* (`wave-1b`) **A missing DEFAULT rate stops without the legacy lines.** `DEFAULT_RATE_MISSING (INT-R08)` is raised after the two fallback lines; `ERROR READING DEFAULT DISCLOSURE GROUP`, the status line and `ABENDING PROGRAM` are ticket 08.
* (`wave-1c`) INT-H04 option B now runs through the job (exit 0, PASS on all seven cases); what 'inactive' means is open: built as exactly Y is active, anything else (N, space, lower case) is not; the expiry date named in the rule's statement is not looked at. Owner: business owner; data map note: target architect.
* (`wave-1c`) INT-Q03 option B against standard S3.8 on where the cycle date comes from; built as the rule says, non-default only. Owner: target architect.
* (`wave-1c`) An account whose balances all have a zero rate is still rewritten at the break (CBACT04C:194-201, 350-356); reproduced, not in section 4 of the standards.
* (`wave-1c`) What SKIP means for lookups, log lines and the counter is not stated by the rules (INT-Q05, INT-Q06, INT-H04); built as described in report.md section 3 finding 4. Owner: business owner.
* (`wave-1c`) INT-Q06, INT-T09 and an inactive account (INT-H04) have no fixture; synthetic tests only. Owner: mainframe application owner for a recorded run.
* (`wave-1c`) A missing DEFAULT rate stops with DEFAULT_RATE_MISSING (INT-R08) without the legacy lines; those are ticket 08.
* (`wave-1d`) INT-Q03 option B and S3.8 disagree on where the cycle date comes from (PARM read as a date vs a separate job parameter). Built as the rule says, non-default only. Owner: target architect.
* (`wave-1d`) INT-H04 option B runs through the job; what 'inactive' means is open: exactly Y is active, anything else (N, space, lower case) is not; the expiry date named in the rule is not looked at; the data map still says ACCT-ACTIVE-STATUS is 'carried, never read'. Owner: target architect (data map), business owner.
* (`wave-1d`) Behaviour not in standards section 4: an account whose balances all have a zero rate is still rewritten at the break (CBACT04C:194-201, 350-356). Reproduced; no fixture shows it. Owner: mainframe application owner.
* (`wave-1d`) What SKIP means for lookups, log lines and the transaction counter is not stated by the rules (INT-Q05, Q06, H04). Owner: business owner.
* (`wave-1d`) INT-Q06, INT-H04 (inactive account) and INT-T09 have no fixture; tested with synthetic data only. A recorded legacy run would settle them. Owner: mainframe application owner.
* (`wave-1d`) A missing DEFAULT rate stops (DEFAULT_RATE_MISSING, INT-R08) without the legacy lines; those are ticket 08.
* (`wave-1d`) Tickets 03 and 04 conflicted on the decoded Account (RecordLayoutTest line 65); settled by the coordinator's grant: the decoded Account now always carries the active flag.

**Ticket 05-extract-loader**

* (`wave-2`) **interest_run has no link to the job's `runId` parameter.** `run_id` is a generated BIGINT, the job parameter `runId` is text, and no column holds it. The loader inserts the row and hands `run_id` on in the job execution context (key `interestRunId`). Evidence: `db/postgres/02-interest.sql:21`, `LoadExtractsStepConfig.RUN_ID_KEY`. Suggested owner: architect (who creates the row: architecture.md section 2 names a listener `interest-run-control` that no ticket of this wave builds).
* (`wave-2`) **"After a failed load the four tables hold what they held before" holds for decode errors only.** All four files are decoded before the first TRUNCATE, so a reject changes nothing. A database failure while table N is replaced rolls back table N, but tables 1..N-1 are already replaced, because architecture.md section 5 makes one table one transaction. Evidence: `LoadExtractsStepIT.failedInsert_rollsBackThatTable`. Suggested owner: architect / data engineering lead (one transaction for the four tables, or accept).
* (`wave-2`) **The reject list has no defined place.** The schema has no reject table and architecture.md names no file. The step writes `<outputDir>/load/rejects.tsv` (file, record number, field, reason) and logs each line. Suggested owner: data engineering lead (TL3).
* (`wave-2`) **RC4 low/high key assumes the file is in the order of the target's primary key.** On host files with mixed type codes (G06) the first/last record of the file and the lowest/highest key under collation C differ. The loader reports the check per table (`TableLoad.isKeyRangeOfFile`) and does not fail on it; on all seven fixtures it is true. Suggested owner: data owner (G06).
* (`wave-2`) **TL4 profile on the seven fixtures: nothing found.** 0 accounts with several cards (G08), type codes digits only, 01 and 02 (G06), 0 minus zero (G03), 0 F-signed (G02), 0 non-blank fillers, 0 accounts not active (INT-H04), 0 balances without a rate and without a DEFAULT rate (INT-R08). Evidence: test-output.txt, lines `TL4 profile`. The fixtures therefore prove none of these gaps on production-like data. Suggested owner: data owner.
* (`wave-2`) **REL-01, REL-02, REL-08 stay open.** The loader implements the proposed options: file bridge at the dataset boundary, PostgreSQL as a copy replaced on every load, `format=host` as IBM037 with C and D signs. `format=host` is not accepted by `InterestJobParameters` yet and has no host fixture, so it is untested in the loader.

**Ticket 06-calc-step-jdbc**

* (`wave-2`) **Ticket 06 Acceptance against standard S6.5.** The ticket says "a failure ends the step FAILED with the account id in the message"; S6.5 and `InterestRunStoppedException` say no account id in the application log or the exception message. Built: the id is in the step's exit description (`batch.batch_step_execution.exit_message`), not in the log. Evidence: `CalcInterestRestartIT.intR07_forcedFailureBetweenInsertsAndUpdateLeavesNeither`. Suggested owner: target architect.
* (`wave-2`) **INT-H02 through the tables.** A second run with a new `runId` on tables that were not reloaded charges the accounts again (i01: 2001 goes 1000.00 -> 1010.00 -> 1020.00) and writes the same transaction ids under a new `run_id`. That is the legacy behaviour with rerunGuard OFF; with tables it depends on the load being repeated before every run (architecture.md section 7, rule 1). Evidence: `CalcInterestTablesIT.intH02_secondRunWithTheSameParmIsNotRefused`. Suggested owner: mainframe operations owner (REL-11, INT-H02).
* (`wave-2`) **REL-11 as proposed leaves a window the legacy does not have.** After a failed execution the committed accounts are updated and their transactions are in `interest_transaction`, status `FAILED`; until the restart nothing marks the replica tables as half-calculated except that status. Suggested owner: mainframe operations owner.
* (`wave-2`) **No link between the job's runId and interest_run.run_id in the schema (FI-4).** See gaps. Suggested owner: target architect.
* (`wave-2`) **`TransactionCounter` cannot be set to a saved value.** The restart restores it by issuing the saved number of ids on a new counter (`CalcInterestTableRun.counterAt`). A constructor `TransactionCounter(int issued)` in the domain would say it directly. Suggested owner: owner of ticket 04 / target architect.
* (`wave-2`) **`RecordValues` has no setter for a FILLER.** A row's FILLER is copied into the encoded image by offset in `JdbcBalanceSource` (and in the test support). Suggested owner: owner of ticket 03.

**Ticket 07-dataset-export**

* (`wave-2`) REL-08 (open, implemented as proposed): host format is the codec's default cp037 with C/D signs; nothing in the export names a code page. Evidence: ExportFormat.recordFormat(); host IT asserts 0xF2 and sign byte 0xC0. Owner: mainframe data owner.
* (`wave-2`) REL-03 (open, implemented as proposed): the ACCTDATA image holds every account as a whole record, also those the run did not change. Evidence: JdbcExportSource.accounts() has no filter. Owner: mainframe application owner.
* (`wave-2`) New: E4 keeps a FILLER as raw bytes. A FILLER loaded from a fixture-format extract (0x20) and exported in host format stays 0x20 inside an EBCDIC record, and the reverse. Load and export of one run must use the same format, or E4 must say how a FILLER crosses formats. Owner: mainframe data owner (with REL-08).
* (`wave-2`) New: the ticket says 'exported counts and sum equal to RC5' and FI-4 has no exported_* column (db/postgres/02-interest.sql:42-45). Implemented as a check against written_transaction_count / written_tran_amt_sum that refuses the export; the only write is status. If exported figures are to be stored, the schema needs columns. Owner: coordinator / business owner of RC5.
* (`wave-2`) New: the job parameter runId is text and interest_run.run_id is a generated BIGINT; no design file maps one to the other. Owner: architecture owner.
* (`wave-2`) rc.json shape: it mirrors the recorded file, which includes STEP05, STEP10, STEP20, STEP25, STEP30 of the harness's setup jobs (written as 0); only STEP15 is a step of INTCALC. What the target writes outside the harness is open (tools/parity/record.py:273). Owner: mainframe operations owner.

**Ticket 08-error-paths**

* (`wave-3`) 1. **INT-H03 and INT-H05 stay OPEN.** The proposed default STOP is built. What CBACT04C does on the host for an account    with several cards (status 02, 00, or an abend) and for an overflow (compiler options ARITH/TRUNC unknown) is not known.    No parity is claimed. Owner: mainframe application owner.
* (`wave-3`) 2. **The run log lines of the two holes are invented.** No legacy text exists. Built: `INT-H03 ACCOUNT HAS SEVERAL CARDS: <11-digit id>`    and `INT-H05 VALUE DOES NOT FIT FIELD: <field>`, each followed by `ABENDING PROGRAM`. The wording needs an owner's    yes. Owner: target architect with operations.
* (`wave-3`) 3. **INT-H05 does not name ACCT-CURR-BAL.** The rule cites the interest (S9(09)V99), WS-TOTAL-INT and the counter.    `ADD WS-TOTAL-INT TO ACCT-CURR-BAL` (`CBACT04C:352`) has no ON SIZE ERROR either; a balance beyond S9(10)V99 would lose    its leading digit. Built: stops with OVERFLOW (INT-H05), field `ACCT-CURR-BAL`    (`intH05_newBalanceAboveFieldStops`). Proposed: add line 352 to the sources of INT-H05. Owner: rules owner.
* (`wave-3`) 4. **A missing card displays `ACCOUNT NOT FOUND:`.** `CBACT04C:397` uses the text of the account read for the    cross-reference read. Reproduced as coded (S1.2). Owner: mainframe application owner (is the text relied on by operations?).
* (`wave-3`) 5. **A stop keeps the run log lines of the rolled-back chunk.** The run log file of a FAILED step holds the echo lines    of the accounts of the failing chunk although their rows were rolled back; the legacy would have committed those    accounts (it has no chunk). The restart cuts the lines off. Operations reading the SYSOUT of a failed run see accounts    that are not posted. Owner: mainframe operations owner (REL-11).
* (`wave-3`) 6. **REL-10 unchanged.** The exit code of every stop is the one constant `InterestExitCode.STOPPED` (12).
* (`wave-3`) 7. **A database failure has no legacy status.** The run log gets the legacy message without `FILE STATUS IS:`; the    application log gets the exception class and SQLSTATE only. Whether operations need a status line here is open.

**Ticket 09-parity-gate**

* (`wave-4`) **REL-11 / architecture.md section 6: a restart of the three steps does not work as designed.** Section 6 repeats `loadExtractsStep` and lets `calcInterestStep` continue after its last committed account. In one job these two contradict each other: the repeated load truncates and refills `account` (the committed interest of the first execution is gone) and opens a second `interest_run` row; the calculation continues the first row; the export is handed the second row, finds it `LOADED` and refuses. Observed with accounts 2001, 2002, 2007, 2009, 2010 and a forced failure at 2009 after the commit of 2001 and 2002: restarted job `FAILED` (`loadExtractsStep:COMPLETED, calcInterestStep:COMPLETED, exportDatasetsStep:FAILED`, `export refused: run 2 has status LOADED`), accounts 2001 and 2002 back at 1000.00 and 2400.00 while their transactions exist, run 1 `CALCULATED` with `written_account_count` 4. Test: `InterestJobRestartIT.rel11_restartOfTheThreeStepsExportsNothing`. The alternative of section 6, a new launch from the start, gives the rows of an uninterrupted run (`rel11_newLaunchAfterAStopGivesTheRowsOfAnUninterruptedRun`). To decide: the load is not repeated once the calculation has committed, or REL-11 becomes option B (restore and rerun). Suggested owner: target architect with the mainframe operations owner.
* (`wave-4`) **`interest.job.steps` is a new setting and its default is not the target.** No ticket owned joining the steps. The tests of tickets 01 to 08 launch `interestCalcJob` as one step (file adapters, or tables filled by test support), so the default stays `calc` and the three steps need `interest.job.steps=load-calc-export` with `interest.calc.store=jdbc` (profile `tables`). A production launch without the profile runs the file adapters. Suggested owner: target architect.
* (`wave-4`) **Two names for one key.** Tickets 05 and 06 use `interestRunId` in the job execution context, ticket 07 reads `interest.run.id`; `batch/InterestRunHandover` copies one to the other. One name would remove the class. Suggested owner: target architect.
* (`wave-4`) **`interest_run.finished_at` is empty on a run that reached `EXPORTED`, and `started_at` is the run clock** (2022-07-05 10:00:00 in every parity run), although the schema comment says "wall clock of the target run". Seen in the `db` service after the gate. Suggested owner: tickets 05 and 07 / data engineering lead.
* (`wave-4`) **RC5 as the gate checks it holds only for INT-Q04 as coded.** The assertion is the strategy's sentence: sum of TRAN-AMT = balance change + interest of the last account. With `interest.policy.updateLastAccount=true` the last term must go. Suggested owner: business owner (INT-Q04).
* (`wave-4`) **The gate proves the as-coded candidate on seven cases only.** INT-Q06, INT-R08, INT-H02 to INT-H06 have no fixture; the host format (REL-08) cannot be run through the job because `InterestJobParameters` accepts `format=fixture` only; RC3, RC4 and RC6 are not checked by the gate.
* (`wave-4`) **"Required CI check" is a branch protection setting.** The workflow now runs the Maven suite, the gate and the negative control on every pull request; making the job `parity` required for `main` and `demo/05b-integration` is a repository setting a person has to set. Until this ticket CI did not build `java/interest` at all.
* (`wave-4`) **The `db` service is shared by every launch.** Each launch replaces the four replica tables; `interest_run` and `interest_transaction` keep every run (retention is REL-12). The gate finds the row of a launch as the one above the highest `run_id` before it, because the job execution context is stored as serialized Java, not as text SQL can read.
* (`wave-4`) Open items of earlier tickets, unchanged: a stopped run writes no `rc.json`; `JdbcAccountStore` raises `WRITE_OUTPUT` for a failed account update and `batch/DatabaseFailureStops` turns it into `ERROR RE-WRITING ACCOUNT FILE`.

**Ticket 10-data-move-kit**

* (`wave-5`) **RC1 to RC4 are meant to be checked between the load and the calculation (CO2), but one launch runs the three steps.** `interest.job.steps` is `calc` or `load-calc-export`; there is no launch that stops after `loadExtractsStep`. After the calculation `carddemo.account` no longer holds the unloaded ACCT-CURR-BAL, ACCT-CURR-CYC-CREDIT and ACCT-CURR-CYC-DEBIT (INT-R07), so on a calculated run the kit compares these three with `interest_run.loaded_*` only and leaves them out of RC3, and says so in the report. A go or no-go after RC1-RC4 and before the calculation needs a load-only launch. Evidence: `recon-report.md`, the RC3 note of `account`. Suggested owner: target architect, mainframe operations owner (CO2, CO4).
* (`wave-5`) **RC4 compares the first and last record of the file with the lowest and highest key in collation C.** That holds for the fixtures (digits only). An unload in EBCDIC key order with letters and digits in TRANCAT-TYPE-CD or in the group id has another first or last record (G06), and RC4 would then show DIFFERENT on correct data. Suggested owner: mainframe data owner (G06), data engineering lead (RC4).
* (`wave-5`) **A run does not name its input.** `interest_run` holds no input directory and the replica tables hold the last load only, so a report without a new load (`RECON_LOAD=no`) relies on the operator giving the files of that run; the kit shows a wrong pairing as DIFFERENT (`recon-report.md`, error path), it cannot name the right files. Suggested owner: data engineering lead (REL-12).
* (`wave-5`) **The RC5 identity is the strategy's sentence and holds for INT-Q04 as coded.** With `interest.policy.updateLastAccount=true` the report shows RC5 DIFFERENT; `interest_run` does not record the policy of the run, so the kit cannot tell. Reported by ticket 09 as well. Suggested owner: business owner (INT-Q04).
* (`wave-5`) **SS4 classes are proposals from single lines.** The kit gives INT-Q03, INT-Q01, INT-Q02 or G05 only where the two values show it and `defect` to everything else; a difference that follows from another (the balance after a one-cent interest difference, `sidebyside-report.md`) stays a `defect`. Who confirms a class, and where agreed classes are kept between cycles, is not decided; no file of agreed classes is built. Suggested owner: test lead (SS4, REL-04, REL-05).
* (`wave-5`) **The reports print keys.** RC4 prints the lowest and highest account id and card number, RC5 the id of the last account, as the reconciliation table of the strategy does. Standard S6.5 forbids account data in the application log; it says nothing about reports. On production data the report is then a document with account ids and card numbers. Suggested owner: data owner.
* (`wave-5`) **The comparator can only compare with `fixtures/<chain>/<case>/expected`.** The kit uses `tools/parity/compare.py` unchanged by pointing its `FIXTURES` root at a link to the legacy directory and calling `compare_case`; a legacy directory without `manifest.json` would compare no dataset and is refused as NOT AVAILABLE. A file that exists only in the target directory is not reported by the comparator, and its Markdown report stops at 40 rows (the kit reads the full list). An `--expected <dir>` option would remove the link. Suggested owner: owner of FI-2.
* (`wave-5`) **RC6 cannot be shown off-host.** It needs ACCTDATA unloaded again after the write-back; the report lists it as NOT AVAILABLE and leaves it out of the result. Suggested owner: mainframe operations owner.
* (`wave-5`) **The kit reads the fixture format only**, like the job (`format=host`, REL-08). A trial load of host files needs the host format in the job parameters first. Suggested owner: mainframe data owner (REL-08).
* (`wave-5`) **`interest_run.finished_at` is empty on an EXPORTED run and `started_at` is the run clock** (reported by ticket 09): the report prints status and PARM of the run and no time, so two runs with the same PARM can be told apart by `run_id` only. Suggested owner: data engineering lead.

## 8. Outputs

| Path | What it is |
|---|---|
| `modernization/design/waves/fan-out.md` | this document: wave plan and results |
| `modernization/design/waves/combined-check-wave-1.txt` to `-5.txt` | output of the combined check after each wave; wave 5 is the final one |
| `modernization/design/waves/combined-check.sh` | the combined check |
| `modernization/design/waves/wave-1.py`, `wave-1b.py`, `wave-1c.py`, `wave-1d.py`, `wave-2.py`, `wave-3.py`, `wave-3b.py`, `wave-4.py`, `wave-5.py` | the workflow script of each run |
| `.agents/skills/interest-ticket-build`, `interest-ticket-verify`, `interest-pitfalls` | the corrected procedure skills |
| `java/interest/**`, `db/postgres/02-interest.sql`, `db/postgres/tests/**`, `Makefile`, `.github/workflows/**` | the code of tickets 02 to 10 |
| `java/interest/evidence/<ticket>/` | each ticket's evidence: report, test output, parity report |

Not produced: the full JSON answers of the children are on the coordinator's machine only (their content is in
the pull requests and the evidence directories); no combined check on the very last revision (section 2.2).
