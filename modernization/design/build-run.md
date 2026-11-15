# Step 5b - build run of tickets 02 to 10

Workflow `modernization/design/build-workflow.py`, run id `wfr-5764ca017ded4e739480040c227a6576`, started from
`demo/05b-integration` at `c20799a` (= `demo/05a-skills` plus the workflow script). Four groups, one child session each.
All four passed on the first attempt: no retry, no skip, no decision asked for. Child sessions used 125.12 ACUs.

No pull request was merged. The four group pull requests are open; their content is in `demo/05b-integration` as one
squash commit per group, made by the group's session after its own check passed.

## 1. Groups

| Group | Tickets | Started after | State | Attempts | Session | Pull request | Combined as | ACUs |
|---|---|---|---|---|---|---|---|---|
| g1-groundwork | 02, 03 | - | passed | 1 | [09a2f0a1](https://partner-workshops.devinenterprise.com/sessions/09a2f0a1ee7c45f28e0d1571cc7e9a72) | [#196](https://github.com/Cognition-Partner-Workshops/ts-cobol-carddemo/pull/196) (open) | `6e3dda8` | 27.49 |
| g2-logic | 04, 06, 08 | g1 passed | passed | 1 | [de490788](https://partner-workshops.devinenterprise.com/sessions/de490788dbac41b6be33d755403025e8) | [#197](https://github.com/Cognition-Partner-Workshops/ts-cobol-carddemo/pull/197) (open) | `57d784a` | 36.64 |
| g3-data-flow | 05, 07, 10 | g1 passed (side by side with g2) | passed | 1 | [8e92c25a](https://partner-workshops.devinenterprise.com/sessions/8e92c25a4bfd48a88359c4afd79cab39) | [#198](https://github.com/Cognition-Partner-Workshops/ts-cobol-carddemo/pull/198) (open) | `f7a7126` | 46.68 |
| g4-gate | 09 | g2 and g3 passed | passed | 1 | [5b87eb9c](https://partner-workshops.devinenterprise.com/sessions/5b87eb9c8526413984093e7740e458a0) | [#199](https://github.com/Cognition-Partner-Workshops/ts-cobol-carddemo/pull/199) (open) | `079b641` | 14.31 |

## 2. Tickets

Tests and parity are those of the ticket's own evidence (`java/interest/evidence/<ticket>/test-output.txt` and
`parity-report.md`), produced on the group's branch when the ticket was finished. g2 and g3 ran side by side, so the
test totals of tickets 05, 07 and 10 do not include g2's tests and their parity runs do not include ticket 04.

| Ticket | Group | State | Session | PR | Tests | Parity (`compare.py`, seven interest cases) | Skills used |
|---|---|---|---|---|---|---|---|
| 02-schema | g1-groundwork | built | [09a2f0a1](https://partner-workshops.devinenterprise.com/sessions/09a2f0a1ee7c45f28e0d1571cc7e9a72) | [#196](https://github.com/Cognition-Partner-Workshops/ts-cobol-carddemo/pull/196) | 60 unit, 12 integration, 0 failures or errors, BUILD SUCCESS | 5 of 7 PASS; i03, i04 FAIL (pilot exceptions, ticket 04 not yet in the branch) | interest-conversion-standards, interest-ticket-build, interest-ticket-verify, interest-pitfalls |
| 03-record-codec | g1-groundwork | built | [09a2f0a1](https://partner-workshops.devinenterprise.com/sessions/09a2f0a1ee7c45f28e0d1571cc7e9a72) | [#196](https://github.com/Cognition-Partner-Workshops/ts-cobol-carddemo/pull/196) | 162 unit, 12 integration, 0 failures or errors, BUILD SUCCESS | 5 of 7 PASS; i03, i04 FAIL (pilot exceptions, ticket 04 not yet in the branch) | interest-conversion-standards, interest-ticket-build, interest-ticket-verify, interest-pitfalls |
| 04-calculation-rules | g2-logic | built | [de490788](https://partner-workshops.devinenterprise.com/sessions/de490788dbac41b6be33d755403025e8) | [#197](https://github.com/Cognition-Partner-Workshops/ts-cobol-carddemo/pull/197) | 282 unit, 20 integration, 0 failures or errors, BUILD SUCCESS | 7 of 7 PASS | interest-conversion-standards, interest-ticket-build, interest-ticket-verify, interest-pitfalls |
| 05-extract-loader | g3-data-flow | built | [8e92c25a](https://partner-workshops.devinenterprise.com/sessions/8e92c25a4bfd48a88359c4afd79cab39) | [#198](https://github.com/Cognition-Partner-Workshops/ts-cobol-carddemo/pull/198) | 195 unit, 28 integration, 0 failures or errors, BUILD SUCCESS | 5 of 7 PASS; i03, i04 FAIL (pilot exceptions, ticket 04 not yet in the branch) | interest-conversion-standards, interest-ticket-build, interest-ticket-verify, interest-pitfalls (*) |
| 06-calc-step-jdbc | g2-logic | built | [de490788](https://partner-workshops.devinenterprise.com/sessions/de490788dbac41b6be33d755403025e8) | [#197](https://github.com/Cognition-Partner-Workshops/ts-cobol-carddemo/pull/197) | 282 unit, 36 integration, 0 failures or errors, BUILD SUCCESS | 7 of 7 PASS | interest-conversion-standards, interest-ticket-build, interest-ticket-verify, interest-pitfalls |
| 07-dataset-export | g3-data-flow | built | [8e92c25a](https://partner-workshops.devinenterprise.com/sessions/8e92c25a4bfd48a88359c4afd79cab39) | [#198](https://github.com/Cognition-Partner-Workshops/ts-cobol-carddemo/pull/198) | 204 unit, 53 integration, 0 failures or errors, BUILD SUCCESS | 5 of 7 PASS; i03, i04 FAIL (pilot exceptions, ticket 04 not yet in the branch) | interest-conversion-standards, interest-ticket-build, interest-ticket-verify, interest-pitfalls (*) |
| 08-error-paths | g2-logic | built | [de490788](https://partner-workshops.devinenterprise.com/sessions/de490788dbac41b6be33d755403025e8) | [#197](https://github.com/Cognition-Partner-Workshops/ts-cobol-carddemo/pull/197) | 299 unit, 43 integration, 0 failures or errors, BUILD SUCCESS | 7 of 7 PASS | interest-conversion-standards, interest-ticket-build, interest-ticket-verify, interest-pitfalls |
| 09-parity-gate | g4-gate | built | [5b87eb9c](https://partner-workshops.devinenterprise.com/sessions/5b87eb9c8526413984093e7740e458a0) | [#199](https://github.com/Cognition-Partner-Workshops/ts-cobol-carddemo/pull/199) | 396 unit, 105 integration, 0 failures or errors, BUILD SUCCESS | 7 of 7 PASS | interest-conversion-standards, interest-ticket-build, interest-ticket-verify, interest-pitfalls |
| 10-data-move-kit | g3-data-flow | built | [8e92c25a](https://partner-workshops.devinenterprise.com/sessions/8e92c25a4bfd48a88359c4afd79cab39) | [#198](https://github.com/Cognition-Partner-Workshops/ts-cobol-carddemo/pull/198) | 254 unit, 64 integration, 0 failures or errors, BUILD SUCCESS | 5 of 7 PASS; i03, i04 FAIL (pilot exceptions, ticket 04 not yet in the branch) | interest-conversion-standards, interest-ticket-build, interest-ticket-verify, interest-pitfalls (*) |

(*) The reports of g3's tickets have no "Skills used" line. The list is taken from what each report contains: the
standards checklist (section 4), the evidence produced with `interest-ticket-verify`'s `parity-report.sh`, and the
corrections to the three procedure skills (section 5).

## 3. Final combined check on `demo/05b-integration`

Run by the coordinating session at `079b641`, after the last group combined and before the skill commits (which change
no code; the corrected query of `parity-report.sh` was run once against the database on its own).

| Command | Result |
|---|---|
| `mvn -B -f java/interest/pom.xml clean verify` | 396 unit, 105 integration, 0 failures, 0 errors, BUILD SUCCESS |
| `make up && make build` | exit 0 |
| `make parity-java` | 7 of 7 cases PASS, exit 0 |
| `make parity` (legacy chains) | all 24 cases PASS, exit 0 |

## 4. Failures

No group failed, was retried, was skipped or returned needs-decision. Failures the sessions hit and fixed while
building are in section 6 (Method) of each `java/interest/evidence/<ticket>/report.md`.

One observation of the coordinating session, not reproduced: the first `mvn clean verify` on the starting branch ended
with 12 errors in `InterestCalcJobIT` (`NoSuchBeanDefinitionException`: no `Job` bean); two reruns of the same command on
the same revision passed with 60 and 12. No child session reported it. Cause unknown.

## 5. Skill corrections

Reported by the child sessions in section 5 or 6 of their reports; applied by the coordinating session, one commit per
skill. The child sessions did not edit any skill.

### interest-ticket-build - commit `98c9065`

| Ticket | Step | What was wrong | Corrected text as reported |
|---|---|---|---|
| 04 | 8 | The quoted totals are the pilot's (60 and 12); on `demo/05b-integration` after tickets 02 and 03 they are 162 and 12 | "Compare with the totals in the newest `java/interest/evidence/*/test-output.txt` of your base branch, not with the figures quoted here." |
| 04 | 5 | Says packages `jdbc`, `load`, `export`, `recon` do not exist and schema `carddemo` has no table beyond `01-transaction-type.sql`; `db/postgres/02-interest.sql` exists since ticket 02 | "`db/postgres/02-interest.sql` (ticket 02, FI-4) holds the six tables of the stream; the Java tests do not load it until ticket 06." |
| 05 | scope check (step 9) | says nothing about a step bean that cannot be added to the job because `batch/**` belongs to another ticket | "A new step is a `@Bean Step` in the ticket's own package; test it by launching a job built in the test. Adding it to `InterestJobConfig` belongs to the ticket that owns `batch/**`; report it in section 2." |
| 06 | 5 | The layout table has no row for package `jdbc`, and says the integration test is `InterestCalcJobIT` only | add: "`.../jdbc` - JDBC adapters of the ports (ticket 06); `batch/JdbcCalcStepIT` runs the step on the tables with `interest.calc.store=jdbc`; `src/test/.../jdbc/InterestTables` loads a case's input into the replica tables" |
| 07 | scope check (step 9) | a ticket deliverable can name a file outside its Allowed files (07: "say so in the README", README not allowed) | "When a Deliverable names a file outside the Allowed files, put the text in the ticket's package-info and report.md section 2 and name the ticket that may edit the file." |
| 08 | 5 | no row for the stop classes | add: "`domain/RunStop`, `StopReason`, `LegacyFile` - every stop goes through `RunStop`, which writes the legacy lines; never throw `InterestRunStoppedException` directly from the calculation." |
| 10 | step 2 | does not say how to run a second main class of the application | "A command other than the job is started from the same jar with `java -cp interest.jar -Dloader.main=<class> org.springframework.boot.loader.launch.PropertiesLauncher`; start the context with `spring.batch.job.enabled=false`." |

### interest-ticket-verify - commit `b781315`

| Ticket | Step | What was wrong | Corrected text as reported |
|---|---|---|---|
| 02 | 8 | `parity-report.sh` refuses to run without a case; ticket 02 names none | reported as a failure hit; covered by the correction of tickets 05 and 10 |
| 04 | 3 | The quoted result (i03, i04 FAIL, `make parity-java` Error 1) is the pilot's | "From ticket 04 on, all seven cases PASS and `make parity-java` exits 0; any FAIL is a regression." |
| 04 | 8 | Does not say where the output of a switch test (step 4) goes; the ticket asks for it as evidence | "Paste the command and output of a step-4 run the ticket asks for into section 6 of `report.md`." |
| 05 | 8 (`parity-report.sh <ticket> <case the ticket names>`) | a ticket whose code is not in the job flow (05) names fixture inputs, not parity cases; the script needs at least one case and marks it "acceptance case of the ticket" | "If the ticket's step is not part of `interestCalcJob`, pass `i01-baseline` as a regression case and change its scope cell to `regression only`; the ticket's own figures come from its integration test in `test-output.txt`." |
| 05 | 7 | the expected table list shows only the two `transaction_type` tables; since ticket 02 the schema also has `account`, `card_xref`, `tran_cat_balance`, `disclosure_group`, `interest_transaction`, `interest_run` | list the eight tables of `db/postgres/01-*.sql` and `02-interest.sql` |
| 06 | 7 | The query shows the job repository only; from ticket 06 the run's row and rows are in schema `carddemo` | add: `select run_id, status, written_transaction_count, written_tran_amt_sum, written_account_count from carddemo.interest_run order by run_id desc limit 1` |
| 06 | 8 | `parity-report.sh` proves the file path only; a ticket whose acceptance is "through the tables" has nothing to put in the report from it | add: "For a ticket proven through the tables, say in `report.md` section 1 which integration test compares the tables with `fixtures/interest/<case>/expected`; the parity report then shows no regression." |
| 07 | 8 | `parity-report.sh` only compares `work/parity-java/<case>/actual`, the output of the job. A ticket whose acceptance is "compare.py PASS on the exported directories" (07) has no place for that in the generated report | "If the ticket's evidence is a directory the job does not write, run `compare.py interest <case> --actual <dir> --label <name>` on it yourself and append the command and its output as a last section of parity-report.md." |
| 07 | 8 | says the evidence comes "from the committed revision" but `test-output.txt` is from `clean verify`, which deletes `target/`; a later step that needs files the tests wrote must run after it and before the next `clean` | add: "Files the tests leave under `java/interest/target/` are those of the last `clean verify`; use them before running Maven again." |
| 08 | 8 | `parity-report.sh <ticket>` without a case exits 2; a ticket whose only fixture requirement is "the seven cases still pass" has no case to name | add: "For a ticket with no acceptance case of its own, pass every case: `parity-report.sh <ticket> $(ls fixtures/interest)`." |
| 08 | 6 | The error path is checked with one unreadable input file only | add: "From ticket 08 the stop conditions are in `StopConditionsTest` and `ErrorPathsJdbcIT`; a new stop needs a row in both and the run-log lines cited from CBACT04C." |
| 09 | 7 and `parity-report.sh` | the step list of the job repository query is in no fixed order (`exportDatasetsStep` before `loadExtractsStep` in some rows): `string_agg` without `ORDER BY` | reported as a failure hit, without text; applied as `order by s.step_execution_id` inside the `string_agg` |
| 10 | 8 | the evidence table has three files; a ticket whose final-revision evidence is a set of reports (10: "the seven reports from the last commit") has no place for them | add a row: "other evidence the ticket's Verification names: files or a sub-folder in the ticket's evidence folder, listed at the top of report.md" |
| 10 | 8 | `parity-report.sh` marks the case on its command line "yes - acceptance case of the ticket"; for a ticket that does not change the job (10) the case is a regression case | "For a ticket that does not change `interestCalcJob`, change that cell to `regression case of the job`." |
| 10 | 3 / 10 | says to run `git checkout -- tools/parity/__pycache__` but not that importing `tools.parity.compare` from Python also leaves an untracked `compare.cpython-*.pyc` there | "Run Python that imports the comparator with `PYTHONDONTWRITEBYTECODE=1`; delete an untracked `tools/parity/__pycache__/compare.*.pyc`." (reproduced: first run of `make sidebyside-java`) |

### interest-pitfalls - commit `9ab8890`

| Ticket | Step | What was wrong | Corrected text as reported |
|---|---|---|---|
| 04 | new P8 | Not listed: a test that reads JSON cannot use Jackson | "Symptom: `package com.fasterxml.jackson.databind does not exist`. Cause: the module has no Jackson. Fix: `org.json.JSONObject` from `spring-boot-starter-test`; do not add a dependency." |
| 05 | missing | the datasource sets `search_path` to `batch`: unqualified table names in application SQL resolve to the job repository schema | "P: qualify every application table as `carddemo.<table>`; an unqualified name is looked up in schema `batch`." (not reproduced: the loader was written with qualified names from the start) |
| 05 | missing | an integration test needs both init scripts; `CREATE SCHEMA carddemo` is only in `01-transaction-type.sql`, so `02-interest.sql` alone cannot create its tables | "P: copy `db/postgres/01-transaction-type.sql` and `02-interest.sql` into `/docker-entrypoint-initdb.d/` of the Testcontainers `postgres:15`, as `ExtractLoaderIT` does." |
| 07 | missing | `file/RecordedReturnCodes` is package-private: another package cannot reuse the rc.json writer | "P: rc.json outside `file/` needs its own writer (`export/ExportedReturnCodes`); test it against every case's recorded rc.json." (not reproduced: seen while reading) |
| 10 | missing | `compare.py` has no option for the legacy directory (F1) | "P: to compare two directories, lay the legacy one out as `<root>/interest/<name>/expected` with a manifest.json and point `compare.FIXTURES` at `<root>`; never edit the comparator." |
| 10 | missing | a factory another ticket needs may be package-private in a package it may not edit (`ExtractProfile.of`, `RoundTrip.compare`, `ExtractDecoder`) | "P: make what a later ticket of the design reads public when you write it; a later ticket cannot widen it." (reproduced: F7) |

How they were applied, where the applied text differs from the reported one:

- The four corrections to `interest-ticket-verify` step 8 about `parity-report.sh` and its case argument (tickets 02, 05, 06, 07, 08, 10) are one table in step 8.
- Ticket 10's second-main-class correction names step 2 of `interest-ticket-build`; it is in step 7 (Build), where the jar is built.
- Ticket 04's sentence "the Java tests do not load it until ticket 06" reads "until tickets 05 and 06": `ExtractLoaderIT` of ticket 05 loads the script too.
- Ticket 05's `search_path` pitfall and ticket 07's rc.json pitfall were not reproduced by their sessions. The first is under "Reported, not reproduced"; the second is P10 and says so.
- Ticket 09's ordering correction also changes `.agents/skills/interest-ticket-verify/parity-report.sh` (the same query).
- Pilot figures quoted in the skills (60 and 12 tests, i03 and i04 failing, two tables) stay, marked as the pilot's, with the present state beside them.

Not applied: failures listed only in a report's Method section without a proposed skill text (ticket 02: `regclass::text` omits a schema on the search_path, POSIX `sh` has no `pipefail`; ticket 03: a copybook entry cut at column 72; ticket 09: two job-context keys for the run id, a 403 on reading branch protection). They are candidates for `interest-pitfalls`.

## 6. Proposals for `interest-conversion-standards` (not applied)

The standards skill was not changed. The proposals, as written by each ticket's session:

### 02-schema

1. **Section 9 has no way to say "schema side".** S3.1, S3.4, S3.8 and rows b, g, h, j, p, u are worded for Java. A schema
   ticket can only mark them N/A or stretch them. Proposal: one sentence in section 9: "for a ticket without Java, a line
   that names a Java construct is checked against its database counterpart (NUMERIC(p,s) for BigDecimal, CHAR(n)/BYTEA for
   width and FILLER) and marked PASS with the SQL assertion that shows it".
2. **No standard for SQL scripts.** Nothing says whether a DDL script is transactional, what a second run does, how
   constraints are named, or that tests of a script live in `db/postgres/tests` and run on a scratch database. Proposal:
   a short S7.5 "SQL tests: plain psql with ON_ERROR_STOP, one transaction rolled back, expected values written by hand
   from the data map, run on an empty database created for the test", and S2.6 "DDL scripts are one transaction and fail
   cleanly on a second run".
3. **S3.7 names key order "in one place" but not the collation of non-key text.** See finding 4. Proposal: say that text
   columns outside keys are never ordered or range-compared in SQL, or give them `COLLATE "C"` in the data map.
4. **S7.3** says integration tests run on a Testcontainers PostgreSQL 15; a psql test against the compose service is also
   "PostgreSQL 15 in a container" and should be named as allowed for tickets without Java.

### 03-record-codec

1. **S3.5/S3.6 do not say what the codec validates.** Proposal: "the codec decodes and validates every field of a record,
   used by the job or not; a record with a bad byte in any field is rejected" - or the opposite. Today both readings
   satisfy the text (finding 5).
2. **Section 4 row f / G07 has no definition of "not a character".** Proposal: add the rule of finding 3 (control
   characters and unmapped bytes) to row f, or name the fields where low-values are legal.
3. **Row d should say that host sign bytes are read by zone and digit nibble**, never by translating to characters first
   (finding 4). Today it only names the overpunch characters.
4. **No standard for where the layouts come from at run time.** Proposal for S3.6: "layouts are read from the copybook
   text; when a copy is carried in the jar a test compares it with `app/cpy`".
5. **Error text.** Section 6 gives the stop reasons but not the form of the detail. Proposal: "a rejection names file,
   record number (from 1) and copybook field, and never prints a field value" (S6.5 then holds by construction).
6. **S7.4** could name the accepted independent sources for expected bytes: hand-written text or hex, the JDK charset for
   whole-record translation, `wc -l` for counts.

### 04-calculation-rules

1. **S3.8 against INT-Q03 option B.** S3.8 says the business date of `timestampSource=CYCLE_DATE` "is a job parameter
   too", but `architecture.md` section 4 lists no such parameter and `rules.yaml` defines option B as "the cycle date of
   the PARM". Proposed text: "With `timestampSource=CYCLE_DATE` the stamp is the first eight characters of `parmDate`,
   copied as text into `YYYY-MM-DD-00.00.00.000000`; a `parmDate` that does not start with eight digits stops the run."
2. **Section 8 of the architecture calls INT-H01 an "empty fee hook"; S5.2 and row aa say the empty paragraph is not
   ported.** The ticket repeats "empty fee hook". Built as row aa says (no hook, one Javadoc sentence, one test).
   Proposed: say in S5.2 that INT-H01 has no code and no extension point until a fee rule exists.
3. **Section 4 row g** names `intR03_defaultGroupKeyPadded` and says the fallback group "is the constant `DEFAULT`
   compared as CHAR(10)". The domain holds trimmed text (S3.5), so the comparison as CHAR(10) happens in the adapter.
   Proposed: "the domain passes `DEFAULT`; the adapter compares it with the 10-byte field".
4. **S7.1 "a switch has a test for the default and for each other value"** does not say what the expected side of a
   non-default value is. Proposed: "the expected values of a non-default option come from
   `modernization/rules/interest/expected/<case>.json` where it lists one, else are worked by hand."
5. **Section 4 has no row for the three SKIP options** (what else a skipped balance or account leaves out: rate
   lookup, fallback lines, transaction id, account update). Proposed: one row per option once the business answers.

### 05-extract-loader

- S2.3: say which ticket adds a new step to the job flow when the step's ticket may not edit `batch/**`.
- S6: add the load to the status table - reject list location, and that a failed load leaves the four tables as they were (all files decoded first).
- S3.7: name `COLLATE "C"` as the key order of text keys in SQL, so a database locale cannot change RC4 or the export order.

### 06-calc-step-jdbc

1. **S6.5 against ticket 06 "account id in the message".** Proposed addition to S6.5: "The account at which a step
   failed is recorded in the step's exit description in the job repository; it is not written to the application log."
2. **S2.3 / S6.3 name the restart state but not where the run log file lives.** Proposed: "The run log is written to
   `<outputDir>/sysout/INTCALC.STEP15.txt` as the step runs; the export does not replace it."
3. **S2.5 says the counter "comes from the step's restart state"** but the pilot's `TransactionCounter` cannot be
   started at a value. Proposed: name the constructor (`new TransactionCounter(issuedSoFar)`).
4. **No standard says how the calculation finds its row in `interest_run`.** Proposed: "the run's `run_id` is in the
   job execution context under `interest.runId`; the first step that needs it creates the row."
5. **S3.7** could say that the reader's cursor needs a connection of its own, so the pool must hold at least two.

### 07-dataset-export

- S6.4: say who owns `sysout/INTCALC.STEP15.txt` once `exportDatasetsStep` replaces the output directory (F2).
- S6.3: name the order "files moved into place, then status EXPORTED" and what a restart does when the status update failed (F6).
- S3.6: state that FILLER bytes are written back as stored, also when the export format differs from the load format (F5).

### 08-error-paths

1. **Section 6 / S6.1 does not say what a stop writes for INT-H03 and INT-H05**, which have no legacy text. Proposed:
   "For a stop the legacy has no message for, the run log gets one line starting with the rule id, then
   `ABENDING PROGRAM`."
2. **S6.1 does not say whether `FILE STATUS IS: NNNN00xx` is written when the cause is a database failure.** Proposed:
   "The status line is written only where the condition determines the legacy status (record not found: 23)."
3. **S3.4 names TRAN-AMT and the counter only.** Proposed: add WS-TOTAL-INT (S9(09)V99) and ACCT-CURR-BAL (S9(10)V99).
4. **S6.5 against the legacy line `ACCOUNT NOT FOUND: <id>`.** Proposed: say that the run log may hold account ids
   where the legacy displays them, the application log never.
5. **S3.6**: allow the 11-digit account id of a text (TRAN-DESC, `ACCOUNT NOT FOUND:`) to be formatted in one named
   domain place, or give the codec a function for it.
6. **S5.1**: say what a switch value that is not built does - proposed: refused at start, naming the decision.

### 09-parity-gate

1. **S6.4 against the run log.** S6.4 says a failed run leaves no SYSTRAN and, for file adapters, no output directory;
   architecture section 6 needs the run log of a stopped calculation for the restart. Say where the run log of a stopped
   run lives when the steps are joined (in the output directory, or beside it like the load's reject list), so "no
   output" has one meaning.
2. **A standard for job-context keys.** Nothing says how steps hand data to each other; two tickets chose two keys for
   the run id. Proposed S2.6: values passed between steps are named once, in `batch`, and every step uses that constant.
3. **S2.3 and the restart of the whole job.** The standard describes the restart of `calcInterestStep` and says the load
   "repeats"; it does not say that a repeated load replaces account rows the failed calculation had committed. Add the
   expected state of the replica tables on a restart, or say that a restart must not repeat the load (REL-11).
4. **Section 9 for a gate ticket.** Most lines are N/A for a ticket that changes no product code. A line for gates would
   help: "the gate fails when the candidate is wrong (negative control), and a failed run is a failed case".
5. **S7.1** has no naming rule for a test that proves a work item (INT-T ids) rather than a rule; `intT09_...` is already
   used for the empty-input behaviour (row m), which collides with ticket 09's work item id INT-T09.

### 10-data-move-kit

- S6.5: say what a report (not a log) may show of a record: proposal "key ends and aggregates only".
- S7: add "a report line that cannot be computed is NOT AVAILABLE; test each cause" as a rule for reconciliation code.
- S8.1: say whether calling the comparator's functions with another fixtures root counts as using it unchanged (F1).
