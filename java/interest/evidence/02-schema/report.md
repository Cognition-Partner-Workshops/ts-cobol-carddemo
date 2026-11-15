# Ticket 02-schema - report

Ticket: `modernization/design/tickets/02-schema.md` (INT-T02). Branch `demo/05-02-schema`, started from
`demo/05b-integration` at `6c5ee61`. Code revision the evidence was produced from: `aaec6b5`.

Read this with [schema-test-output.txt](schema-test-output.txt) (the ticket's own evidence: psql output of the
apply and of the assertion script), [parity-report.md](parity-report.md) and [test-output.txt](test-output.txt).

## 1. Result

| Acceptance line of the ticket | Result | Where it is shown |
|---|---|---|
| Empty PostgreSQL 15 with `01-transaction-type.sql` applied; `psql -v ON_ERROR_STOP=1 -f db/postgres/02-interest.sql` exits 0 | met | schema-test-output.txt, "apply db/postgres/02-interest.sql", `exit 0` (server 15.20) |
| A second run fails cleanly or is a no-op, as the script documents | met: it fails (`relation "interest_run" already exists`, psql exit 3) and changes nothing; the header of the script says so | schema-test-output.txt, "a second time", `PASS  catalog of schema carddemo unchanged by the second apply` |
| `information_schema.columns` of the six tables equals the data map row for row (name, data type, precision, scale, character length, nullability) | met | schema-test-output.txt, the seven `PASS  columns:` lines per table (66 columns) plus identity and no-default lines |
| Primary keys of the six tables; `card_xref_acct_ix` exists and is not unique; `(run_id, tran_id)` is unique | met | schema-test-output.txt, `PASS  keys:` and `PASS  index:` lines |
| Rejected: `acct_curr_bal = 10000000000.00`, `tran_amt = 1000000000.00`, `dis_int_rate = 10000.00`, a negative `acct_id`, a 177-byte `acct_filler` | met (SQLSTATE 22003 for the three overflows, 23514 for the two CHECKs) | schema-test-output.txt, `PASS  rejects:` lines |
| Verification, Build: the script applied to the db service of docker-compose | met: a fresh `db` volume runs `01-transaction-type.sql` then `02-interest.sql` from the existing mount `./db/postgres:/docker-entrypoint-initdb.d:ro`; `docker-compose.yml` is unchanged | parity-report.md, "Job repository and schema after these runs" (eight tables in `carddemo`); section 6 |
| Verification, Tests: `db/postgres/tests` run with psql and exit non-zero on any failed assertion | met: `db/postgres/tests/run.sh`, 43 assertions, 0 failed, exit 0; a failed assertion raises and the script exits 1 (seen during the build, section 6) | schema-test-output.txt, last lines |

Fixture cases of the ticket: none. `make parity-java` was run once with the six new tables present in the `db`
service: i01, i02, i05, i06, i07 PASS; i03 and i04 FAIL with Java exit 12, exactly as on `demo/05b-integration`
(pilot evidence `java/interest/evidence/parity-report.md`). `mvn -B -f java/interest/pom.xml clean verify`: BUILD
SUCCESS, 60 unit and 12 integration tests, 0 failures - no Java was changed.

Not touched: `app/`, `fixtures/`, `tools/`, `modernization/`, `.agents/`, `java/interest` outside this directory,
`db/postgres/01-transaction-type.sql`, `docker-compose.yml`, `Makefile`. No golden file was re-recorded. No ticket
system was written to. Nothing was merged.

## 2. What the ticket does not cover (gaps)

Data-map rows that could not be expressed in PostgreSQL 15 exactly as written:

| Gap | State in this branch | Owner |
|---|---|---|
| `interest_run`: the row "`loaded_* / written_* counts and sums` - `BIGINT / NUMERIC(18,2)`" is a pattern, not a list of columns | 18 nullable columns were named here from strategy items RC1, RC2 and RC5 (4 loaded counts, 7 loaded sums, 3 hash totals of the account ids, 2 written counts, 2 written sums). The names are this ticket's choice and become part of FI-4 | architect / data engineering lead; consumers: 05 (loader), 06, 07 |
| `interest_run.status`: `VARCHAR(12) NOT NULL` with the values LOADED, CALCULATED, EXPORTED, FAILED, ABANDONED in the Use column | no CHECK on the values: the data map's type cell has none and the ticket says "taken without change" | architect |
| `interest_run.started_at`, `finished_at`: `TIMESTAMPTZ` | nullable, as the type cell has no NOT NULL | architect |

Every other row (55 copybook columns, `run_id`, `seq_no`, keys, index, collations) is expressed exactly.

Left to other tickets, as the ticket excludes them: any Java (the JDBC adapters, the loader, the export: 05, 06, 07);
the expected table list in `InterestCalcJobIT` (it copies only `01-transaction-type.sql` into its container, so it
does not see the new tables; the ticket that makes the job use them changes that test); Spring Batch job repository
tables; foreign keys to `carddemo.transaction_type` (G09); G10 timestamp typing; retention (REL-12).

## 3. For the decision register

| # | Finding | Evidence | Suggested owner |
|---|---|---|---|
| 1 | **The reconciliation columns of `interest_run` are not enumerated in the data map.** FI-4 freezes names that no design file states. The data map should list them (or accept the 18 of this script). RC2's "sum of the account ids of each file" is stored as `NUMERIC(18,2)` because the data map gives only that type for sums; a hash total has no decimals. RC3, RC4 and RC6 figures have no column (RC6 is measured after the write-back, outside the run row). | `modernization/design/data-map-interest.md:126`; `db/postgres/02-interest.sql` table `interest_run` | architect |
| 2 | **`CHAR(n)` keys compare without trailing blanks.** `disclosure_group` rejects `('DEFAULT', ...)` after `('DEFAULT   ', ...)` as a duplicate, which matches the fixed-width legacy key; but a key that differs only in trailing low-values or other non-blank padding is a different key. Belongs with G07. | assertion "rejects: a second disclosure_group row with the key (DEFAULT, 01, 0001), group padded to CHAR(10)" | data owner |
| 3 | **`CHAR(n)` silently accepts shorter text and pads it; longer text with only trailing blanks is cut on an explicit cast.** Width is enforced by the codec (S3.6), not by the schema. The schema enforces the exact length only for the FILLER `BYTEA` columns. | PostgreSQL 15 character type rules; no assertion claims a width rejection for CHAR | architect |
| 4 | **The second apply fails rather than being a no-op.** Chosen so that a changed script can never be half-applied to an existing schema; a release that needs to re-run it needs a migration tool decision (none is named in the architecture). | header of `db/postgres/02-interest.sql`; schema-test-output.txt | release manager |
| 5 | **The init mount picks the script up only on an empty volume.** An existing `db` volume created before this ticket does not get the tables; `make reset` (wipes the volume) or a manual `psql -f` is needed. In this session the volume was new. | `docker-compose.yml` mount; docker-entrypoint log "running /docker-entrypoint-initdb.d/02-interest.sql", "ignoring /docker-entrypoint-initdb.d/tests" | whoever owns ticket 05 / 09 |
| 6 | **The role `carddemo` has schema `carddemo` first on its search path** (`"$user", public`), so catalog functions print unqualified names. Tests that compare catalog text must set `search_path`. | first run of the assertions: 9 false failures, fixed by `SET LOCAL search_path = pg_catalog` | none (note for 05, 06) |

## 4. Standards review checklist

Standards:

- [x] S1.1 Sources used in order; recorded output decides - **PASS** - data map, then architecture and strategy for `interest_run`; no recorded output concerns a schema
- [ ] S1.2 Rule/code disagreements reproduced as coded and reported - **N/A** - no rule is implemented by this ticket
- [ ] S2.1 Designed from rules; no method per paragraph - **N/A** - no Java
- [ ] S2.2 Immutable business records; copybook names only at the mapping - **N/A** - no Java; column names are the data map's
- [ ] S2.3 Spring Batch shape, transaction and restart boundaries of the architecture - **N/A** - no Java; no job repository table created
- [ ] S2.4 Types and return values, no flags or result codes - **N/A** - no Java
- [ ] S2.5 Constructor injection; no mutable static state - **N/A** - no Java
- [x] S3.1 `BigDecimal` with data-map precision and scale; no `double`/`float` - **PASS** for the schema half - every numeric column is `NUMERIC(p,s)` of the data map; assertions "columns:" and the five overflow rejections. Java half N/A
- [ ] S3.2 Formula: exact product, one division, mode from the policy - **N/A** - no calculation
- [ ] S3.3 Rounding mode named on every other arithmetic step - **N/A** - no arithmetic
- [x] S3.4 Range check before store; overflow goes to the policy - **PASS** for the schema half - an out-of-range value is refused (22003), never cut; the policy is ticket 04
- [ ] S3.5 Encodings only in `record-codec` - **N/A** - no encoding in the schema
- [x] S3.6 Width, padding and FILLER only in codec and schema - **PASS** - `CHAR(n)` widths and `BYTEA` + `octet_length` CHECKs of the data map; assertions for 177/178/179 bytes
- [x] S3.7 Code page and key order each in one place - **PASS** for key order - `COLLATE "C"` on `trancat_type_cd`, `dis_acct_group_id`, `dis_tran_type_cd`; assertion "order:". Code page N/A
- [x] S3.8 Clock and run parameters injected; `parmDate` is text - **PASS** for the schema half - `interest_run.parm_date CHAR(10)`; dates and timestamps of the records stay text (G04, G10)
- [ ] S4.1 Every PRESENT behaviour touched has its test; new ones reported - **PASS** - behaviours touched (d, g, h, j, p, y below) each have an assertion; none new
- [ ] S5.1 Each quirk/open decision in one named place behind its switch, rule id in names - **N/A** - no decision implemented; G09 and G10 stay open
- [x] S5.2 Nothing fixed, tidied or added without a rule id - **PASS** - legacy spelling `acct_expiraion_date` kept; no CHECK, default, index or FK beyond the data map (assertions "checks:", "index:", "foreign keys:", "no column ... has a default"). The names of the `interest_run` reconciliation columns are the one addition, reported in sections 2 and 3
- [ ] S6.1 Status and message mapping as in section 6 - **N/A** - no Java
- [ ] S6.2 One stop exception, one exit-code constant, no skip or retry - **N/A** - no Java
- [ ] S6.3 Restart as in the architecture - **N/A** - no Java
- [ ] S6.4 Outputs in the recorded format; failed run leaves no SYSTRAN - **N/A** - no output written; `make parity-java` unchanged (parity-report.md)
- [ ] S6.5 Run log byte for byte; no account data in the application log - **N/A** - no Java
- [ ] S7.1 A test per rule id, named for it - **N/A** - the ticket has no rule (Rules: none)
- [ ] S7.2 Fixture and edge cases of each rule covered - **N/A** - no rule; the edge values of each type are asserted (largest and one past it)
- [x] S7.3 Integration tests on PostgreSQL 15 in a container - **PASS** - `db/postgres/tests/run.sh` runs in the `db` service (`postgres:15`, server 15.20) on a scratch database
- [x] S7.4 No assertion against the code's own output - **PASS** - expected columns, keys, checks are written out in `02-interest-assertions.sql` from the data map, not read back from the DDL
- [x] S8.1 No edit under `app/`, `fixtures/`, `tools/parity` - **PASS** - `git diff --name-only origin/demo/05b-integration...HEAD`
- [x] S8.2 No golden file re-recorded - **PASS** - `make record` not run
- [x] S8.3 Every new dependency explained in the pull request - **PASS** - none (section 7)
- [ ] S8.4 Frozen shared interfaces unchanged - **PASS** - FI-1, FI-2, FI-3, FI-5, FI-6 untouched; FI-4 is defined by this ticket
- [x] S8.5 Synthetic data only - **PASS** - the assertions insert made-up rows and roll them back ("the assertions left no row behind")

Behaviours PRESENT in section 4:

- [ ] a Truncation without ROUNDED - **N/A** - calculation
- [x] b High-order digit loss (range check, STOP) - **PASS** for the schema half - `NUMERIC(p,s)` refuses the value; assertions "rejects: acct_curr_bal = 10000000000.00", "tran_amt = 1000000000.00", "dis_int_rate = 10000.00"
- [ ] c Intermediate precision of the COMPUTE - **N/A** - calculation
- [x] d Zoned overpunch signs - **PASS** for the schema half - signed fields are plain `NUMERIC`, unsigned ones carry `CHECK (>= 0)` (7 CHECKs); encoding is the codec's
- [ ] e Binary items stay out of data and domain - **N/A** - no binary item in the five layouts
- [ ] f Non-numeric data rejected at the boundary - **N/A** - loader (05)
- [x] g Fixed width, padding, STRING - **PASS** for the schema half - `CHAR(n)` of the data map; "columns:" assertions. See finding 3
- [x] h Code page and collating order - **PASS** for the order - `COLLATE "C"`, assertion "order:" (G06 stays open)
- [ ] i REDEFINES replaced by formatter and function - **N/A** - no column for a REDEFINES
- [x] j FILLER and record length round trip - **PASS** for the schema half - five `BYTEA` columns with exact `octet_length` (178, 14, 22, 28, 20)
- [ ] l No state carried between accounts except the counter - **N/A** - calculation
- [ ] m Control break and end of file (INT-Q04, INT-T09) - **N/A** - calculation
- [ ] o Level-88 result codes replaced by types - **N/A** - no Java
- [x] p Indexed access through the ports; several cards to the policy - **PASS** for the schema half - `card_xref_acct_ix (xref_acct_id, xref_card_num)` not unique; assertion "accepts: two cards for one account" (G08)
- [ ] q File statuses and abend - **N/A** - no Java
- [ ] r PARM, generation, disposition, return code - **N/A** - no Java; `interest_run` holds the run row only
- [ ] s DISPLAY output in the run log - **N/A** - no Java
- [ ] t PARM text against the clock - **N/A** - `parm_date` is `CHAR(10)`, no date arithmetic in the schema
- [ ] u Timestamp format and hundredths - **N/A** - `tran_orig_ts`, `tran_proc_ts` stay `CHAR(26)` (G10)
- [ ] w PARM length checked - **N/A** - job launch
- [ ] x No stale rate after a failed lookup - **N/A** - calculation
- [x] y Typed fields where COBOL mixed text and numbers - **PASS** - `trancat_cd`/`dis_tran_cat_cd`/`tran_cat_cd` are `NUMERIC(4,0)`, type codes are `CHAR(2)`, as the data map (G09)
- [ ] z Abend ends the step; stub output not reproduced - **N/A** - no Java
- [ ] aa Dead and empty code not ported - **N/A** - no code ported

## 5. What is wrong or missing in the standards skill

Proposals only.

| Standard | What the work shows | Proposal |
|---|---|---|
| Section 9 | Most lines are about Java; a schema-only ticket marks 40 lines N/A and several "PASS for the schema half" | Add under the heading: "A ticket with no Java marks S2.x, S6.x and behaviours a, c, l, m, o, q, s, w, x, z, aa N/A in one line." |
| S3.6 | `CHAR(n)` does not enforce the width: shorter text is padded by the database | Add: "The schema enforces exact length only for FILLER (`BYTEA` + `octet_length`); the width of text fields is enforced by the codec before the insert." |
| S7.3 | Names Testcontainers only; SQL assertions run with psql in the compose `db` service are the test form the ticket prescribes | Add: "SQL assertions under `db/postgres/tests`, run by `db/postgres/tests/run.sh` on a scratch database of the `db` service, count as integration tests of the schema." |
| (missing) | No standard says how a schema script behaves on re-application or how it is changed once FI-4 is frozen | Add a standard: "Schema scripts are one transaction, never DROP or ALTER an existing object, and fail on re-application; a change to FI-4 is a new numbered script." |

Skill corrections (interest-ticket-verify, interest-ticket-build): see the pull request and the session's output;
the main one is that `parity-report.sh` needs at least one case (`usage: ... <ticket> <case> [<case> ...]`, exit 2)
and labels it "yes - acceptance case of the ticket", which is wrong for a ticket with no fixture case. Here
`i01-baseline` was passed and the label and the section heading were corrected by hand in parity-report.md.

## 6. Method

```sh
git fetch origin && git checkout -b demo/05-02-schema origin/demo/05b-integration
docker compose up -d --wait db                 # new volume: init runs 01 then 02 from the existing mount
db/postgres/tests/run.sh                       # scratch database: apply 01, 02, 02 again, assertions; exit 0
git commit ... && git push                     # code revision aaec6b5
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
mvn -B -f java/interest/pom.xml clean verify   # -> test-output.txt
.agents/skills/interest-ticket-verify/parity-report.sh 02-schema i01-baseline   # runs make parity-java for all cases
db/postgres/tests/run.sh > java/interest/evidence/02-schema/schema-test-output.txt 2>&1
git checkout -- tools/parity/__pycache__
```

`run.sh` uses `docker compose exec -T db psql -U carddemo` by default (`PSQL=` overrides it), creates the
database `interest_schema_test_<pid>`, and drops it at the end; it never touches the database `carddemo`.

Failures hit:

| Symptom | Cause | Fix |
|---|---|---|
| 9 of 43 assertions failed on the first run (primary keys "(nothing)", CHECK/FK/UNIQUE text without schema) | role `carddemo` has schema `carddemo` on its search path, so `pg_get_constraintdef` and `regclass` text came back unqualified while the expected text was qualified | `SET LOCAL search_path = pg_catalog` at the top of the assertion script; expected text unchanged |
| `parity-report.sh 02-schema` -> usage, exit 2 | the script needs one case; the ticket has none | passed `i01-baseline`, corrected the table by hand |
| `make parity-java` exit 2 | i03 and i04 stop (ticket 04), as on the base branch | none - not this ticket's |

Also checked outside the evidence files: a throw-away `postgres:15` container with `./db/postgres` mounted as
init directory logs `running .../01-transaction-type.sql`, `running .../02-interest.sql`, `ignoring .../tests`
and ends with eight tables in `carddemo`; so the `tests` directory under the mount does no harm and
`docker-compose.yml` needs no change.

## 7. Dependencies

None. No file under `java/interest` except this directory changed; the tests need only `psql` inside the
`postgres:15` image and bash.
