# java/interest: INTCALC (CBACT04C) on Java 21 / Spring Batch / PostgreSQL 15

The `interest` chain's only business step, `INTCALC STEP15 EXEC PGM=CBACT04C`, re-implemented as
a Spring Batch job and strangled in behind the legacy one. The JCL, the datasets and the COBOL are
unchanged. The JCL runner decides per chain which implementation sits behind `EXEC PGM=CBACT04C`,
and legacy stays the default.

## The switch

`tools/runjcl/impl/interest.json` registers the Java implementation for program `CBACT04C` and
holds the chain's default (`"default": "legacy"`). `tools/runjcl/candidate.py` reads every
`impl/<chain>.json`, so each chain is switched on its own:

Selection is scoped to the running chain, including when chains share a program. The
parity/run harness passes that chain to the runner; direct `runjcl.py` invocations must
pass `--chain <name>` to enable candidates (without it they stay legacy). Unrelated
chains' defaults and build requirements do not affect the run.

The `programs` mapping accepts plain program names (the existing interest format),
step names such as `STEP15`, or step/program names such as `STEP15.CBACT04C`.
Resolution prefers `STEP.PROGRAM`, then `STEP`, then `PROGRAM` (case-insensitive).
For `IKJEFT01`, resolution uses the inner `DSN RUN PROGRAM(x)` program and its PARM,
with the enclosing step name; an unmatched program still runs through legacy `DSNRUN`.
`make test-runjcl` tests these rules and also runs as part of `make parity` in CI.

| What | How |
|---|---|
| Run one case on Java | `make run CHAIN=interest IMPL=java [CASE=i02-tie-half-cent]` |
| Parity on Java (report `work/parity/report-java-interest.md`) | `make parity-java-interest [CASE=..]` (also `make parity-java`) |
| Ad hoc, several chains | `CARDDEMO_IMPL=interest=java,posting=legacy make run CHAIN=interest` |
| Cut over | set `"default": "java"` in `tools/runjcl/impl/interest.json` (one reviewed line) |
| Roll back | set it back to `"legacy"`, or `IMPL=legacy` / `CARDDEMO_IMPL=interest=legacy` for a single run |

With no `IMPL` and no `CARDDEMO_IMPL` everything runs exactly as before, and `make parity` stays the
legacy golden-file check. The joblog shows which implementation ran
(`CBACT04C -> interest=java: java -jar ...`). If the jar is missing, the runner stops with a
JCL error that names the build target.

### Step contract

The Java program has the same contract as the COBOL program it replaces:

* datasets arrive as `DD_<ddname>` paths (`TCATBALF`, `XREFFILE`, `ACCTFILE`, `DISCGRP`, `TRANSACT`);
* the `PARM` text (`2022071800`) is the first argument;
* STDOUT is the step's SYSOUT (only the program's DISPLAY lines; logging goes to STDERR / joblog);
* the exit status is the step return code (0, or 12 after an abend, like the `CEE3ABD` stand-in).

KSDS clusters are stored as Berkeley DB files that only GnuCOBOL can read. The runner therefore
unloads them to flat images for the step. After the step it reloads any cluster the program changed
(`ACCTFILE`), alternate indexes included. `SYSTRAN(+1)` is a plain new generation and is written
directly.

### Inside the job

```
InterestRunner     DISPLAY 'START...'; load DD images -> schema interest; run job; write ACCTFILE/TRANSACT back
  Job INTCALC
    Step STEP15    chunk-oriented, not restartable (carries CBACT04C working storage)
      reader       JdbcCursorItemReader  tran_cat_balance in KSDS key order
      processor    InterestCalculator    account break / 1050 / 1100 / 1110 / 1200 / 1300 / 1300-B
      writer       PostingWriter         UPDATE account, INSERT systran
```

`schema-interest.sql` maps the four KSDS clusters and the SYSTRAN generation to tables in the
`interest` schema of the estate's PostgreSQL 15 (`db` service). Spring Batch's metadata tables live
in the same schema. During coexistence the VSAM files remain the system of record, so every run
rebuilds the schema from the step's DD images (`VsamDatasets`) and writes the results back.
Raw TCATBALF records are retained for DISPLAY, and raw account images are retained for
writeback. Only accounts actually rewritten by 1050 have their balance and cycle amount
fields re-encoded; untouched fields keep their original zoned-sign bytes.
Once the neighbouring chains read PostgreSQL directly, that adapter and the reload go away.

Record layouts (`record/`) are local copies of `CVACT01Y`, `CVACT03Y`, `CVTRA01Y`, `CVTRA02Y` and
`CVTRA05Y` with `cobc -fsign=EBCDIC` overpunch signs. They are deliberately not shared with
other migrated chains.

## Legacy behaviour reproduced on purpose

| # | Legacy behaviour (CBACT04C) | Where in Java | Fixture |
|---|---|---|---|
| 1 | `COMPUTE WS-MONTHLY-INT = (TRAN-CAT-BAL * DIS-INT-RATE) / 1200` has no `ROUNDED`, so interest is **truncated** toward zero (12.355 -> 12.35, -x.xx5 -> toward zero). No half-even rounding. | `InterestCalculator.monthlyInterest` (`RoundingMode.DOWN`) | i02, i06, i07 |
| 2 | **The last account on TCATBALF is never rewritten.** The rewrite (1050) runs only on an account break. The end-of-file branch that would rewrite the final account cannot be reached because the loop exits first. The last account's interest transactions are still written. | `InterestCalculator.process` (rewrite only on account break) | i01, i02, i05, i07 |
| 3 | 1050 also **zeroes `ACCT-CURR-CYC-CREDIT` and `ACCT-CURR-CYC-DEBIT`** on every rewritten account | `InterestCalculator.withInterest` | i01, i02, i07 |
| 4 | Categories with a **zero rate** produce no transaction (but their TCATBALF record is still DISPLAYed) | `process` (`rate.signum() == 0`) | i03 |
| 5 | Missing disclosure group: DISPLAYs `DISCLOSURE GROUP RECORD MISSING` / `TRY WITH DEFAULT GROUP CODE` and **falls back to group `DEFAULT`** with the same type/category | `interestRate` | i04 |
| 6 | SYSOUT is `START OF EXECUTION...`, every TCATBALF record verbatim (50 bytes, trailing spaces kept), `END OF EXECUTION...` | `InterestRunner`, `process` | all |
| 7 | `TRAN-ID` = PARM date (10) + `WS-TRANID-SUFFIX` `9(06)`, one counter for the whole run (wraps after 999999) | `interestTransaction` | i05 |
| 8 | Transaction constants: type `01`, category `0005`, source `System`, description `Int. for a/c ` + 11-digit account id, merchant id 0 and blank merchant fields, card = the account's cross-reference card | `interestTransaction` | all |
| 9 | `TRAN-ORIG-TS` = `TRAN-PROC-TS` = `FUNCTION CURRENT-DATE` as `yyyy-mm-dd-hh.mm.ss.hh0000`. GnuCOBOL's `COB_CURRENT_DATE` (frozen per chain to `2022-07-05 10:00:00.00`) is honoured | `ClockConfig`, `db2Timestamp` | all |
| 10 | Negative balances give negative interest, written with an overpunch sign (`}` / `J`-`R`) | `record/Fixed` | i06 |
| 11 | Stores into `S9(n)V99` fields drop excess high-order digits (no `ON SIZE ERROR`) | `Fixed.fit` | (unit test) |
| 12 | Unrecoverable lookups (account, cross reference, `DEFAULT` group missing) DISPLAY the error, `FILE STATUS IS: NNNN0023`, `ABENDING PROGRAM`, `CEE3ABD: USER ABEND U+0000000999` and end the step with RC 12. Writes made before the abend are kept (including the previous account's rewrite), `END OF EXECUTION` is not displayed, and later records are not processed. Checked against legacy with throwaway cases (missing account, missing `DEFAULT` group): datasets and return codes are identical. SYSOUT differs only by the `libcob: warning: implicit CLOSE of ...` lines that the GnuCOBOL runtime prints to STDERR. Those are an artifact of the off-host runtime, not of CBACT04C, so they are not reproduced. | `Abend`, `PostingWriter` (commits, then stops the step) | (no fixture) |

## Decisions

These are legacy behaviours with no fixture coverage, settled with the requester ("go with your defaults"). None of them changes behaviour.

| # | Question | Decision |
|---|---|---|
| D1 | GnuCOBOL prints `libcob: warning: implicit CLOSE of ...` on STDERR when CBACT04C abends. Should the Java step output them? | **No.** They come from the off-host GnuCOBOL runtime, not from CBACT04C (z/OS prints nothing like them). On abend paths, datasets and return codes match legacy, and SYSOUT differs only by these lines. |
| D2 | `WS-TRANID-SUFFIX` is `PIC 9(06)`. What happens past 999999 transactions in one run? | **Keep the six-digit wrap** (999999 -> 000000), as a COBOL `ADD 1` without `ON SIZE ERROR` does. |
| D3 | Several card cross-references for the same account (`XREFFIL1` alternate key on account id). Which card goes on the interest transaction? | **The first in file order**, i.e. the first duplicate the alternate-index read returns (`card_xref.load_seq`). |

## Build and test

```sh
make up && make build             # legacy estate (once)
make java-interest-build          # mvn package in maven:3.9-eclipse-temurin-21 (uses ~/.m2 as cache)
make parity-java-interest         # 7 interest fixtures through the Java implementation
make parity                       # legacy, unchanged
```

`mvn test` runs the codec / arithmetic unit tests. CI (`.github/workflows/parity.yml`) runs
`make parity-java-interest` after the legacy parity.
