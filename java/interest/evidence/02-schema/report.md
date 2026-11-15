# Ticket 02 schema (INT-T02) - report

Revision of the code: `b0e47a4f26d0c33852b4b2ec11879e124ee30c15`. Evidence: `test-output.txt` (psql apply and assertions), `parity-report.md`.
Files added: `db/postgres/02-interest.sql`, `db/postgres/tests/02-interest-assert.sql`, `db/postgres/tests/run.sh`.
`docker-compose.yml` is unchanged: the db service already mounts `./db/postgres` as its init directory.

## 1. Result

| Acceptance line | Met | Shown by |
|---|---|---|
| Empty PostgreSQL 15 with 01 applied; `psql -v ON_ERROR_STOP=1 -f db/postgres/02-interest.sql` exits 0 | yes | `test-output.txt` "## 2", exit code 0 |
| A second run fails cleanly or is a no-op, as the script documents | yes - it fails cleanly: `relation "account" already exists`, psql exit 3, 19 objects unchanged; documented in the script header | `test-output.txt` "## 3" |
| `information_schema.columns` of the six tables equals the data map row for row | yes, with the limit of gap 1 below for the reconciliation columns of `interest_run` | `PASS columns: ...` (6 lines, 79 columns) and the identity line |
| Primary keys of the six tables; `card_xref_acct_ix` exists and is not unique; `(run_id, tran_id)` unique | yes | `PASS keys: ...` (3 lines), `PASS index: ...` (2 lines) |
| `acct_curr_bal = 10000000000.00`, `tran_amt = 1000000000.00`, `dis_int_rate = 10000.00`, a negative `acct_id`, a 177-byte `acct_filler` are rejected | yes: SQLSTATE 22003 for the three amounts, 23514 for the other two | first five `PASS errors: ...` lines |
| Build: the script applied to the db service of docker-compose | yes: in a scratch database of the running db service, and by the init directory of a fresh `postgres:15` | `test-output.txt`, both parts |
| Tests exit non-zero on any failed assertion | yes: every assertion raises an exception under `ON_ERROR_STOP=1`; `run.sh` returns 1. Seen during the build (section 6, failure 1) | `db/postgres/tests/run.sh` |
| Fixture cases: none | n/a | `parity-report.md` |

Assertions beyond the acceptance lines: the 13 CHECKs and no other, exactly one foreign key
(`interest_transaction.run_id -> interest_run`), no extra unique constraint or index, boundary values
accepted, 21 further rejections, C collation order of the key. 48 PASS lines in all. The assertion
script runs in one transaction and rolls back; the tables are left empty.

## 2. What the ticket does not cover (gaps)

Data-map rows that could not be expressed in PostgreSQL 15 exactly as written:

| # | Data-map row | What was done |
|---|---|---|
| 1 | `interest_run`: "`loaded_* / written_*` counts and sums - `BIGINT / NUMERIC(18,2)`" names no column | 31 nullable columns were named from strategy items RC1-RC6 (`loaded_<table>_count`, `loaded_<field>_sum`, `loaded_g02/g03_difference_count`, `loaded_<table>_key_count`, `written_*`); counts are `BIGINT`, sums `NUMERIC(18,2)`. The names are this ticket's choice, not the data map's. The account-id hash totals are `NUMERIC(18,2)` because the row allows only these two types. RC4's lowest and highest key have no column: they are neither a count nor a sum |
| 2 | `interest_run`: "`started_at / finished_at` - `TIMESTAMPTZ`" is one row | two nullable columns `started_at`, `finished_at` |
| 3 | `interest_run.status`: the five values are in the Purpose column, not in the type | `VARCHAR(12) NOT NULL` and no CHECK, as the type column says |

Every other row is expressed exactly. Also not in this ticket:

| Gap | Owner ticket |
|---|---|
| Nothing in `java/interest` reads or writes the tables; `InterestCalcJobIT` does not apply the script, so its expected table list is unchanged | 05, 06, 07 |
| An existing volume of the db service does not get the script (init scripts run only on an empty volume): apply it by hand or `make reset` | operations / 09 |
| Retention of `interest_run` and `interest_transaction` (REL-12), the job repository (schema `batch`) | 06 / release owner |
| No foreign key to `carddemo.transaction_type` (G09) | data architect |

## 3. For the decision register

| # | Finding | Evidence | Suggested owner |
|---|---|---|---|
| 1 | The data map does not name the reconciliation columns of `interest_run`. Tickets 05 and 07 write them, so the names in `02-interest.sql` become a shared interface (FI-4) once merged. The data map should list them. | `data-map-interest.md` line 126; `02-interest.sql` lines 84-121 | data architect |
| 2 | RC4 asks for the lowest and highest key per file; the row allows only counts and sums. Either the loader checks them without storing them, or the data map adds columns. | `strategy-interest.md` RC4 | data engineering lead |
| 3 | `interest_run.status` has five allowed values and no CHECK. A CHECK would catch a misspelt status in tickets 05-07. | `data-map-interest.md` line 125 | data architect |
| 4 | The script is not re-runnable and there is no migration tool in the architecture. How the schema reaches an existing database, and how it changes later, is undecided. | script header | release owner |

## 4. Standards review checklist (`.agents/skills/interest-conversion-standards/SKILL.md`, section 9)

This ticket has no Java. Lines about Java code are N/A for that reason.

Standards:

- [x] S1.1 Sources used in order; recorded output decides - **PASS** - the data map is the only source of the DDL; no recorded output applies
- [x] S1.2 Rule/code disagreements reproduced as coded and reported - **N/A** - no rule built
- [x] S2.1 Designed from rules; no method per paragraph - **N/A** - no Java
- [x] S2.2 Immutable business records; copybook names only at the mapping - **N/A** - no Java
- [x] S2.3 Spring Batch shape, transaction and restart boundaries of the architecture - **N/A** - no Java
- [x] S2.4 Types and return values, no flags or result codes - **N/A** - no Java
- [x] S2.5 Constructor injection; no mutable static state - **N/A** - no Java
- [x] S3.1 `BigDecimal` with data-map precision and scale; no `double`/`float` - **PASS** (schema side) - `NUMERIC` precision and scale of the data map, `PASS columns` lines; no floating type
- [x] S3.2 Formula: exact product, one division, mode from the policy - **N/A** - no arithmetic
- [x] S3.3 Rounding mode named on every other arithmetic step - **N/A** - no arithmetic
- [x] S3.4 Range check before store; overflow goes to the policy - **PASS** (schema side) - the database rejects out-of-range values, `PASS errors` lines; the policy is ticket 04
- [x] S3.5 Encodings only in `record-codec` - **N/A** - no codec change
- [x] S3.6 Width, padding and FILLER only in codec and schema - **PASS** - `CHAR(n)` widths and `BYTEA` fillers with `octet_length` CHECKs in `02-interest.sql`
- [x] S3.7 Code page and key order each in one place - **PASS** (schema side) - `COLLATE "C"` on the three key columns the data map names; `PASS collation` line
- [x] S3.8 Clock and run parameters injected; `parmDate` is text - **PASS** (schema side) - `parm_date CHAR(10)`
- [x] S4.1 Every PRESENT behaviour touched has its test; new ones reported - **PASS** - rows g, h, j below; none new
- [x] S5.1 Each quirk/open decision in one named place behind its switch, rule id in names - **N/A** - no rule built
- [x] S5.2 Nothing fixed, tidied or added without a rule id - **PASS** - no column, CHECK, key or index beyond the data map (asserted: "and no other"); the column names of gap 1 are reported
- [x] S6.1 Status and message mapping as in section 6 - **N/A** - no Java
- [x] S6.2 One stop exception, one exit-code constant, no skip or retry - **N/A** - no Java
- [x] S6.3 Restart as in the architecture - **N/A** - no Java
- [x] S6.4 Outputs in the recorded format; failed run leaves no SYSTRAN - **N/A** - no output
- [x] S6.5 Run log byte for byte; no account data in the application log - **N/A** - no Java
- [x] S7.1 A test per rule id, named for it - **N/A** - no rule built; assertions are named for what they check
- [x] S7.2 Fixture and edge cases of each rule covered - **N/A** - no rule built; boundary values on both sides are asserted
- [x] S7.3 Integration tests on PostgreSQL 15 in a container - **PASS** - `run.sh` in the `postgres:15` db service (15.20)
- [x] S7.4 No assertion against the code's own output - **PASS** - expected rows are written by hand from the data map in `02-interest-assert.sql`
- [x] S8.1 No edit under `app/`, `fixtures/`, `tools/parity` - **PASS** - `git diff --stat origin/demo/05b-integration -- app fixtures tools modernization .agents` is empty
- [x] S8.2 No golden file re-recorded - **PASS** - same diff
- [x] S8.3 Every new dependency explained in the pull request - **PASS** - none
- [x] S8.4 Frozen shared interfaces unchanged - **PASS** - `01-transaction-type.sql` and the data map unchanged; FI-4 is created here as specified
- [x] S8.5 Synthetic data only - **PASS** - the assertion rows are invented and rolled back

Behaviours PRESENT in section 4:

- [x] a Truncation without ROUNDED - **N/A** - no arithmetic
- [x] b High-order digit loss (range check, STOP) - **PASS** (schema side) - overflow rejected with SQLSTATE 22003
- [x] c Intermediate precision of the COMPUTE - **N/A**
- [x] d Zoned overpunch signs - **N/A** - codec, ticket 03
- [x] e Binary items stay out of data and domain - **N/A**
- [x] f Non-numeric data rejected at the boundary - **N/A** - loader, ticket 05
- [x] g Fixed width, padding, STRING - **PASS** - `CHAR(n)` columns; over-length values rejected (22001)
- [x] h Code page and collating order - **PASS** - `PASS collation` line (01, 1A, A1, a1)
- [x] i REDEFINES replaced by formatter and function - **N/A**
- [x] j FILLER and record length round trip - **PASS** - five `BYTEA` fillers of exactly 178, 14, 22, 28 and 20 bytes; a wrong length is rejected (23514)
- [x] l No state carried between accounts except the counter - **N/A**
- [x] m Control break and end of file - **N/A**
- [x] o Level-88 result codes replaced by types - **N/A**
- [x] p Indexed access through the ports; several cards to the policy - **PASS** (schema side) - `card_xref_acct_ix` not unique; two cards for one account accepted
- [x] q File statuses and abend - **N/A**
- [x] r PARM, generation, disposition, return code - **N/A**
- [x] s DISPLAY output in the run log - **N/A**
- [x] t PARM text against the clock - **N/A**
- [x] u Timestamp format and hundredths - **PASS** (schema side) - `tran_orig_ts`, `tran_proc_ts` are `CHAR(26)`

## 5. What is wrong or missing in the standards skill (not edited)

- Section 9 has no way to mark a ticket without Java: 20 of its lines can only be N/A. A line "ticket has no Java" or a short schema-only list would make the review shorter.
- Nothing in the standards says how a schema script must behave on a second run, or where SQL assertions live. This ticket used `db/postgres/tests/` and "fails cleanly".

Skills, not edited:

- `interest-ticket-verify` step 8: `parity-report.sh` needs a case the ticket names; for a ticket with no case there is no instruction. `parity-report.md` was written by hand.
- `interest-ticket-verify` step 7 says "a ticket that adds tables changes the expected list" in `InterestCalcJobIT`. Not so for this ticket: the test's container does not apply `db/postgres`, and `java/interest` is outside the Allowed files.
- `interest-pitfalls`: `pg_get_constraintdef` qualifies a table name only when its schema is not on the `search_path`, and user `carddemo` has schema `carddemo` on it through `"$user"`. An assertion on catalog text must set `search_path` first.

## 6. Method: commands run and failures hit

```sh
git checkout -b demo/05-02-schema origin/demo/05b-integration
docker compose up -d --wait db                 # postgres:15 (15.20)
db/postgres/tests/run.sh                       # apply 01, apply 02, apply 02 again, assertions
git checkout -- tools/parity/__pycache__       # before every commit
git diff --stat origin/demo/05b-integration -- app fixtures tools modernization .agents   # empty
# regression, at the committed revision
mvn -B -f java/interest/pom.xml clean verify
make parity-java                               # i03, i04 fail as on the base
```

| # | Symptom | Cause | Fix |
|---|---|---|---|
| 1 | `FAIL keys: one foreign key ...`: expected `REFERENCES carddemo.interest_run(run_id)`, found `REFERENCES interest_run(run_id)`; `run.sh` exit 1 | the catalog prints a name unqualified when its schema is on the `search_path` | `SET LOCAL search_path = pg_catalog` at the start of the assertion script; the expected text stays qualified |

## 7. Dependencies

None added.
