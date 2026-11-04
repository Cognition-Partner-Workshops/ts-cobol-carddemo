---
name: carddemo-parity
description: Prove that a converted CardDemo batch chain (Java/PostgreSQL or any other target) is equivalent to the legacy COBOL/JCL run using the repository's golden fixtures and field-level comparator. Use when migrating a chain, when `make parity` fails, or when asked to explain a parity difference.
---

# CardDemo parity

## Workflow

1. `make up && make build` - start the estate and compile the legacy programs.
2. `make parity CHAIN=<chain>` - confirm the legacy chain is green before touching anything.
3. Build the candidate so it reads the same inputs (`fixtures/<chain>/<case>/input/*.txt`, one
   record per line, copybook layouts in `app/cpy`) and writes the same outputs in the recorded
   format: `datasets/<DSN>.txt`, `sysout/<JOB>.<STEP>.txt`, `rc.json`, `db/<table>.csv`
   (see any `expected/` directory).
4. Compare: `python3 tools/parity/compare.py <chain> <case> --actual <candidate-dir> --label java`
   (inside the container, or on a host with Python 3.10+).  Markdown lands in
   `work/parity/report.md`; exit code 1 on any difference.
5. Repeat for every case under `fixtures/<chain>/`.  A chain is proven when all cases pass.

## Reading a difference

* The report names the dataset, record (with its key field), field, expected (legacy) and actual
  values, already decoded from COMP-3 / zoned / EBCDIC.
* `TRAN-AMT` or `ACCT-CURR-BAL` off by one cent on `i02-tie-half-cent` only: you round half-even;
  COBOL `COMPUTE` without `ROUNDED` truncates.  Use `RoundingMode.DOWN` (or scale then truncate).
* Last account's balance differs in every interest case: you "fixed" CBACT04C's end-of-file
  quirk.  The legacy program does not update the final account; reproduce it and raise it as a
  finding, do not silently correct it.
* `PROC-TS` differs: the candidate must use the frozen clock `2022-07-05 10:00:00.00`.
* `db/*.csv` differs in trailing spaces: ocesql sends padded VARCHARs; match it or document.
* SYSOUT differs: display lines are part of parity because operators read them.

## Never

* Edit `fixtures/*/expected`, `app/**` COBOL or copybooks to make parity pass.
* Re-record golden files (`make record`) without being asked.
* Compare with `diff` on raw bytes and call it done - use the copybook-driven comparator.
