# POSTTRAN / TRANREPT posting candidate

Java 21 / Spring Batch on PostgreSQL 15, following the other `modern/` modules.
The isolated `posting` schema holds typed `account`, `card_xref`,
`tran_cat_balance`, `tran_type`, `tran_category` and `"transaction"` tables,
plus Spring Batch metadata. Named copybook fields retain exact NUMERIC
precision and scale; FILLER stays in the shared record codec, not SQL.
The account alternate index on cross-reference is nonunique. Runs are serialized
by the harness. Only synthetic data is supported.

The application reads input files only, never expected files. It imports the
initial transaction dataset, posts the daily sequential input, exports indexed
datasets in primary-key order (REPRO), and performs a stable numeric-card sort.
Like TRANREPT.jcl, SORT first includes processing dates 2022-01-01 through
2022-07-06; CBTRN03C then independently applies the inclusive DATEPARM range
(columns 1–10 and 12–21). DATEPARM does not change the SORT constants.
The command line is `posting <input-directory> <output-directory>`.

Outputs follow the recorder: `datasets/<DSN>.txt`,
`sysout/POSTTRAN.STEP15.txt`, `sysout/TRANREPT.STEP10R.txt`, and `rc.json`.
GDGs start at G0001V00 each run. Trailing spaces are removed from recorded
dataset lines, but transaction DISPLAY lines retain all 350 characters.

Missing input files are treated as empty, as in the recorder. Indexed loaders
keep the first duplicate key. Supplementary `setup/<JOB>.STEP15.txt` and
`setup/<JOB>.rc.json` record processed/skipped counts and raw loader RC 4;
utility RC remains 0, matching the runner's normalization. Reject days return
RC 4 without failing the Spring Batch job; fatal indexed-write/report-lookup
errors produce file-status/CEE3ABD diagnostics and RC 12, retaining prior writes.
Those fatal paths are unit-tested, not proven by an existing fixture.

## Cycle amounts, not current balance

`p04-overlimit-reject` proves the inclusive limit comparison against
`ACCT-CURR-CYC-CREDIT - ACCT-CURR-CYC-DEBIT + DALYTRAN-AMT`.
The current balance is deliberately not consulted. The S9(9)V99 intermediate
drops high-order digits; stored balances follow their own copybook widths.

## Validation order and reject trailer

`p02-invalid-card` rejects with 0100 / INVALID CARD NUMBER FOUND;
`p03-account-missing` with 0101 / ACCOUNT RECORD NOT FOUND;
`p04-overlimit-reject` with 0102 / OVERLIMIT TRANSACTION;
`p05-expired-account` with 0103 / TRANSACTION RECEIVED AFTER ACCT EXPIRATION.
The 430-byte reject preserves the original 350 bytes and adds a 4-digit reason
and 76-character description. Expiration is checked after the credit limit and
overrides reason 102 when both fail (also covered by a synthetic unit test).
Any rejects give STEP15/maxrc 4; DISPLAY counts remain in input order.

## Expiration is inclusive and uses origin date

`p05-expired-account` accepts an account expiring on the origin date itself.
The comparison uses the first ten origin-timestamp characters, not today's date.

## Negative cycle debits and category create versus update

`p06-credits-new-category` adds negative amounts directly to cycle debit (it
does not take their absolute value), while nonnegative amounts go to credit.
Category balances add the signed amount regardless of transaction type.
Missing compound keys create a zero-initialized record and DISPLAY
`TCATBAL record not found for key : ... Creating.` before account/transaction
writes. `p01-baseline-mixed` covers updating existing category records.

## Frozen processing timestamp

`p01-baseline-mixed` proves `2022-07-05-10.00.00.000000`, corresponding to the
chain clock `2022-07-05 10:00:00.00`; no wall clock enters transaction records.

## Report EOF doubles the final amount

`p01-baseline-mixed` and `p04-overlimit-reject` prove that the EOF read leaves
the last transaction in the record area. CBTRN03C adds that amount to the page
and account accumulators again before page/grand totals. Java deliberately
does the same, including the EOF `TRAN-AMT` and `WS-PAGE-TOTAL` DISPLAY lines.
The EOF date check can suppress totals entirely if the last SORT record is
outside DATEPARM. This additional path has a synthetic test.

## Card breaks and missing final account total

`p01-baseline-mixed` groups the stable SORT output by card, not by account.
Only card breaks write an account total; no final account total is flushed.
Headers/details/edited signed amounts and 133-column rules follow CVTRA07Y.
Page breaks occur when the line counter modulo 20 is zero *before* a detail,
including headers, totals and rules in that count (synthetic pagination test).

## Empty day has no report headers

Despite the p07 README's description, its actual golden report is empty.
`p07-empty-day` proves no details, headers or totals, RC 0, and start/end SYSOUT
only (plus the reporting date range). Header emission is deferred to the first
qualifying transaction. This follows the code and recorded bytes.

## Run and verify

```
docker compose up -d db java
docker compose exec -T java mvn -f modern/pom.xml test
docker compose exec -T java mvn -f modern/pom.xml -pl posting -am package -DskipTests
docker compose exec -T java java -jar modern/posting/target/posting-0.1.0.jar fixtures/posting/p01-baseline-mixed/input work/run/posting/p01-baseline-mixed
make parity CHAIN=posting
make parity CHAIN=posting TARGET=legacy
```

The existing Makefile discovers `modern/posting`, builds its reactor module and
selects its Java jar automatically; no Dockerfile or Makefile change is needed.
Tests use the Compose `db` service and isolated `posting_test` schema. Tests may
read golden files; production code cannot. No parity run or container build was
performed during this conversion. Interest's cutover behavior (KAN-249) is untouched.
