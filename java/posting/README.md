# posting chain on Java 21 / Spring Batch / PostgreSQL 15

Strangler-fig slice of the CardDemo migration: the `posting` chain (JCL `POSTTRAN` +
`TRANREPT`, programs `CBTRN02C` + `CBTRN03C`) rewritten as one Spring Batch job. The legacy
COBOL/JCL path is untouched and stays the default; this implementation runs only when it
is selected.

## The switch

`make run` and `make parity` take an implementation selector, `IMPL`:

```sh
make parity CHAIN=posting                 # legacy COBOL/JCL (default, IMPL=legacy)  -> work/parity/report.md
make parity CHAIN=posting IMPL=java       # this implementation                      -> work/parity/report-java.md
make run    CHAIN=posting IMPL=java CASE=p04-overlimit-reject   # outputs in work/run/posting/<case>
```

Both go through `tools/impl/run-case.sh <chain> <case> <out>`:

* `IMPL=legacy` (or unset) runs `tools/parity/record.py` in the `estate` container, the same
  command the Makefile ran before the switch existed.
* `IMPL=java` runs `java/<chain>/run-case.sh`, here `java/posting/run-case.sh`. A chain that has
  no implementation under `java/` fails with a clear message instead of falling back.

Cutting posting over means running the chain with `IMPL=java`; rolling back means dropping
the selector (or `IMPL=legacy`). Nothing is shared between the two paths except the fixture
inputs and the output layout, so either can run at any time and both are compared with the
same `tools/parity/compare.py`. `make record` (golden files) stays legacy-only.

`java/posting/run-case.sh` builds and runs the `posting-java` service from
`java/posting/compose.yml`, an overlay on the root `docker-compose.yml` that uses the same
PostgreSQL 15 `db` service. CI runs `make parity CHAIN=posting IMPL=java` after the legacy
`make parity`. If Maven Central is rate-limited on your host, set `MAVEN_MIRROR_URL` (for
example `https://maven-central.storage-download.googleapis.com/maven2`) and the image build
uses that mirror.

## How the chain maps

| Legacy | Java |
|---|---|
| setup jobs `ACCTFILE`, `XREFFILE`, `TCATBALF`, `TRANTYPE`, `TRANCATG`, `TRANFILE` (DELETE/DEFINE + REPRO) | step `setup.loadClusters` (`LoadClustersTasklet`): truncate and load the `posting` schema tables from the case's flat files |
| VSAM KSDS clusters ACCTDATA, CARDXREF, TCATBALF, TRANTYPE, TRANCATG, TRANSACT | tables `account`, `card_xref`, `tran_cat_balance`, `tran_type`, `tran_category`, `transaction` (Flyway `V1`) |
| `POSTTRAN` STEP15, `CBTRN02C` | chunk step `POSTTRAN.STEP15` (`PostTransactionsStep`: reader, processor, writer, listener) |
| `DALYREJS(+1)` GDG | `datasets/AWS.M2.CARDDEMO.DALYREJS.G0001V00.txt` |
| `TRANREPT` PRC001/STEP05R (REPRO + DFSORT), STEP10R `CBTRN03C` | tasklet `TRANREPT.STEP10R` (`TransactionReportTasklet`, extract via `PostingStore.reportExtract`) |
| `TRANREPT(+1)` GDG | `datasets/AWS.M2.CARDDEMO.TRANREPT.G0001V00.txt` |
| `record.py` unload, SYSOUT capture, `rc.json` | step `unload` (`UnloadTasklet`) |

Spring Batch's JobRepository tables live in the same `posting` schema (Flyway `V2`).
Record layouts are in `record/Records.java`, a private copy of the copybook offsets. No Java
code is shared with other migrated chains.

Return codes: the Java steps report under the legacy step names in `rc.json`. Every step is 0
except `POSTTRAN/STEP15`, which is 4 when any transaction was rejected, as in the legacy run.
The JVM exits 0 when the job completes (RC 4 is recorded, not raised) and 12 when it fails.

## Legacy quirks reproduced on purpose

Each one below is in the Java code with a comment, because the golden files depend on it or
because CBTRN02C/CBTRN03C behave that way even where no fixture covers it yet.

CBTRN02C (posting):

1. **The credit-limit check uses cycle amounts.** `WS-TEMP-BAL = ACCT-CURR-CYC-CREDIT -
   ACCT-CURR-CYC-DEBIT + DALYTRAN-AMT` is compared with `ACCT-CREDIT-LIMIT`. `ACCT-CURR-BAL`
   is ignored. Debits are stored negative, so subtracting them adds them back (p04).
2. **Both checks run and the last one wins.** An over-limit transaction on an expired account
   is rejected as `0103`, not `0102`.
3. **Expiry is a string comparison** of `ACCT-EXPIRAION-DATE` against the first 10 bytes of
   `DALYTRAN-ORIG-TS`, so a transaction on the expiry date is accepted (p05).
4. **Credits and debits by sign:** an amount `>= 0`, including zero, goes to
   `ACCT-CURR-CYC-CREDIT` and a negative one to `ACCT-CURR-CYC-DEBIT`. `ACCT-CURR-BAL` always
   gets the amount.
5. **A missing TCATBALF record is created**, and the step DISPLAYs
   `TCATBAL record not found for key : <key>.. Creating.` (p06).
6. **The processing timestamp** is `FUNCTION CURRENT-DATE` reshaped to
   `YYYY-MM-DD-HH.MM.SS.hh0000`: hundredths, then a literal `0000`, not microseconds. The clock
   is the chain's frozen clock from `tools/runjcl/chains.json` (`posting.clock` overrides it,
   and it falls back to the wall clock when neither is set).
7. **Reject records** are the 350-byte daily record exactly as read, plus the reason
   `PIC 9(4)` and the description `X(76)`.
8. **RC 4 when anything was rejected**, plus the `TRANSACTIONS PROCESSED :` and
   `TRANSACTIONS REJECTED  :` counts as `9(9)` (two spaces in the second label).
9. **Arithmetic truncates** like ADD/COMPUTE without `ON SIZE ERROR` or `ROUNDED`: high-order
   digits beyond the receiving PIC are dropped (`Zoned.fit`).

TRANREPT / CBTRN03C (report):

10. **The SORT window is hardcoded in the JCL**, not taken from DATEPARM. STEP05R keeps
    records with `TRAN-PROC-TS(1:10)` between `2022-01-01` and `2022-07-06` (SYMNAMES in
    `TRANREPT.jcl`, here `posting.report.sort-start-date` / `sort-end-date`). DATEPARM only
    drives CBTRN03C's own filter and the header.
11. **Sort order** is by card number only. The sort is stable, so ties keep TRANSACT key
    (TRAN-ID) order.
12. **`NEXT SENTENCE` leaves the whole report loop.** A record outside the DATEPARM range ends
    the report on the spot, with no page or grand totals. On an empty day the
    still-blank `TRAN-RECORD` fails the date test the same way, so the report dataset is empty,
    not just headers, and SYSOUT has no `TRAN-AMT` lines (p07).
13. **At end of file the last transaction is counted twice.** CBTRN03C DISPLAYs `TRAN-AMT`
    and `WS-PAGE-TOTAL` (digits plus a trailing sign), adds the last amount to the page and
    account totals again, then writes page totals and grand totals. Grand total equals page
    total plus that extra amount (p01: page/grand total -26.25 for transactions that sum to
    -10.50).
14. **The last account never gets an `Account Total` line.** Account totals are written only
    when the card number changes.
15. **Page breaks** happen when `WS-LINE-COUNTER MOD 20 = 0` before a detail line. Headers,
    account totals and page totals all add to the counter, so a page can run past 20 lines
    when the counter steps over a multiple of 20.
16. **The account total is never added to the page or grand totals.** Only page totals roll
    into the grand total.
17. **Each detail line is the full `TRAN-RECORD` DISPLAYed to SYSOUT** before it is written to
    the report.
18. **Edited amounts:** `-ZZZ,ZZZ,ZZZ.ZZ` on detail lines (blank sign when positive) and
    `+ZZZ,ZZZ,ZZZ.ZZ` on totals. Both have a fixed sign position and leading-zero suppression,
    and a zero value edits to all spaces. Type and category descriptions are cut to 15 and 29
    characters.
19. **An unknown card, type or category abends the step**
    (`INVALID CARD NUMBER : ...` / `INVALID TRANSACTION TYPE : ...` / `INVALID TRAN CATG KEY : ...`
    then `ABENDING PROGRAM`). Java fails the job (exit 12) and keeps SYSOUT and `rc.json`.

## Not proven by the fixtures

The seven fixtures pass field for field, but they do not cover the behaviour below. The Java
code follows the COBOL as written; confirm on the mainframe before relying on it.

* **Abend return codes** (quirk 19, and I/O errors): `rc.json` for an abended legacy step was
  not recorded, so the Java job only reports failure.
* **Overflow** (quirk 9): no fixture comes close to the PIC limits. Left truncation is the
  COBOL standard; GnuCOBOL vs Enterprise COBOL `TRUNC` options were not compared.
* **A zero total or amount** edits to spaces (quirk 18). No fixture prints a zero total.
* **A record outside the DATEPARM range** (quirk 12) only shows up on an empty day in the
  fixtures. The SORT window and DATEPARM agree in every case.
* **ZD sort key:** the JCL sorts `TRAN-CARD-NUM` as `ZD`, but the off-host runner (and this
  code) compares bytes. That only differs for card numbers that are not all digits.
* **Duplicate TRAN-ID in one day:** COBOL abends on the failed WRITE. Java fails the chunk
  on the primary key, so the effect is the same (job fails), but the reject/SYSOUT state at
  the point of failure was not compared.
* **The fixture README for p07** says "report with headers and zero detail lines". The golden
  report is empty (quirk 12). Java matches the golden file.

## Build and test

```sh
cd java/posting && mvn -B package        # Java 21; unit tests for the zoned/edited-number codec
make parity CHAIN=posting IMPL=java      # the real test: golden-file parity, in Docker
```
