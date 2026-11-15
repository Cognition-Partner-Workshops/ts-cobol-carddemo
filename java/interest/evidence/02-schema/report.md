# Ticket 02 schema - report

Ticket: `modernization/design/tickets/02-schema.md` (INT-T02, frozen interface FI-4). Branch `demo/05-g1-groundwork`,
started from `origin/demo/05b-integration` at `c20799a`. Code revision the evidence was produced from: `201b4a6`.

Read this with [test-output.txt](test-output.txt) (the apply and the assertions; open that first) and
[parity-report.md](parity-report.md) (regression only: the ticket names no fixture case).

## 1. Result

| Acceptance line of the ticket | Result | Where it is shown |
|---|---|---|
| Empty PostgreSQL 15 with 01-transaction-type.sql applied; `psql -v ON_ERROR_STOP=1 -f db/postgres/02-interest.sql` exits 0 | met | test-output.txt, first block (`# exit code 0` after the apply; server 15.20; a scratch database created empty by `db/postgres/tests/run.sh`) |
| A second run fails cleanly or is a no-op, as the script documents | met: fails cleanly. `ERROR: relation "account" already exists`, psql exit 3, the transaction is rolled back, 19 objects in schema carddemo before and after | test-output.txt, "second run"; documented in the header of `db/postgres/02-interest.sql` |
| information_schema.columns of the six tables equals the data map row for row (name, type, precision, scale, length, nullability) | met: 66 columns compared in both directions (missing and extra), ordinal position and collation as well | `db/postgres/tests/02-interest-assertions.sql` section 2; test-output.txt lines "columns: ..." |
| Primary keys of the six tables | met | assertions section 3; six "primary key ..." lines |
| card_xref_acct_ix exists and is not unique; (run_id, tran_id) is unique | met | assertions section 4 |
| Rejected: acct_curr_bal = 10000000000.00, tran_amt = 1000000000.00, dis_int_rate = 10000.00, a negative acct_id, a 177-byte acct_filler | met: SQLSTATE 22003 for the three values, 23514 for the id and the filler | assertions section 8, first five lines |
| Build: the script applied to the db service of docker-compose | met: applied to database `carddemo` of the running service; a new volume applies it at initdb (the service already mounts `db/postgres`; checked on a throw-away `postgres:15` container, section 6) | test-output.txt, second block |
| Tests: db/postgres/tests run with psql and exit non-zero on any failed assertion | met: `db/postgres/tests/run.sh` exit 0; with one expected value changed on purpose (acct_id precision 12) psql exits 3 and the runner non-zero (section 6) | test-output.txt; section 6 |

Beyond the acceptance lines the assertion script also checks: the 13 CHECK constraints and that there are no others; the
only foreign key is interest_transaction(run_id) -> interest_run(run_id); run_id is GENERATED ALWAYS; boundary rows at the
largest and smallest PIC values are accepted; `CHAR(10)` keeps `DEFAULT` padded (E3); FILLER keeps X'00' bytes (E4);
key order under `COLLATE "C"` is 01, 1A, A1 (E6); twelve more rejections (NULL, duplicate keys, widths, missing run).

Java: unchanged. `mvn -B -f java/interest/pom.xml clean verify`: 60 unit and 12 integration tests, 0 failures, BUILD SUCCESS
(test-output.txt, third block) - the same totals as `origin/demo/05b-integration`. Parity: i01, i02, i05, i06, i07 PASS;
i03, i04 FAIL as on the starting branch (ticket 04). `make parity` (legacy chains, with the six tables present in the
database): exit 0.

Changed: `db/postgres/02-interest.sql`, `db/postgres/tests/02-interest-assertions.sql`, `db/postgres/tests/run.sh` and this
folder. `docker-compose.yml` is not changed: the db service already mounts `./db/postgres` as its initdb directory.
Nothing under `app/`, `fixtures/`, `tools/`, `modernization/`, `.agents/` or `java/interest/src`. Nothing was merged.

## 2. What the ticket does not cover (gaps)

**Data-map rows that could not be expressed in PostgreSQL 15 exactly as written: none of the copybook rows.** Every row of
CVACT01Y, CVACT03Y, CVTRA01Y, CVTRA02Y, CVTRA05Y and the two extra columns of interest_transaction are in the script with
the type text of the data map. Two rows of interest_run are not single columns in the data map and had to be read:

| Data-map row | How it is written | Why |
|---|---|---|
| `started_at / finished_at` `TIMESTAMPTZ` | two nullable columns | one row names two columns; no NOT NULL is given, and finished_at is unknown while the run is open |
| `loaded_* / written_* counts and sums` `BIGINT / NUMERIC(18,2)` | 18 nullable columns, named by this ticket from RC1, RC2 and RC5 of strategy-interest.md (finding 1) | the data map gives a pattern, not names |

| Gap | State in this branch | Owner ticket |
|---|---|---|
| Nobody uses the tables | the job still reads and writes files; no JDBC adapter, no loader, no export | 05, 06, 07 |
| `InterestCalcJobIT` does not apply 02-interest.sql | its container copies `01-transaction-type.sql` only and asserts schema carddemo holds two tables; that stays true and the test passes. The first ticket that needs the tables in that test must copy the second script and change that assertion | 05 / 06 |
| An existing db volume | initdb scripts run only on a new volume. On a volume created before this ticket the script is applied once by hand (the command in its header) or by `make reset && make up` | - |
| Allowed values of `interest_run.status` | no CHECK: the data map lists five values in the Purpose column, not as a constraint (finding 2) | architect |
| G04, G10 date typing; G09 foreign keys to transaction_type; REL-12 retention; Spring Batch tables | not in this script, as the ticket's Out of scope says | - |
| Migration tool (Flyway/Liquibase) | none; the scripts are ordered by file name, as the estate's are | architect |
| CI for `db/postgres/tests` | none | 09 |

## 3. For the decision register

| # | Finding | Evidence | Suggested owner |
|---|---|---|---|
| 1 | **The reconciliation columns of interest_run have no names in the data map.** FI-4 is frozen by this ticket, so the names chosen here become the interface: `loaded_{account,card_xref,tran_cat_balance,disclosure_group}_count`; `loaded_{acct_curr_bal,acct_credit_limit,acct_cash_credit_limit,acct_curr_cyc_credit,acct_curr_cyc_debit,tran_cat_bal,dis_int_rate}_sum`; hash totals `loaded_{acct_id,xref_acct_id,trancat_acct_id}_sum`; `written_transaction_count`, `written_tran_amt_sum`, `written_account_count`, `written_acct_curr_bal_sum`. RC3, RC4 and RC6 have no column: they are comparisons made at the time, not figures of a run. The data map should list the columns. | data-map-interest.md:126; strategy-interest.md:91-96; `db/postgres/02-interest.sql` interest_run | data engineering lead |
| 2 | **Hash totals as NUMERIC(18,2).** The data map gives two types: counts BIGINT, sums NUMERIC(18,2). A sum of 11-digit account ids is a sum, so it is NUMERIC(18,2); it holds 16 integer digits, that is at most 99,999 accounts of the highest id before it overflows. Enough for the estate; NUMERIC(20,0) would be the exact type. | same | data engineering lead |
| 3 | **`interest_run.status` is unconstrained.** Five values are named (LOADED, CALCULATED, EXPORTED, FAILED, ABANDONED) but as Purpose text; a row is created before the load ends, and none of the five says "started". A CHECK and a sixth value need a decision. | data-map-interest.md:125; architecture.md:56 | architect |
| 4 | **`CHAR(n)` columns take the database's default collation, except the three key columns the data map marks `COLLATE "C"`.** Comparison and ORDER BY on e.g. tran_id or xref_card_num then depend on the database's locale. `postgres:15` creates the database with en_US.utf8. No rule orders by those columns today (the export uses seq_no). | `select datcollate from pg_database`; data map E6 | architect |
| 5 | **A second run of the script fails rather than doing nothing.** Chosen because `CREATE TABLE IF NOT EXISTS` would silently keep an older, different table. With a migration tool this choice goes away. | header of `db/postgres/02-interest.sql` | architect |
| 6 | **`TRUNCATE carddemo.interest_run` is refused while interest_transaction references it**, and interest_run rows cannot be deleted before their transactions. Intended (the FK is in the data map), but retention (REL-12) must delete in that order. | assertion "a transaction of a run that does not exist" | release manager |

## 4. Standards review checklist

Standards:

- [x] S1.1 Sources used in order; recorded output decides - PASS: the script follows data-map-interest.md row by row; each column comment cites its copybook field
- [x] S1.2 Rule/code disagreements reproduced as coded and reported - N/A: no rule is implemented; the two under-specified data-map rows are reported in sections 2 and 3
- [x] S2.1 Designed from rules; no method per paragraph - N/A: no Java
- [x] S2.2 Immutable business records; copybook names only at the mapping - N/A: no Java; column names are the data map's
- [x] S2.3 Spring Batch shape, transaction and restart boundaries of the architecture - N/A: no job code; the tables are those of architecture.md "table ownership"
- [x] S2.4 Types and return values, no flags or result codes - N/A: no Java
- [x] S2.5 Constructor injection; no mutable static state - N/A: no Java
- [x] S3.1 `BigDecimal` with data-map precision and scale; no `double`/`float` - PASS for the schema side: every amount is NUMERIC(p,s) of the data map, no REAL/DOUBLE/MONEY (assertions section 2)
- [x] S3.2 Formula: exact product, one division, mode from the policy - N/A: no arithmetic
- [x] S3.3 Rounding mode named on every other arithmetic step - N/A: no arithmetic
- [x] S3.4 Range check before store; overflow goes to the policy - PASS for the schema side: the database refuses a value beyond the PIC (assertions section 8, SQLSTATE 22003); the check before the store is ticket 04's
- [x] S3.5 Encodings only in `record-codec` - PASS: no encoding in the schema; signs are NUMERIC signs, FILLER is BYTEA
- [x] S3.6 Width, padding and FILLER only in codec and schema - PASS: CHAR(n) and BYTEA with octet_length CHECK (assertions "DEFAULT ... stored padded", "acct_filler keeps 178 raw bytes")
- [x] S3.7 Code page and key order each in one place - PASS: key order is `COLLATE "C"` on the three text key columns only (assertion "key order reads 01, 1A, A1"); no code page in the schema
- [x] S3.8 Clock and run parameters injected; `parmDate` is text - PASS for the schema side: `parm_date CHAR(10)`, not DATE
- [x] S4.1 Every PRESENT behaviour touched has its test; new ones reported - PASS: rows g, h, j below; none new
- [x] S5.1 Each quirk/open decision in one named place behind its switch, rule id in names - N/A: no switch in a schema
- [x] S5.2 Nothing fixed, tidied or added without a rule id - PASS: no foreign key between replica tables, no DATE typing, no extra index or CHECK (assertions sections 5 and 6 count them)
- [x] S6.1 Status and message mapping as in section 6 - N/A
- [x] S6.2 One stop exception, one exit-code constant, no skip or retry - N/A
- [x] S6.3 Restart as in the architecture - N/A: the script itself is one transaction and fails cleanly on a second run
- [x] S6.4 Outputs in the recorded format; failed run leaves no SYSTRAN - N/A
- [x] S6.5 Run log byte for byte; no account data in the application log - N/A
- [x] S7.1 A test per rule id, named for it - N/A: the ticket implements no rule id; each assertion is named for the data-map item it checks
- [x] S7.2 Fixture and edge cases of each rule covered - PASS: largest and smallest PIC values accepted, one step beyond rejected
- [x] S7.3 Integration tests on PostgreSQL 15 in a container - PASS: `db/postgres/tests/run.sh` against the compose `postgres:15` service (15.20)
- [x] S7.4 No assertion against the code's own output - PASS: the expected column list is written by hand from the data map in the test file; the mutation in section 6 shows it bites
- [x] S8.1 No edit under `app/`, `fixtures/`, `tools/parity` - PASS: `git diff --stat origin/demo/05b-integration -- app fixtures tools modernization .agents java/interest/src docker-compose.yml` is empty
- [x] S8.2 No golden file re-recorded - PASS
- [x] S8.3 Every new dependency explained in the pull request - PASS: none (section 7)
- [x] S8.4 Frozen shared interfaces unchanged - PASS: FI-4 is this ticket's to create; FI-1, FI-2, FI-3, FI-6 untouched
- [x] S8.5 Synthetic data only - PASS: test rows use account ids 0 and 99999999999 and card numbers 900000000000200x

Behaviours PRESENT in section 4:

- [x] a Truncation without ROUNDED - N/A: no arithmetic
- [x] b High-order digit loss (range check, STOP) - PASS for the schema side: NUMERIC(p,s) refuses the eleventh integer digit (assertions section 8)
- [x] c Intermediate precision of the COMPUTE - N/A
- [x] d Zoned overpunch signs - N/A: codec (ticket 03); the schema stores signed NUMERIC
- [x] e Binary items stay out of data and domain - PASS: no column for a COMP item
- [x] f Non-numeric data rejected at the boundary - N/A: codec; unsigned PIC 9 columns refuse negatives (CHECK >= 0)
- [x] g Fixed width, padding, STRING - PASS: CHAR(n) (assertion "DEFAULT compares as CHAR(10) and is stored padded")
- [x] h Code page and collating order - PASS: `COLLATE "C"` on the text key columns (assertion "key order")
- [x] i REDEFINES replaced by formatter and function - N/A
- [x] j FILLER and record length round trip - PASS: BYTEA NOT NULL with octet_length = 178, 14, 22, 28, 20 (assertions section 6, "177-byte", "179-byte", "X'00'")
- [x] l No state carried between accounts except the counter - N/A
- [x] m Control break and end of file (INT-Q04, INT-T09) - N/A
- [x] o Level-88 result codes replaced by types - N/A
- [x] p Indexed access through the ports; several cards to the policy - PASS for the schema side: card_xref_acct_ix is not unique, two cards of one account are accepted (assertion "boundary rows accepted")
- [x] q File statuses and abend - N/A
- [x] r PARM, generation, disposition, return code - N/A; `run_id` + `seq_no` carry the generation and write order
- [x] s DISPLAY output in the run log - N/A
- [x] t PARM text against the clock - N/A
- [x] u Timestamp format and hundredths - PASS for the schema side: tran_orig_ts, tran_proc_ts are CHAR(26), not TIMESTAMP (G10)
- [x] w PARM length checked - N/A; parm_date is CHAR(10)
- [x] x No stale rate after a failed lookup - N/A
- [x] y Typed fields where COBOL mixed text and numbers - N/A
- [x] z Abend ends the step; stub output not reproduced - N/A
- [x] aa Dead and empty code not ported - N/A

## 5. What is wrong or missing in the standards skill (proposals only)

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

## 6. Method

Skills used: `interest-conversion-standards` (sections 3, 4 rows b/g/h/j/p/u, 7, 8, 9), `interest-ticket-build` (steps 1-4,
9), `interest-ticket-verify` (steps 1, 3, 7, 8, 9, 10), `interest-pitfalls` (P on the JDK, on the db service without a
host port, on the tracked `.pyc` files, on the stale compare.md).

Commands, from the repository root, `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64`:

```sh
docker compose up -d --wait db
db/postgres/tests/run.sh                    # scratch database: 01, 02, assertions, 02 again; exit 0
docker compose exec -T db psql -X -v ON_ERROR_STOP=1 -U carddemo -d carddemo -f /docker-entrypoint-initdb.d/02-interest.sql   # the service's own database, once
mvn -B -f java/interest/pom.xml clean verify
.agents/skills/interest-ticket-verify/parity-report.sh 02-schema i01-baseline
make build && make parity                   # exit 0 with the six tables in the database
```

A new volume: `docker run --rm -v $PWD/db/postgres:/docker-entrypoint-initdb.d:ro postgres:15` logs
`running /docker-entrypoint-initdb.d/01-transaction-type.sql`, `running .../02-interest.sql`, `ignoring .../tests` and ends
with the eight tables, so `docker-compose.yml` needs no change and the test directory is not executed at initdb.

Mutation check of the tests: with the expected precision of `account.acct_id` changed from 11 to 12 in the assertion file,
the run prints `ERROR: ASSERTION FAILED: columns: none missing or different from the data map (account.acct_id)` and psql
exits 3. Changed back.

Failures hit:

| Symptom | Cause | Fix |
|---|---|---|
| `ASSERTION FAILED: primary key account (acct_id)` on a correct schema | `regclass::text` omits the schema when it is on the search_path, so `carddemo.account` printed as `account` | the assertion script sets `search_path = pg_catalog` for its transaction |
| The runner printed `PASSED` after that failed assertion | POSIX `sh` has no `pipefail`: the exit code of `psql ... \| sed` was sed's | the runner is bash with `set -euo pipefail`; proved by the mutation check |
| `parity-report.sh` refuses to run without a case | the script needs at least one case; ticket 02 names none | run with `i01-baseline` and the row marked "regression check" by hand |

## 7. Dependencies

None. The tests use `psql` of the `postgres:15` image the compose file already runs, through `docker compose exec`.
Considered and not used: pgTAP (an extension to install in the image; the ticket asks for psql assertions), a
Testcontainers Java test (Java is outside the ticket's Allowed files).
