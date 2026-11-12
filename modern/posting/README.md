# Posting: POSTTRAN + TRANREPT

Java 21 / Spring Batch / PostgreSQL implementation of CBTRN02C and CBTRN03C.
Uses the shared `org.carddemo.modern:record-codec`; there is no dependency on
the interest application. The parent reactor retains both applications.

```sh
make modern-build
make parity CHAIN=posting
# Extra synthetic edge cases, after building (never updates goldens):
docker compose exec -T estate python3 -B modern/posting/probe.py
```

The generic candidate harness launches `modern/posting/target/posting.jar`
with `--case=<fixture directory> --out=<recording directory>
--estate.root=<repository>`. Database connection settings are `PGHOST`,
`PGPORT`, `PGDATABASE`, `PGUSER`, and `PGPASSWORD`, as in the estate container.
The fixture runner recreates the posting data for each run; it is not a
production, concurrent, or restartable processing service.

Batch metadata persists across candidate invocations and is shared with
interest in the database user's default schema (`carddemo` in the estate).
Neither application's loader resets the `BATCH_*` tables or sequences.
`spring.batch.jdbc.initialize-schema=always` is safe here: Spring Boot 3.3.5's
`BatchDataSourceScriptDatabaseInitializer.getSettings` explicitly enables
continue-on-error for the Batch schema script. Existing-table/sequence DDL
errors are ignored, while job metadata continues accumulating. This is
separate from `spring.sql.init`, which initializes the application's
idempotent business schema. Full default parity and consecutive posting-jar
launches after interest verify reuse of the same persistent metadata tables.

## Design

* `InputLoader` recreates setup REPROs, preserving the first duplicate key.
  PostgreSQL primary keys represent the KSDS keys. `schema-posting.sql`
  defines accounts, category balances, card cross-references, transaction
  types/categories, disclosure groups, master, daily/reject/report staging.
* `PostingJobConfig` runs POSTTRAN in daily input order, with a step-scoped
  `Cbtrn02cProcessor`, one-record chunks, and `PostWriter`. Each SQL WRITE or
  REWRITE has its own nested-transaction savepoint: a failed operation rolls
  back only itself, not preceding operations in that record. Terminating the
  step after recording an abend commits those preceding writes.
* TRANREPT unloads the master in transaction-key order, applies the JCL date
  range, and sorts by card with unload sequence as the stable tie-breaker.
  `Cbtrn03cProcessor` applies DATEPARM independently and maintains report
  working storage. `PostingRunner` and the copied `RecordedRun`, `PythonJson`,
  and `Estate` helpers emit the five datasets, manifest, return codes, SYSOUT,
  frozen timestamp, and Python-compatible JSON formatting.
* ACCOUNT and TCATBAL are decoded with `decode(line, true)` and retain their
  `FILLER@offset` values when rewritten. Rejects retain the original daily
  record. C-locale database keys reproduce the runner's lexical key order.

## Preserved quirks and evidence

| Behavior | Implementation | Evidence |
|---|---|---|
| Limit uses cycle credit minus cycle debit plus amount, not balance; equality accepted | `Cbtrn02cProcessor.validate` / 1500-B-LOOKUP-ACCT | p04; `PostingRulesTest.limitUsesCyclesRatherThanBalanceAndAcceptsEquality` distinguishes balance from cycles |
| Card, account, limit, then expiry; expiry overwrites overlimit; equal expiry date accepted | `Cbtrn02cProcessor.validate` | p02–p05; `validationOrderAndExpirationOverride` covers combined failures |
| Unsigned reject codes 0100–0103, original descriptions, RC 4 | `reasonDescription`, `RejectRecord`, `returnCode` | p02–p05; `rejectCodesAreUnsignedAndDescriptionsMatch` |
| Signed amounts, including negative cycle debit and -15.75 ending in N | `Cbtrn02cProcessor.post`, shared zoned codec | p01/p06 inputs have negative daily amounts; both have negative transaction/category/debit outputs; `negativeOverpunchIsPreservedForAllPostingSignedFields` |
| Existing categories add; missing categories are created with SYSOUT notice | `Cbtrn02cProcessor.post` / 2700-UPDATE-TCATBAL | p01, p06 |
| Category WRITE/REWRITE failures abend, but 2800 does not check account REWRITE status and still writes the transaction | `PostWriter`, `PostingRepository.postCategory` | `categoryWriteAndRewriteFailuresStopBeforeAccountOrTransaction`, `uncheckedAccountRewriteStatusStillAllowsTransactionWrite` unit tests |
| OPEN OUTPUT discards even records loaded by initial TRANFILE REPRO | `Cbtrn02cProcessor.open` / 0100-TRANFILE-OPEN | `duplicate-repro` scratch probe |
| REPRO keeps first duplicate, RC 0 | `InputLoader.load` | `duplicate-repro` probe includes differing duplicate payloads |
| Frozen processing timestamp | `Estate.clock`, processor constructor | p01–p06 |
| Card grouping (not account grouping), stable sort, independent DATEPARM filtering | `PostingJobConfig`, `Cbtrn03cProcessor.process` | p01; `date-excluded` probe |
| EOF adds last retained eligible amount again; no final Account Total | `Cbtrn03cProcessor.finish` | p01, `eofDoubleAddsLastAmountAndDoesNotFlushLastAccount` |
| Page break checks total line count MOD 20, including headers/totals | `Cbtrn03cProcessor.process` / 1100-WRITE-TRANSACTION-REPORT | `pagination` probe (30 detail records) |
| Empty day/date-excluded/empty DATEPARM produces no report headers | `Cbtrn03cProcessor.process`, `finish` | p07; `date-excluded`, `empty-dateparm` probes; unit empty-report test |
| FILLER survives ACCOUNT/TCATBAL REWRITE | `PostingRepository`, `InputLoader` | `rewrite-fillers` probe, unit filler test |
| Writes preceding abend survive; CEE3ABD, blank line, reverse-open implicit CLOSE warnings | `PostWriter`, `Abend`, processors' `open` methods | `duplicate-daily-abend`, `missing-report-type`, `missing-report-category` probes; unit open/lookup/error-output tests |

The normal fixtures do not independently discriminate combined validation
failures, balance versus cycle arithmetic, multi-page reports, nonblank filler,
duplicate keys, or abends. The unit tests and scratch probes cover these gaps.
OPEN-error unit tests simulate unavailable database objects; OS-level report
output failures and a completely unavailable database are not parity-tested.

## Observed differences log

The first complete Java run passed all seven normal cases. Two actual
red-to-green differences were observed during codec analysis/edge probing:

| Case/check | Observed difference | Cause | Fix and verification |
|---|---|---|---|
| REJTRAN focused reason-byte probe (p02–p05 goldens) | Java encoded spaces at 350–353 instead of 0100/0101/0102/0103 | Shared REJTRAN mapped reason as FILLER | Standalone codec commit maps unsigned zoned PIC 9(04); focused test asserts 0102 |
| `duplicate-repro` scratch case | Java TRANSACT had one extra initial record; manifest differed | Loader correctly kept first duplicate, but Java omitted CBTRN02C OPEN OUTPUT truncation | Truncate transaction master in `Cbtrn02cProcessor.open`; all recorded probe files become byte-identical |

Filler preservation, per-operation write durability, reverse CLOSE order,
and duplicate-first loading were implemented before their first probe; they
are coverage additions, not claimed red-to-green observations. No fixtures,
goldens, COBOL, or copybooks were changed, and `make record` was not used.
