# posting chain on Java 21 / Spring Batch / PostgreSQL 15

Strangler-fig slice of the CardDemo migration: the `posting` chain (JCL `POSTTRAN` +
`TRANREPT`, programs `CBTRN02C` + `CBTRN03C`) rewritten as one Spring Batch job. The legacy
COBOL/JCL path is untouched and stays the default; this implementation runs only when it
is selected.

## The switch

Posting uses the same step-level strangler seam as `java/interest` (stacked on that PR). The JCL,
the setup jobs, POSTTRAN's other steps and TRANREPT's REPRO and SORT steps stay on the JCL runner.
Only the program behind `EXEC PGM=` changes:

| JCL step | Legacy | Java |
|---|---|---|
| `POSTTRAN STEP15 EXEC PGM=CBTRN02C` | GnuCOBOL | `java -jar java/posting/target/posting.jar CBTRN02C` |
| `TRANREPT STEP10R EXEC PGM=CBTRN03C` | GnuCOBOL | `java -jar java/posting/target/posting.jar CBTRN03C` |

`tools/runjcl/impl/posting.json` registers both programs and holds the chain's default
(`"default": "legacy"`). `tools/runjcl/candidate.py` reads it.

| What | How |
|---|---|
| Run one case on Java | `make run CHAIN=posting IMPL=java [CASE=p04-overlimit-reject]` |
| Parity on Java (report `work/parity/report-java-posting.md`) | `make parity-java-posting [CASE=..]` (also part of `make parity-java`) |
| Ad hoc, several chains | `CARDDEMO_IMPL=posting=java,interest=legacy make run CHAIN=posting` |
| Cut over | set `"default": "java"` in `tools/runjcl/impl/posting.json` (one reviewed line) |
| Roll back | set it back to `"legacy"`, or `IMPL=legacy` / `CARDDEMO_IMPL=posting=legacy` for a single run |

With no `IMPL` and no `CARDDEMO_IMPL`, everything runs as before, and `make parity` remains the
legacy golden-file check. The joblog shows which implementation ran
(`CBTRN02C -> posting=java: java -jar ...`). CI runs `make parity-java-posting` after the legacy
parity and the interest Java parity.

### Step contract

Each program keeps the contract of the COBOL program it replaces (`StepFiles`):

* Datasets arrive as `DD_<ddname>` paths and hold fixed-length unblocked records. The runner unloads
  KSDS clusters to flat images before the step. Afterwards it reloads any image the program changed
  (CBTRN02C changes `TRANFILE`, `ACCTFILE` and `TCATBALF`). `DALYREJS(+1)`, `TRANREPT(+1)`,
  the sorted `TRANFILE` and `DATEPARM` are plain sequential files.
* STDOUT is SYSOUT and carries only the DISPLAY lines. Logging goes to STDERR, which ends up in the joblog.
* The exit status is the step return code: 0, 4 when CBTRN02C rejected anything, or 12 after an
  abend (as from the `CEE3ABD` stand-in).
* The clock is `COB_CURRENT_DATE`, the chain's frozen clock that the runner sets.

### Inside the jobs

```
PostingRunner           picks the job from argv[0], prints SYSOUT, exits with the RC
  Job CBTRN02C
    CBTRN02C.load       LoadClustersTasklet     XREFFILE, ACCTFILE, TCATBALF images -> schema posting
    CBTRN02C.post       chunk step              PostTransactionsStep: read DALYTRAN, validate,
                                                post (tran_cat_balance, account, transaction), write DALYREJS
    CBTRN02C.unload     UnloadClustersTasklet   tables -> TRANFILE, ACCTFILE, TCATBALF images (key order)
  Job CBTRN03C
    CBTRN03C.load       LoadClustersTasklet     CARDXREF, TRANTYPE, TRANCATG images -> schema posting
    CBTRN03C.report     TransactionReportTasklet  sorted TRANFILE + DATEPARM -> TRANREPT (133-byte records)
```

Flyway creates the tables in a `posting` schema on the estate's PostgreSQL 15 (`db` service): V1 for
the clusters, V2 for Spring Batch's JobRepository tables. While the chain is strangled, the KSDS
clusters remain the system of record, so every run rebuilds the schema from the step's DD images and
writes the results back. That adapter (`LoadClustersTasklet` / `UnloadClustersTasklet`) is the part
that goes away once the neighbouring chains read PostgreSQL directly. Record layouts
(`record/Records.java`, `Zoned.java`) are a private copy of the copybooks, and no Java code is
shared with `java/interest`.

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
   is `COB_CURRENT_DATE`, the chain's frozen clock that the JCL runner sets. Without it the
   wall clock is used.
7. **Reject records** are the 350-byte daily record exactly as read, plus the reason
   `PIC 9(4)` and the description `X(76)`.
8. **RC 4 when anything was rejected**, plus the `TRANSACTIONS PROCESSED :` and
   `TRANSACTIONS REJECTED  :` counts as `9(9)` (two spaces in the second label).
9. **Arithmetic truncates** like ADD/COMPUTE without `ON SIZE ERROR` or `ROUNDED`: high-order
   digits beyond the receiving PIC are dropped (`Zoned.fit`).

TRANREPT / CBTRN03C (report):

10. **The SORT window is hardcoded in the JCL**, not taken from DATEPARM. STEP05R keeps
    records with `TRAN-PROC-TS(1:10)` between `2022-01-01` and `2022-07-06`. That step still runs
    on the JCL runner. DATEPARM only drives CBTRN03C's own filter and the header.
11. **Sort order** is by card number only (the JCL SORT, unchanged). CBTRN03C reads the sorted
    file as it is.
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
    then `FILE STATUS IS: NNNN0023`, `ABENDING PROGRAM`, `CEE3ABD: USER ABEND U+0000000999`).
    The step ends with RC 12, `END OF EXECUTION` is not displayed, and report lines written
    before the abend are kept.
20. **CBTRN02C opens TRANFILE OUTPUT**, so the transaction cluster is rebuilt from this run's
    postings only (in every fixture it starts empty).

## Not proven by the fixtures

The seven fixtures pass field for field, but they do not cover the behaviour below. The Java
code follows the COBOL as written; confirm on the mainframe before relying on it.

* **Abends** (quirk 19, and I/O errors): no fixture covers them. The SYSOUT lines and RC 12
  follow the COBOL and the `CEE3ABD` stand-in. If CBTRN02C abends mid-run, Java writes no cluster
  changes back, while legacy keeps the records written before the abend.
* **Overflow** (quirk 9): no fixture comes close to the PIC limits. Left truncation is the
  COBOL standard; GnuCOBOL vs Enterprise COBOL `TRUNC` options were not compared.
* **A zero total or amount** edits to spaces (quirk 18). No fixture prints a zero total.
* **A record outside the DATEPARM range** (quirk 12) only shows up on an empty day in the
  fixtures. The SORT window and DATEPARM agree in every case.
* **TRANFILE OPEN OUTPUT** (quirk 20): GnuCOBOL recreates the indexed file. On z/OS, OPEN OUTPUT
  on a non-empty KSDS without REUSE fails. The fixtures never start with transactions.
* **ACCTDATA filler:** the account record's 178-byte FILLER is rewritten as spaces. It is blank
  in every fixture.
* **Duplicate TRAN-ID in one day:** COBOL abends on the failed WRITE. Java fails the chunk
  on the primary key, so the effect is the same (job fails), but the reject/SYSOUT state at
  the point of failure was not compared.
* **The fixture README for p07** says "report with headers and zero detail lines". The golden
  report is empty (quirk 12). Java matches the golden file.

## Build and test

```sh
make up && make build              # legacy estate (once)
make java-posting-build            # mvn package in maven:3.9-eclipse-temurin-21 (uses ~/.m2 as cache)
make parity-java-posting           # 7 posting fixtures through the Java implementation
make parity                        # legacy, unchanged
```

`mvn test` runs the zoned / edited-number codec unit tests.
