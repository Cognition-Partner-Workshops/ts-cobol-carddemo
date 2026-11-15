# Build run - interest stream, tickets 02 to 10

> **PROPOSED - not approved.** Written by an agent in step 5b from the run data of
> `modernization/design/build-workflow.py` and from the pull requests and evidence folders the run data names.
> Owner who must review the nine pull requests and decide the corrections and proposals below: **the target architect.**

Integration branch `demo/05b-integration` at `db2ad19` when this report was written. Nothing was merged: every
ticket branch was combined onto the integration branch as one squash commit by its integration session, and
every pull request named here is open. This step changed no code, no skill and no ticket; the states below are
the states of the run data, unchanged.

Pilot: `01-pilot-thin-slice` (built before this run, not a ticket of this run). Tickets already covered: none.

## 1. Tickets

Counts are Maven's `Tests run` with 0 failures, 0 errors, 0 skipped in every quoted run. "Build" is the build
session on the ticket branch; "combined" is the integration session on the squash of the ticket onto
`demo/05b-integration`. Parity is `make parity-java` with `tools/parity/compare.py` on the seven interest cases
(i01 to i07); "legacy" is `make parity` on the unchanged estate. `run.sh` is `db/postgres/tests/run.sh` on PostgreSQL 15.

| Ticket | State | Build session(s) | PR | Tests | Parity | Skills used |
|---|---|---|---|---|---|---|
| `02-schema` | integrated | [f86da132](https://partner-workshops.devinenterprise.com/sessions/f86da132a1f64759b9d479f086605256) | [#186](https://github.com/Cognition-Partner-Workshops/ts-cobol-carddemo/pull/186) | Build: no Java; psql apply exit 0, second apply exit 3 (fails cleanly), 48 assertions PASS, `run.sh` PASS; regression 60 unit / 12 integration. Combined: 60 / 12, BUILD SUCCESS; `run.sh` PASS | Names no fixture case. Build (regression) and combined: 5 PASS, i03 and i04 FAIL as on the base (owner ticket 04). Legacy `make parity`: 24 PASS | interest-conversion-standards, interest-ticket-build, interest-ticket-verify, interest-pitfalls, pr-comment-information |
| `03-record-codec` | integrated | [55124bf9](https://partner-workshops.devinenterprise.com/sessions/55124bf9f7d3434b9d619ad98f58398c) | [#187](https://github.com/Cognition-Partner-Workshops/ts-cobol-carddemo/pull/187) | Build: 168 unit / 12 integration. Combined: 168 / 12, BUILD SUCCESS; `run.sh` PASS | Names i01: PASS. Build and combined: 5 PASS, i03 and i04 FAIL as on the base (owner ticket 04). Legacy: 24 PASS | interest-conversion-standards, interest-ticket-build, interest-ticket-verify, interest-pitfalls |
| `04-calculation-rules` | integrated | [99a095a3](https://partner-workshops.devinenterprise.com/sessions/99a095a3d03f46d2bd8e21112995d1e3), [af377c0e](https://partner-workshops.devinenterprise.com/sessions/af377c0ebc0540a6949b3b670255a0bb) | [#188](https://github.com/Cognition-Partner-Workshops/ts-cobol-carddemo/pull/188) | Build 1: 205 unit / 20 integration. Build 2: 313 / 20. Combined 1: not built (squash conflict). Combined 2: 313 / 20, BUILD SUCCESS; `run.sh` PASS | Build 1 and 2: 7 of 7 PASS. Combined 1: not run (squash conflict). Combined 2: 7 of 7 PASS (baseline: i03, i04 FAIL). Legacy: 24 PASS | interest-conversion-standards, interest-ticket-build, interest-ticket-verify, interest-pitfalls, pr-comment-information |
| `05-extract-loader` | integrated | [2c9dd95e](https://partner-workshops.devinenterprise.com/sessions/2c9dd95e17b24cf5934056396440d0ec) | [#190](https://github.com/Cognition-Partner-Workshops/ts-cobol-carddemo/pull/190) | Build: 182 unit / 23 integration. Combined: 344 / 52, BUILD SUCCESS; `run.sh` exit 0 | Build: 5 PASS, i03 and i04 FAIL as on its base (ticket 04 not yet integrated); loader proof 7 of 7 in `LoadExtractsStepIT`. Combined: 7 of 7 PASS. Legacy: 24 PASS | interest-conversion-standards, interest-ticket-build, interest-ticket-verify, interest-pitfalls, pr-comment-information |
| `06-calc-step-jdbc` | integrated | [06f8094d](https://partner-workshops.devinenterprise.com/sessions/06f8094d07a44ff99ac82729c1ec3a86) | [#191](https://github.com/Cognition-Partner-Workshops/ts-cobol-carddemo/pull/191) | Build: 319 unit / 44 integration. Combined: 350 / 76, BUILD SUCCESS; `run.sh` PASS | Build: 7 of 7 PASS through the tables and through the file adapters. Combined: 7 of 7 PASS. Legacy: 24 PASS | interest-conversion-standards, interest-ticket-build, interest-ticket-verify, interest-pitfalls, carddemo-parity, pr-comment-information |
| `07-dataset-export` | integrated | [c7789127](https://partner-workshops.devinenterprise.com/sessions/c77891272fba47a795ba4268111eb342) | [#189](https://github.com/Cognition-Partner-Workshops/ts-cobol-carddemo/pull/189) | Build: 185 unit / 33 integration. Combined: 330 / 41, BUILD SUCCESS; `run.sh` PASS | Build: export i01 and i06 PASS (the other five have no finished run in the ticket); `make parity-java` 5 PASS, i03 and i04 FAIL as on its base. Combined: 7 of 7 PASS. Legacy: all cases PASS | interest-conversion-standards, interest-ticket-build, interest-ticket-verify, interest-pitfalls, carddemo-parity |
| `08-error-paths` | integrated | [3c314141](https://partner-workshops.devinenterprise.com/sessions/3c31414121b44ee6af7db9898e5cd0ef) | [#193](https://github.com/Cognition-Partner-Workshops/ts-cobol-carddemo/pull/193) | Build: 361 unit / 84 integration. Combined: 382 / 110, BUILD SUCCESS; `run.sh` exit 0 | Build: 7 of 7 PASS. Combined: 7 of 7 PASS. Legacy: 24 PASS | interest-conversion-standards, interest-ticket-build, interest-ticket-verify, interest-pitfalls, pr-comment-information |
| `09-parity-gate` | integrated | [c17cb3c3](https://partner-workshops.devinenterprise.com/sessions/c17cb3c345a3445faf660983fe336a77) | [#194](https://github.com/Cognition-Partner-Workshops/ts-cobol-carddemo/pull/194) | Build: 385 unit / 119 integration. Combined: 385 / 119, BUILD SUCCESS; `run.sh` PASS | Build: 7 of 7 PASS. Combined: 7 of 7 PASS. Legacy: 24 PASS | interest-conversion-standards, interest-ticket-build, interest-ticket-verify, interest-pitfalls, carddemo-parity, pr-comment-information |
| `10-data-move-kit` | integrated | [104c34eb](https://partner-workshops.devinenterprise.com/sessions/104c34eb84ef4da19934dbe3c2b6f639) | [#192](https://github.com/Cognition-Partner-Workshops/ts-cobol-carddemo/pull/192) | Build: 365 unit / 78 integration. Combined: 371 / 102, BUILD SUCCESS; `run.sh` PASS | Build: 7 of 7 PASS. Combined: 7 of 7 PASS; `make recon-java` (7 cases) and `make sidebyside-java` exit 0. Legacy: 24 PASS | interest-conversion-standards, interest-ticket-build, interest-ticket-verify, interest-pitfalls |

Nine tickets, nine `integrated`. Full figures: `java/interest/evidence/<ticket>/` (`test-output.txt`,
`parity-report.md`, `report.md`) and `java/interest/evidence/integration/<ticket>.txt`.

### 1.1 Integrations

| Ticket | Attempt | Integration session | Result | Commit on `demo/05b-integration` | Depends on | Granted beyond the Allowed files |
|---|---|---|---|---|---|---|
| `02-schema` | 1 | [eb49b2a8](https://partner-workshops.devinenterprise.com/sessions/eb49b2a8436844febed4603fa7d18176) | passed | `d2b6511` | none | none |
| `03-record-codec` | 1 | [a9d66292](https://partner-workshops.devinenterprise.com/sessions/a9d66292662a4cd8bc0876b585d531bb) | passed | `b4ab502` | 01-pilot-thin-slice | none |
| `04-calculation-rules` | 1 | [2efd0867](https://partner-workshops.devinenterprise.com/sessions/2efd08676c1641aea3a3c4eaec69d351) | **failed** | none | 01-pilot-thin-slice | codec/AccountRecord.java may pass ACCT-ACTIVE-STATUS to the domain, the RecordLayoutTest assertions this breaks may be updated, and the ticket's new policy switches may be added to application.yml. |
| `04-calculation-rules` | 2 | [6879e7ed](https://partner-workshops.devinenterprise.com/sessions/6879e7ed466a4404bae12586a417dc24) | passed | `f09b623` | 01-pilot-thin-slice | codec/AccountRecord.java may pass ACCT-ACTIVE-STATUS to the domain, the RecordLayoutTest assertions this breaks may be updated, and the ticket's new policy switches may be added to application.yml. |
| `05-extract-loader` | 1 | [ba7431ce](https://partner-workshops.devinenterprise.com/sessions/ba7431ce763a4317a15780702ad8e3d8) | passed | `1443bb4` | 02-schema, 03-record-codec | none |
| `06-calc-step-jdbc` | 1 | [d057235c](https://partner-workshops.devinenterprise.com/sessions/d057235c2fb0406c80324bf75b19ecbf) | passed | `2b9536c` | 02-schema, 04-calculation-rules | none |
| `07-dataset-export` | 1 | [c8bfcdd1](https://partner-workshops.devinenterprise.com/sessions/c8bfcdd155be4821ba7a534d21e6a390) | passed | `38a1ad3` | 02-schema, 03-record-codec | none |
| `08-error-paths` | 1 | [31d338c1](https://partner-workshops.devinenterprise.com/sessions/31d338c169434c609f5d51a1e52fa8fe) | passed | `28401d4` | 06-calc-step-jdbc | switches multiCard and overflow in policy/InterestPolicy.java and application.yml. |
| `09-parity-gate` | 1 | [d9123e18](https://partner-workshops.devinenterprise.com/sessions/d9123e1858524ff59c0c0a8dfa4ae500) | passed | `db2ad19` | 05-extract-loader, 06-calc-step-jdbc, 07-dataset-export, 08-error-paths | batch/InterestJobConfig.java and new batch/ classes that only join the load, calculation and export steps into one job, with their tests. |
| `10-data-move-kit` | 1 | [74edc23f](https://partner-workshops.devinenterprise.com/sessions/74edc23f98714b8fb6758180050bc121) | passed | `84a6176` | 05-extract-loader, 07-dataset-export | none |

The log of the integration of ticket 04 in `java/interest/evidence/integration/04-calculation-rules.txt` is the
log of attempt 2; attempt 1 pushed nothing, its failure is quoted in section 2.1.

After their squash the branches of tickets 03, 04, 06 and 10 received further commits that merge
`demo/05b-integration` back into the ticket branch (heads `4ee5472`, `bc8d2ed`, `b6d5722`, `ec05d3c` when this
report was written; squashed at `464b93d`, `c63f18c`, `6c1b7ff`, `7820520`). These commits are not part of the
integration commits above and were not checked by an integration session.

## 2. Failures

### 2.1 Failed integrations and retries

One integration failed and one ticket was retried once: `04-calculation-rules`. No build session ended in a
failed state and no other integration failed.

**`04-calculation-rules`, integration attempt 1 - failed** (session [2efd0867](https://partner-workshops.devinenterprise.com/sessions/2efd08676c1641aea3a3c4eaec69d351)). State log:
`integrating -> retrying (Integration (combined check) failed: back to the ticket once)`. The failure as the
integration session returned it:

````text
`git merge --squash origin/demo/05-04-calculation-rules` (head 940f639c4200df8bc23ccb6ca57ed3d89f17eb25, merge base a5cce81a0d9b5ce45b066e06666f8617b721a9b9) onto demo/05b-integration at b4ab502d97e2c74f02b193bb4e02bf0a7401f0cd ("Integrate 03-record-codec"):

CONFLICT (content): Merge conflict in java/interest/src/main/java/com/carddemo/interest/codec/AccountRecord.java
CONFLICT (content): Merge conflict in java/interest/src/test/java/com/carddemo/interest/codec/RecordLayoutTest.java
Automatic merge failed; fix conflicts and then commit the result.

Not resolved; reset with `git reset --hard origin/demo/05b-integration`; nothing pushed. Ticket 04 was built on the pre-03 String/Field codec, which the integrated ticket 03 replaced with RecordLayout/RecordCodec on byte[].

AccountRecord.java, hunk 1 (class comment):
<<<<<<< HEAD
 * Layout CVACT01Y (app/cpy/CVACT01Y.cpy:4-17, RECLN 300). Data-map rows CVACT01Y/*.
 * The job reads ACCT-ID, ACCT-CURR-BAL and ACCT-GROUP-ID and writes three amounts; every other
 * byte of the record, the 178-byte FILLER included, is carried untouched (E4).
=======
 * Layout CVACT01Y (app/cpy/CVACT01Y.cpy:4-17, RECLN 300). Data-map rows CVACT01Y/ACCT-ID,
 * ACCT-ACTIVE-STATUS (read for INT-H04 only), ACCT-CURR-BAL, ACCT-CURR-CYC-CREDIT, ACCT-CURR-CYC-DEBIT, ACCT-GROUP-ID.
 * Every other byte of the record, the 178-byte FILLER included, is carried in the image untouched (E4).
>>>>>>> origin/demo/05-04-calculation-rules

AccountRecord.java, hunk 2 (fields):
<<<<<<< HEAD
    private final RecordCodec codec;
=======
    private static final Field ACCOUNT_ID = new Field("ACCT-ID", 0, 11);
    private static final Field ACTIVE_STATUS = new Field("ACCT-ACTIVE-STATUS", 11, 1);
    private static final Field CURRENT_BALANCE = new Field("ACCT-CURR-BAL", 12, 12);
    private static final Field CYCLE_CREDIT = new Field("ACCT-CURR-CYC-CREDIT", 78, 12);
    private static final Field CYCLE_DEBIT = new Field("ACCT-CURR-CYC-DEBIT", 90, 12);
    private static final Field GROUP_ID = new Field("ACCT-GROUP-ID", 112, 10);
>>>>>>> origin/demo/05-04-calculation-rules

AccountRecord.java, hunk 3 (decode):
<<<<<<< HEAD
                values.unsigned(ACCOUNT_ID),
                values.signed(CURRENT_BALANCE),
                values.text(GROUP_ID));
=======
                ACCOUNT_ID.unsigned(image),
                ACTIVE_STATUS.text(image),
                CURRENT_BALANCE.signed(image, 2),
                GROUP_ID.text(image));
    }

    public static String key(String image) {
        return image.substring(ACCOUNT_ID.offset(), ACCOUNT_ID.end());
>>>>>>> origin/demo/05-04-calculation-rules

RecordLayoutTest.java, cvact01y_decodesTheAccountOfI01:
<<<<<<< HEAD
        assertEquals(new Account(2001L, new BigDecimal("1000.00"), "GOLD"), new AccountRecord(FIXTURE).decode(accountOfI01(), SOURCE));
=======
        String image = RecordImage.fromLine(
                Fixtures.lines(Fixtures.input(CASE).resolve("AWS.M2.CARDDEMO.ACCTDATA.PS.txt")).get(0), AccountRecord.LENGTH);

        assertEquals(new Account(2001L, "Y", new BigDecimal("1000.00"), "GOLD"), AccountRecord.decode(image));
>>>>>>> origin/demo/05-04-calculation-rules
````

Tests and parity of that attempt, as returned: Baseline only, on demo/05b-integration at b4ab502d97e2c74f02b193bb4e02bf0a7401f0cd (mvn -B -f java/interest/pom.xml verify): Tests run: 168, Failures: 0, Errors: 0, Skipped: 0 (unit); Tests run: 12, Failures: 0, Errors: 0, Skipped: 0 (integration); BUILD SUCCESS. The combined tree was never built: the squash conflicted. Baseline only (make parity-java on b4ab502, exit 2): i01-baseline PASS, i02-tie-half-cent PASS, i03-zero-rate-skipped FAIL (java job exit code 12, 4 differences), i04-default-group-fallback FAIL (java job exit code 12, 4 differences), i05-multi-category PASS, i06-negative-balance PASS, i07-nontie-rounding PASS. Combined-tree parity and `make parity` not run: the squash conflicted.

**`04-calculation-rules`, build attempt 2** (session [af377c0e](https://partner-workshops.devinenterprise.com/sessions/af377c0ebc0540a6949b3b670255a0bb), same branch, same PR #188). As returned:

> Attempt 2 of ticket 04 (INT-T04). The combined check had run on head 940f639, from before the merge of demo/05b-integration into the branch was pushed. On checkout the branch already contained that merge (commit 633b098: AccountRecord passes ACCT-ACTIVE-STATUS through ticket 03's RecordCodec with one line, one RecordLayoutTest assertion updated), so no code change was needed. Confirmed origin/demo/05b-integration (b4ab502) is an ancestor of the branch and that git merge-tree against it is clean. Repeated the whole verification on a clean tree at 8ac4b77: mvn clean verify 313 unit + 20 integration tests, BUILD SUCCESS; parity-report.sh on all seven cases exit 0; switch test through the job jar and compare.py (rounding HALF_EVEN fails i02 on TRAN-AMT, i07 passes; the other switches; empty input COMPLETED with no transaction and the account file unchanged). Evidence replaced under java/interest/evidence/04-calculation-rules/ and pushed as c63f18c; PR 188 description updated. Nothing outside the Allowed files and the grant changed; nothing merged.

**`04-calculation-rules`, integration attempt 2 - passed**, commit `f09b623` (squash of the branch at `c63f18c`).

### 2.2 Failures hit and fixed inside a build session

None of these left a ticket in a failed state. The first table is the `failures` field of the run data, quoted.

| Ticket | Failure, as the build session returned it |
|---|---|
| `02-schema` | Fixed during the build: 'FAIL keys: one foreign key ...: expected, not found: (interest_transaction,f,"FOREIGN KEY (run_id) REFERENCES carddemo.interest_run(run_id)") / found, not expected: (interest_transaction,f,"FOREIGN KEY (run_id) REFERENCES interest_run(run_id)")', run.sh exit 1. Cause: pg_get_constraintdef prints a name unqualified when its schema is on the search_path (user carddemo has schema carddemo there through "$user"). Fix: SET LOCAL search_path = pg_catalog in the assertion script; the expected text was not loosened. |
| `03-record-codec` | EncodingRulesTest.e2_signedZoned:105 expected: <99999999.99> but was: <999999999.99> - two test rows of mine had one 9 too few; fixed the rows, the codec was right |
| `06-calc-step-jdbc` | Hit and fixed: every table run failed with 'PSQLException: ERROR: syntax error at or near "trancat_acct_id"' - two Java text blocks joined without a separator; fixed in JdbcBalanceSource.KEY_ORDER. |
| `06-calc-step-jdbc` | Hit and fixed: the exit message of a failed step began with the stack trace ('FAILED commits=1 rollbacks=1 com.carddemo.interest.domain.InterestRunStoppedException: WRITE_OUTPUT (INT-R08): cannot insert into table carddemo.interest_transaction'), because Spring Batch appends a listener's description after it and the job repository cuts the message at 2500 characters; the sentence naming the account is now also the job's exit message. |

Tickets 04, 05, 07, 08, 09 and 10 returned an empty `failures` list. Their `report.md` (section 6, "Failures
hit") records these, all fixed in the same session:

| Ticket | Symptom, as quoted in `java/interest/evidence/<ticket>/report.md` | Fix |
|---|---|---|
| `04-calculation-rules` | `FixtureCasesTest.intT02_runLogIsStartRecordsEnd` failed: expected `000000020010100010000010000{`, was the same followed by 22 spaces | test states the 50-byte image; the code was right |
| `04-calculation-rules` | `mvn -o ... test`: `Plugin org.apache.maven.plugins:maven-failsafe-plugin:3.5.4 ... could not be resolved ... in offline mode` | run without `-o` |
| `04-calculation-rules` | the pull request reported merge conflicts with `demo/05b-integration` after tickets 02 and 03 were integrated (`codec/AccountRecord.java`, `codec/RecordLayoutTest.java`); then `PolicySwitchExpectationsTest` did not compile (`codec.Field` no longer exists) | base merged into the branch (commit `633b098`, no rebase, no force-push); evidence produced again |
| `04-calculation-rules` | `parity-report.md` said "with uncommitted changes" | written with `REPORT=work/attempt2/parity-report.md` and copied in (see section 3) |
| `05-extract-loader` | `ExtractCheckTest.tl4_nonBlankFillerIsCountedNotRejected`: `StringIndexOutOfBounds Range [37, 36) out of bounds for length 36` | test data: the test appends the byte instead of replacing it |
| `05-extract-loader` | `rel08_hostFormat...`: expected filler `4040...` but was `2020...` | test data: the assertion compares the rows as they are; loader unchanged |
| `05-extract-loader` | `parity-report.sh` exit 0 but i03 and i04 `12 / 1` | none: the base held the pilot's calculation (ticket 04 not integrated) |
| `07-dataset-export` | `Fatal error compiling: error: release version 21 not supported` | pitfall P1: `JAVA_HOME` on every command |
| `08-error-paths` | `mvn -o`: `maven-failsafe-plugin:jar:3.5.4 has not been downloaded` | run without `-o` |
| `08-error-paths` | `CalcInterestTablesRestartIT` (3 failures): work log expected to hold committed lines only | the two assertions now expect the stop lines and `ABENDING PROGRAM` |
| `08-error-paths` | `ErrorPathsTablesIT`: run-log echo expected 28 characters, was 50 | expected echo is the line of the recorded run log |
| `08-error-paths` | `ErrorPathsTablesIT.intH05_interestAboveFieldStops`: `bad SQL grammar` | column is `tran_cat_bal`, not `trancat_bal` |
| `08-error-paths` | `ErrorPathsTablesIT.intH05_millionthTransactionStops`: 0 rows updated | restart state set through `JobRepository.updateExecutionContext` |
| `09-parity-gate` | `make parity-naive CASE=i02-tie-half-cent` ended with `service "estate" is not running` | `make up` first |
| `09-parity-gate` | `git status` showed `tools/runjcl/__pycache__/runjcl.cpython-310.pyc` modified after `make parity` | `git checkout -- tools/parity/__pycache__ tools/runjcl/__pycache__` |
| `10-data-move-kit` | `mvn -o`: `maven-failsafe-plugin:jar:3.5.4 has not been downloaded` | run without `-o` |
| `10-data-move-kit` | `java` on the PATH is 17 | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64` |
| `10-data-move-kit` | first side-by-side run of i01 against i02 classed `ACCT-CURR-BAL` 2400.00 / 1234.50 and `ACCT-CURR-CYC-CREDIT` 2400.00 / 1234.50 on the last record as INT-Q04 | `DifferenceClassifier`: cycle amounts are INT-Q04 only when the target has 0.00; test added |
| `10-data-move-kit` | `TS=sometimes` printed `No enum constant ...TimestampRule.SOMETIMES` | `TimestampRule.ofParameter` says `ts must be format or exact`; test added |
| `10-data-move-kit` | README said exit code 1 for a difference; `make` ends with 2 | README states `Error 1` / `Error 2` |
| `10-data-move-kit` | `compare.py` has no option for the expected directory | `sidebyside-java` copies both directories under `work/sidebyside-java`; nothing under `tools/parity` edited |

### 2.3 Needs-decision tickets

None. No ticket of the run data is in the state `needs-decision`.

### 2.4 Skipped tickets

None. No ticket was skipped; every ticket waited only until the tickets it depends on were integrated (state log, section 5).

### 2.5 Points the sessions left for the reviewer

Not failures and not states; taken from the summaries and integration results of the run data.

| Ticket | Point |
|---|---|
| `02-schema` | The data map does not name the reconciliation columns of `interest_run` ("loaded_* / written_*"); the 31 column names (from strategy items RC1-RC6) are the ticket's choice. |
| `03-record-codec` | Control characters in PIC X are treated as "outside the code page" (G07): a reading that needs a decision. The job still runs in the fixture format only; REL-08 is open, cp037 is the unconfirmed default. |
| `04-calculation-rules` | INT-Q06 and INT-H04 have no fixture and are tested with synthetic data; `java/interest/README.md` still describes the pilot (outside the Allowed files). |
| `05-extract-loader` | A database failure inside a later table's transaction rolls back that table only; earlier tables stay replaced. |
| `06-calc-step-jdbc` | Default chunk size 100 accounts, a guess until G11. Tickets 05 and 07 use different job-context keys for the run id (`interest.runId`, `interestRunId`). |
| `07-dataset-export` | The export also fills the RC6 columns of `interest_run` and refuses to export when the run log is missing (both listed for the decision register); REL-03 as proposed cannot be done from the tables, so every account is exported. |
| `08-error-paths` | The on-disk run log of a stopped run exists for the table adapters only. Integration note: the squash also changes `policy/InterestPolicy.java` and `application.yml`, outside the ticket's Allowed files list; the run data records a grant for exactly these (section 1.1). |
| `09-parity-gate` | Not proven by the gate (no fixture): INT-Q06, INT-R08, INT-H02 to INT-H06. Integration note: the squash changes `batch/InterestJobConfig.java` and adds `batch/LoadedRunHandOver.java` plus two tests, outside the ticket's Allowed files list; the run data records a grant for these (section 1.1). |
| `10-data-move-kit` | The "loaded and calculated case" is made by a stand-in in package `recon` (`CalculatedRunStaging`), which the build session says should go when ticket 06 is merged; `format=host` was not run (no host extract exists). |


## 3. Skill corrections (reported by the run; applied after it)

Every entry of `skillCorrections` in the run data, as the build session returned it. The run itself applied
none. After the run the parent session of step 5b applied them to the three procedure skills, one commit each:
`f081308` (`interest-ticket-build`), `5cac3ac` (`interest-ticket-verify`, with two lines of its
`parity-report.sh`), `db076a5` (`interest-pitfalls`, entries P8 to P12, marked as reported and not reproduced).
`interest-conversion-standards` was not changed; section 4 lists its proposals.

Not applied, because they ask for a new function of `parity-report.sh` and not a correction of its text: a
directory per case to compare (ticket 07) and a section for a ticket proven through the tables (ticket 06).
The skill now says to add these by hand.

### 3.0 Final combined check

After the last integration and the report commit, the parent session ran the combined check once more on
`demo/05b-integration` at `649136a`: `java/interest/evidence/integration/final-combined-check.txt`.
`mvn clean verify` 385 unit / 119 integration, BUILD SUCCESS; `db/postgres/tests/run.sh` exit 0;
`make parity-java` 7 of 7 PASS (job, comparator, RC1-RC5); `compare.py` on each of the seven cases exit 0;
`make up && make build && make parity` 24 PASS, exit 0. The three skill commits came after it and change
nothing under `java/`, `db/`, `tools/` or the `Makefile`. Ticket 04 reported in both attempts; both lists are given, the second repeats the four of the first and adds two.

### 3.1 `interest-ticket-build`



| Ticket | Build attempt | Reported |
|---|---|---|
| `04-calculation-rules` | 1 | interest-ticket-build step 5 (layout table): say that the module has no JSON library at compile scope and that tests reading modernization/rules/interest/expected/*.json can use org.json from spring-boot-starter-test without a new dependency. |
| `04-calculation-rules` | 2 | interest-ticket-build step 5: the layout table should say the module has no JSON library at compile scope and that tests can use org.json from spring-boot-starter-test without a new dependency. |
| `04-calculation-rules` | 2 | interest-ticket-build step 3 says to stop when a branch or open PR already covers the ticket; it should except a repeat attempt of the same ticket, which continues on the existing branch and PR. |
| `05-extract-loader` | 1 | interest-ticket-build step 5: says packages jdbc and load do not exist and no table exists in schema carddemo beyond 01-transaction-type.sql; after ticket 02 the seven objects exist and an integration test must copy db/postgres/02-interest.sql into its Testcontainers postgres itself. |
| `05-extract-loader` | 1 | interest-ticket-build step 9: the sentence 'Allowed files are java/interest/** and the Makefile' does not fit tickets with narrower scopes; the check that matters is git diff --name-only origin/<base> against the ticket's Allowed files. |
| `06-calc-step-jdbc` | 1 | interest-ticket-build step 5: says packages jdbc/load/export/recon do not exist and schema carddemo holds only the tables of 01-transaction-type.sql; after tickets 02-04 db/postgres/02-interest.sql exists and the compose db service applies it on a new volume. |
| `06-calc-step-jdbc` | 1 | interest-ticket-build step 8: quotes 60 unit and 12 integration tests; the base after ticket 04 has 313 and 20. |
| `07-dataset-export` | 1 | interest-ticket-build step 5 is out of date after tickets 02 and 03: it says packages jdbc and export do not exist and no table exists beyond 01-transaction-type.sql, and it names codec/Field (now RecordCodec/FieldSpec). An integration test that needs the tables must copy db/postgres/02-interest.sql into the Testcontainers postgres:15 as a second init script. |
| `07-dataset-export` | 1 | interest-ticket-build step 2: the export JAVA_HOME line lasts for one shell only; say to set it in the environment of every command, otherwise pitfall P1 is hit ('release version 21 not supported'). |
| `10-data-move-kit` | 1 | interest-ticket-build: make ends with 2 for any failing recipe; a recipe's own exit code 1 shows only as 'Error 1' in the output, so evidence of make targets should quote that line. |
### 3.2 `interest-ticket-verify`



| Ticket | Build attempt | Reported |
|---|---|---|
| `02-schema` | 1 | interest-ticket-verify step 8: parity-report.sh needs at least one case the ticket names; there is no instruction for a ticket with no fixture case (parity-report.md was written by hand). |
| `02-schema` | 1 | interest-ticket-verify step 7: 'a ticket that adds tables changes the expected list' in InterestCalcJobIT does not hold for ticket 02: the test's container does not apply db/postgres, and java/interest is outside the ticket's Allowed files. |
| `04-calculation-rules` | 1 | interest-ticket-verify step 8: it names three evidence files; a ticket whose Verification asks for more (here 'the switch-test output') has no place named for it. This ticket wrote switch-test-output.txt next to the three. Proposed: further output a ticket's Verification names goes into the same directory under a name that says what it is. |
| `04-calculation-rules` | 1 | interest-ticket-verify step 8 (parity-report.sh): when every case is named on the command line the 'Why' column is left empty, not '_fill in_'; the step's text should say so. |
| `04-calculation-rules` | 2 | interest-ticket-verify step 8, parity-report.sh line 55: run again on a ticket whose report is already committed, it writes 'with uncommitted changes' although only its own report (or a test-output.txt written just before) differs from HEAD, because it checks `git diff --quiet HEAD -- java Makefile` after starting to write under java/interest/evidence. Worked around with REPORT=work/... and a copy. Proposed: exclude java/interest/evidence from that check, or tell the step to produce both files outside the tree first. |
| `04-calculation-rules` | 2 | interest-ticket-verify step 8 names three evidence files; a ticket whose Verification asks for more (the switch-test output here) has no place named for it. Proposed: further output a ticket's Verification names goes into the same directory under a name that says what it is. |
| `04-calculation-rules` | 2 | interest-ticket-verify step 8, parity-report.sh: when every case is named on the command line the 'Why' column is left empty, not `_fill in_`; the step text should say so. |
| `05-extract-loader` | 1 | interest-ticket-verify steps 1-7: they assume the ticket's proof is job output through the comparator; a ticket whose result is tables (05) has no command to launch one step against the db service, and parity-report.sh labels the named case 'acceptance case of the ticket' although the comparator proves nothing about it. Add a step for tickets without comparator output: the integration test is the proof, the comparator run only shows nothing changed. |
| `05-extract-loader` | 1 | interest-ticket-verify step 8: parity-report.sh pulls eclipse-temurin:21-jre on first use and the pull progress lands in parity-report.md; pull quietly before the run. |
| `06-calc-step-jdbc` | 1 | interest-ticket-verify step 8: parity-report.sh only runs make parity-java (file adapters); a ticket proven through the tables has to add its own section to parity-report.md by hand. |
| `07-dataset-export` | 1 | interest-ticket-verify steps 1, 6, 7 and parity-report.sh assume the ticket's code runs inside make parity-java. For a ticket whose code is not yet a step of the job (07), the script should accept a directory per case to compare, and the skill should say where such a ticket leaves its directories (here java/interest/target/export-parity/<case>/actual, compared by hand and added to the parity report). |
| `09-parity-gate` | 1 | interest-ticket-verify steps 1 and 8: `make parity-java` is described as the file-adapter pilot (sample output has i03 and i04 ending 12). After ticket 09 it runs load, calculation and export under profile `tables`, all seven cases end 0, each case also writes work/parity-java/<case>/recon.md, and PARITY_JAVA_OUT and ROUNDING are new variables. |
| `09-parity-gate` | 1 | interest-ticket-verify step 7: the RC1-RC5 database assertions are now part of `make parity-java` (reconciliation report per case); hand-run psql is only needed to look further. |
| `10-data-move-kit` | 1 | interest-ticket-verify step 8: parity-report.sh labels every case given on the command line 'acceptance case of the ticket'; for a ticket whose acceptance is not make parity-java the label is misleading. |
| `10-data-move-kit` | 1 | interest-ticket-verify steps 6 and 7 give the commands of ticket 01 only; a ticket with its own make targets has to design its own command-line evidence (here kit-output.txt). Say so. |
### 3.3 `interest-pitfalls`



| Ticket | Build attempt | Reported |
|---|---|---|
| `02-schema` | 1 | interest-pitfalls: add that pg_get_constraintdef (and other catalog text) qualifies a table only when its schema is not on the search_path; user carddemo has schema carddemo on it, so SQL assertions on catalog text must set search_path first. |
| `04-calculation-rules` | 1 | interest-pitfalls: add the failure hit here - a test compared the run-log record echo with the fixture input line and failed, because the legacy DISPLAY writes the 50-byte record with trailing spaces while the fixture input line has none; the test must state the 50-byte image. |
| `04-calculation-rules` | 2 | interest-pitfalls: add that the SYSOUT record echo is the 50-byte record with trailing spaces, while the fixture input lines are stored without them, so a test must not compare the echo with the input line. |
| `09-parity-gate` | 1 | interest-pitfalls: `make parity` also rewrites the tracked tools/runjcl/__pycache__/runjcl.cpython-310.pyc; restore with `git checkout -- tools/parity/__pycache__ tools/runjcl/__pycache__` before committing. |
| `09-parity-gate` | 1 | interest-pitfalls: `make parity-naive` needs the estate service (`make up`); after only `docker compose up -d --wait db` it ends with `service "estate" is not running`. |
| `10-data-move-kit` | 1 | interest-pitfalls: importing tools/parity leaves a new untracked tools/parity/__pycache__/compare.cpython-310.pyc; 'git checkout -- tools/parity/__pycache__' does not remove it, it has to be deleted. |

No correction reported: `03-record-codec` ("no correction; every command worked as written") and `08-error-paths`.

## 4. Proposals for the standards (reported, not applied)

Every entry of `standardsProposals` in the run data for `interest-conversion-standards`, as the build session returned it. None was applied; the skill is unchanged by this run. Ticket 04 returned the same five proposals in both attempts.

| Ticket | Build attempt | Reported |
|---|---|---|
| `02-schema` | 1 | Section 9 has no way to mark a ticket without Java: most lines can only be N/A. Add a 'no Java' marker or a short schema-only checklist. |
| `02-schema` | 1 | The standards do not say how a schema script must behave on a second run or where SQL assertions live (this ticket used db/postgres/tests/ and 'fails cleanly'). |
| `03-record-codec` | 1 | S3.5 / data-map G07 say a byte outside the code page is rejected, but ISO-8859-1 and cp037 map all 256 bytes. The standard should say which bytes count as outside (this ticket chose control characters) or say that PIC X carries any byte. |
| `04-calculation-rules` | 1 | S3.8 against INT-Q03 option B: S3.8 says a frozen business date for timestampSource=CYCLE_DATE is a job parameter, but the register's option is 'the cycle date of the PARM', the expectations are computed from the PARM, and architecture.md section 4 lists no such parameter. Proposed wording: with CYCLE_DATE the timestamp is built from the first eight characters of parmDate, as text; a PARM that does not start with eight digits stops the run. |
| `04-calculation-rules` | 1 | S5.2 'Do not' column forbids 'add an active-status check' as having no rule id, yet INT-H04 option B and the switch interest.policy.inactiveAccounts are exactly that check. Proposed: forbid it only outside the INT-H04 switch or with a default other than CHARGE. |
| `04-calculation-rules` | 1 | Section 4 row aa says 1400-COMPUTE-FEES is not ported, while architecture.md section 8 and ticket 04 ask for an empty fee hook. Proposed: say the hook is one named domain class that returns no transaction and is the only trace of the paragraph. |
| `04-calculation-rules` | 1 | Section 6 table: add a row for the stop reason CYCLE_DATE_UNREADABLE (INT-Q03, only when the switch is not the default). |
| `04-calculation-rules` | 1 | Section 4 row m: intT09_emptyInputWritesNothing is marked 'no fixture'; say the test builds its input from a fixture's other three files and an empty balance file. |
| `04-calculation-rules` | 2 | S3.8 against INT-Q03 option B: with timestampSource=CYCLE_DATE the timestamp is built from the first eight characters of parmDate, as text, not from a further job parameter; a PARM that does not start with eight digits stops the run. |
| `04-calculation-rules` | 2 | S5.2 'Do not' column forbids 'add an active-status check', yet INT-H04 option B and the switch interest.policy.inactiveAccounts are that check. Proposed wording: '... add an active-status check outside the INT-H04 switch, or with a default other than CHARGE'. |
| `04-calculation-rules` | 2 | Section 4 row aa against INT-H01: the row says 1400-COMPUTE-FEES is not ported, while architecture.md section 8 and ticket 04 ask for an empty fee hook. Say the hook is one named domain class that returns no transaction and is the only trace of the paragraph. |
| `04-calculation-rules` | 2 | Section 6 table: add a row for CYCLE_DATE_UNREADABLE (INT-Q03, only when the switch is not the default). |
| `04-calculation-rules` | 2 | Section 4 row m: intT09_emptyInputWritesNothing is marked 'no fixture'; say the test builds its input from a fixture's other files and an empty balance file. |
| `05-extract-loader` | 1 | S2.2/S3.5: record-codec should expose its FieldSpecs by name, a rejection with source() and field(), and whether a decoded signed field was F-signed or minus zero; the loader had to repeat three field names, parse the field out of the rejection message and tell G02 from G03 by decoding the sign byte as PIC 9(01). |
| `05-extract-loader` | 1 | Section 6: add a row for the load (does not reconcile / table write fails) with its own stop reason (e.g. LOAD_RECONCILIATION) and say where the reject list goes; used RECORD_REJECTED, OPEN_INPUT and WRITE_OUTPUT. |
| `05-extract-loader` | 1 | S7.1: allow a strategy item, gap or decision id (tl3_, rc3_, g02_, rel08_) as test-name prefix for tickets that name no INT-R/Q/H rule. |
| `05-extract-loader` | 1 | S6.4: say that a reject list of a stopped run lives outside the output directory. |
| `06-calc-step-jdbc` | 1 | S6.5 against the ticket's 'account id in the message': say where the account of a failed step is recorded, since it may not be in the application log (built: exit description of the step and exit message of the job in the job repository). |
| `06-calc-step-jdbc` | 1 | Section 6: no row for a table that cannot be read or written at run time with JDBC adapters; READ_BALANCES, UPDATE_ACCOUNT, WRITE_TRANSACTION are not in StopReason yet - say which existing reason to use until ticket 08 (used: OPEN_INPUT, WRITE_OUTPUT). |
| `06-calc-step-jdbc` | 1 | S2.3: the restart state also needs the interest_run.run_id of the run and the interest not added so far (RC5 spans the restart). |
| `06-calc-step-jdbc` | 1 | S7.4: allow the comparison with an uninterrupted run for restart tests; the ticket defines the result that way and no recorded run has more than two accounts. |
| `07-dataset-export` | 1 | Section 6 has no row for the export's own stops (run not CALCULATED, run log missing) and names CLOSE_<file> 'from the export step' although StopReason is in package domain, which the export ticket may not change; say which reason the export uses. |
| `07-dataset-export` | 1 | S6.4 (export to a temporary directory, move on success) and S6.5 (the calculation writes sysout/INTCALC.STEP15.txt) meet in the same output directory; say that the export carries the run log over and what it does when the run log is missing. |
| `07-dataset-export` | 1 | Rule E4 against column tran_filler: say whether the export reads the column or writes spaces of the output code page (G05). |
| `07-dataset-export` | 1 | S7.1 has no naming rule for tests of acceptance lines without a rule id (order, errors, host format); the work item id INT-T07 is also a technical note in rules.yaml about the empty fee paragraph, so intT07_ test names would mislead. |
| `07-dataset-export` | 1 | S2.2: the FieldSpec constants of codec/*Record are private, so a row mapping outside codec can only map by position; say whether row mappings belong in codec or how jdbc reaches the fields. |
| `08-error-paths` | 1 | Section 6 / S6.5 ask for the stop lines in the run log while S6.4 says a stopped file-adapter run keeps nothing: state where the run log of a stopped run is kept for both adapter sets (proposal: a file beside the output directory, never inside it). |
| `08-error-paths` | 1 | Section 6 lists stop reasons READ_BALANCES, UPDATE_ACCOUNT, WRITE_TRANSACTION, CLOSE_<file>, but the code of earlier tickets uses WRITE_OUTPUT and OPEN_INPUT without the operation's legacy message: name the ticket that owns the adapters' messages or change the table. |
| `08-error-paths` | 1 | S3.4 says overflow goes to the policy without listing the fields: list TRAN-AMT, WS-MONTHLY-INT, WS-TOTAL-INT, ACCT-CURR-BAL and WS-TRANID-SUFFIX. |
| `09-parity-gate` | 1 | S2.3: name one job execution context key for the interest_run id passed between the steps, defined once in `batch`. Tickets 05 and 06/07 chose different keys (`interest.runId`, `interestRunId`), so ticket 09 needed the listener LoadedRunHandOver to copy one to the other. |
| `10-data-move-kit` | 1 | Section 9 has no line for commands that are not job steps (reports, comparison wrappers): S2.3 and S6.1-S6.3 can only be N/A. Add a line: tools outside the job read only, open no job repository and state their exit codes. |

## 5. State log

As recorded by the workflow, in order.

```text
STATE 02-schema: waiting -> building (attempt 1)
STATE 03-record-codec: waiting -> building (attempt 1)
STATE 04-calculation-rules: waiting -> building (attempt 1)
STATE 02-schema: building -> built (session https://partner-workshops.devinenterprise.com/sessions/f86da132a1f64759b9d479f086605256, PR https://github.com/Cognition-Partner-Workshops/ts-cobol-carddemo/pull/186)
STATE 02-schema: built -> integrating (attempt 1)
STATE 03-record-codec: building -> built (session https://partner-workshops.devinenterprise.com/sessions/55124bf9f7d3434b9d619ad98f58398c, PR https://github.com/Cognition-Partner-Workshops/ts-cobol-carddemo/pull/187)
STATE 02-schema: integrating -> integrated (commit d2b6511eb85ade8f0b7114ad486ef391cccf43e2)
STATE 03-record-codec: built -> integrating (attempt 1)
STATE 04-calculation-rules: building -> built (session https://partner-workshops.devinenterprise.com/sessions/99a095a3d03f46d2bd8e21112995d1e3, PR https://github.com/Cognition-Partner-Workshops/ts-cobol-carddemo/pull/188)
STATE 03-record-codec: integrating -> integrated (commit b4ab502d97e2c74f02b193bb4e02bf0a7401f0cd)
STATE 04-calculation-rules: built -> integrating (attempt 1)
STATE 05-extract-loader: waiting -> building (attempt 1)
STATE 07-dataset-export: waiting -> building (attempt 1)
STATE 04-calculation-rules: integrating -> retrying (Integration (combined check) failed: back to the ticket once)
STATE 04-calculation-rules: retrying -> building (attempt 2)
STATE 04-calculation-rules: building -> built (session https://partner-workshops.devinenterprise.com/sessions/af377c0ebc0540a6949b3b670255a0bb, PR https://github.com/Cognition-Partner-Workshops/ts-cobol-carddemo/pull/188)
STATE 04-calculation-rules: built -> integrating (attempt 2)
STATE 04-calculation-rules: integrating -> integrated (commit f09b623d5cab3b461e2992d820927afe8c311f86)
STATE 06-calc-step-jdbc: waiting -> building (attempt 1)
STATE 07-dataset-export: building -> built (session https://partner-workshops.devinenterprise.com/sessions/c77891272fba47a795ba4268111eb342, PR https://github.com/Cognition-Partner-Workshops/ts-cobol-carddemo/pull/189)
STATE 07-dataset-export: built -> integrating (attempt 1)
STATE 05-extract-loader: building -> built (session https://partner-workshops.devinenterprise.com/sessions/2c9dd95e17b24cf5934056396440d0ec, PR https://github.com/Cognition-Partner-Workshops/ts-cobol-carddemo/pull/190)
STATE 07-dataset-export: integrating -> integrated (commit 38a1ad3778fe53e3d19308acd2524c95fd01f333)
STATE 05-extract-loader: built -> integrating (attempt 1)
STATE 05-extract-loader: integrating -> integrated (commit 1443bb426861263738bff198d58c1a0887d99f65)
STATE 10-data-move-kit: waiting -> building (attempt 1)
STATE 06-calc-step-jdbc: building -> built (session https://partner-workshops.devinenterprise.com/sessions/06f8094d07a44ff99ac82729c1ec3a86, PR https://github.com/Cognition-Partner-Workshops/ts-cobol-carddemo/pull/191)
STATE 06-calc-step-jdbc: built -> integrating (attempt 1)
STATE 06-calc-step-jdbc: integrating -> integrated (commit 2b9536c7c81b4acf846e99c9c58d09965f325c3d)
STATE 08-error-paths: waiting -> building (attempt 1)
STATE 10-data-move-kit: building -> built (session https://partner-workshops.devinenterprise.com/sessions/104c34eb84ef4da19934dbe3c2b6f639, PR https://github.com/Cognition-Partner-Workshops/ts-cobol-carddemo/pull/192)
STATE 10-data-move-kit: built -> integrating (attempt 1)
STATE 08-error-paths: building -> built (session https://partner-workshops.devinenterprise.com/sessions/3c31414121b44ee6af7db9898e5cd0ef, PR https://github.com/Cognition-Partner-Workshops/ts-cobol-carddemo/pull/193)
STATE 10-data-move-kit: integrating -> integrated (commit 84a6176f3b1843c51530da9075ef735f1caff3f2)
STATE 08-error-paths: built -> integrating (attempt 1)
STATE 08-error-paths: integrating -> integrated (commit 28401d4716dc98aedc6c00091f343a392c7b6e79)
STATE 09-parity-gate: waiting -> building (attempt 1)
STATE 09-parity-gate: building -> built (session https://partner-workshops.devinenterprise.com/sessions/c17cb3c345a3445faf660983fe336a77, PR https://github.com/Cognition-Partner-Workshops/ts-cobol-carddemo/pull/194)
STATE 09-parity-gate: built -> integrating (attempt 1)
STATE 09-parity-gate: integrating -> integrated (commit db2ad19eed4b8034025f0e8087504239585d4d61)
```
